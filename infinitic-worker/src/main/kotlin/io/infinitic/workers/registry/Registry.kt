package io.infinitic.workers.registry

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.registry.RegistryInterface
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

private typealias ClassFactoryInstance = Triple<Class<out Workflow>, () -> Workflow, Workflow>

@Suppress("unused")
class Registry(private val config: RegistryConfigInterface) : RegistryInterface,
  ConfigGettersInterface {

  val services = config.services

  val workflows = config.workflows

  val eventListener = config.eventListener

  override fun getServiceExecutorInstance(serviceName: ServiceName): Any =
      getServiceExecutor(serviceName).factory.invoke()

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
      config.services.firstOrNull { it.name == serviceName.name }

  private fun getServiceExecutor(serviceName: ServiceName): ServiceExecutorConfig =
      getService(serviceName)?.executor ?: thisShouldNotHappen()

  private fun getWorkflow(workflowName: WorkflowName): WorkflowConfig? =
      config.workflows.firstOrNull { it.name == workflowName.name }

  private fun getWorkflowExecutor(workflowName: WorkflowName): WorkflowExecutorConfig =
      getWorkflow(workflowName)?.executor ?: thisShouldNotHappen()

  private fun getInstanceByVersion(
    workflowName: WorkflowName,
    workflowVersion: WorkflowVersion?
  ): Workflow = getFactory(
      workflowName,
      workflowVersion ?: getLastVersion(workflowName),
  ).invoke()

  private val factoryByVersionByWorkflowName = config.workflows
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

  override fun getEventListenersConfig() = eventListener

  override fun getServiceExecutorConfig(name: String) =
      getService(ServiceName(name))?.executor

  override fun getServiceTagEngineConfig(name: String) =
      getService(ServiceName(name))?.tagEngine

  override fun getWorkflowExecutorConfig(name: String) =
      getWorkflow(WorkflowName(name))?.executor

  override fun getWorkflowTagEngineConfig(name: String) =
      getWorkflow(WorkflowName(name))?.tagEngine

  override fun getWorkflowStateEngineConfig(name: String) =
      getWorkflow(WorkflowName(name))?.stateEngine
}
