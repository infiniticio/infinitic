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
