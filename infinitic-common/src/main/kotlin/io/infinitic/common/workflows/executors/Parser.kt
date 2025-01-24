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
package io.infinitic.common.workflows.executors

import com.fasterxml.jackson.annotation.JsonView
import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.annotations.Ignore
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.workflows.Channel
import io.infinitic.workflows.Workflow
import org.slf4j.Logger
import java.lang.reflect.Proxy
import java.security.InvalidParameterException
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField

context(KLogger)
fun Workflow.setProperties(
  propertiesHashValue: Map<PropertyHash, PropertyValue>,
  propertiesNameHash: Map<PropertyName, PropertyHash>
) {
  val properties = propertiesNameHash.mapValues {
    propertiesHashValue[it.value]
      ?: thisShouldNotHappen("unknown hash ${it.value} in $propertiesHashValue")
  }

  setProperties(properties)
}

private fun isStorableProperty(prop: KProperty1<out Workflow, *>, value: Any?): Boolean =
    !(  // exclude channels
        prop.returnType.isSubtypeOf(Channel::class.starProjectedType) ||
            // exclude SLF4J loggers
            prop.returnType.isSubtypeOf(Logger::class.createType()) ||
            // exclude KotlinLogging loggers
            prop.returnType.isSubtypeOf(KLogger::class.createType()) ||
            // exclude Ignore annotation
            prop.hasAnnotation<Ignore>() ||
            // excludes Proxies (tasks and workflows) and null
            (value?.let { Proxy.isProxyClass(it::class.java) } ?: true))

fun Workflow.getProperties() = this::class.memberProperties
    .map { p: KProperty1<out Workflow, *> -> p to getProperty(p) }
    .filter { isStorableProperty(it.first, it.second) }
    .associateBy(
        { PropertyName(it.first.name) },
        { PropertyValue.from(it.second, it.first.javaField!!.genericType) },
    )

context(KLogger)
private fun Workflow.setProperties(values: Map<PropertyName, PropertyValue>) {
  val properties = this::class.memberProperties
  values.forEach { (name, value) ->
    properties.find { it.name == name.name }
        ?.let {
          // here `value` is a PropertyValue, not the deserialized value
          // so `isStorableProperty` will only check correctly the type of the property
          if (isStorableProperty(it, value)) setProperty(it, value)
        }
      ?: warn {
        "The property '${name.name}' present in the workflow history for class " +
            "'${this::class.java.name} is not recognized and will be ignored."
      }
  }
}

private fun <T : Workflow> T.getProperty(prop: KProperty1<out T, *>): Any? =
    prop.javaField?.let {
      try {
        it.isAccessible = true
      } catch (e: SecurityException) {
        throw RuntimeException(
            "Property ${this::class.java.name}:${it.name} is not readable (can not set accessible)",
            e,
        )
      }

      it.get(this@getProperty)
    }

private fun <T : Workflow> T.setProperty(
  prop: KProperty1<out T, *>,
  propertyValue: PropertyValue
) {
  prop.javaField?.apply {
    try {
      isAccessible = true
    } catch (e: SecurityException) {
      throw RuntimeException(
          "Property ${this@setProperty::class.java.name}:$name can not be set (not accessible)",
          e,
      )
    }
    val value = propertyValue.value(genericType, prop.jsonViewClass)

    set(this@setProperty, value)
  }
}

private val KProperty1<*, *>.jsonViewClass
  get(): Class<*>? {
    val jsonViewAnnotation = findAnnotations(JsonView::class)
        .also {
          if (it.size > 1)
            throw InvalidParameterException("Property '$name' should not have more than one @JsonView annotation")
        }
        .firstOrNull()
    return jsonViewAnnotation?.value
        ?.also {
          if (it.size != 1)
            throw InvalidParameterException("The annotation @JsonView on property '$name' must have exactly one parameter")
        }
        ?.firstOrNull()?.java
  }
