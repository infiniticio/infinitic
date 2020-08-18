<template>
  <router-link :to="to" v-slot="{ href, isActive }">
    <a
      :href="href"
      class="group flex items-center px-2 py-2 text-base leading-6 font-medium rounded-md focus:outline-none focus:bg-gray-700 transition ease-in-out duration-150"
      :class="[
        isActive
          ? 'text-white bg-gray-900'
          : 'text-gray-300 hover:text-white hover:bg-gray-700 focus:text-white',
        !first ? 'mt-1' : ''
      ]"
    >
      <slot name="icon"></slot>
      <slot></slot>
    </a>
  </router-link>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import { RouteConfig } from "vue-router";

export default Vue.extend({
  name: "NavigationLinkMobile",

  props: {
    to: {
      type: Object as PropType<RouteConfig>,
      required: true
    },

    first: {
      type: Boolean as PropType<boolean>,
      required: false,
      default: false
    }
  }
});
</script>
