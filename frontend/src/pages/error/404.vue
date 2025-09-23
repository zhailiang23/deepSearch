<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50">
    <div class="max-w-md w-full text-center">
      <div class="mb-8">
        <h1 class="text-9xl font-bold text-gray-300">404</h1>
        <p class="text-2xl font-semibold text-gray-700 mb-4">页面未找到</p>
        <p class="text-gray-500 mb-8">
          抱歉，您访问的页面不存在或已被移除。
        </p>
      </div>

      <div class="space-y-4">
        <button
          @click="$router.go(-1)"
          class="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-md transition duration-200"
        >
          返回上一页
        </button>

        <router-link
          to="/"
          class="block w-full bg-gray-600 hover:bg-gray-700 text-white font-medium py-2 px-4 rounded-md transition duration-200"
        >
          返回首页
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 404 Not Found页面
</script>