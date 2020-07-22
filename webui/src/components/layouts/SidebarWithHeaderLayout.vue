<template>
  <div class="h-screen flex overflow-hidden bg-gray-100">
    <the-sidebar />

    <div class="flex flex-col w-0 flex-1 overflow-hidden">
      <div class="relative z-10 flex-shrink-0 flex h-16 bg-white shadow">
        <button
          class="px-4 border-r border-gray-200 text-gray-500 focus:outline-none focus:bg-gray-100 focus:text-gray-600 md:hidden"
          aria-label="Open sidebar"
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
import TheSidebar from "@/components/layouts/sidebar/TheSidebar.vue";

export default Vue.extend({
  components: { TheSidebar }
});
</script>
