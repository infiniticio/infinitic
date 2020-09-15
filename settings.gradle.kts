rootProject.name = "io.infinitic"

include("infinitic-storage-api")
include("infinitic-storage-pulsar")
include("infinitic-avro")
include("infinitic-taskManager-common")
include("infinitic-taskManager-worker")
include("infinitic-taskManager-worker-pulsar")
include("infinitic-taskManager-client")
include("infinitic-taskManager-engine")
include("infinitic-taskManager-engine-pulsar")
include("infinitic-taskManager-tests")
include("infinitic-taskManager-dispatcher-pulsar")
include("infinitic-rest-api")
include("infinitic-workflowManager-common")
include("infinitic-workflowManager-worker")
include("infinitic-workflowManager-client")
include("infinitic-workflowManager-engine")
include("infinitic-workflowManager-engine-pulsar")
include("infinitic-workflowManager-tests")
include("infinitic-workflowManager-dispatcher-pulsar")

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven(url = "https://dl.bintray.com/gradle/gradle-plugins")
    }
}
