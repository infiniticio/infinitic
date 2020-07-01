<template>
  <div>
    <h3 class="text-4xl leading-6 text-center font-medium text-gray-900 mt-12">
      Oops! Looks like something went wrong.
    </h3>
    <img
      src="@/assets/images/errors/undraw_server_down_s4lk.svg"
      class="mt-8 w-1/2 mx-auto"
    />
  </div>
</template>

<script lang="ts">
import Vue from "vue";
export default Vue.extend({
  name: "ServerError"
});
</script>
