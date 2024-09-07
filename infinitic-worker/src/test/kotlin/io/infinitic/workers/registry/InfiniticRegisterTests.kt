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
package io.infinitic.workers.registry

import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workers.config.ExponentialBackoffRetryPolicy
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.storage.compression.CompressionConfig
import io.infinitic.storage.config.MySQLConfig
import io.infinitic.storage.config.MySQLStorageConfig
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workers.config.InfiniticWorkerConfig
import io.infinitic.workers.samples.EventListenerImpl
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.WorkflowA
import io.infinitic.workers.samples.WorkflowAImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.random.Random

private const val yaml = """
transport: inMemory
storage:
  inMemory:
"""

internal class InfiniticRegisterTests :
  StringSpec(
      {
        val serviceName = ServiceName(ServiceA::class.java.name)
        val serviceImplName = ServiceAImpl::class.java.name
        val workflowName = WorkflowName(WorkflowA::class.java.name)
        val workflowImplName = WorkflowAImpl::class.java.name
        val eventListenerImplName = EventListenerImpl::class.java.name

        "checking default Service Executor settings" {
          val config = InfiniticWorkerConfig.fromYamlString(
              yaml,
              """
services:
  - name: $serviceName
    executor:
      class: $serviceImplName
""",
          )
          with(config.services.first { it.name == serviceName.name }) {
            executor?.concurrency shouldBe 1
            executor?.withTimeout shouldBe WithTimeout.UNSET
            executor?.withRetry shouldBe WithRetry.UNSET
            tagEngine shouldBe null
          }
        }

        "checking explicit Service Executor settings" {
          val concurrency = Random.nextInt(from = 1, until = Int.MAX_VALUE)
          val timeoutSeconds = Random.nextDouble()
          val withRetry = ExponentialBackoffRetryPolicy(minimumSeconds = Random.nextDouble())
          val config = InfiniticWorkerConfig.fromYamlString(
              yaml,
              """
services:
  - name: $serviceName
    executor:
      class: $serviceImplName
      concurrency: $concurrency
      timeoutSeconds: $timeoutSeconds
      retry:
        minimumSeconds: ${withRetry.minimumSeconds}
""",
          )
          with(config.services.first { it.name == serviceName.name }) {
            executor?.factory?.invoke().shouldBeInstanceOf<ServiceAImpl>()
            executor?.concurrency shouldBe concurrency
            executor?.withTimeout?.getTimeoutSeconds() shouldBe timeoutSeconds
            executor?.withRetry shouldBe withRetry
            tagEngine shouldBe null
          }
        }

        "checking default Service Tag Engine settings" {
          val db = MySQLConfig(host = "localhost", port = 3306, username = "root", password = "p")
          val config = InfiniticWorkerConfig.fromYamlString(
              yaml,
              """
storage:
  compression: bzip2
  mysql:
    host: ${db.host}
    port: ${db.port}
    username: ${db.username}
    password: ${db.password}

services:
  - name: $serviceName
    tagEngine:
      concurrency: 42
""",
          )
          with(config.services.first { it.name == serviceName.name }) {
            executor shouldBe null
            tagEngine?.concurrency shouldBe 42
            tagEngine?.storage shouldBe MySQLStorageConfig(
                compression = CompressionConfig.bzip2,
                mysql = db,
            )
          }
        }

        "checking explicit Service Tag Engine settings" {
          val db = MySQLConfig(
              host = "localhost",
              port = 3306,
              username = "root",
              password = "<PASSWORD>",
          )
          val config = InfiniticWorkerConfig.fromYamlString(
              yaml,
              """
services:
  - name: $serviceName
    tagEngine:
      concurrency: 42
      storage:
        compression: bzip2
        mysql:
          host: ${db.host}
          port: ${db.port}
          username: ${db.username}
          password: ${db.password}
""",
          )
          with(config.services.first { it.name == serviceName.name }) {
            executor shouldBe null
            tagEngine?.concurrency shouldBe 42
            tagEngine?.storage shouldBe MySQLStorageConfig(
                compression = CompressionConfig.bzip2,
                mysql = db,
            )
          }
        }

        "Get default Service EventListener settings" {
          val config = InfiniticWorkerConfig.fromYamlString(
              yaml,
              """
eventListener:
  class: $eventListenerImplName
""",
          )
          with(config.eventListener) {
            this?.listener.shouldBeInstanceOf<EventListenerImpl>()
            this?.concurrency shouldBe 1
            this?.subscriptionName shouldBe null
          }
        }

        "Get explicit Service EventListener settings" {
          val config = InfiniticWorkerConfig.fromYamlString(
              yaml,
              """
eventListener:
  class: $eventListenerImplName
  concurrency: 100
""",
          )
          with(config.eventListener) {
            this?.listener.shouldBeInstanceOf<EventListenerImpl>()
            this?.concurrency shouldBe 100
            this?.subscriptionName shouldBe null
          }
        }

        "checking default workflow executor settings" {
          val config = InfiniticWorkerConfig.fromYamlString(
              yaml,
              """
workflows:
  - name: $workflowName
    executor:
      class: $workflowImplName
""",
          )
          config.services.size shouldBe 0
          with(config.workflows.first { it.name == workflowName.name }) {
            executor?.withTimeout shouldBe WithTimeout.UNSET
            executor?.withRetry shouldBe WithRetry.UNSET
            executor?.concurrency shouldBe 1
            executor?.checkMode shouldBe null
            tagEngine shouldBe null
            stateEngine shouldBe null
          }
        }

        "checking explicit workflow executor settings" {
          val concurrency = Random.nextInt(from = 1, until = Int.MAX_VALUE)
          val timeoutSeconds = Random.nextDouble()
          val withRetry = ExponentialBackoffRetryPolicy(minimumSeconds = Random.nextDouble())
          val config = InfiniticWorkerConfig.fromYamlString(
              yaml,
              """
workflows:
  - name: $workflowName
    executor:
      class: $workflowImplName
      concurrency: $concurrency
      timeoutSeconds: $timeoutSeconds
      retry:
        minimumSeconds: ${withRetry.minimumSeconds}
""",
          )
          config.services.size shouldBe 0
          with(config.workflows.first { it.name == workflowName.name }) {
            executor?.withTimeout?.getTimeoutSeconds() shouldBe timeoutSeconds
            executor?.withRetry shouldBe withRetry
            executor?.concurrency shouldBe concurrency
            executor?.checkMode shouldBe null
            tagEngine shouldBe null
            stateEngine shouldBe null
          }
        }
      },
  )

