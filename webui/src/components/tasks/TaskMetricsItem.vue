<template>
  <div>
    <h3 class="text-lg leading-6 font-medium text-gray-900">
      {{ taskType.name }}
    </h3>
    <div class="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-3">
      <div class="bg-white overflow-hidden shadow rounded-lg">
        <div class="px-4 py-5 sm:p-6">
          <div v-if="loading">
            Loading
          </div>
          <dl v-else-if="!loading && error">
            Error
          </dl>
          <dl v-else>
            <dt class="text-sm leading-5 font-medium text-gray-500 truncate">
              OK
            </dt>
            <dd class="mt-1 text-3xl leading-9 font-semibold text-gray-900">
              {{ metrics.runningOkCount }}
            </dd>
          </dl>
        </div>
      </div>
      <div class="bg-white overflow-hidden shadow rounded-lg">
        <div class="px-4 py-5 sm:p-6">
          <div v-if="loading">
            Loading
          </div>
          <dl v-else-if="!loading && error">
            Error
          </dl>
          <dl v-else>
            <dt class="text-sm leading-5 font-medium text-gray-500 truncate">
              Warning
            </dt>
            <dd class="mt-1 text-3xl leading-9 font-semibold text-gray-900">
              {{ metrics.runningWarningCount }}
            </dd>
          </dl>
        </div>
      </div>
      <div class="bg-white overflow-hidden shadow rounded-lg">
        <div class="px-4 py-5 sm:p-6">
          <div v-if="loading">
            Loading
          </div>
          <dl v-else-if="!loading && error">
            Error
          </dl>
          <dl v-else>
            <dt class="text-sm leading-5 font-medium text-gray-500 truncate">
              Error
            </dt>
            <dd class="mt-1 text-3xl leading-9 font-semibold text-gray-900">
              {{ metrics.runningErrorCount }}
            </dd>
          </dl>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import { TaskType, getTaskTypeMetrics, TaskTypeMetrics } from "@/api";

export default Vue.extend({
  props: {
    taskType: {
      type: Object as PropType<TaskType>,
      required: true
    }
  },

  data() {
    return {
      loading: false,
      error: undefined,
      metrics: undefined
    } as {
      loading: boolean;
      error: Error | undefined;
      metrics: TaskTypeMetrics | undefined;
    };
  },

  created() {
    this.loadData();
  },

  methods: {
    async loadData() {
      if (this.loading) {
        return;
      }

      this.loading = true;

      try {
        this.metrics = await getTaskTypeMetrics(this.taskType);
      } catch (error) {
        this.error = error;
      } finally {
        this.loading = false;
      }
    }
  }
});
</script>
