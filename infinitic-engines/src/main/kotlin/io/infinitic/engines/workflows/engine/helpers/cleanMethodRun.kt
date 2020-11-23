/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.engines.workflows.engine.helpers

import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.state.WorkflowState

fun cleanMethodRunIfNeeded(methodRun: MethodRun, state: WorkflowState) {
    // if everything is completed in methodRun then filter state
    if (methodRun.methodOutput != null &&
        methodRun.pastCommands.all { it.isTerminated() } &&
        methodRun.pastSteps.all { it.isTerminated() }
    ) {
        state.methodRuns.remove(methodRun)

        removeUnusedPropertyHash(state)
    }
}

private fun removeUnusedPropertyHash(state: WorkflowState) {
    // get set of all hashes still in use
    val propertyHashes = mutableSetOf<PropertyHash>()

    state.methodRuns.forEach { methodRun ->
        methodRun.pastSteps.forEach { pastStep ->
            pastStep.propertiesNameHashAtTermination?.forEach {
                propertyHashes.add(it.value)
            }
        }
        methodRun.pastCommands.forEach { pastCommand ->
            pastCommand.propertiesNameHashAtStart?.forEach {
                propertyHashes.add(it.value)
            }
        }
        methodRun.propertiesNameHashAtStart.forEach {
            propertyHashes.add(it.value)
        }
    }
    state.currentPropertiesNameHash.forEach {
        propertyHashes.add(it.value)
    }

    // remove each propertyHashValue entry not in propertyHashes
    state.propertiesHashValue.keys.filter {
        it !in propertyHashes
    }.forEach {
        state.propertiesHashValue.remove(it)
    }
}
