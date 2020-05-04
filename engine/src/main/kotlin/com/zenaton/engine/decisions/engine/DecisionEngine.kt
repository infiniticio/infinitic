package com.zenaton.engine.decisions.engine

import com.zenaton.engine.decisions.data.DecisionState
import com.zenaton.engine.decisions.messages.DecisionAttemptCompleted
import com.zenaton.engine.decisions.messages.DecisionAttemptFailed
import com.zenaton.engine.decisions.messages.DecisionAttemptStarted
import com.zenaton.engine.decisions.messages.DecisionDispatched
import com.zenaton.engine.decisions.messages.DecisionMessageInterface
import com.zenaton.engine.interfaces.LoggerInterface
import com.zenaton.engine.interfaces.StaterInterface
import com.zenaton.engine.interfaces.data.DateTime

class DecisionEngine(
    private val stater: StaterInterface<DecisionState>,
    private val dispatcher: DecisionEngineDispatcherInterface,
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
            is DecisionAttemptFailed -> failDecisionAttempt(state, msg)
            is DecisionAttemptStarted -> startDecisionAttempt(state, msg)
            is DecisionDispatched -> dispatchDecision(state, msg)
        }
    }

    private fun completeDecisionAttempt(state: DecisionState, msg: DecisionAttemptCompleted) {
        TODO()
    }

    private fun failDecisionAttempt(state: DecisionState, msg: DecisionAttemptFailed) {
        TODO()
    }

    private fun startDecisionAttempt(state: DecisionState, msg: DecisionAttemptStarted) {
        TODO()
    }

    private fun dispatchDecision(state: DecisionState, msg: DecisionDispatched) {
        TODO()
    }
}
