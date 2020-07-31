<template>
  <div
    class="h-screen flex overflow-hidden"
    :class="{ 'bg-gray-100': background === Background.Gray }"
  >
    <the-sidebar />

    <div class="flex flex-col w-0 flex-1 overflow-hidden">
      <div class="md:hidden pl-1 pt-1 sm:pl-3 sm:pt-3">
        <button
          class="-ml-0.5 -mt-0.5 h-12 w-12 inline-flex items-center justify-center rounded-md text-gray-500 hover:text-gray-900 focus:outline-none focus:bg-gray-200 transition ease-in-out duration-150"
          aria-label="Open sidebar"
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
import TheSidebar from "@/components/layouts/sidebar/TheSidebar.vue";

export enum Background {
  Gray = "GRAY",
  White = "WHITE"
}

export default Vue.extend({
  components: { TheSidebar },

  props: {
    background: {
      type: String as PropType<Background>,
      required: false,
      default: Background.Gray
    }
  },

  data() {
    return {
      Background
    };
  }
});
</script>
