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
package io.infinitic.workers.registry

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.registry.ExecutorRegistryInterface
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.WorkflowContext
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.emptyWorkflowContext
import io.infinitic.exceptions.workflows.UnknownWorkflowVersionException
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workers.config.ServiceConfig
import io.infinitic.workers.config.ServiceExecutorConfig
import io.infinitic.workers.config.WorkflowConfig
import io.infinitic.workers.config.WorkflowExecutorConfig
import io.infinitic.workers.config.WorkflowFactory
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode

class ExecutorRegistry(
  private val services: List<ServiceConfig>,
  private val workflows: List<WorkflowConfig>,
) : ExecutorRegistryInterface {

  override fun getServiceExecutorInstance(serviceName: ServiceName): Any =
      (getServiceExecutor(serviceName).factory ?: thisShouldNotHappen()).invoke()

  override fun getServiceExecutorWithTimeout(serviceName: ServiceName): WithTimeout? =
      getServiceExecutor(serviceName).withTimeout

  override fun getServiceExecutorWithRetry(serviceName: ServiceName): WithRetry? =
      getServiceExecutor(serviceName).withRetry

  override fun getWorkflowExecutorInstance(workflowTaskParameters: WorkflowTaskParameters): Workflow =
      with(workflowTaskParameters) {
        // set WorkflowContext before Workflow instance creation
        Workflow.setContext(
            WorkflowContext(
                workflowName = workflowName.toString(),
                workflowId = workflowId.toString(),
                methodName = workflowMethod.methodName.toString(),
                methodId = workflowMethod.workflowMethodId.toString(),
                meta = workflowMeta.map,
                tags = workflowTags.map { it.tag }.toSet(),
            ),
        )

        getInstanceByVersion(workflowName, workflowVersion)
      }

  override fun getWorkflowExecutorWithTimeout(workflowName: WorkflowName): WithTimeout? =
      getWorkflowExecutor(workflowName).withTimeout

  override fun getWorkflowExecutorWithRetry(workflowName: WorkflowName): WithRetry? =
      getWorkflowExecutor(workflowName).withRetry

  override fun getWorkflowExecutorCheckMode(workflowName: WorkflowName): WorkflowCheckMode? =
      getWorkflowExecutor(workflowName).checkMode

  private fun getService(serviceName: ServiceName): ServiceConfig? =
      services.firstOrNull { it.name == serviceName.name }

  private fun getServiceExecutor(serviceName: ServiceName): ServiceExecutorConfig =
      getService(serviceName)?.executor ?: thisShouldNotHappen()

  private fun getWorkflow(workflowName: WorkflowName): WorkflowConfig? =
      workflows.firstOrNull { it.name == workflowName.name }

  private fun getWorkflowExecutor(workflowName: WorkflowName): WorkflowExecutorConfig =
      getWorkflow(workflowName)?.executor ?: thisShouldNotHappen()

  private fun getInstanceByVersion(
    workflowName: WorkflowName,
    workflowVersion: WorkflowVersion?
  ): Workflow = getFactory(
      workflowName,
      workflowVersion ?: getLastVersion(workflowName),
  ).invoke()

  private val factoryByVersionByWorkflowName = workflows
      .associateBy { WorkflowName(it.name) }
      .mapValues { it.value.executor?.instanceByVersion }

  private fun getFactory(
    workflowName: WorkflowName,
    workflowVersion: WorkflowVersion
  ): WorkflowFactory =
      (factoryByVersionByWorkflowName[workflowName] ?: thisShouldNotHappen())[workflowVersion]
        ?: throw UnknownWorkflowVersionException(workflowName, workflowVersion)

  private fun getLastVersion(workflowName: WorkflowName) =
      factoryByVersionByWorkflowName[workflowName]?.keys?.maxOrNull()
        ?: thisShouldNotHappen()

  private val WorkflowExecutorConfig.instanceByVersion: Map<WorkflowVersion, WorkflowFactory>
    get() {
      // this is needed in case the workflow properties use the workflow context
      Workflow.setContext(emptyWorkflowContext)

      // list of versions
      val versions = factories.mapIndexed { index, factory ->
        try {
          factory()
        } catch (e: Exception) {
          throw IllegalArgumentException("Error when running factory #$index", e)
        }
      }.map { WorkflowVersion.from(it::class.java) }

      return versions.zip(factories).toMap()
    }
}
