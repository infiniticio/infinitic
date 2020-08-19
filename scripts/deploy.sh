#!/bin/bash

# Variables definitions

DEFAULT_PULSAR_ADMIN_URL=http://localhost:8080
DEFAULT_PULSAR_TENANT=public
DEFAULT_PULSAR_NAMESPACE=default

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

JOB_MANAGER_JAR_PATH=$SCRIPT_DIR/../zenaton-taskManager-engine-pulsar/build/libs/zenaton-taskManager-engine-pulsar-1.0.0-SNAPSHOT-all.jar
SCHEMA_FILES_PATH=./zenaton-taskManager-engine-pulsar/build/schemas

tmp_dir=$(mktemp -d -t infinitic-deploy-XXXXXXXXXX)

# Error handling

function on_exit() {
    # Cleanup the temporary directory in case of error
    rm -rf $tmp_dir
}
trap on_exit EXIT

# Functions definitions

function print_error() {
    printf "\033[31m$1\033[0m\n" >&2
}

function print_question() {
    printf "\033[1;33m$1\033[0m\n"
}

function print_success() {
    printf "\033[32m$1\033[0m\n"
}

# Start of script

# Preflight checks

if [ ! $(command -v pulsarctl) ]; then
    print_error "Command 'pulsarctl' is not available."
    print_error "$(cat <<-END

	This script makes use of pulsarctl to configure and deploy elements to the Pulsar cluster.
	Please install pulsarctl by following the installation instructions: https://github.com/streamnative/pulsarctl

	After installation is completed, try running this script again.
END
)"
    exit 1
fi

if [ ! $(command -v java) ]; then
    print_error "Command 'java' is not available."
    print_error "$(cat <<-END

	This script makes use of java to configure and deploy elements to the Pulsar cluster.
	Please install java by following the installation instructions: https://java.com/en/download/help/download_options.xml

	After installation is completed, try running this script again.
END
)"
    exit 1
fi

# Prompt for required values if not provided using environment variables

if [ ! $PULSAR_ADMIN_URL ]; then
    print_question "$(cat <<-END
	Please provide the Pulsar Admin URL to use (default is $DEFAULT_PULSAR_ADMIN_URL).
END
)"
    read -p '> ' PULSAR_ADMIN_URL
    if [ -z $PULSAR_ADMIN_URL ]; then
        PULSAR_ADMIN_URL=$DEFAULT_PULSAR_ADMIN_URL
    fi
fi

if [ ! $PULSAR_TENANT ]; then
    print_question "$(cat <<-END
	Please provide the Pulsar tenant to use (default is $DEFAULT_PULSAR_TENANT).
END
)"
    read -p '> ' PULSAR_TENANT
    if [ -z $PULSAR_TENANT ]; then
        PULSAR_TENANT=$DEFAULT_PULSAR_TENANT
    fi
fi

if [ ! $PULSAR_NAMESPACE ]; then
    print_question "$(cat <<-END
	Please provide the Pulsar namespace to use (default is $DEFAULT_PULSAR_NAMESPACE).
END
)"
    read -p '> ' PULSAR_NAMESPACE
    if [ -z $PULSAR_NAMESPACE ]; then
        PULSAR_NAMESPACE=$DEFAULT_PULSAR_NAMESPACE
    fi
fi

printf "Configuring retention limit ...\n"

HTTP_STATUS=$(curl -sS -w "%{http_code}" -X POST $PULSAR_ADMIN_URL/admin/v2/namespaces/$PULSAR_TENANT/$PULSAR_NAMESPACE/retention -H 'Content-Type: application/json' -d '{
  "retentionTimeInMinutes": -1,
  "retentionSizeInMB": 1024
}')

if [ "$HTTP_STATUS" != "204" ]; then
    print_error "Cannot set Pulsar retention policy for namespace $PULSAR_TENANT/$PULSAR_NAMESPACE."
    print_error "$(cat <<-END

	Some of the possible causes for this issue are:
    - The Pulsar Admin URL provided ($PULSAR_ADMIN_URL) is wrong. Make sure you provided a correct Pulsar Admin URL.
    - The Pulsar tenant provided ($PULSAR_TENANT) is wrong. Make sure you provided a correct tenant and that it already exists on the cluster.
    - The Pulsar namespace provided ($PULSAR_NAMESPACE) is wrong. Make sure you provided a correct namespace and that it already exists on the cluster.
END
)"
    exit 1
else
    print_success "Successfully set Pulsar retention policy for namespace $PULSAR_TENANT/$PULSAR_NAMESPACE"
fi

printf "Creating schema files ...\n"
cd $tmp_dir && java -cp $JOB_MANAGER_JAR_PATH com.zenaton.taskManager.pulsar.utils.GenerateSchemaFilesKt
if [ $? -ne 0 ]; then
    print_error "Cannot generate schema files"
    print_error "$(cat <<-END

	Please check that the path to the Job Manager JAR file ($JOB_MANAGER_JAR_PATH) is correct.
END
)"
    exit 1
fi

printf "Uploading schemas to topics ...\n"
pulsarctl --admin-service-url=$PULSAR_ADMIN_URL schemas upload $PULSAR_TENANT/$PULSAR_NAMESPACE/tasks-engine --filename build/schemas/AvroEnvelopeForTaskEngine.schema && \
pulsarctl --admin-service-url=$PULSAR_ADMIN_URL schemas upload $PULSAR_TENANT/$PULSAR_NAMESPACE/tasks-monitoring-per-name --filename build/schemas/AvroEnvelopeForMonitoringPerName.schema && \
pulsarctl --admin-service-url=$PULSAR_ADMIN_URL schemas upload $PULSAR_TENANT/$PULSAR_NAMESPACE/tasks-monitoring-global --filename build/schemas/AvroEnvelopeForMonitoringGlobal.schema
if [ $? -ne 0 ]; then
    print_error "Cannot upload schema files to topic"
    exit 1
fi
print_success "Successfully uploaded schemas to topics."

printf "Deploying pulsar functions to the cluster ...\n"
pulsarctl --admin-service-url=$PULSAR_ADMIN_URL functions create --fqfn $PULSAR_TENANT/$PULSAR_NAMESPACE/infinitic-tasks-engine --inputs $PULSAR_TENANT/$PULSAR_NAMESPACE/tasks-engine --jar $JOB_MANAGER_JAR_PATH --classname com.zenaton.taskManager.pulsar.functions.TaskEnginePulsarFunction --user-config '{"topicPrefix":"tasks"}' && \
pulsarctl --admin-service-url=$PULSAR_ADMIN_URL functions create --fqfn $PULSAR_TENANT/$PULSAR_NAMESPACE/infinitic-tasks-monitoring-global --inputs $PULSAR_TENANT/$PULSAR_NAMESPACE/tasks-monitoring-global --jar $JOB_MANAGER_JAR_PATH --classname com.zenaton.taskManager.pulsar.functions.MonitoringGlobalPulsarFunction --user-config '{"topicPrefix":"tasks"}' && \
pulsarctl --admin-service-url=$PULSAR_ADMIN_URL functions create --fqfn $PULSAR_TENANT/$PULSAR_NAMESPACE/infinitic-tasks-monitoring-per-name --inputs $PULSAR_TENANT/$PULSAR_NAMESPACE/tasks-monitoring-per-name --jar $JOB_MANAGER_JAR_PATH --classname com.zenaton.taskManager.pulsar.functions.MonitoringPerNamePulsarFunction --user-config '{"topicPrefix":"tasks"}'
if [ $? -ne 0 ]; then
    print_error "Cannot deploy pulsar functions to the cluster."
    exit 1
fi
print_success "Successfully deployed pulsar functions to the cluster."
