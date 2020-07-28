rootProject.name = "com.zenaton"

include("zenaton-common")
include("zenaton-jobManager")
include("zenaton-jobManager-common")
include("zenaton-jobManager-worker")
include("zenaton-jobManager-pulsar")
include("zenaton-jobManager-client")
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
