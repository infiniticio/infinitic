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

import { createAxiosClient } from "./axios";

type WorkflowName = string;

export * from "./tasks";

interface WorkflowStartedMetric {
  readonly value: number;
  readonly previousValue: number;
}

interface WorkflowCompletedMetric {
  readonly value: number;
  readonly previousValue: number;
}

interface WorkflowErrorRateMetric {
  readonly value: number;
  readonly previousValue: number;
}

interface WorkflowMetrics {
  readonly workflowName: WorkflowName;
  readonly metrics: [
    WorkflowStartedMetric,
    WorkflowCompletedMetric,
    WorkflowErrorRateMetric
  ];
}

interface Info {
  readonly version: string;
}

export async function isValidApiUrl(url: string): Promise<boolean> {
  const client = createAxiosClient({ baseURL: url });

  try {
    await client.get<Info>("/info");

    return true;
  } catch (error) {
    return false;
  }
}

export function getAllWorkflowMetrics(): WorkflowMetrics[] {
  return [
    {
      workflowName: "SequentialWorkflow",
      metrics: [
        { value: 71897, previousValue: 70946 },
        { value: 70946, previousValue: 71897 },
        { value: 28.62, previousValue: 26.14 }
      ]
    },
    {
      workflowName: "AsyncWorkflow",
      metrics: [
        { value: 71897, previousValue: 70946 },
        { value: 71897, previousValue: 70946 },
        { value: 28.62, previousValue: 26.14 }
      ]
    },
    {
      workflowName: "ParallelWorkflow",
      metrics: [
        { value: 71897, previousValue: 70946 },
        { value: 70946, previousValue: 71897 },
        { value: 26.14, previousValue: 28.62 }
      ]
    }
  ];
}
