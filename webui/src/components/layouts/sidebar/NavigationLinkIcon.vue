<template>
  <svg
    class="mr-3 h-6 w-6 text-gray-300 group-hover:text-gray-300 group-focus:text-gray-300 transition ease-in-out duration-150"
    stroke="currentColor"
    fill="none"
    viewBox="0 0 24 24"
  >
    <slot></slot>
  </svg>
</template>

<script lang="ts">
import Vue from "vue";

export default Vue.extend({
  name: "NavigationLinkIcon"
});
</script>
