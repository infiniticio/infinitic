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
  <!-- Off-canvas menu for mobile -->
  <div class="md:hidden">
    <transition
      enter-active-class="transition-opacity ease-linear duration-300"
      enter-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity ease-linear duration-300"
      leave-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-show="isOpen" class="fixed inset-0 flex z-40">
        <!--
        Off-canvas menu overlay, show/hide based on off-canvas menu state.

        Entering: "transition-opacity ease-linear duration-300"
          From: "opacity-0"
          To: "opacity-100"
        Leaving: "transition-opacity ease-linear duration-300"
          From: "opacity-100"
          To: "opacity-0"
      -->

        <div class="fixed inset-0">
          <div class="absolute inset-0 bg-gray-600 opacity-75"></div>
        </div>

        <!--
        Off-canvas menu, show/hide based on off-canvas menu state.

        Entering: "transition ease-in-out duration-300 transform"
          From: "-translate-x-full"
          To: "translate-x-0"
        Leaving: "transition ease-in-out duration-300 transform"
          From: "translate-x-0"
          To: "-translate-x-full"
      -->
        <transition
          enter-active-class="transition ease-in-out duration-300 transform"
          enter-class="-translate-x-full"
          enter-to-class="translate-x-0"
          leave-active-class="transition ease-in-out duration-300 transform"
          leave-class="translate-x-0"
          leave-to-class="-translate-x-full"
        >
          <div
            v-show="isOpen"
            class="relative flex-1 flex flex-col max-w-xs w-full pt-5 pb-4 bg-gray-800"
          >
            <div class="absolute top-0 right-0 -mr-14 p-1">
              <button
                class="flex items-center justify-center h-12 w-12 rounded-full focus:outline-none focus:bg-gray-600"
                v-show="isOpen"
                aria-label="Close sidebar"
                @click.stop="$emit('close-sidebar')"
              >
                <svg
                  class="h-6 w-6 text-white"
                  stroke="currentColor"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
            <div class="flex-shrink-0 flex items-center px-4">
              <img
                class="h-8 w-auto"
                src="@/assets/images/logo-with-text.svg"
                alt="Infinitic logo"
              />
            </div>
            <div class="mt-5 flex-1 h-0 overflow-y-auto">
              <nav class="px-2">
                <navigation-link-mobile
                  :to="{ name: 'Dashboard' }"
                  :first="true"
                >
                  <template v-slot:icon>
                    <navigation-link-mobile-icon>
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
                      />
                    </navigation-link-mobile-icon>
                  </template>
                  Dashboard
                </navigation-link-mobile>
                <navigation-link-mobile :to="{ name: 'Workflows' }">
                  <template v-slot:icon>
                    <navigation-link-mobile-icon>
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"
                      />
                    </navigation-link-mobile-icon>
                  </template>
                  Workflows
                </navigation-link-mobile>
                <navigation-link-mobile :to="{ name: 'Tasks' }">
                  <template v-slot:icon>
                    <navigation-link-mobile-icon>
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
                      />
                    </navigation-link-mobile-icon>
                  </template>
                  Tasks
                </navigation-link-mobile>
                <navigation-link-mobile to="/documentation">
                  <template v-slot:icon>
                    <navigation-link-mobile-icon>
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                      />
                    </navigation-link-mobile-icon>
                  </template>
                  Documentation
                </navigation-link-mobile>
                <navigation-link-mobile to="/support">
                  <template v-slot:icon>
                    <navigation-link-mobile-icon>
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </navigation-link-mobile-icon>
                  </template>
                  Support
                </navigation-link-mobile>
                <navigation-link-mobile :to="{ name: 'Settings' }">
                  <template v-slot:icon>
                    <navigation-link-mobile-icon>
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                      />
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                      />
                    </navigation-link-mobile-icon>
                  </template>
                  Settings
                </navigation-link-mobile>
              </nav>
            </div>
          </div>
        </transition>
        <div class="flex-shrink-0 w-14">
          <!-- Dummy element to force sidebar to shrink to fit close icon -->
        </div>
      </div>
    </transition>
  </div>
</template>

<script lang="ts">
import Vue, { PropType } from "vue";
import NavigationLinkMobile from "@/components/layouts/sidebar/NavigationLinkMobile.vue";
import NavigationLinkMobileIcon from "@/components/layouts/sidebar/NavigationLinkMobileIcon.vue";

export default Vue.extend({
  name: "SidebarMobile",

  components: {
    NavigationLinkMobile,
    NavigationLinkMobileIcon
  },

  props: {
    isOpen: {
      type: Boolean as PropType<boolean>,
      required: true
    }
  }
});
</script>
