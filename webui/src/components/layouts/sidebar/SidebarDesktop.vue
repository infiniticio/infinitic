<template>
  <!-- Static sidebar for desktop -->
  <div class="hidden md:flex md:flex-shrink-0">
    <div class="flex flex-col w-64">
      <div class="flex items-center h-16 flex-shrink-0 px-4 bg-gray-900">
        <img
          class="h-8 w-auto"
          src="@/assets/images/logo-with-text.svg"
          alt="Infinitic logo"
        />
      </div>
      <div class="h-0 flex-1 flex flex-col overflow-y-auto">
        <!-- Sidebar component, swap this element with another sidebar if you like -->
        <nav class="flex-1 px-2 py-4 bg-gray-800">
          <navigation-link :to="{ name: 'Dashboard' }" :first="true">
            <template v-slot:icon>
              <navigation-link-icon>
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
                />
              </navigation-link-icon>
            </template>
            Dashboard
          </navigation-link>
          <navigation-link :to="{ name: 'Workflows' }">
            <template v-slot:icon>
              <navigation-link-icon>
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"
                />
              </navigation-link-icon>
            </template>
            Workflows
          </navigation-link>
          <navigation-link :to="{ name: 'Tasks' }">
            <template v-slot:icon>
              <navigation-link-icon>
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
                />
              </navigation-link-icon>
            </template>
            Tasks
          </navigation-link>
          <navigation-link to="/documentation">
            <template v-slot:icon>
              <navigation-link-icon>
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                />
              </navigation-link-icon>
            </template>
            Documentation
          </navigation-link>
          <navigation-link to="/support">
            <template v-slot:icon>
              <navigation-link-icon>
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </navigation-link-icon>
            </template>
            Support
          </navigation-link>
          <navigation-link :to="{ name: 'Settings' }">
            <template v-slot:icon>
              <navigation-link-icon>
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                ></path>
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                ></path>
              </navigation-link-icon>
            </template>
            Settings
          </navigation-link>
        </nav>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import NavigationLink from "@/components/layouts/sidebar/NavigationLink.vue";
import NavigationLinkIcon from "@/components/layouts/sidebar/NavigationLinkIcon.vue";

export default Vue.extend({
  name: "SidebarDesktop",

  components: {
    NavigationLink,
    NavigationLinkIcon
  }
});
</script>
