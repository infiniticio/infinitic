// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.workflows.avro

import io.infinitic.common.workflows.data.commands.Command
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.states.WorkflowState
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.StepStatus
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.avro.workflowManager.data.commands.AvroCommand
import io.infinitic.avro.workflowManager.data.commands.AvroCommandStatus
import io.infinitic.avro.workflowManager.data.commands.AvroPastCommand
import io.infinitic.avro.workflowManager.data.methodRuns.AvroMethodRun
import io.infinitic.avro.workflowManager.data.steps.AvroPastStep
import io.infinitic.avro.workflowManager.data.steps.AvroStep
import io.infinitic.avro.workflowManager.data.steps.AvroStepStatus
import io.infinitic.avro.workflowManager.data.workflowTasks.AvroWorkflowTaskInput
import io.infinitic.avro.workflowManager.data.workflowTasks.AvroWorkflowTaskOutput
import io.infinitic.common.fixtures.TestFactory
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class AvroReversibilityOfDataTests : ShouldSpec({
    context("Commands") {
        // CommandStatus avro-reversibility
        CommandStatus::class.sealedSubclasses.forEach {
            should("${it.simpleName} be avro-reversible") {
                val obj1 = TestFactory.random(it)
                val avro1 = AvroConverter.convertJson<AvroCommandStatus>(obj1)
                val obj2 = AvroConverter.convertJson<CommandStatus>(avro1)
                val avro2 = AvroConverter.convertJson<AvroCommandStatus>(obj2)

                obj1 shouldBe obj2
                avro1 shouldBe avro2
            }
        }
        // Command avro-reversibility
        Command::class.sealedSubclasses.forEach {
            should("${it.simpleName} be avro-reversible") {
                val obj1 = TestFactory.random(it)
                val avro1 = AvroConverter.convertJson<AvroCommand>(obj1)
                val obj2 = AvroConverter.convertJson<Command>(avro1)
                val avro2 = AvroConverter.convertJson<AvroCommand>(obj2)

                obj1 shouldBe obj2
                avro1 shouldBe avro2
            }
        }
        // PastCommand avro-reversibility
        should("PastCommand be avro-reversible") {
            val obj1 = TestFactory.random<PastCommand>()
            val avro1 = AvroConverter.convertJson<AvroPastCommand>(obj1)
            val obj2 = AvroConverter.convertJson<PastCommand>(avro1)
            val avro2 = AvroConverter.convertJson<AvroPastCommand>(obj2)

            obj1 shouldBe obj2
            avro1 shouldBe avro2
        }
    }

    context("Steps") {
        // StepStatus avro-reversibility
        StepStatus::class.sealedSubclasses.forEach {
            should("${it.simpleName} be avro-reversible") {
                val obj1 = TestFactory.random(it)
                val avro1 = AvroConverter.convertJson<AvroStepStatus>(obj1)
                val obj2 = AvroConverter.convertJson<StepStatus>(avro1)
                val avro2 = AvroConverter.convertJson<AvroStepStatus>(obj2)

                obj1 shouldBe obj2
                avro1 shouldBe avro2
            }
        }
        // Step avro-reversibility
        TestFactory.steps().forEach {
            val obj1 = it.value
            should("${it.key} be avro-reversible") {
                val avro1 = AvroConverter.convertJson<AvroStep>(obj1)
                val obj2 = AvroConverter.convertJson<Step>(avro1)
                val avro2 = AvroConverter.convertJson<AvroStep>(obj2)

                obj1 shouldBe obj2
                avro1 shouldBe avro2
            }
        }
        // PastStep avro-reversibility
        should("PastStep be avro-reversible") {
            val obj1 = TestFactory.random<PastStep>()
            val avro1 = AvroConverter.convertJson<AvroPastStep>(obj1)
            val obj2 = AvroConverter.convertJson<PastStep>(avro1)
            val avro2 = AvroConverter.convertJson<AvroPastStep>(obj2)

            obj1 shouldBe obj2
            avro1 shouldBe avro2
        }
    }

    context("MethodRuns") {
        should("MethodRun be avro-reversible") {
            val obj1 = TestFactory.random<MethodRun>()
            val avro1 = AvroConverter.convertJson<AvroMethodRun>(obj1)
            val obj2 = AvroConverter.convertJson<MethodRun>(avro1)
            val avro2 = AvroConverter.convertJson<AvroMethodRun>(obj2)

            obj1 shouldBe obj2
            avro1 shouldBe avro2
        }
    }

    context("WorkflowTasks") {
        should("WorkflowTaskInput be avro-reversible") {
            val obj1 = TestFactory.random<WorkflowTaskInput>()
            val avro1 = AvroConverter.convertJson<AvroWorkflowTaskInput>(obj1)
            val obj2 = AvroConverter.convertJson<WorkflowTaskInput>(avro1)
            val avro2 = AvroConverter.convertJson<AvroWorkflowTaskInput>(obj2)

            obj1 shouldBe obj2
            avro1 shouldBe avro2
        }

        should("WorkflowTaskOutput be avro-reversible") {
            val obj1 = TestFactory.random<WorkflowTaskOutput>()
            val avro1 = AvroConverter.convertJson<AvroWorkflowTaskOutput>(obj1)
            val obj2 = AvroConverter.convertJson<WorkflowTaskOutput>(avro1)
            val avro2 = AvroConverter.convertJson<AvroWorkflowTaskOutput>(obj2)

            obj1 shouldBe obj2
            avro1 shouldBe avro2
        }
    }

    context("States") {
        should("WorkflowState be avro-reversible") {
            val obj1 = TestFactory.random<WorkflowState>()
            val avro1 = AvroConverter.toStorage(obj1)
            val obj2 = AvroConverter.fromStorage(avro1)
            val avro2 = AvroConverter.toStorage(obj2)

            obj1 shouldBe obj2
            avro1 shouldBe avro2
        }
    }
})
