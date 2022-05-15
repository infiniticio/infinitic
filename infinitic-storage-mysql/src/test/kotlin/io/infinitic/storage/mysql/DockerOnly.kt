package io.infinitic.storage.mysql

import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.spec.Spec
import org.testcontainers.DockerClientFactory
import kotlin.reflect.KClass

internal class DockerOnly : EnabledCondition {
    override fun enabled(kclass: KClass<out Spec>) = shouldRun

    companion object {
        val shouldRun by lazy {
            // if Docker is not available, skip the tests
            // On GitHub, we must run all tests
            (System.getenv("GITHUB_RUN_NUMBER") != null) || DockerClientFactory.instance().isDockerAvailable
        }
    }
}
