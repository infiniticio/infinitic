package com.zenaton.decisionmanager.engine

import com.zenaton.decisionmanager.messages.DecisionAttemptCompleted
import com.zenaton.decisionmanager.messages.DecisionAttemptDispatched
import com.zenaton.decisionmanager.messages.DecisionAttemptFailed
import com.zenaton.decisionmanager.messages.DecisionAttemptRetried
import com.zenaton.decisionmanager.messages.DecisionAttemptStarted
import com.zenaton.decisionmanager.messages.DecisionAttemptTimeout
import com.zenaton.decisionmanager.messages.DecisionDispatched
import com.zenaton.decisionmanager.messages.interfaces.DecisionAttemptFailingMessageInterface
import com.zenaton.decisionmanager.messages.interfaces.DecisionAttemptMessageInterface
import com.zenaton.decisionmanager.messages.interfaces.DecisionMessageInterface
import com.zenaton.decisionmanager.state.DecisionState
import com.zenaton.workflowManager.interfaces.LoggerInterface
import com.zenaton.workflowManager.interfaces.StaterInterface
import com.zenaton.workflowManager.messages.DecisionCompleted

class DecisionEngine(
    private val stater: StaterInterface<DecisionState>,
    private val dispatcher: DecisionEngineDispatcherInterface,
    private val logger: LoggerInterface
) {
    fun handle(msg: DecisionMessageInterface) {
        // get associated state
        var state = stater.getState(msg.getStateId())
        if (state == null) {
            // a null state should mean that this decision is already terminated => all messages others than DecisionDispatched are ignored
            if (msg !is DecisionDispatched) {
                logger.warn("No state found for message:%s(It's normal if this decision is already terminated)", msg)
                return
            }
            // init a state
            state = DecisionState(
                decisionId = msg.decisionId,
                decisionName = msg.decisionName,
                decisionData = msg.decisionData,
                workflowId = msg.workflowId
            )
        } else {
            // this should never happen
            if (state.decisionId != msg.decisionId) {
                logger.error("Inconsistent decisionId in message:%s and State:%s)", msg, state)
                return
            }
            if (msg is DecisionAttemptMessageInterface) {
                if (state.decisionAttemptId != msg.decisionAttemptId) {
                    logger.warn("Inconsistent decisionAttemptId in message: (Can happen if the decision has been manually retried)%s and State:%s", msg, state)
                    return
                }
                if (state.decisionAttemptIndex != msg.decisionAttemptIndex) {
                    logger.warn("Inconsistent decisionAttemptIndex in message: (Can happen if this decision has had timeout)%s and State:%s", msg, state)
                    return
                }
            }
            // a non-null state with DecisionDispatched should mean that this message has been replicated
            if (msg is DecisionDispatched) {
                logger.error("Already existing state for message:%s", msg)
                return
            }
        }

        when (msg) {
            is DecisionAttemptCompleted -> completeDecisionAttempt(state, msg)
            is DecisionAttemptFailed -> failDecisionAttempt(state, msg)
            is DecisionAttemptRetried -> retryDecisionAttempt(state, msg)
            is DecisionAttemptStarted -> startDecisionAttempt(state, msg)
            is DecisionAttemptTimeout -> timeoutDecisionAttempt(state, msg)
            is DecisionDispatched -> dispatchDecision(state, msg)
        }
    }

    private fun completeDecisionAttempt(state: DecisionState, msg: DecisionAttemptCompleted) {
        // if this decision belongs to a workflow
        if (state.workflowId != null) {
            val tc = DecisionCompleted(
                workflowId = state.workflowId,
                decisionId = msg.decisionId,
                decisionOutput = msg.decisionOutput
            )
            dispatcher.dispatch(tc)
        }
        // delete state
        stater.deleteState(msg.getStateId())
    }

    private fun failDecisionAttempt(state: DecisionState, msg: DecisionAttemptFailed) {
        triggerDelayedRetry(state = state, msg = msg)
    }

    private fun retryDecisionAttempt(state: DecisionState, msg: DecisionAttemptRetried) {
        val tad = DecisionAttemptDispatched(
            decisionId = msg.decisionId,
            decisionAttemptId = msg.decisionAttemptId,
            decisionAttemptIndex = msg.decisionAttemptIndex,
            decisionName = state.decisionName,
            decisionData = state.decisionData
        )
        dispatcher.dispatch(tad)
    }

    private fun startDecisionAttempt(state: DecisionState, msg: DecisionAttemptStarted) {
        if (msg.decisionAttemptDelayBeforeTimeout > 0f) {
            val tad = DecisionAttemptTimeout(
                decisionId = msg.decisionId,
                decisionAttemptId = msg.decisionAttemptId,
                decisionAttemptIndex = msg.decisionAttemptIndex,
                decisionAttemptDelayBeforeRetry = msg.decisionAttemptDelayBeforeRetry
            )
            dispatcher.dispatch(tad, after = msg.decisionAttemptDelayBeforeTimeout)
        }
    }

    private fun timeoutDecisionAttempt(state: DecisionState, msg: DecisionAttemptTimeout) {
        triggerDelayedRetry(state = state, msg = msg)
    }

    private fun dispatchDecision(state: DecisionState, msg: DecisionDispatched) {
        // dispatch a decision attempt
        val tad = DecisionAttemptDispatched(
            decisionId = msg.decisionId,
            decisionAttemptId = state.decisionAttemptId,
            decisionAttemptIndex = state.decisionAttemptIndex,
            decisionName = msg.decisionName,
            decisionData = msg.decisionData
        )
        dispatcher.dispatch(tad)
        // update and save state
        stater.createState(msg.getStateId(), state)
    }

    private fun triggerDelayedRetry(state: DecisionState, msg: DecisionAttemptFailingMessageInterface) {
        if (msg.decisionAttemptDelayBeforeRetry >= 0f) {
            val newIndex = 1 + msg.decisionAttemptIndex
            // schedule next attempt
            val tar = DecisionAttemptRetried(
                decisionId = state.decisionId,
                decisionAttemptId = state.decisionAttemptId,
                decisionAttemptIndex = newIndex
            )
            if (msg.decisionAttemptDelayBeforeRetry <= 0f) {
                retryDecisionAttempt(state, tar)
            } else {
                dispatcher.dispatch(tar, after = msg.decisionAttemptDelayBeforeRetry)
            }
            // update state
            state.decisionAttemptIndex = newIndex
            stater.updateState(msg.getStateId(), state)
        }
    }
}
