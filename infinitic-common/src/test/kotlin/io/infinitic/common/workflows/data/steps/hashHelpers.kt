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

private fun getHashFolder() = Step::class.java.getResource("/$STEPS_FOLDER/")!!.path
    .replace("build/resources/main", "src/main/resources")

private fun <T : Step> getJsonFile(klass: KClass<T>, version: String) =
    File("${getHashFolder()}/step-${klass.simpleName}-$version.json")

private fun <T : Step> getHashFile(klass: KClass<T>, version: String) =
    File("${getHashFolder()}/step-${klass.simpleName}-$version.hash")

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
    val jsonFile = getJsonFile(klass, currentVersion.trim())
    val hashFile = getHashFile(klass, currentVersion.trim())

    when (jsonFile.exists()) {
      // create files for the current version
      false -> {
        val step = getRandomStep(klass)
        val json = Json.encodeToString(klass.serializer(), step)
        jsonFile.writeText(json)
        hashFile.writeText(step.hash().toString())
      }

      // check that hash were not modified
      true -> {
        val json = jsonFile.readText()
        val step = Json.decodeFromString(klass.serializer(), json)
        step.hash().toString() shouldBe hashFile.readText()
      }
    }
  }
}

@OptIn(InternalSerializationApi::class)
internal fun checkStepHashForAllVersions(klass: KClass<out Step>) {
  // for all versions
  versions.mapNotNull { version ->
    val jsonFileUrl = "/$STEPS_FOLDER/${getJsonFile(klass, version).name}"
    val hashFileUrl = "/$STEPS_FOLDER/${getHashFile(klass, version).name}"
    Step::class.java.getResource(jsonFileUrl)?.let {
      val json = it.readText()
      val step = Json.decodeFromString(klass.serializer(), json)
      // hash calculated by previous version should be the same as for this version
      Step::class.java.getResource(hashFileUrl)?.readText() shouldBe (step.hash().toString())
    }
  }.size shouldBeGreaterThan 0 // checking we did not break something silently
}

