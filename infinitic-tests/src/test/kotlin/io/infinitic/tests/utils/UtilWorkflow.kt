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

package io.infinitic.tests.utils

import io.infinitic.annotations.Ignore
import io.infinitic.tests.channels.ChannelsWorkflow
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow

interface UtilWorkflow {
    val log: String
    val channelA: SendChannel<String>
    fun concat(input: String): String
    fun receive(str: String): String
    fun add(str: String): String
    fun factorial(n: Long): Long
    fun cancelChild1(): Long
    fun cancelChild2(): Long
    fun cancelChild2bis(deferred: Deferred<String>): String
}

@Suppress("unused")
class UtilWorkflowImpl : Workflow(), UtilWorkflow {
    override val channelA = channel<String>()
    override var log = ""
    private val utilService = newService(UtilService::class.java)
    private val utilWorkflow = newWorkflow(UtilWorkflow::class.java)
    private val channelsWorkflow = newWorkflow(ChannelsWorkflow::class.java)

    @Ignore
    private val self by lazy { getWorkflowById(UtilWorkflow::class.java, context.id) }

    override fun concat(input: String): String {
        log = utilService.concat(log, input)

        return log
    }

    override fun receive(str: String): String {
        log += str

        val signal = channelA.receive().await()

        log += signal

        return log
    }

    override fun add(str: String): String {
        log += str

        return log
    }

    override fun cancelChild1(): Long {
        val deferred = dispatch(channelsWorkflow::channel1)

        utilService.cancelWorkflow(ChannelsWorkflow::class.java.name, deferred.id!!)

        deferred.await()

        return utilService.await(200)
    }

    override fun cancelChild2(): Long {
        val deferred = dispatch(channelsWorkflow::channel1)

        utilService.cancelWorkflow(ChannelsWorkflow::class.java.name, deferred.id!!)

        dispatch(self::cancelChild2bis, deferred)

        return utilService.await(200)
    }

    override fun cancelChild2bis(deferred: Deferred<String>): String {
        return deferred.await()
    }

    override fun factorial(n: Long) = when {
        n > 1 -> n * utilWorkflow.factorial(n - 1)
        else -> 1
    }
}
