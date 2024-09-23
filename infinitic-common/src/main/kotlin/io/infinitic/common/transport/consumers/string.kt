package io.infinitic.common.transport.consumers

/**
 * Extension property to safely convert any object to its io.infinitic.workers.consumers.getString representation.
 *
 * This property ensures that a call to the `toString()` method does not throw an exception
 * This is used in catch() sections to avoid creating a potential additional issue
 */
internal val Any.string: String
  get() = try {
    toString()
  } catch (e: Exception) {
    //logger.warn(e) { "Error when calling toString()" }
    "${this::class.java.name}(${e::class.java.name}(${e.message}))"
  }

