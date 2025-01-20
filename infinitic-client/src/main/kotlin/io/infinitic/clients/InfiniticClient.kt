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
package io.infinitic.clients

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.clients.config.InfiniticClientConfig
import io.infinitic.clients.config.InfiniticClientConfigInterface
import io.infinitic.clients.dispatcher.ClientDispatcher
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.RequestByWorkflowId
import io.infinitic.common.proxies.RequestByWorkflowTag
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.interfaces.InfiniticResources
import io.infinitic.common.transport.logged.LoggedInfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.utils.annotatedName
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.exceptions.clients.InvalidIdTagSelectionException
import io.infinitic.exceptions.clients.InvalidStubException
import io.infinitic.properties.isLazyInitialized
import io.infinitic.transport.config.TransportConfig
import io.infinitic.workflows.DeferredStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.annotations.TestOnly
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
class InfiniticClient(
  val config: InfiniticClientConfigInterface
) : InfiniticClientInterface {

  private val resources: InfiniticResources by lazy { config.transport.resources }

  private val consumerFactory by lazy {
    // get consumerFactory from transport
    config.transport.consumerFactory
  }

  private val producer: InfiniticProducer by lazy {
    // get producer from transport, apply name if present
    LoggedInfiniticProducer(
        logger = logger,
        producer = config.transport.producerFactory
            .apply { config.name?.let { setName(it) } }
            .newProducer(null),
    )
  }

  override fun getName() = producer.emitterName.toString()

  private val shutdownGracePeriodSeconds = config.transport.shutdownGracePeriodSeconds

  private var isClosed: AtomicBoolean = AtomicBoolean(false)

  // Scope used to asynchronously send message, and also to consumes messages
  internal val clientScope = CoroutineScope(Dispatchers.IO)

  private val dispatcher by lazy { ClientDispatcher(clientScope, consumerFactory, producer) }

  /** Get last Deferred created by the call of a stub */
  override val lastDeferred get() = dispatcher.getLastDeferred()

  override fun close() {
    if (isClosed.compareAndSet(false, true)) {
      logger.info { "Closing client..." }
      clientScope.cancel()
      runBlocking {
        try {
          withTimeout((shutdownGracePeriodSeconds * 1000).toLong()) {
            clientScope.coroutineContext.job.join()
          }
        } catch (e: TimeoutCancellationException) {
          logger.warn {
            "The grace period (${shutdownGracePeriodSeconds}s) allotted when closing the client was insufficient." +
                "Some ongoing messages may not have been sent properly."
          }
        } finally {
          deleteClientTopic()
          config.transport.close()
        }
      }
      logger.info { "Client closed." }
    }
  }

  /**
   * Deletes the topic associated with the client
   * (Do NOT delete the client DLQ topic to allow manual inspection of failed messages)
   */
  private suspend fun deleteClientTopic() {
    if (::consumerFactory.isLazyInitialized) {
      val name = getName()
      resources.deleteTopicForClient(name).getOrElse {
        logger.warn(it) { "Unable to delete topic for client $name, please delete it manually." }
      }?.let {
        logger.info { "Client topic $it deleted." }
      }
    }
  }

  /** Create a stub for a new workflow */
  override fun <T : Any> newWorkflow(
    klass: Class<out T>,
    tags: Set<String>?,
    meta: Map<String, ByteArray>?
  ): T = NewWorkflowProxyHandler(
      klass = klass,
      workflowTags = tags?.map { WorkflowTag(it) }?.toSet() ?: setOf(),
      workflowMeta = WorkflowMeta(meta ?: mapOf()),
  ) { dispatcher }.stub()

  /** Create a stub for an existing workflow targeted by id */
  override fun <T : Any> getWorkflowById(klass: Class<out T>, id: String): T =
      ExistingWorkflowProxyHandler(klass, RequestByWorkflowId.by(id)) { dispatcher }.stub()

  /** Create a stub for existing workflow targeted by tag */
  override fun <T : Any> getWorkflowByTag(klass: Class<out T>, tag: String): T =
      ExistingWorkflowProxyHandler(klass, RequestByWorkflowTag.by(tag)) { dispatcher }.stub()


  /** Cancel a workflow */
  override fun cancelAsync(stub: Any): CompletableFuture<Unit> =
      when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler ->
          dispatcher.cancelWorkflowAsync(handler.workflowName, handler.requestBy, null)

        else -> throw InvalidStubException("$stub")
      }

  /** Complete timer within workflow(s) */
  override fun completeTimersAsync(stub: Any, id: String?): CompletableFuture<Unit> =
      when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler -> {
          dispatcher.completeTimersAsync(
              workflowName = handler.workflowName,
              requestBy = handler.requestBy,
              workflowMethodId = id?.let { WorkflowMethodId(id) },
          )
        }

        else -> throw InvalidStubException("$stub")
      }

  override fun completeDelegatedTaskAsync(
    serviceName: String,
    taskId: String,
    result: Any?
  ): CompletableFuture<Unit> = dispatcher.completeTaskAsync(
      ServiceName(serviceName),
      TaskId(taskId),
      MethodReturnValue.from(result, null),
  )

  /** Retry a workflow task */
  override fun retryWorkflowTaskAsync(stub: Any): CompletableFuture<Unit> =
      when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler ->
          dispatcher.retryWorkflowTaskAsync(handler.workflowName, handler.requestBy)

        else -> throw InvalidStubException("$stub")
      }

  /** Retry task within workflow(s) by its id */
  override fun retryTasksAsync(stub: Any, taskId: String): CompletableFuture<Unit> =
      retryTasksAsync(stub, null, null, taskId)

  /** Retry task within workflow(s) by its status and/or its class */
  override fun retryTasksAsync(
    stub: Any,
    taskStatus: DeferredStatus?,
    taskClass: Class<*>?
  ): CompletableFuture<Unit> = retryTasksAsync(stub, taskStatus, taskClass, null)


  /** get ids of a stub, associated to a specific tag */
  override fun <T : Any> getIds(stub: T): Set<String> = runBlocking {
    when (val handler = getProxyHandler(stub)) {
      is ExistingWorkflowProxyHandler -> when (handler.requestBy) {
        is RequestByWorkflowTag -> dispatcher.getWorkflowIdsByTag(
            handler.workflowName,
            (handler.requestBy as RequestByWorkflowTag).workflowTag,
        )

        is RequestByWorkflowId -> throw InvalidIdTagSelectionException("$stub")
      }

      else -> throw InvalidStubException("$stub")
    }
  }

  override fun <R> startAsync(invoke: () -> R): CompletableFuture<Deferred<R>> {
    val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

    return dispatcher.dispatchAsync(handler)
  }

  override fun startVoidAsync(invoke: () -> Unit): CompletableFuture<Deferred<Void>> {
    val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

    return dispatcher.dispatchAsync(handler)
  }

  @TestOnly
  internal suspend fun handle(message: ClientMessage) = dispatcher.responseFlow.emit(message)

  private fun getProxyHandler(stub: Any): ProxyHandler<*> {
    val exception by lazy { InvalidStubException("$stub") }

    val handler =
        try {
          Proxy.getInvocationHandler(stub)
        } catch (e: IllegalArgumentException) {
          throw exception
        }

    if (handler !is ProxyHandler<*>) throw exception

    return handler
  }

  private fun retryTasksAsync(
    stub: Any,
    taskStatus: DeferredStatus?,
    taskClass: Class<*>?,
    taskId: String?
  ): CompletableFuture<Unit> =
      when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler -> {
          val taskName = taskClass?.annotatedName

          dispatcher.retryTaskAsync(
              workflowName = handler.workflowName,
              requestBy = handler.requestBy,
              serviceName = taskName?.let { ServiceName(it) },
              taskStatus = taskStatus,
              taskId = taskId?.let { TaskId(it) },
          )
        }

        else -> throw InvalidStubException("$stub")
      }

  companion object {

    internal val logger = LoggerWithCounter(KotlinLogging.logger {})

    @JvmStatic
    fun builder() = InfiniticClientBuilder()

    /** Create InfiniticClient with YAML from resources directory */
    @JvmStatic
    fun fromYamlResource(vararg resources: String) =
        InfiniticClient(InfiniticClientConfig.fromYamlResource(*resources))

    /** Create InfiniticClient with YAML from system file */
    @JvmStatic
    fun fromYamlFile(vararg files: String) =
        InfiniticClient(InfiniticClientConfig.fromYamlFile(*files))

    /** Create InfiniticClient with YAML from yaml strings */
    @JvmStatic
    fun fromYamlString(vararg yamls: String) =
        InfiniticClient(InfiniticClientConfig.fromYamlString(*yamls))
  }

  /**
   * InfiniticWorker builder
   */
  class InfiniticClientBuilder {
    private var name: String? = null
    private var transport: TransportConfig? = null

    fun setName(name: String) =
        apply { this.name = name }

    fun setTransport(transport: TransportConfig) =
        apply { this.transport = transport }

    fun setTransport(transport: TransportConfig.TransportConfigBuilder) =
        setTransport(transport.build())

    fun build(): InfiniticClient {
      require(transport != null) { "transport must not be null" }

      val config = InfiniticClientConfig(name, transport!!)

      return InfiniticClient(config)
    }
  }
}
