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
include("infinitic-engine")
include("infinitic-taskManager-engine-pulsar")
include("infinitic-tests")
include("infinitic-workflowManager-engine")
include("infinitic-workflowManager-engine-pulsar")

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven(url = "https://dl.bintray.com/gradle/gradle-plugins")
    }
}
