<template>
  <div>
    <div class="relative z-10 flex-shrink-0 flex h-16 bg-white shadow">
      <div class="flex-1 px-4 flex justify-between">
        <div class="flex-1 flex">
          <div class="w-full flex md:ml-0">
            <label for="search_field" class="sr-only">Search </label>
            <div
              class="relative w-full text-gray-400 focus-within:text-gray-600"
            >
              <div
                class="absolute inset-y-0 left-0 flex items-center pointer-events-none"
              >
                <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                  <path
                    fill-rule="evenodd"
                    clip-rule="evenodd"
                    d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                  />
                </svg>
              </div>
              <input
                id="search_field"
                class="block w-full h-full pl-8 pr-3 py-2 rounded-md text-gray-900 placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 sm:text-sm"
                placeholder="Search"
                type="search"
                v-model="searchInput"
                @keypress.enter="searchTask()"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <main
      class="flex-1 relative z-0 overflow-y-auto pt-2 pb-6 focus:outline-none md:py-6"
      tabindex="0"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
        <div v-if="loading$">
          Loading
        </div>
        <div v-else-if="error$">
          Display error
        </div>
        <div v-else>
          <task-metrics-list :taskTypes="taskTypes$" />
        </div>
      </div>
    </main>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { getTaskTypes } from "@/api";
import TaskMetricsList from "@/components/tasks/TaskMetricsList.vue";
import { from, of } from "rxjs";
import { share, mapTo, startWith, catchError } from "rxjs/operators";

export default Vue.extend({
  name: "Tasks",

  components: { TaskMetricsList },

  data() {
    return {
      searchInput: ""
    } as {
      searchInput: string;
    };
  },

  subscriptions() {
    const taskTypes = from(getTaskTypes()).pipe(share());

    return {
      loading$: taskTypes.pipe(mapTo(false), startWith(true)),
      error$: taskTypes.pipe(
        mapTo(undefined),
        catchError(err => of<Error>(err))
      ),
      taskTypes$: taskTypes
    };
  },

  methods: {
    async searchTask() {
      console.log(this.searchInput);
    }
  }
});
</script>
