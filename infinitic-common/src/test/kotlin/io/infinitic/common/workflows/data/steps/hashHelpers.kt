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
package io.infinitic.common.workflows.data.steps

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.currentVersion
import io.infinitic.isRelease
import io.infinitic.versions
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import kotlin.reflect.KClass

const val STEPS_FOLDER = "steps"

private fun getHashFolder() = Step::class.java.getResource("/")!!.path
    .replace("build/resources/main", "src/main/resources")
    .replace("build/classes/java/test", "src/main/resources")

private fun <T : Step> getJsonPath(klass: KClass<T>, version: String) =
    "/$STEPS_FOLDER/step-${klass.simpleName}-$version.json"

private fun <T : Step> getHashPath(klass: KClass<T>, version: String) =
    "/$STEPS_FOLDER/step-${klass.simpleName}-$version.hash"

@Suppress("UNCHECKED_CAST")
private fun <T : Step> getRandomStep(klass: KClass<T>): T = when (klass) {
  Step.Id::class -> TestFactory.random<Step.Id>()
  Step.Or::class -> Step.Or(listOf(TestFactory.random(), TestFactory.random()))
  Step.And::class -> Step.And(listOf(TestFactory.random(), TestFactory.random()))
  else -> thisShouldNotHappen()
} as T

@OptIn(InternalSerializationApi::class)
internal fun <T : Step> checkOrCreateCurrentStepFiles(klass: KClass<T>) {
  // Non-release version are not saved to the sources
  if (isRelease) {
    val jsonPath = getJsonPath(klass, currentVersion)
    val hashPath = getHashPath(klass, currentVersion)

    when (Step::class.java.getResource(jsonPath) == null) {
      // create files for the current version
      true -> {
        val step = getRandomStep(klass)
        val json = Json.encodeToString(klass.serializer(), step)
        File(getHashFolder() + jsonPath).writeText(json)
        File(getHashFolder() + hashPath).writeText(step.hash().toString())
      }

      // check that hashes were not modified
      false -> {
        val json = Step::class.java.getResource(jsonPath)!!.readText()
        val step = Json.decodeFromString(klass.serializer(), json)
        step.hash().toString() shouldBe Step::class.java.getResource(hashPath)!!.readText()
      }
    }
  }
}

@OptIn(InternalSerializationApi::class)
internal fun checkStepHashForAllVersions(klass: KClass<out Step>) {
  // for all versions
  versions.mapNotNull { version ->
    val jsonFileUrl = getJsonPath(klass, version)
    val hashFileUrl = getHashPath(klass, version)
    Step::class.java.getResource(jsonFileUrl)?.let { jsonFile ->
      val json = jsonFile.readText()
      val step = Json.decodeFromString(klass.serializer(), json)
      // hash calculated by previous version should be the same as for this version
      val previousHash = StepHash(Step::class.java.getResource(hashFileUrl)?.readText()!!)
      step.hasHash(previousHash) shouldBe true
    }
  }.size shouldBeGreaterThan 1 // checking we did not break something silently
}

