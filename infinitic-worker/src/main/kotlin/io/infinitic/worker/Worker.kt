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

package io.infinitic.worker

// class Worker(
//    sendToWorkflowEngine: SendToWorkflowEngine,
//    sendToTaskEngine: SendToTaskEngine,
//    sendToMonitoringPerName: SendToMonitoringPerName,
//    sendToMonitoringGlobal: SendToMonitoringGlobal,
//    sendToExecutors: SendToExecutors,
//    workflowStateStorage: WorkflowStateStorage,
//    taskStateStorage: TaskStateStorage,
//    monitoringPerNameStateStorage: MonitoringPerNameStateStorage,
//    monitoringGlobalStateStorage: MonitoringGlobalStateStorage,
// ) {
//    val taskEngineWorker: TaskEngineWorker? = null
//    val taskExecutorWorker: TaskExecutorWorker? = null
//    val workflowEngineWorker: WorkflowEngineWorker? = null
//    val workflowExecutorWorker: WorkflowExecutorWorker? = null
//    val monitoringGlobalEngineWorker: MonitoringGlobalEngineWorker? = null
//    val monitoringPerNameEngineWorker: MonitoringPerNameEngineWorker? = null
//
//    companion object {
//    }
// }
//
// fun main() = runBlocking {
//    val config = ConfigLoader().loadConfigOrThrow<Config>("/infinitic.yml")
//
//    val stateStorage = getStateStorage(config)
//
//    if (config.roles.contains(WorkerRole.MonitoringGlobal)) {
//        MonitoringGlobalEngineWorker(stateStorage)
//    }
//
//    if (config.roles.contains(WorkerRole.MonitoringPerName)) {
//    }
// }
//
// fun getStateStorage(config: Config): KeyValueStorage = when (config.stateStorage) {
//    Storage.InMemory -> InMemoryStorage()
//    Storage.Redis -> redis {
//        config.redis?.host?. let { host = it }
//        config.redis?.port?. let { port = it }
//        config.redis?.timeout?. let { timeout = it }
//        config.redis?.user?. let { user = it }
//        config.redis?.password?. let { password = it }
//        config.redis?.database?. let { database = it }
//    }
// }
