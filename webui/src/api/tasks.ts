import axios from "axios";
import { GenericError, NotFoundError } from "./errors";
import { isAxiosError } from "./axios";

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
    const result = await axios.get<TaskType[]>(
      "http://localhost:3010/task-types/"
    );

    return result.data;
  } catch (e) {
    throw new GenericError();
  }
}

export async function getTaskTypeMetrics(
  taskType: TaskType
): Promise<TaskTypeMetrics> {
  try {
    const result = await axios.get<TaskTypeMetrics>(
      `http://localhost:3010/task-types/${taskType.name}/metrics`
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
    const result = await axios.get<Task>(`http://localhost:3010/tasks/${id}`);

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
