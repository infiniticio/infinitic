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
package io.infinitic.common.proxies

import io.infinitic.annotations.Name
import io.infinitic.annotations.Timeout
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.utils.findAnnotation
import io.infinitic.common.utils.getEmptyConstructor
import io.infinitic.exceptions.workflows.InvalidInlineException
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.getTimeoutInMillis
import io.infinitic.workflows.SendChannel
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.full.isSubclassOf

sealed class ProxyHandler<T : Any>(
  open val klass: Class<out T>,
  open val dispatcherFn: () -> ProxyDispatcher
) : InvocationHandler {

  companion object {
    @JvmStatic
    private val isInlined: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    @JvmStatic
    private val isInvocationAsync: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    @JvmStatic
    private val invocationHandler: ThreadLocal<ProxyHandler<*>?> = ThreadLocal.withInitial { null }

    fun <R> async(fct: () -> R): ProxyHandler<*>? {
      if (isInlined.get()) throw InvalidInlineException

      // set invocation flag to Async
      isInvocationAsync.set(true)
      // call the method reference
      fct()
      val handler = invocationHandler.get()
      // reset default value
      isInvocationAsync.set(false)
      invocationHandler.set(null)

      return handler
    }

    /**
     * This method can be used to get the result of fct()
     * an InvalidInlineException is thrown if [fct] uses a proxy
     */
    fun <R> inline(fct: () -> R): R {
      isInlined.set(true)
      return try {
        fct()
      } finally {
        isInlined.set(false)
      }
    }
  }

  /** Timeout provided by @Timeout annotation, if any */
  val timeout: Result<MillisDuration?>
    get() = try {
      val annotation = method.findTimeoutAnnotation() ?: klass.findTimeoutAnnotation()
      val timeout = annotation?.getEmptyConstructor()?.newInstance()?.getTimeoutInMillis()
      Result.success(timeout?.let { MillisDuration(it) })
    } catch (e: Exception) {
      Result.failure(e)
    }

  /** Method called */
  lateinit var method: Method

  /** Args of method called */
  lateinit var methodArgs: Array<out Any>

  /** Name provided by @Name annotation, if any */
  private val annotatedName: String? by lazy { klass.findNameAnnotation() }

  /** Class name provided by @Name annotation, or java class name by default */
  protected val name: String by lazy { annotatedName ?: klass.name }

  /** SimpleName provided by @Name annotation, or class name by default */
  val simpleName: String
    // MUST be a get() as this.methodName can change when reusing instance
    get() = "${annotatedName ?: klass.simpleName}::$methodName"

  /** MethodName provided by @Name annotation, or java method name by default */
  val methodName: MethodName
    //  MUST be a get() as this.method changes
    get() = MethodName(method.findNameAnnotation() ?: method.name)

  /** MethodParameterTypes from method */
  val methodParameterTypes: MethodParameterTypes
    //  MUST be a get() as this.method changes
    get() = MethodParameterTypes.from(method)

  /** ReturnType from method */
  val returnType: Class<*>
    //  MUST be a get() as this.method changes
    get() = method.returnType

  /** MethodParameters from method */
  val methodParameters: MethodParameters
    //  MUST be a get() as this.method changes
    get() = MethodParameters.from(method, methodArgs)

  /** provides a stub of type T */
  @Suppress("UNCHECKED_CAST")
  fun stub() = Proxy.newProxyInstance(klass.classLoader, arrayOf(klass), this) as T

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
    if (isInlined.get()) throw InvalidInlineException

    val any = getAsyncReturnValue(method)

    // if the method is defined at Object level, return default value
    if (method.declaringClass == Object::class.java) {
      return when (method.name) {
        "toString" -> klass.name
        else -> any
      }
    }

    this.method = method
    this.methodArgs = args ?: arrayOf()

    return when (isInvocationAsync.get()) {
      // sync => run directly from dispatcher
      false -> dispatcherFn().dispatchAndWait(this)
      // store current instance to get retrieved from ProxyHandler.async
      true -> {
        // set current handler
        invocationHandler.set(this)
        // return fake value
        any
      }
    }
  }

  /** Check if method is a getter on a SendChannel */
  fun isChannelGetter(): Boolean = method.returnType.kotlin.isSubclassOf(SendChannel::class)

  // Returns a type-compatible value to avoid an exception at runtime
  private fun getAsyncReturnValue(method: Method) =
      when (method.returnType.name) {
        "long" -> 0L
        "int" -> 0
        "short" -> 0.toShort()
        "byte" -> 0.toByte()
        "double" -> 0.toDouble()
        "float" -> 0.toFloat()
        "char" -> 0.toChar()
        "boolean" -> false
        else -> null
      }

  // search for a @Timeout annotation on this method or its parents' methods
  // Interfaces are EXCLUDED, as this annotation has a different meaning on interfaces
  private fun Method.findTimeoutAnnotation(): Class<out WithTimeout>? =
      findAnnotation(Timeout::class.java, false)?.with?.java

  // search for a @Timeout annotation on this class
  // Interfaces are EXCLUDED, as this annotation has a different meaning on interfaces
  private fun Class<*>.findTimeoutAnnotation(): Class<out WithTimeout>? =
      findAnnotation(Timeout::class.java, false)?.with?.java

  // search for a @Name annotation on this method,
  // Interfaces are included in the search
  private fun Method.findNameAnnotation(): String? = findAnnotation(Name::class.java)?.name


  // search for a @Name annotation on this class,
  // Interfaces are included in the search
  private fun Class<*>.findNameAnnotation(): String? = findAnnotation(Name::class.java)?.name
}
