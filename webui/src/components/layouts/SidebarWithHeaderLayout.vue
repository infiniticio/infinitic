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
  <div class="h-screen flex overflow-hidden bg-gray-100">
    <sidebar-mobile
      :isOpen="sidebarIsOpen"
      @close-sidebar="sidebarIsOpen = false"
    />
    <sidebar-desktop />

    <div class="flex flex-col w-0 flex-1 overflow-hidden">
      <div class="relative z-10 flex-shrink-0 flex h-16 bg-white shadow">
        <button
          class="px-4 border-r border-gray-200 text-gray-500 focus:outline-none focus:bg-gray-100 focus:text-gray-600 md:hidden"
          aria-label="Open sidebar"
          @click.stop="sidebarIsOpen = true"
        >
          <svg
            class="h-6 w-6"
            stroke="currentColor"
            fill="none"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 6h16M4 12h16M4 18h7"
            />
          </svg>
        </button>

        <slot name="header"></slot>
      </div>

      <slot></slot>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import SidebarDesktop from "@/components/layouts/sidebar/SidebarDesktop.vue";
import SidebarMobile from "@/components/layouts/sidebar/SidebarMobile.vue";

export default Vue.extend({
  name: "SidebarWithHeaderLayout",

  components: { SidebarDesktop, SidebarMobile },

  data() {
    return {
      sidebarIsOpen: false
    };
  }
});
</script>
