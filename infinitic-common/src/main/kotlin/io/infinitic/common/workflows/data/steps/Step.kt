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
package io.infinitic.common.workflows.data.steps

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.CommandStatus.Canceled
import io.infinitic.common.workflows.data.commands.CommandStatus.Completed
import io.infinitic.common.workflows.data.commands.CommandStatus.Failed
import io.infinitic.common.workflows.data.commands.CommandStatus.Ongoing
import io.infinitic.common.workflows.data.commands.CommandStatus.TimedOut
import io.infinitic.common.workflows.data.commands.CommandStatus.Unknown
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.commands.ReceiveSignalPastCommand
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.exceptions.workflows.OutOfBoundAwaitException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.reflect.Type
import kotlin.Int.Companion.MAX_VALUE
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
sealed class Step {
  // is this step currently terminated?
  @JsonIgnore
  fun isTerminated(): Boolean = isTerminatedAt(WorkflowTaskIndex(MAX_VALUE))

  // get current status
  fun status(): StepStatus = statusAt(WorkflowTaskIndex(MAX_VALUE))

  // is this step terminated at provided index?
  @JsonIgnore
  abstract fun isTerminatedAt(index: WorkflowTaskIndex): Boolean

  // get status at provided index
  abstract fun statusAt(index: WorkflowTaskIndex): StepStatus

  // increase wait index and update current status
  abstract fun nextAwaitIndex()

  // check wait index is valid
  abstract fun checkAwaitIndex()

  /** hash provides a unique hash linked to the structure of the step (excluding commandStatus) */
  abstract fun hash(): StepHash

  /**
   * Function providing the value when completed. This should be used only in workflow task
   */
  abstract fun valueCompletedFn(): Any?

  @Serializable
  @SerialName("Step.Id")
  data class Id(
    val commandId: CommandId,
      // store the # of time we have already waited this command
    @AvroDefault("0") // before this feature was added, we consider it was the first wait
    var awaitIndex: Int = 0
  ) : Step() {

    // the type of the returned value, can be null for inline tasks
    @Transient
    @JsonIgnore
    var returnValueType: Type? = null

    // the JsonViewClass of the returned value
    @Transient
    @JsonIgnore
    var returnValueJsonViewClass: Class<*>? = null

    // status of first wait occurrence
    @JsonIgnore
    var commandStatus: CommandStatus = Ongoing

    // only used in workflow task
    // statuses of multiple wait occurrences, non-null for ReceiveSignalPastCommand only
    @Transient
    @JsonIgnore
    @AvroDefault(Avro.NULL)
    var commandStatuses: List<CommandStatus>? = null

    // max number of result for the command
    @Transient
    @JsonIgnore
    @AvroDefault("1")
    var commandStatusLimit: Int? = null

    companion object {
      // only used in workflow task
      fun from(
        pastCommand: PastCommand,
        returnValueType: Type?,
        returnValueJsonViewClass: Class<*>?
      ) = Id(pastCommand.commandId).apply {
        this.returnValueType = returnValueType
        this.returnValueJsonViewClass = returnValueJsonViewClass
        this.commandStatus = pastCommand.commandStatus

        if (pastCommand is ReceiveSignalPastCommand) {
          this.commandStatuses = pastCommand.commandStatuses
          this.commandStatusLimit = pastCommand.command.receivedSignalLimit
        }
      }

      // This is needed for Jackson deserialization
      @JsonCreator
      @JvmStatic
      fun new(commandId: String) = Id(CommandId(commandId))
    }

    override fun hash() =
        StepHash(SerializedData.encode(commandId, CommandId::class.java, null).hash())

    @JsonIgnore
    override fun isTerminatedAt(index: WorkflowTaskIndex) =
        when (statusAt(index)) {
          is StepStatus.Waiting -> false
          is StepStatus.Unknown -> true
          is StepStatus.Canceled -> true
          is StepStatus.CurrentlyFailed -> true
          is StepStatus.Failed -> thisShouldNotHappen()
          is StepStatus.CurrentlyTimedOut -> true
          is StepStatus.TimedOut -> thisShouldNotHappen()
          is StepStatus.Completed -> true
        }

    override fun checkAwaitIndex() {
      // user is asking more than the limit, we consider it as a failure
      if (commandStatuses != null &&
        commandStatusLimit != null &&
        awaitIndex >= commandStatusLimit!!) {
        throw OutOfBoundAwaitException
      }
    }

    override fun nextAwaitIndex() {
      awaitIndex++

      // update commandStatus if needed
      if (commandStatuses != null) {
        // update current status
        commandStatus = commandStatuses!!.firstOrNull {
          (it is Completed) && (it.returnIndex == awaitIndex)
        } ?: Ongoing
      }
    }

    override fun statusAt(index: WorkflowTaskIndex) =
        when (val status = commandStatus) {
          is Ongoing -> StepStatus.Waiting
          
          is Unknown ->
            when (index >= status.unknowingWorkflowTaskIndex) {
              true -> StepStatus.Unknown(
                  status.deferredUnknownError, status.unknowingWorkflowTaskIndex,
              )

              false -> StepStatus.Waiting
            }

          is Canceled ->
            when (index >= status.cancellationWorkflowTaskIndex) {
              true -> StepStatus.Canceled(
                  status.deferredCanceledError, status.cancellationWorkflowTaskIndex,
              )

              false -> StepStatus.Waiting
            }

          is TimedOut ->
            when (index >= status.timeoutWorkflowTaskIndex) {
              true -> StepStatus.CurrentlyTimedOut(
                  status.deferredTimedOutError, status.timeoutWorkflowTaskIndex,
              )

              false -> StepStatus.Waiting
            }

          is Failed ->
            when (index >= status.failureWorkflowTaskIndex) {
              true -> StepStatus.CurrentlyFailed(
                  status.deferredFailedError, status.failureWorkflowTaskIndex,
              )

              false -> StepStatus.Waiting
            }

          is Completed ->
            when (index >= status.completionWorkflowTaskIndex) {
              true -> StepStatus.Completed(
                  status.returnValue, status.completionWorkflowTaskIndex,
              )

              false -> StepStatus.Waiting
            }
        }

    override fun valueCompletedFn() = when (val status = status()) {
      is StepStatus.Completed -> status.returnValue.deserialize(
          returnValueType,
          returnValueJsonViewClass,
      )

      else -> thisShouldNotHappen(status.toString())
    }
  }

  @Serializable
  @SerialName("Step.And")
  data class And(var steps: List<Step>) : Step() {

    override fun hash() = StepHash(
        SerializedData.encode(
            steps.map { it.hash() },
            typeOf<List<StepHash>>().javaType,
            null,
        ).hash(),
    )

    @JsonIgnore
    override fun isTerminatedAt(index: WorkflowTaskIndex) =
        steps.all { it.isTerminatedAt(index) }

    override fun checkAwaitIndex() {
      steps.map { it.checkAwaitIndex() }
    }

    override fun nextAwaitIndex() {
      steps.map { it.nextAwaitIndex() }
    }

    override fun statusAt(index: WorkflowTaskIndex): StepStatus {
      val statuses = steps.map { it.statusAt(index) }

      // if at least one step is canceled or currentlyFailed, then And(...steps) is the first of them
      val firstTerminated = statuses
          .filter {
            it is StepStatus.CurrentlyFailed ||
                it is StepStatus.CurrentlyTimedOut ||
                it is StepStatus.Canceled ||
                it is StepStatus.Unknown
          }
          .minByOrNull {
            when (it) {
              is StepStatus.Unknown -> it.unknowingWorkflowTaskIndex
              is StepStatus.Canceled -> it.cancellationWorkflowTaskIndex
              is StepStatus.CurrentlyFailed -> it.failureWorkflowTaskIndex
              is StepStatus.CurrentlyTimedOut -> it.timeoutWorkflowTaskIndex
              is StepStatus.Completed,
              is StepStatus.Failed,
              is StepStatus.TimedOut,
              is StepStatus.Waiting -> thisShouldNotHappen()
            }
          }

      if (firstTerminated != null) return firstTerminated

      // if at least one step is ongoing, then And(...steps) is ongoing
      if (statuses.any { it is StepStatus.Waiting }) return StepStatus.Waiting

      // if all steps are completed, then And(...steps) is completed
      if (statuses.all { it is StepStatus.Completed }) {
        val maxIndex = statuses.maxOf { (it as StepStatus.Completed).completionWorkflowTaskIndex }

        return StepStatus.Completed(MethodReturnValue(SerializedData.NULL), maxIndex)
      }

      thisShouldNotHappen()
    }

    override fun valueCompletedFn() = steps.map { it.valueCompletedFn() }
  }

  @Serializable
  @SerialName("Step.Or")
  data class Or(var steps: List<Step>) : Step() {

    override fun hash() = StepHash(
        SerializedData.encode(
            steps.map { it.hash() },
            typeOf<List<StepHash>>().javaType,
            null,
        ).hash(),
    )

    @JsonIgnore
    override fun isTerminatedAt(index: WorkflowTaskIndex) =
        steps.any { it.isTerminatedAt(index) }

    override fun checkAwaitIndex() {
      steps.map { it.checkAwaitIndex() }
    }

    override fun nextAwaitIndex() {
      steps.map { it.nextAwaitIndex() }
    }

    override fun statusAt(index: WorkflowTaskIndex): StepStatus {
      val statuses = steps.map { it.statusAt(index) }

      // return first completed status (if it exists)
      statuses.filterIsInstance<StepStatus.Completed>().minByOrNull {
        it.completionWorkflowTaskIndex
      }?.let {
        return it
      }

      // if at least one step is ongoing, then Or(...steps) is ongoing
      if (statuses.any { it is StepStatus.Waiting }) return StepStatus.Waiting

      // all steps are neither completed, neither ongoing => canceled, failed based on last one
      val lastTerminated = statuses.maxByOrNull {
        when (it) {
          is StepStatus.Canceled -> it.cancellationWorkflowTaskIndex
          is StepStatus.Unknown -> it.unknowingWorkflowTaskIndex
          is StepStatus.CurrentlyFailed -> it.failureWorkflowTaskIndex
          is StepStatus.CurrentlyTimedOut -> it.timeoutWorkflowTaskIndex
          is StepStatus.Completed,
          is StepStatus.Failed,
          is StepStatus.TimedOut,
          is StepStatus.Waiting -> thisShouldNotHappen()
        }
      }
      if (lastTerminated != null) return lastTerminated

      thisShouldNotHappen()
    }

    override fun valueCompletedFn(): Any? {
      when (val orStatus = status()) {
        is StepStatus.Completed -> {
          // retrieve first completed step
          val completedStep = steps.first { it.status() == orStatus }
          // return value of first completed step
          return completedStep.valueCompletedFn()
        }

        else -> thisShouldNotHappen()
      }
    }
  }

  /** Used in engine to update a step after having cancelled or completed a command */
  fun updateWith(
    commandId: CommandId,
    commandStatus: CommandStatus,
    commandStatuses: List<CommandStatus>? = null
  ): Step {
    when (this) {
      is Id -> if (this.commandId == commandId) {
        this.commandStatus = when (commandStatuses) {
          null -> commandStatus
          else -> when (awaitIndex) {
            0 -> commandStatus
            else -> commandStatuses.firstOrNull {
              it is Completed && it.returnIndex == awaitIndex
            } ?: thisShouldNotHappen()
          }
        }
      }

      is And -> steps = steps.map { it.updateWith(commandId, commandStatus, commandStatuses) }
      is Or -> steps = steps.map { it.updateWith(commandId, commandStatus, commandStatuses) }
    }
    return this.resolveOr().compose()
  }

  private fun resolveOr(): Step {
    when (this) {
      is Id -> Unit
      is And -> steps = steps.map { it.resolveOr() }
      is Or -> steps = when (isTerminated()) {
        true -> listOf(steps.first { it.isTerminated() }.resolveOr())
        false -> steps.map { s -> s.resolveOr() }
      }
    }
    return this
  }

  private fun compose(): Step {
    when (this) {
      is Id -> Unit

      is And -> while (steps.any { it is And || (it is Or && it.steps.count() == 1) }) {
        steps = steps.fold(mutableListOf()) { l, s ->
          when (s) {
            is Id -> l.add(s)
            is And -> l.addAll(s.steps)
            is Or -> if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s)
          }
          return@fold l
        }
      }

      is Or -> while (steps.any { it is Or || (it is And && it.steps.count() == 1) }) {
        steps = steps.fold(mutableListOf()) { l, s ->
          when (s) {
            is Id -> l.add(s)
            is And -> if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s)
            is Or -> l.addAll(s.steps)
          }
          return@fold l
        }
      }
    }
    return this
  }
}
