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
  <sidebar-layout>
    <main
      class="flex-1 relative z-0 overflow-y-auto focus:outline-none"
      tabindex="0"
    >
      <div class="pt-2 pb-6 md:py-6">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h1 class="text-2xl font-semibold text-gray-900">Dashboard</h1>
        </div>
        <div class="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
          <div>
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              Last 30 days
            </h3>
            <div class="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-3">
              <div class="bg-white overflow-hidden shadow rounded-lg">
                <div class="px-4 py-5 sm:p-6">
                  <dl>
                    <dt
                      class="text-sm leading-5 font-medium text-gray-500 truncate"
                    >
                      Workflows started
                    </dt>
                    <dd
                      class="mt-1 text-3xl leading-9 font-semibold text-gray-900"
                    >
                      71,897
                    </dd>
                  </dl>
                </div>
              </div>
              <div class="bg-white overflow-hidden shadow rounded-lg">
                <div class="px-4 py-5 sm:p-6">
                  <dl>
                    <dt
                      class="text-sm leading-5 font-medium text-gray-500 truncate"
                    >
                      Tasks started
                    </dt>
                    <dd
                      class="mt-1 text-3xl leading-9 font-semibold text-gray-900"
                    >
                      123,865
                    </dd>
                  </dl>
                </div>
              </div>
              <div class="bg-white overflow-hidden shadow rounded-lg">
                <div class="px-4 py-5 sm:p-6">
                  <dl>
                    <dt
                      class="text-sm leading-5 font-medium text-gray-500 truncate"
                    >
                      Error Rate
                    </dt>
                    <dd
                      class="mt-1 text-3xl leading-9 font-semibold text-gray-900"
                    >
                      24.57%
                    </dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </sidebar-layout>
</template>

<script lang="ts">
import Vue from "vue";
import SidebarLayout from "@/components/layouts/SidebarLayout.vue";

export default Vue.extend({
  name: "Dashboard",

  components: { SidebarLayout }
});
</script>
