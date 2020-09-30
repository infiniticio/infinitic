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

import { Server } from "miragejs";
import { TaskType, TaskTypeMetrics, Task } from "./tasks";
import { v4 as uuidv4 } from "uuid";

new Server({
  routes() {
    this.logging = true;

    this.get("/info", () => {
      return {
        version: "0.1.0"
      };
    });

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
