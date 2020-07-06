import { Server } from "miragejs";
import { TaskType, TaskTypeMetrics, Task } from "./tasks";
import { v4 as uuidv4 } from "uuid";

new Server({
  routes() {
    this.logging = true;

    this.get(
      "/task-types/",
      (): Array<TaskType> => {
        return [
          {
            name: "RefundBooking"
          }
        ];
      }
    );

    this.get(
      "/task-types/:name/metrics",
      (_schema, request): TaskTypeMetrics => {
        return {
          name: request.params.name,
          runningOkCount: 123,
          runningWarningCount: 11,
          runningErrorCount: 2,
          terminatedCompletedCount: 9876,
          terminatedCanceledCount: 321
        };
      }
    );

    this.get(
      "/tasks/:id",
      (_schema, request): Task => {
        const now = new Date();
        const dispatchedAt = new Date(now);
        dispatchedAt.setSeconds(dispatchedAt.getSeconds() - 10);
        const startedAt = new Date(now);
        startedAt.setSeconds(startedAt.getSeconds() - 8);
        const completedAt = new Date(now);

        return {
          id: request.params.id,
          name: "RefundBooking",
          status: "ok",
          dispatchedAt: dispatchedAt.toISOString(),
          startedAt: startedAt.toISOString(),
          completedAt: completedAt.toISOString(),
          failedAt: null,
          attempts: [
            {
              id: uuidv4(),
              index: 0,
              tries: [
                {
                  id: uuidv4(),
                  retry: 0,
                  dispatchedAt: dispatchedAt.toISOString(),
                  startedAt: startedAt.toISOString(),
                  completedAt: completedAt.toISOString(),
                  failedAt: null,
                  delayBeforeRetry: null
                }
              ]
            }
          ]
        };
      }
    );
  }
});
