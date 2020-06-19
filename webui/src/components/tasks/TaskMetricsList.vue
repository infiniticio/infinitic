<template>
  <div>
    <div class="my-6" v-for="taskType in taskTypes" :key="taskType.name">
      <task-metrics-item :taskType="taskType" />
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import TaskMetricsItem from "./TaskMetricsItem.vue";

export default Vue.extend({
  props: {
    taskTypes: {
      type: Array,
      required: true
    }
  },

  components: { TaskMetricsItem }
});
</script>
