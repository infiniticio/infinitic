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
import io.infinitic.workers.samples.TaskA
import io.infinitic.workers.samples.WorkflowA
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class WorkerConfigTests : StringSpec({
    "task instance should not be reused" {
        val config = WorkerConfig.fromResource("/config/tasks/instance.yml")

        val task = config.tasks.first { it.name == TaskA::class.java.name }
        task.instance shouldNotBe task.instance
    }

    "workflow instance should not be reused" {
        val config = WorkerConfig.fromResource("/config/workflows/instance.yml")

        val workflow = config.workflows.first { it.name == WorkflowA::class.java.name }
        workflow.instance shouldNotBe workflow.instance
    }

    "task with InvocationTargetException should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/tasks/invocationTargetException.yml")
        }
        e.message!! shouldContain "Error when trying to instantiate class \"io.infinitic.workers.samples.TaskWithInvocationTargetException\""
    }

    "workflow with InvocationTargetException should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/invocationTargetException.yml")
        }
        e.message!! shouldContain "Error when trying to instantiate class \"io.infinitic.workers.samples.WorkflowWithInvocationTargetException\""
    }

    "task with ExceptionInInitializerError should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/tasks/exceptionInInitializerError.yml")
        }
        e.message!! shouldContain "Underlying error was java.lang.ExceptionInInitializerError"
    }

    "workflow with ExceptionInInitializerError should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/exceptionInInitializerError.yml")
        }
        e.message!! shouldContain "Underlying error was java.lang.ExceptionInInitializerError"
    }

    "task Unknown" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/tasks/unknown.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.workers.samples.UnknownTask\" is unknown"
    }

    "workflow Unknown" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/unknown.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.workers.samples.UnknownWorkflow\" unknown"
    }

    "not a task" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/tasks/notATask.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.workers.samples.NotATask\" must extend io.infinitic.tasks.Task"
    }

    "not a workflow" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/notAWorkflow.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.workers.samples.NotAWorkflow\" must extend io.infinitic.workflows.Workflow"
    }

    "default retry policy should be RetryExponentialBackoff" {
        val config = WorkerConfig.fromResource("/config/tasks/instance.yml")

        // config.retryPolicy shouldBe RetryExponentialBackoff()
    }
})
