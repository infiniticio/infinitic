<template>
  <div>
    <h3 class="text-lg leading-6 font-medium text-gray-900">
      {{ taskType.name }}
    </h3>
    <div class="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-3">
      <simple-stat label="OK" :value="runningOkCount" />
      <simple-stat label="Warning" :value="runningWarningCount" />
      <simple-stat label="Error" :value="runningErrorCount" />
    </div>
  </div>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import { from, Observable } from "rxjs";
import { pluck, share, map } from "rxjs/operators";
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
    const metrics: Observable<TaskTypeMetrics> = from(
      getTaskTypeMetrics(this.$props.taskType)
    ).pipe(share());

    return {
      runningOkCount: metrics.pipe(
        pluck("runningOkCount"),
        map(value => value.toString())
      ),
      runningWarningCount: metrics.pipe(
        pluck("runningWarningCount"),
        map(value => value.toString())
      ),
      runningErrorCount: metrics.pipe(
        pluck("runningErrorCount"),
        map(value => value.toString())
      )
    } as {
      runningOkCount: Observable<string>;
      runningWarningCount: Observable<string>;
      runningErrorCount: Observable<string>;
    };
  }
});
</script>
