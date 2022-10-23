/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.workers.config

import com.sksamuel.hoplite.ConfigException
import io.infinitic.workers.register.WorkerRegister.Companion.DEFAULT_CONCURRENCY
import io.infinitic.workers.samples.ServiceA
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class WorkerConfigTests : StringSpec({
    "task instance should not be reused" {
        val config = WorkerConfig.fromResource("/config/services/instance.yml")

        val task = config.services.first { it.name == ServiceA::class.java.name }
        task.getInstance() shouldNotBe task.getInstance()
    }

    "task with InvocationTargetException should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/invocationTargetException.yml")
        }
        println(e)
        e.message!! shouldContain "Error during instantiation of class \"io.infinitic.workers.samples.ServiceWithInvocationTargetException\""
    }

    "workflow with InvocationTargetException should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/invocationTargetException.yml")
        }
        e.message!! shouldContain "Error when trying to instantiate class \"io.infinitic.workers.samples.WorkflowWithInvocationTargetException\""
    }

    "task with ExceptionInInitializerError should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/exceptionInInitializerError.yml")
        }
        e.message!! shouldContain "Underlying error was java.lang.ExceptionInInitializerError"
    }

    "workflow with ExceptionInInitializerError should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/exceptionInInitializerError.yml")
        }
        e.message!! shouldContain "Underlying error was java.lang.ExceptionInInitializerError"
    }

    "service Unknown" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/unknown.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.workers.samples.UnknownService\" is unknown"
    }

    "workflow Unknown" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/unknown.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.workers.samples.UnknownWorkflow\" unknown"
    }

    "not a workflow" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/notAWorkflow.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.workers.samples.NotAWorkflow\" must extend io.infinitic.workflows.Workflow"
    }

    "checking default service config" {
        val config = WorkerConfig.fromResource("/config/services/instance.yml")

        config.service shouldBe ServiceDefault()
        config.services.size shouldBe 1
        config.services[0].retry shouldBe null
        config.services[0].timeoutInSeconds shouldBe null
        config.services[0].concurrency shouldBe DEFAULT_CONCURRENCY
    }

    "checking default workflow config" {
        val config = WorkerConfig.fromResource("/config/workflows/instance.yml")

        config.workflow shouldBe WorkflowDefault()
        config.workflows.size shouldBe 1
        config.workflows[0].retry shouldBe null
        config.workflows[0].timeoutInSeconds shouldBe null
        config.workflows[0].checkMode shouldBe null
        config.workflows[0].concurrency shouldBe DEFAULT_CONCURRENCY
    }
})
