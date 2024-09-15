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

package io.infinitic.pulsar.resources

import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.EventListenerSubscription
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorRetryTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PulsarSubscriptionTest : StringSpec(
    {
      "Main subscription MUST not change" {
        MainSubscription(ClientTopic).defaultName shouldBe "${ClientTopic.prefix}-subscription"
        MainSubscription(WorkflowTagEngineTopic).defaultName shouldBe "${WorkflowTagEngineTopic.prefix}-subscription"
        MainSubscription(WorkflowStateCmdTopic).defaultName shouldBe "${WorkflowStateCmdTopic.prefix}-subscription"
        MainSubscription(WorkflowStateEngineTopic).defaultName shouldBe "${WorkflowStateEngineTopic.prefix}-subscription"
        MainSubscription(WorkflowStateTimerTopic).defaultName shouldBe "${WorkflowStateTimerTopic.prefix}-subscription"
        MainSubscription(WorkflowStateEventTopic).defaultName shouldBe "${WorkflowStateEventTopic.prefix}-subscription"
        MainSubscription(WorkflowExecutorTopic).defaultName shouldBe "${WorkflowExecutorTopic.prefix}-subscription"
        MainSubscription(WorkflowExecutorRetryTopic).defaultName shouldBe "${WorkflowExecutorRetryTopic.prefix}-subscription"
        MainSubscription(WorkflowExecutorEventTopic).defaultName shouldBe "${WorkflowExecutorEventTopic.prefix}-subscription"
        MainSubscription(ServiceTagEngineTopic).defaultName shouldBe "${ServiceTagEngineTopic.prefix}-subscription"
        MainSubscription(ServiceExecutorTopic).defaultName shouldBe "${ServiceExecutorTopic.prefix}-subscription"
        MainSubscription(ServiceExecutorRetryTopic).defaultName shouldBe "${ServiceExecutorRetryTopic.prefix}-subscription"
        MainSubscription(ServiceExecutorEventTopic).defaultName shouldBe "${ServiceExecutorEventTopic.prefix}-subscription"
      }

      "Listener subscription MUST not change" {
        val name = "listener-subscription"

        EventListenerSubscription(ClientTopic, null).defaultName shouldBe name
        EventListenerSubscription(WorkflowTagEngineTopic, null).defaultName shouldBe name
        EventListenerSubscription(WorkflowStateCmdTopic, null).defaultName shouldBe name
        EventListenerSubscription(WorkflowStateEngineTopic, null).defaultName shouldBe name
        EventListenerSubscription(WorkflowStateTimerTopic, null).defaultName shouldBe name
        EventListenerSubscription(WorkflowStateEventTopic, null).defaultName shouldBe name
        EventListenerSubscription(WorkflowExecutorTopic, null).defaultName shouldBe name
        EventListenerSubscription(WorkflowExecutorRetryTopic, null).defaultName shouldBe name
        EventListenerSubscription(WorkflowExecutorEventTopic, null).defaultName shouldBe name
        EventListenerSubscription(ServiceTagEngineTopic, null).defaultName shouldBe name
        EventListenerSubscription(ServiceExecutorTopic, null).defaultName shouldBe name
        EventListenerSubscription(ServiceExecutorRetryTopic, null).defaultName shouldBe name
        EventListenerSubscription(ServiceExecutorEventTopic, null).defaultName shouldBe name
      }
    },
)
