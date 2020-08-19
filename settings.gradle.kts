rootProject.name = "com.zenaton"

include("zenaton-avro")
include("zenaton-taskManager-common")
include("zenaton-taskManager-worker")
include("zenaton-taskManager-client")
include("zenaton-taskManager-engine")
include("zenaton-taskManager-engine-pulsar")
include("zenaton-taskManager-tests")
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
