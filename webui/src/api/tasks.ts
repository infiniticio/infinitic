import axios from "axios";
import { GenericError } from "./errors";
import { isAxiosError } from "./axios";

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
