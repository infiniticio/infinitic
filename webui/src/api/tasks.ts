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

import { GenericError, NotFoundError } from "./errors";
import { axiosClient, isAxiosError } from "./axios";

export type TaskStatus = "ok" | "warning" | "error";

export interface Task {
  id: string;
  name: string;
  status: TaskStatus;
  dispatchedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  attempts: Array<TaskAttempt>;
}

export interface TaskAttempt {
  id: string;
  index: number;
  tries: Array<TaskAttemptTry>;
}

export interface TaskAttemptTry {
  id: string;
  retry: number;
  dispatchedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  delayBeforeRetry: number | null;
}

export interface TaskType {
  name: string;
}

export interface TaskTypeMetrics {
  name: string;
  runningOkCount: number;
  runningWarningCount: number;
  runningErrorCount: number;
  terminatedCompletedCount: number;
  terminatedCanceledCount: number;
}

export async function getTaskTypes(): Promise<TaskType[]> {
  try {
    const result = await axiosClient.get<TaskType[]>("/task-types/");

    return result.data;
  } catch (e) {
    throw new GenericError();
  }
}

export async function getTaskTypeMetrics(
  taskType: TaskType
): Promise<TaskTypeMetrics> {
  try {
    const result = await axiosClient.get<TaskTypeMetrics>(
      `/task-types/${taskType.name}/metrics`
    );

    return result.data;
  } catch (error) {
    if (isAxiosError(error)) {
      throw GenericError.withMessage(error.message);
    } else {
      throw new GenericError();
    }
  }
}

export async function getTaskDetails(id: Task["id"]) {
  try {
    const result = await axiosClient.get<Task>(`/tasks/${id}`);

    return result.data;
  } catch (error) {
    if (isAxiosError(error)) {
      if (error.response?.status === 404) {
        throw new NotFoundError(
          `Unable to retrieve task information for task ${id}.`
        );
      }
    }

    throw error;
  }
}
