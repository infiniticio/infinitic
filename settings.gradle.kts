rootProject.name = "com.zenaton"

include("zenaton-commons")
include("zenaton-jobManager")
include("zenaton-jobManager-pulsar")
include("zenaton-workflowManager")
include("zenaton-workflowManager-pulsar")

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven(url = "https://dl.bintray.com/gradle/gradle-plugins")
    }
}
