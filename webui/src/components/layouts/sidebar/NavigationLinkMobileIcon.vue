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
  <svg
    class="mr-4 h-6 w-6 text-gray-300 group-hover:text-gray-300 group-focus:text-gray-300 transition ease-in-out duration-150"
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
  name: "NavigationLinkMobileIcon"
});
</script>
