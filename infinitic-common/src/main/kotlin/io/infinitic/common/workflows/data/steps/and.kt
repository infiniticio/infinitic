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

package io.infinitic.common.workflows.data.steps

internal fun and(step1: Step, step2: Step) = Step.And(
    when (step1) {
        is Step.And -> when (step2) {
            is Step.And -> step1.steps + step2.steps
            is Step.Id -> step1.steps + step2
            is Step.Or -> listOf(step1, step2)
        }
        is Step.Id -> when (step2) {
            is Step.And -> listOf(step1) + step2.steps
            is Step.Id -> listOf(step1, step2)
            is Step.Or -> listOf(step1, step2)
        }
        is Step.Or -> when (step2) {
            is Step.And -> listOf(step1, step2)
            is Step.Id -> listOf(step1, step2)
            is Step.Or -> listOf(step1, step2)
        }
    }
)
