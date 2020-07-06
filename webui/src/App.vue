<template>
  <div id="app">
    <div class="h-screen flex overflow-hidden bg-gray-100">
      <the-sidebar />

      <div class="flex flex-col w-0 flex-1 overflow-hidden">
        <div class="md:hidden pl-1 pt-1 sm:pl-3 sm:pt-3">
          <button
            class="-ml-0.5 -mt-0.5 h-12 w-12 inline-flex items-center justify-center rounded-md text-gray-500 hover:text-gray-900 focus:outline-none focus:bg-gray-200 transition ease-in-out duration-150"
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
                d="M4 6h16M4 12h16M4 18h16"
              />
            </svg>
          </button>
        </div>

        <router-view :key="$route.fullPath" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import TheSidebar from "@/components/TheSidebar.vue";

export default Vue.extend({
  components: { TheSidebar }
});
</script>

<style src="./assets/css/tailwind.css" />
