<template>
  <div>
    <h3 class="text-lg leading-6 font-medium text-gray-900">
      {{ taskType.name }}
    </h3>
    <div class="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-3">
      <simple-stat
        label="OK"
        :loading="loading"
        :value="runningOkCount"
        :error="error"
      />
      <simple-stat
        label="Warning"
        :loading="loading"
        :value="runningWarningCount"
        :error="error"
      />
      <simple-stat
        label="Error"
        :loading="loading"
        :value="runningErrorCount"
        :error="error"
      />
    </div>
  </div>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import { TaskType, getTaskTypeMetrics, TaskTypeMetrics } from "@/api";
import SimpleStat from "@/components/data-display/stats/SimpleStat.vue";

export default Vue.extend({
  components: { SimpleStat },

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

  computed: {
    runningOkCount(): string | undefined {
      return this.metrics ? this.metrics.runningOkCount.toString() : undefined;
    },

    runningWarningCount(): string | undefined {
      return this.metrics
        ? this.metrics.runningWarningCount.toString()
        : undefined;
    },

    runningErrorCount(): string | undefined {
      return this.metrics
        ? this.metrics.runningErrorCount.toString()
        : undefined;
    }
  },

  methods: {
    async loadData() {
      if (this.loading) {
        return;
      }

      this.loading = true;

      try {
        this.metrics = await getTaskTypeMetrics(this.taskType);
      } catch (err) {
        this.error = err;
      } finally {
        this.loading = false;
      }
    }
  }
});
</script>
