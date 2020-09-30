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
  <div class="bg-white overflow-hidden shadow rounded-lg">
    <div class="px-4 py-5 sm:p-6">
      <dl>
        <dt class="text-sm leading-5 font-medium text-gray-500 truncate">
          {{ label }}
        </dt>
        <dd class="mt-1 text-3xl leading-9 font-semibold text-gray-900">
          <pulse-loader
            v-if="loading"
            class="mx-auto text-center"
            color="#6875F5"
            :loading="true"
          />
          <template v-else>
            {{ error ? error : value }}
          </template>
        </dd>
      </dl>
    </div>
  </div>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import { PulseLoader } from "@saeris/vue-spinners";

export default Vue.extend({
  components: { PulseLoader },

  props: {
    loading: {
      type: Boolean as PropType<boolean>,
      required: true
    },

    error: {
      type: Error as PropType<Error | undefined>,
      required: false
    },

    value: {
      type: String as PropType<string>,
      required: false
    },

    label: {
      type: String as PropType<string>,
      required: true
    }
  }
});
</script>
