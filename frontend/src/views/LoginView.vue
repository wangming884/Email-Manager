<template>
  <div class="login-screen">
    <div class="panel login-card">
      <h2>管理员登录</h2>
      <p class="muted">使用后端配置中的管理员账号进入邮箱管理后台。</p>
      <div class="form-row">
        <input v-model="form.username" placeholder="用户名" />
      </div>
      <div class="form-row">
        <input v-model="form.password" type="password" placeholder="密码" />
      </div>
      <div class="form-row">
        <button class="btn" :disabled="authStore.loading" @click="submit">
          {{ authStore.loading ? "登录中..." : "登录" }}
        </button>
      </div>
      <div v-if="authStore.error" class="badge danger">{{ authStore.error }}</div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  username: "admin",
  password: "admin123456"
});

async function submit() {
  try {
    await authStore.login(form);
    router.push("/");
  } catch (error) {
    console.error(error);
  }
}
</script>
