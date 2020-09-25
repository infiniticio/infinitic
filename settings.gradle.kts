rootProject.name = "io.infinitic"

include("infinitic-rest-api")
include("infinitic-avro")
include("infinitic-storage-api")
include("infinitic-storage-pulsar")
include("infinitic-messaging-api")
include("infinitic-messaging-pulsar")
include("infinitic-common")
include("infinitic-worker")
include("infinitic-worker-pulsar")
include("infinitic-client")
include("infinitic-taskManager-engine")
include("infinitic-taskManager-engine-pulsar")
include("infinitic-taskManager-tests")
include("infinitic-workflowManager-worker")
include("infinitic-workflowManager-engine")
include("infinitic-workflowManager-engine-pulsar")
include("infinitic-workflowManager-tests")

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven(url = "https://dl.bintray.com/gradle/gradle-plugins")
    }
}
