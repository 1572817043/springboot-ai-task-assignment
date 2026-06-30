<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function handleSubmit() {
  errorMessage.value = ''
  loading.value = true
  try {
    await authStore.login({
      username: username.value,
      password: password.value,
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.push(redirect)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-[#f7f7f8] px-4 py-10">
    <section class="w-full max-w-[420px] rounded-lg border border-slate-200 bg-white p-7">
      <div class="mb-7">
        <div class="mb-4 flex h-9 w-9 items-center justify-center rounded-md bg-slate-950 text-sm font-semibold text-white">
          AI
        </div>
        <h1 class="text-xl font-semibold leading-7 text-slate-950">AI 任务分配平台</h1>
        <p class="mt-1 text-sm leading-6 text-slate-500">登录后进入项目任务工作台</p>
      </div>

      <form class="space-y-4" @submit.prevent="handleSubmit">
        <label class="block">
          <span class="text-sm font-medium text-slate-700">账号</span>
          <input
            v-model="username"
            class="mt-1 h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-slate-400"
            name="username"
            placeholder="请输入账号"
            type="text"
          />
        </label>

        <label class="block">
          <span class="text-sm font-medium text-slate-700">密码</span>
          <input
            v-model="password"
            class="mt-1 h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-slate-400"
            name="password"
            placeholder="请输入密码"
            type="password"
          />
        </label>

        <button
          class="h-10 w-full rounded-md bg-slate-950 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          :disabled="loading"
          type="submit"
        >
          {{ loading ? '登录中' : '登录' }}
        </button>

        <p v-if="errorMessage" class="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {{ errorMessage }}
        </p>
      </form>
    </section>
  </main>
</template>
