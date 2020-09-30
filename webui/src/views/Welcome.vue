<!--
 "Commons Clause" License Condition v1.0

 The Software is provided to you by the Licensor under the License, as defined
 below, subject to the following condition.

 Without limiting other conditions in the License, the grant of rights under the
 License will not include, and the License does not grant to you, the right to
 Sell the Software.

 For purposes of the foregoing, “Sell” means practicing any or all of the rights
 granted to you under the License to provide to third parties, for a fee or
 other consideration (including without limitation fees for hosting or
 consulting/ support services related to the Software), a product or service
 whose value derives, entirely or substantially, from the functionality of the
 Software. Any license notice or attribution required by the License must also
 include this Commons Clause License Condition notice.

 Software: Infinitic

 License: MIT License (https://opensource.org/licenses/MIT)

 Licensor: infinitic.io
-->

<template>
  <div class="h-screen relative bg-gray-50 overflow-hidden">
    <div class="hidden sm:block sm:absolute sm:inset-y-0 sm:h-full sm:w-full">
      <div class="relative h-full max-w-screen-xl mx-auto">
        <svg
          class="absolute right-full transform translate-y-1/4 translate-x-1/4 lg:translate-x-1/2"
          width="404"
          height="784"
          fill="none"
          viewBox="0 0 404 784"
        >
          <defs>
            <pattern
              id="f210dbf6-a58d-4871-961e-36d5016a0f49"
              x="0"
              y="0"
              width="20"
              height="20"
              patternUnits="userSpaceOnUse"
            >
              <rect
                x="0"
                y="0"
                width="4"
                height="4"
                class="text-gray-200"
                fill="currentColor"
              />
            </pattern>
          </defs>
          <rect
            width="404"
            height="784"
            fill="url(#f210dbf6-a58d-4871-961e-36d5016a0f49)"
          />
        </svg>
        <svg
          class="absolute left-full transform -translate-y-3/4 -translate-x-1/4 md:-translate-y-1/2 lg:-translate-x-1/2"
          width="404"
          height="784"
          fill="none"
          viewBox="0 0 404 784"
        >
          <defs>
            <pattern
              id="5d0dd344-b041-4d26-bec4-8d33ea57ec9b"
              x="0"
              y="0"
              width="20"
              height="20"
              patternUnits="userSpaceOnUse"
            >
              <rect
                x="0"
                y="0"
                width="4"
                height="4"
                class="text-gray-200"
                fill="currentColor"
              />
            </pattern>
          </defs>
          <rect
            width="404"
            height="784"
            fill="url(#5d0dd344-b041-4d26-bec4-8d33ea57ec9b)"
          />
        </svg>
      </div>
    </div>

    <div class="relative pt-6 pb-12 sm:pb-16 md:pb-20 lg:pb-28 xl:pb-32">
      <main
        class="mt-10 mx-auto max-w-screen-xl px-4 sm:mt-12 sm:px-6 md:mt-16 lg:mt-20 xl:mt-28"
      >
        <div class="text-center">
          <h2
            class="text-4xl tracking-tight leading-10 font-extrabold text-gray-900 sm:text-5xl sm:leading-none md:text-6xl"
          >
            Welcome to
            <br class="xl:hidden" />
            <span class="text-indigo-600">infinitic</span>
          </h2>
          <p
            class="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl"
          >
            To start exploring your data, please enter the URL of your infinitic
            API.
          </p>
          <div class="mt-3 max-w-md mx-auto md:mt-5 md:max-w-3xl">
            <label for="api_url" class="sr-only">API URL</label>
            <div class="relative rounded-md shadow-sm inline-block w-1/2">
              <input
                id="api_url"
                class="form-input block w-full sm:text-sm sm:leading-5"
                :class="[
                  urlIsInvalid
                    ? 'border-red-300 text-red-900 placeholder-red-300 focus:border-red-300 focus:shadow-outline-red'
                    : ''
                ]"
                placeholder="http://localhost:3010"
                v-model="urlInput"
              />
            </div>
            <span class="inline-flex rounded-md shadow-sm mx-2">
              <button
                type="button"
                class="inline-flex items-center px-4 py-2 border border-transparent text-sm leading-5 font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-500 focus:outline-none focus:border-indigo-700 focus:shadow-outline-indigo active:bg-indigo-700 transition ease-in-out duration-150"
                @click="verifyApiUrl()"
              >
                <pulse-loader
                  v-if="loading"
                  class="mx-auto text-center"
                  color="#ffffff"
                  :size="9"
                  :loading="true"
                />
                <span v-else class="flex-none">
                  Connect
                </span>
              </button>
            </span>
            <p class="mt-2 text-sm text-red-600" v-if="urlIsInvalid">
              We were unable to connect to your infinitic API. Please make sure
              the URL is correct and try again.
            </p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { PulseLoader } from "@saeris/vue-spinners";
import { isValidApiUrl } from "@/api";
import { State } from "@/store";

export default Vue.extend({
  name: "Welcome",

  components: { PulseLoader },

  data() {
    return {
      urlInput: "",
      urlIsInvalid: false,
      loading: false
    } as {
      urlInput: string;
      urlIsInvalid: boolean;
      loading: boolean;
    };
  },

  created() {
    this.urlInput =
      (this.$store.state as State).connections.currentConnection?.url || "";
  },

  methods: {
    async verifyApiUrl() {
      if (this.loading) {
        return;
      }

      this.loading = true;

      const url = this.urlInput;

      let isUrlValid = false;
      try {
        isUrlValid = await isValidApiUrl(url);
      } catch (error) {
        // Do nothing
      }

      this.loading = false;

      if (isUrlValid) {
        this.urlIsInvalid = false;

        const connection = {
          url
        };

        this.$store.commit("SET_CURRENT_CONNECTION", {
          connection
        });

        this.$router.push({
          name: "Dashboard"
        });
      } else {
        this.urlIsInvalid = true;
      }
    }
  }
});
</script>
