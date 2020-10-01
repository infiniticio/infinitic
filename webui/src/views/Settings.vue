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
  <sidebar-layout :background="background">
    <main
      class="flex-1 relative z-0 overflow-y-auto pt-2 pb-6 focus:outline-none md:py-6"
      tabindex="0"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
        <form>
          <div>
            <div>
              <div>
                <h3 class="text-lg leading-6 font-medium text-gray-900">
                  Infinitic API
                </h3>
                <p class="mt-1 text-sm leading-5 text-gray-500">
                  Configure the connection to the infinitic API.
                </p>
              </div>
              <div
                class="mt-6 grid grid-cols-1 row-gap-6 col-gap-4 sm:grid-cols-6"
              >
                <div class="sm:col-span-4">
                  <label
                    for="api_url"
                    class="block text-sm leading-5 font-medium text-gray-700"
                  >
                    URL
                  </label>
                  <div class="mt-1 flex items-center">
                    <div class="rounded-md shadow-sm">
                      <input
                        id="api_url"
                        class="form-input block w-full transition duration-150 ease-in-out sm:text-sm sm:leading-5 bg-gray-50 text-gray-500"
                        :value="apiUrl"
                        :disabled="true"
                      />
                    </div>
                    <span class="ml-5 rounded-md shadow-sm">
                      <button
                        type="button"
                        class="py-2 px-3 border border-gray-300 rounded-md text-sm leading-4 font-medium text-gray-700 hover:text-gray-500 focus:outline-none focus:border-blue-300 focus:shadow-outline-blue active:bg-gray-50 active:text-gray-800 transition duration-150 ease-in-out"
                        @click="changeApiUrl()"
                      >
                        Change
                      </button>
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </form>
      </div>
    </main>
  </sidebar-layout>
</template>

<script lang="ts">
import Vue from "vue";
import SidebarLayout, {
  Background
} from "@/components/layouts/SidebarLayout.vue";
import { State } from "@/store";

export default Vue.extend({
  name: "Workflows",

  components: { SidebarLayout },

  data() {
    return {
      verifyingApiUrl: false
    } as {
      verifyingApiUrl: boolean;
    };
  },

  computed: {
    background() {
      return Background.White;
    },

    apiUrl() {
      return (this.$store.state as State).connections.currentConnection?.url;
    }
  },

  methods: {
    changeApiUrl() {
      this.$router.push({ name: "Welcome" });
    }
  }
});
</script>
