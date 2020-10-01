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
  <div
    class="h-screen flex overflow-hidden"
    :class="{ 'bg-gray-100': background === Background.Gray }"
  >
    <sidebar-mobile
      :isOpen="sidebarIsOpen"
      @close-sidebar="sidebarIsOpen = false"
    />
    <sidebar-desktop />

    <div class="flex flex-col w-0 flex-1 overflow-hidden">
      <div class="md:hidden pl-1 pt-1 sm:pl-3 sm:pt-3">
        <button
          class="-ml-0.5 -mt-0.5 h-12 w-12 inline-flex items-center justify-center rounded-md text-gray-500 hover:text-gray-900 focus:outline-none focus:bg-gray-200 transition ease-in-out duration-150"
          aria-label="Open sidebar"
          @click.stop="sidebarIsOpen = true"
        >
          <svg
            class="h-6 w-6"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 6h16M4 12h16M4 18h16"
            />
          </svg>
        </button>
      </div>

      <slot></slot>
    </div>
  </div>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import SidebarDesktop from "@/components/layouts/sidebar/SidebarDesktop.vue";
import SidebarMobile from "@/components/layouts/sidebar/SidebarMobile.vue";

export enum Background {
  Gray = "GRAY",
  White = "WHITE"
}

export default Vue.extend({
  name: "SidebarLayout",

  components: { SidebarDesktop, SidebarMobile },

  props: {
    background: {
      type: String as PropType<Background>,
      required: false,
      default: Background.Gray
    }
  },

  data() {
    return {
      sidebarIsOpen: false,
      Background
    };
  }
});
</script>
