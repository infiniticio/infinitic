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
