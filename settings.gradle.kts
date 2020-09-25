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
include("infinitic-engine-pulsar")
include("infinitic-tests")

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven(url = "https://dl.bintray.com/gradle/gradle-plugins")
    }
}
