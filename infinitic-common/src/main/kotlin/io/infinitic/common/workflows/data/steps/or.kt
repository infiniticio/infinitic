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

internal fun or(step1: Step.Id, step2: Step.Id) = Step.Or(listOf(step1, step2))
internal fun or(step1: Step.Id, step2: Step.And) = Step.Or(listOf(step1, step2))
internal fun or(step1: Step.Id, step2: Step.Or) = Step.Or(listOf(step1) + step2)
internal fun or(step1: Step.And, step2: Step.Id) = Step.Or(listOf(step1, step2))
internal fun or(step1: Step.And, step2: Step.And) = Step.Or(listOf(step1, step2))
internal fun or(step1: Step.And, step2: Step.Or) = Step.Or(listOf(step1, step2))
internal fun or(step1: Step.Or, step2: Step.Id) = Step.Or(step1.steps + step2)
internal fun or(step1: Step.Or, step2: Step.And) = Step.Or(listOf(step1, step2))
internal fun or(step1: Step.Or, step2: Step.Or) = Step.Or(step1.steps + step2.steps)

internal fun or(step1: Step, step2: Step) = when (step1) {
    is Step.And -> when (step2) {
        is Step.And -> or(step1, step2)
        is Step.Id -> or(step1, step2)
        is Step.Or -> or(step1, step2)
    }
    is Step.Id -> when (step2) {
        is Step.And -> or(step1, step2)
        is Step.Id -> or(step1, step2)
        is Step.Or -> or(step1, step2)
    }
    is Step.Or -> when (step2) {
        is Step.And -> or(step1, step2)
        is Step.Id -> or(step1, step2)
        is Step.Or -> or(step1, step2)
    }
}
