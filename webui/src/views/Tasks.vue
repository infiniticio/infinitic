<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
    <div v-if="loading">
      Loading
    </div>
    <div v-else-if="error">
      Display error
    </div>
    <div v-else>
      <task-metrics-list :taskTypes="taskTypes" />
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { getTaskTypes, TaskType } from "@/api";
import TaskMetricsList from "../components/tasks/TaskMetricsList.vue";

export default Vue.extend({
  name: "Tasks",

  components: { TaskMetricsList },

  data() {
    return {
      loading: false,
      taskTypes: undefined,
      error: undefined
    } as {
      loading: boolean;
      taskTypes: TaskType[] | undefined;
      error: Error | undefined;
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
        this.taskTypes = await getTaskTypes();
      } catch (e) {
        this.error = e;
      } finally {
        this.loading = false;
      }
    }
  }
});
</script>
