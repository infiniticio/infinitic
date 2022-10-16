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

package io.infinitic.tests.inline

import io.infinitic.tests.utils.UtilService
import io.infinitic.workflows.Workflow

interface InlineWorkflow {
    fun inline1(i: Int): String
    fun inline2(i: Int): String
    fun inline3(i: Int): String
}

@Suppress("unused")
class InlineWorkflowImpl : Workflow(), InlineWorkflow {

    private val utilService = newService(UtilService::class.java)

    override fun inline1(i: Int): String {
        val result = inline { 2 * i }
        return utilService.concat("2 * $i = ", "$result")
    }

    override fun inline2(i: Int): String {
        val result = inline {
            dispatch(utilService::reverse, "ab")
            2 * i
        }

        return utilService.concat("2 * $i = ", "$result")
    }

    override fun inline3(i: Int): String {
        val result = inline {
            utilService.concat("1", "2")
            2 * i
        }
        return utilService.concat("2 * $i = ", "$result") // should throw
    }
}
