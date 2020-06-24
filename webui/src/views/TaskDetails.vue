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
          v-if="loading$"
          class="mt-5 text-center"
          color="#6875F5"
          :loading="true"
        />
        <div v-else-if="error$">
          {{ error$ }}
        </div>
        <div v-else-if="taskDetails$">
          <div class="bg-white shadow overflow-hidden  sm:rounded-lg">
            <div class="px-4 py-5 border-b border-gray-200 sm:px-6">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ taskDetails$.name }}
              </h3>
              <p class="mt-1 max-w-2xl text-sm leading-5 text-gray-500">
                {{ taskDetails$.id }}
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
                    {{ taskDetails$.status }}
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
                    {{ formatDate(taskDetails$.dispatchedAt) }}
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
                    {{ formatDate(taskDetails$.startedAt) }}
                  </dd>
                </div>
                <div
                  v-if="taskDetails$.status !== 'error'"
                  class="mt-8 sm:mt-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:border-t sm:border-gray-200 sm:px-6 sm:py-5"
                >
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Completion date
                  </dt>
                  <dd
                    class="mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    {{ formatDate(taskDetails$.completedAt) }}
                  </dd>
                </div>
                <div
                  v-if="taskDetails$.status === 'error'"
                  class="mt-8 sm:mt-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:border-t sm:border-gray-200 sm:px-6 sm:py-5"
                >
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Failure date
                  </dt>
                  <dd
                    class="mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    {{ formatDate(taskDetails$.failedAt) }}
                  </dd>
                </div>
                <div
                  class="mt-8 sm:mt-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:border-t sm:border-gray-200 sm:px-6 sm:py-5"
                >
                  <dt class="text-sm leading-5 font-medium text-gray-500">
                    Attempts
                  </dt>
                  <dd
                    class="mt-1 text-sm leading-5 text-gray-900 sm:mt-0 sm:col-span-2"
                  >
                    <div
                      class="shadow w-full bg-grey-light"
                      v-for="attempt in taskDetails$.attempts"
                      :key="attempt.id"
                    >
                      <div
                        class="bg-blue-400 text-xs leading-none py-1 text-center text-white"
                        style="width: 45%"
                      >
                        45%
                      </div>
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
import { getTaskDetails } from "@/api";
import { from, of } from "rxjs";
import {
  share,
  mapTo,
  startWith,
  catchError,
  tap,
  onErrorResumeNext
} from "rxjs/operators";
import { PulseLoader } from "@saeris/vue-spinners";

export default Vue.extend({
  name: "TaskDetails",

  components: { PulseLoader },

  props: {
    id: {
      type: String as PropType<string>,
      required: true
    }
  },

  data() {
    return {
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
      searchInput: string;
      dateFormatter: Intl.DateTimeFormat;
    };
  },

  subscriptions() {
    const taskDetails = from(getTaskDetails(this.$props.id)).pipe(share());

    return {
      loading$: taskDetails.pipe(
        mapTo(false),
        catchError(() => of(false)),
        startWith(true)
      ),
      error$: taskDetails.pipe(
        mapTo(undefined),
        catchError(err => of<Error>(err))
      ),
      taskDetails$: taskDetails.pipe(catchError(() => of(undefined)))
    };
  },

  methods: {
    formatDate(date: string | null): string {
      if (date === null) {
        return "-";
      }

      const dateObject = new Date(date);

      return this.dateFormatter.format(dateObject);
    },

    async searchTask() {
      this.$router.push({
        name: "TaskDetails",
        params: { id: this.searchInput }
      });
    }
  }
});
</script>
