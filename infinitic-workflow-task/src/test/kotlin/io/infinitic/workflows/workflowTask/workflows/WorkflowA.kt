/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.workflows.workflowTask.workflows

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.annotations.Ignore
import io.infinitic.common.utils.Tsid
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.workflowTask.tasks.TaskA
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

sealed class Obj

@Serializable
@Suppress("unused")
data class Obj1(val foo: String, val bar: Int) : Obj()

@Serializable
@Suppress("unused")
data class Obj2(val foo: String, val bar: Int) : Obj()

interface WorkflowA {
  val channelObj: SendChannel<Obj>
}

@Suppress("unused")
class WorkflowAImpl : Workflow(), WorkflowA {

  // a channel
  override val channelObj = channel<Obj>()

  // a new task
  private val newTaskA = newService(TaskA::class.java)

  // an existing task
  //    private val getTaskA = getTaskById(TaskA::class.java, Tsid.random())

  // a new workflow
  private val newWorkflowA = newWorkflow(WorkflowA::class.java)

  // an existing workflow
  private val getWorkflowA = getWorkflowById(WorkflowA::class.java, Tsid.random())

  // a logger
  private var logger1 = LoggerFactory.getLogger(WorkflowAImpl::class.qualifiedName)

  // another logger
  private val logger2 = KotlinLogging.logger {}

  // this property should be ignored
  @Ignore
  private var key1 = "42"

  // this property should be kept
  private var key2 = 42
}
