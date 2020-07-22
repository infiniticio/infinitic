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
        <pulse-loader
          v-if="loading"
          class="mt-5 text-center"
          color="#6875F5"
          :loading="true"
        />
        <div v-else-if="error">
          <h3
            v-if="isNotFound(error)"
            class="text-4xl leading-6 text-center font-medium text-gray-600 mt-12"
          >
            There is no task with identifier {{ id }}.
          </h3>
          <server-error v-else />
        </div>
        <div v-else-if="task">
          <div class="bg-white shadow overflow-hidden  sm:rounded-lg">
            <div class="px-4 py-5 border-b border-gray-200 sm:px-6">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ task.name }}
              </h3>
              <p class="mt-1 max-w-2xl text-sm leading-5 text-gray-500">
                {{ task.id }}
              </p>
            </div>
            <div class="px-4 py-5 sm:p-0">
              <dl>
                <div class="sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6 sm:py-5">
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Status
                  </dt>
                  <dd
                    class="mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    {{ task.status }}
                  </dd>
                </div>
                <div
                  class="mt-8 sm:mt-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:border-t sm:border-gray-200 sm:px-6 sm:py-5"
                >
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Dispatch date
                  </dt>
                  <dd
                    class="mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    {{ formatDate(task.dispatchedAt) }}
                  </dd>
                </div>
                <div
                  class="mt-8 sm:mt-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:border-t sm:border-gray-200 sm:px-6 sm:py-5"
                >
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Start date
                  </dt>
                  <dd
                    class="mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    {{ formatDate(task.startedAt) }}
                  </dd>
                </div>
                <div
                  v-if="task.status !== 'error'"
                  class="mt-8 sm:mt-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:border-t sm:border-gray-200 sm:px-6 sm:py-5"
                >
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Completion date
                  </dt>
                  <dd
                    class="mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    {{ formatDate(task.completedAt) }}
                  </dd>
                </div>
                <div
                  v-if="task.status === 'error'"
                  class="mt-8 sm:mt-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:border-t sm:border-gray-200 sm:px-6 sm:py-5"
                >
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Failure date
                  </dt>
                  <dd
                    class="mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    {{ formatDate(task.failedAt) }}
                  </dd>
                </div>
                <div
                  class="mt-8 sm:mt-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:border-t sm:border-gray-200 sm:px-6 sm:py-5"
                >
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Attempts
                  </dt>
                  <dd
                    class="flex flex-col mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    <div
                      class="relative shadow w-full h-5 bg-gray-100"
                      v-for="(attempt, index) in task.attempts"
                      :key="attempt.id"
                      :class="{ 'mt-5': index > 0 }"
                    >
                      <div
                        v-for="attemptTry in attempt.tries"
                        :key="'progress-' + attemptTry.retry"
                        :class="{
                          'bg-blue-400':
                            !attemptTry.failedAt && !attemptTry.completedAt,
                          'bg-red-400': !!attemptTry.failedAt,
                          'bg-green-400': !!attemptTry.completedAt
                        }"
                        class="absolute inline-block h-5 text-xs leading-none py-1 text-center text-white"
                        :style="{
                          width: `${segmentLength(task, attemptTry)}%`,
                          left: `${segmentOffset(task, attemptTry)}%`
                        }"
                      ></div>
                    </div>
                  </dd>
                </div>
              </dl>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import { getTaskDetails, Task, TaskAttemptTry } from "@/api";
import { PulseLoader } from "@saeris/vue-spinners";
import ServerError from "@/components/errors/ServerError.vue";
import { NotFoundError } from "@/api/errors";

export default Vue.extend({
  name: "TaskDetails",

  components: { PulseLoader, ServerError },

  props: {
    id: {
      type: String as PropType<string>,
      required: true
    }
  },

  data() {
    return {
      loading: false,
      task: undefined,
      error: undefined,
      searchInput: this.$props.id,
      dateFormatter: new Intl.DateTimeFormat([...navigator.languages], {
        year: "numeric",
        month: "numeric",
        day: "numeric",
        hour: "numeric",
        minute: "numeric",
        second: "numeric",
        timeZoneName: "short"
      })
    } as {
      loading: boolean;
      task: Task | undefined;
      error: Error | undefined;
      searchInput: string;
      dateFormatter: Intl.DateTimeFormat;
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
        this.task = await getTaskDetails(this.id);
      } catch (err) {
        this.error = err;
      } finally {
        this.loading = false;
      }
    },

    async searchTask() {
      if (this.searchInput === "") {
        this.$router.push({
          name: "Tasks"
        });
      } else {
        this.$router.push({
          name: "TaskDetails",
          params: { id: this.searchInput }
        });
      }
    },

    isNotFound(error: Error) {
      return error instanceof NotFoundError;
    },

    formatDate(date: string | null): string {
      if (date === null) {
        return "-";
      }

      const dateObject = new Date(date);

      return this.dateFormatter.format(dateObject);
    },

    barOriginDate(task: Task) {
      return new Date(task.dispatchedAt);
    },

    segmentOffset(task: Task, attemptTry: TaskAttemptTry) {
      if (attemptTry.startedAt === null) {
        return 0;
      }

      const taskEndDate = task.completedAt
        ? new Date(task.completedAt)
        : new Date();
      const taskStartDate = this.barOriginDate(task);
      const segmentStartDate = new Date(attemptTry.startedAt);

      return Math.abs(
        ((segmentStartDate.getTime() - taskStartDate.getTime()) * 100) /
          (taskEndDate.getTime() - taskStartDate.getTime())
      );
    },

    segmentLength(task: Task, attemptTry: TaskAttemptTry) {
      if (attemptTry.startedAt === null) {
        return 0;
      }

      const taskEndDate = task.completedAt
        ? new Date(task.completedAt)
        : new Date();

      const taskStartDate = this.barOriginDate(task);

      const segmentEndDate = attemptTry.completedAt
        ? new Date(attemptTry.completedAt)
        : attemptTry.failedAt
        ? new Date(attemptTry.failedAt)
        : new Date();
      const segmentStartDate = new Date(attemptTry.startedAt);

      return Math.abs(
        ((segmentEndDate.getTime() - segmentStartDate.getTime()) * 100) /
          (taskEndDate.getTime() - taskStartDate.getTime())
      );
    }
  }
});
</script>
