<template>
  <div class="bg-white overflow-hidden shadow rounded-lg">
    <div class="px-4 py-5 sm:p-6">
      <div v-if="loading$">
        Loading
      </div>
      <dl v-else-if="error$">
        Error
      </dl>
      <dl v-else>
        <dt class="text-sm leading-5 font-medium text-gray-500 truncate">
          {{ label }}
        </dt>
        <dd class="mt-1 text-3xl leading-9 font-semibold text-gray-900">
          {{ value$ }}
        </dd>
      </dl>
    </div>
  </div>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import { Observable, of } from "rxjs";
import { startWith, mapTo, catchError } from "rxjs/operators";

export default Vue.extend({
  props: {
    value: {
      type: Object as PropType<Observable<string>>,
      required: true
    },

    label: {
      type: String as PropType<string>,
      required: true
    }
  },

  subscriptions() {
    const value = this.$props.value as Observable<string>;

    return {
      loading$: value.pipe(mapTo(false), startWith(true)),
      error$: value.pipe(
        mapTo(undefined),
        catchError(err => of<Error>(err))
      ),
      value$: value
    };
  }
});
</script>
