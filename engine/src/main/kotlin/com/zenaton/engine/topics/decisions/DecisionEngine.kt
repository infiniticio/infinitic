package com.zenaton.engine.topics.decisions

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.decisions.DecisionState
import com.zenaton.engine.topics.LoggerInterface
import com.zenaton.engine.topics.StaterInterface
import com.zenaton.engine.topics.decisions.messages.DecisionAttemptCompleted
import com.zenaton.engine.topics.decisions.messages.DecisionAttemptDispatched
import com.zenaton.engine.topics.decisions.messages.DecisionAttemptFailed
import com.zenaton.engine.topics.decisions.messages.DecisionAttemptStarted
import com.zenaton.engine.topics.decisions.messages.DecisionCompleted
import com.zenaton.engine.topics.decisions.messages.DecisionDispatched
import com.zenaton.engine.topics.decisions.messages.DecisionMessageInterface

class DecisionEngine(
    private val stater: StaterInterface<DecisionState>,
    private val dispatcher: DecisionDispatcherInterface,
    private val logger: LoggerInterface
) {
    fun handle(msg: DecisionMessageInterface) {
        // timestamp the message
        msg.receivedAt = DateTime()
        // get associated state
        var state = stater.getState(msg.getKey())
        if (state == null) {
            // a null state should mean that this decision is already terminated => all messages others than DecisionDispatched are ignored
            if (msg !is DecisionDispatched) {
                logger.warn("No state found for message:%s(It's normal if this decision is already terminated)", msg)
                return
            }
            // init a state
            state = DecisionState(decisionId = msg.decisionId)
        } else {
            // this should never happen
            if (state.decisionId != msg.decisionId) {
                logger.error("Inconsistent decisionId in message:%s and State:%s)", msg, state)
                return
            }
            // a non-null state with DecisionDispatched should mean that this message has been replicated
            if (msg is DecisionDispatched) {
                logger.error("Already existing state for message:%s", msg)
                return
            }
        }

        when (msg) {
            is DecisionAttemptCompleted -> completeDecisionAttempt(state, msg)
            is DecisionAttemptDispatched -> dispatchDecisionAttempt(state, msg)
            is DecisionAttemptFailed -> failDecisionAttempt(state, msg)
            is DecisionAttemptStarted -> startDecisionAttempt(state, msg)
            is DecisionCompleted -> completeDecision(state, msg)
            is DecisionDispatched -> dispatchDecision(state, msg)
        }
    }

    private fun completeDecisionAttempt(state: DecisionState, msg: DecisionAttemptCompleted) {
        TODO()
    }

    private fun dispatchDecisionAttempt(state: DecisionState, msg: DecisionAttemptDispatched) {
        TODO()
    }

    private fun failDecisionAttempt(state: DecisionState, msg: DecisionAttemptFailed) {
        TODO()
    }

    private fun startDecisionAttempt(state: DecisionState, msg: DecisionAttemptStarted) {
        TODO()
    }

    private fun completeDecision(state: DecisionState, msg: DecisionCompleted) {
        TODO()
    }

    private fun dispatchDecision(state: DecisionState, msg: DecisionDispatched) {
        TODO()
    }
}
