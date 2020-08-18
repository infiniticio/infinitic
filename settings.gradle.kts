rootProject.name = "com.zenaton"

include("zenaton-avro")
include("zenaton-jobManager-common")
include("zenaton-jobManager-worker")
include("zenaton-jobManager-client")
include("zenaton-jobManager-engine")
include("zenaton-jobManager-engine-pulsar")
include("zenaton-jobManager-tests")
include("zenaton-rest-api")
include("zenaton-workflowManager")
include("zenaton-workflowManager-pulsar")

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven(url = "https://dl.bintray.com/gradle/gradle-plugins")
    }
}
