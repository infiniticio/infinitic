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

package io.infinitic.config

import com.sksamuel.hoplite.ConfigException
import io.infinitic.config.tasks.TaskA
import io.infinitic.config.workflows.WorkflowA
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class WorkerConfigTests : StringSpec({
    "task instance should not be reused" {
        val config = WorkerConfig.fromResource("/tasks/instance.yml")

        val task = config.tasks.first { it.name == TaskA::class.java.name }
        task.instance shouldNotBe task.instance
    }

    "workflow instance should not be reused" {
        val config = WorkerConfig.fromResource("/workflows/instance.yml")

        val workflow = config.workflows.first { it.name == WorkflowA::class.java.name }
        workflow.instance shouldNotBe workflow.instance
    }

    "task with InvocationTargetException should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/tasks/invocationTargetException.yml")
        }
        e.message!! shouldContain "Underlying error was java.lang.Exception: InvocationTargetException"
    }

    "workflow with InvocationTargetException should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/workflows/invocationTargetException.yml")
        }
        e.message!! shouldContain "Underlying error was java.lang.Exception: InvocationTargetException"
    }

    "task with ExceptionInInitializerError should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/tasks/exceptionInInitializerError.yml")
        }
        e.message!! shouldContain "Underlying error was java.lang.Exception: ExceptionInInitializerError"
    }

    "workflow with ExceptionInInitializerError should throw cause" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/workflows/exceptionInInitializerError.yml")
        }
        e.message!! shouldContain "Underlying error was java.lang.Exception: ExceptionInInitializerError"
    }

    "task Unknown" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/tasks/unknown.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.config.tasks.UnknownTask\" is unknown"
    }

    "workflow Unknown" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/workflows/unknown.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.config.workflows.UnknownWorkflow\" is unknown"
    }

    "not a task" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/tasks/notATask.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.config.tasks.NotATask\" must extend io.infinitic.tasks.Task"
    }

    "not a workflow" {
        val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/workflows/notAWorkflow.yml")
        }
        e.message!! shouldContain "class \"io.infinitic.config.workflows.NotAWorkflow\" must extend io.infinitic.workflows.Workflow"
    }
})
