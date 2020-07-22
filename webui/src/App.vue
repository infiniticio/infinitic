<template>
  <div id="app">
    <router-view :key="$route.fullPath" />
  </div>
</template>

<script lang="ts">
import Vue from "vue";

export default Vue.extend({});
</script>

<style src="./assets/css/tailwind.css" />
