<template>
  <main
    class="flex-1 relative z-0 overflow-y-auto pt-2 pb-6 focus:outline-none md:py-6"
    tabindex="0"
  >
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <h1 class="text-2xl font-semibold text-gray-900">Dashboard</h1>
    </div>
    <div class="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
      <div>
        <h3 class="text-lg leading-6 font-medium text-gray-900">
          Last 30 days
        </h3>
        <div class="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-3">
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <dl>
                <dt
                  class="text-sm leading-5 font-medium text-gray-500 truncate"
                >
                  Workflows started
                </dt>
                <dd class="mt-1 text-3xl leading-9 font-semibold text-gray-900">
                  71,897
                </dd>
              </dl>
            </div>
          </div>
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <dl>
                <dt
                  class="text-sm leading-5 font-medium text-gray-500 truncate"
                >
                  Tasks started
                </dt>
                <dd class="mt-1 text-3xl leading-9 font-semibold text-gray-900">
                  123,865
                </dd>
              </dl>
            </div>
          </div>
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <dl>
                <dt
                  class="text-sm leading-5 font-medium text-gray-500 truncate"
                >
                  Avg. Click Rate
                </dt>
                <dd class="mt-1 text-3xl leading-9 font-semibold text-gray-900">
                  24.57%
                </dd>
              </dl>
            </div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
