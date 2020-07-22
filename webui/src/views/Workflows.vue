<template>
  <sidebar-layout>
    <main
      class="flex-1 relative z-0 overflow-y-auto pt-2 pb-6 focus:outline-none md:py-6"
      tabindex="0"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
        <div
          class="my-6"
          v-for="workflowMetric in workflowMetrics"
          :key="workflowMetric.workflowName"
        >
          <h3 class="text-lg leading-6 font-medium text-gray-900">
            {{ workflowMetric.workflowName }}
          </h3>
          <div
            class="mt-5 grid grid-cols-1 rounded-lg bg-white overflow-hidden shadow md:grid-cols-3"
          >
            <div class="px-4 py-5 sm:p-6">
              <dl>
                <dt class="text-base leading-6 font-normal text-gray-900">
                  Started
                </dt>
                <dd
                  class="mt-1 flex justify-between items-baseline md:block lg:flex"
                >
                  <div
                    class="flex items-baseline text-2xl leading-8 font-semibold text-indigo-600"
                  >
                    {{ workflowMetric.metrics[0].value }}
                    <span
                      class="ml-2 text-sm leading-5 font-medium text-gray-500"
                      >from {{ workflowMetric.metrics[0].previousValue }}</span
                    >
                  </div>
                  <div
                    v-if="
                      workflowMetric.metrics[0].value >=
                        workflowMetric.metrics[0].previousValue
                    "
                    class="inline-flex items-baseline px-2.5 py-0.5 rounded-full text-sm font-medium leading-5 bg-green-100 text-green-800 md:mt-2 lg:mt-0"
                  >
                    <svg
                      class="-ml-1 mr-0.5 flex-shrink-0 self-center h-5 w-5 text-green-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fill-rule="evenodd"
                        d="M5.293 9.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 7.414V15a1 1 0 11-2 0V7.414L6.707 9.707a1 1 0 01-1.414 0z"
                        clip-rule="evenodd"
                      />
                    </svg>
                    <span class="sr-only">Increased by</span>
                    {{
                      (
                        (workflowMetric.metrics[0].value * 100) /
                          workflowMetric.metrics[0].previousValue -
                        100
                      ).toFixed(2)
                    }}%
                  </div>
                  <div
                    v-else
                    class="inline-flex items-baseline px-2.5 py-0.5 rounded-full text-sm font-medium leading-5 bg-red-100 text-red-800 md:mt-2 lg:mt-0"
                  >
                    <svg
                      class="-ml-1 mr-0.5 flex-shrink-0 self-center h-5 w-5 text-red-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fill-rule="evenodd"
                        d="M14.707 10.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 12.586V5a1 1 0 012 0v7.586l2.293-2.293a1 1 0 011.414 0z"
                        clip-rule="evenodd"
                      />
                    </svg>
                    <span class="sr-only">Decreased by</span>
                    {{
                      (
                        100 -
                        (workflowMetric.metrics[0].value * 100) /
                          workflowMetric.metrics[0].previousValue
                      ).toFixed(2)
                    }}%
                  </div>
                </dd>
              </dl>
            </div>
            <div class="border-t border-gray-200 md:border-0 md:border-l">
              <div class="px-4 py-5 sm:p-6">
                <dl>
                  <dt class="text-base leading-6 font-normal text-gray-900">
                    Completed
                  </dt>
                  <dd
                    class="mt-1 flex justify-between items-baseline md:block lg:flex"
                  >
                    <div
                      class="flex items-baseline text-2xl leading-8 font-semibold text-indigo-600"
                    >
                      {{ workflowMetric.metrics[1].value }}
                      <span
                        class="ml-2 text-sm leading-5 font-medium text-gray-500"
                        >from
                        {{ workflowMetric.metrics[1].previousValue }}</span
                      >
                    </div>
                    <div
                      v-if="
                        workflowMetric.metrics[1].value >=
                          workflowMetric.metrics[1].previousValue
                      "
                      class="inline-flex items-baseline px-2.5 py-0.5 rounded-full text-sm font-medium leading-5 bg-green-100 text-green-800 md:mt-2 lg:mt-0"
                    >
                      <svg
                        class="-ml-1 mr-0.5 flex-shrink-0 self-center h-5 w-5 text-green-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fill-rule="evenodd"
                          d="M5.293 9.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 7.414V15a1 1 0 11-2 0V7.414L6.707 9.707a1 1 0 01-1.414 0z"
                          clip-rule="evenodd"
                        />
                      </svg>
                      <span class="sr-only">Increased by</span>
                      {{
                        (
                          (workflowMetric.metrics[1].value * 100) /
                            workflowMetric.metrics[1].previousValue -
                          100
                        ).toFixed(2)
                      }}%
                    </div>
                    <div
                      v-else
                      class="inline-flex items-baseline px-2.5 py-0.5 rounded-full text-sm font-medium leading-5 bg-red-100 text-red-800 md:mt-2 lg:mt-0"
                    >
                      <svg
                        class="-ml-1 mr-0.5 flex-shrink-0 self-center h-5 w-5 text-red-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fill-rule="evenodd"
                          d="M14.707 10.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 12.586V5a1 1 0 012 0v7.586l2.293-2.293a1 1 0 011.414 0z"
                          clip-rule="evenodd"
                        />
                      </svg>
                      <span class="sr-only">Decreased by</span>
                      {{
                        (
                          100 -
                          (workflowMetric.metrics[1].value * 100) /
                            workflowMetric.metrics[1].previousValue
                        ).toFixed(2)
                      }}%
                    </div>
                  </dd>
                </dl>
              </div>
            </div>
            <div class="border-t border-gray-200 md:border-0 md:border-l">
              <div class="px-4 py-5 sm:p-6">
                <dl>
                  <dt class="text-base leading-6 font-normal text-gray-900">
                    Error rate
                  </dt>
                  <dd
                    class="mt-1 flex justify-between items-baseline md:block lg:flex"
                  >
                    <div
                      class="flex items-baseline text-2xl leading-8 font-semibold text-indigo-600"
                    >
                      {{ workflowMetric.metrics[2].value }}%
                      <span
                        class="ml-2 text-sm leading-5 font-medium text-gray-500"
                        >from
                        {{ workflowMetric.metrics[2].previousValue }}</span
                      >
                    </div>
                    <div
                      v-if="
                        workflowMetric.metrics[2].value <=
                          workflowMetric.metrics[2].previousValue
                      "
                      class="inline-flex items-baseline px-2.5 py-0.5 rounded-full text-sm font-medium leading-5 bg-green-100 text-green-800 md:mt-2 lg:mt-0"
                    >
                      <svg
                        class="-ml-1 mr-0.5 flex-shrink-0 self-center h-5 w-5 text-green-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fill-rule="evenodd"
                          d="M14.707 10.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 12.586V5a1 1 0 012 0v7.586l2.293-2.293a1 1 0 011.414 0z"
                          clip-rule="evenodd"
                        />
                      </svg>
                      <span class="sr-only">Decreased by</span>
                      {{
                        (
                          (workflowMetric.metrics[2].value * 100) /
                            workflowMetric.metrics[2].previousValue -
                          100
                        ).toFixed(2)
                      }}%
                    </div>
                    <div
                      v-else
                      class="inline-flex items-baseline px-2.5 py-0.5 rounded-full text-sm font-medium leading-5 bg-red-100 text-red-800 md:mt-2 lg:mt-0"
                    >
                      <svg
                        class="-ml-1 mr-0.5 flex-shrink-0 self-center h-5 w-5 text-red-500"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fill-rule="evenodd"
                          d="M5.293 9.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 7.414V15a1 1 0 11-2 0V7.414L6.707 9.707a1 1 0 01-1.414 0z"
                          clip-rule="evenodd"
                        />
                      </svg>
                      <span class="sr-only">Increased by</span>
                      {{
                        (
                          100 -
                          (workflowMetric.metrics[2].value * 100) /
                            workflowMetric.metrics[2].previousValue
                        ).toFixed(2)
                      }}%
                    </div>
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </sidebar-layout>
</template>

<script lang="ts">
import Vue from "vue";
import { getAllWorkflowMetrics } from "@/api";
import SidebarLayout from "@/components/layouts/SidebarLayout.vue";

export default Vue.extend({
  name: "Workflows",

  components: { SidebarLayout },

  data: function() {
    return {
      workflowMetrics: getAllWorkflowMetrics()
    };
  }
});
</script>
