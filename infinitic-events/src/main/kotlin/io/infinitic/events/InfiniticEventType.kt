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

package io.infinitic.events

private const val TYPE_DOMAIN = "io.infinitic"
private const val TYPE_TASK = "$TYPE_DOMAIN.task"

enum class InfiniticEventType(val type: String) {
  TASK_DISPATCHED(
      type = "$TYPE_TASK.dispatched",
  ),
  TASK_STARTED(
      type = "$TYPE_TASK.started",
  ),
  TASK_COMPLETED(
      type = "$TYPE_TASK.completed",
  ),
  TASK_FAILED(
      type = "$TYPE_TASK.failed",
  ),
  TASK_RETRIED(
      type = "$TYPE_TASK.retried",
  );
}
