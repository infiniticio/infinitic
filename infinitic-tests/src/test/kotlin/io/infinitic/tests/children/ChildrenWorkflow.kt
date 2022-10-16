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

package io.infinitic.tests.children

import io.infinitic.tests.utils.UtilService
import io.infinitic.tests.utils.UtilWorkflow
import io.infinitic.workflows.Workflow
import java.time.Duration

interface ChildrenWorkflow {
    fun await()

    fun parent(): String
    fun wparent(): String

    fun child1(): String
    fun child2(): String
    fun factorial(n: Long): Long

    fun cancel()
}

@Suppress("unused")
class ChildrenWorkflowImpl : Workflow(), ChildrenWorkflow {

    private val utilService =
        newService(UtilService::class.java, tags = setOf("foo", "bar"), meta = mapOf("foo" to "bar".toByteArray()))
    private val childrenWorkflow = newWorkflow(ChildrenWorkflow::class.java, tags = setOf("foo", "bar"))
    private val utilWorkflow = newWorkflow(UtilWorkflow::class.java)

    override fun await() {
        timer(Duration.ofSeconds(60)).await()
    }

    override fun parent() = utilService.parent()

    override fun wparent(): String = childrenWorkflow.parent()

    override fun child1(): String {
        var str: String = utilWorkflow.concat("a")
        str = utilService.concat(str, "b")

        return str // should be "ab"
    }

    override fun child2(): String {
        val str = utilService.reverse("ab")
        val deferred = dispatch(utilWorkflow::concat, str)

        return utilService.concat(deferred.await(), str) // should be "baba"
    }

    override fun factorial(n: Long) = when {
        n > 1 -> n * childrenWorkflow.factorial(n - 1)
        else -> 1
    }

    override fun cancel() {
        childrenWorkflow.await()
    }
}
