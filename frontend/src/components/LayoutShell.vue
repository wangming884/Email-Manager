<template>
  <div class="page-shell">
    <aside class="sidebar">
      <div class="brand">
        <h1>邮箱管理器</h1>
        <p>账号池、邮件解析、Webhook 与平台对接后台</p>
      </div>
      <RouterLink class="nav-link" to="/">总览</RouterLink>
      <RouterLink class="nav-link" to="/accounts">账号列表</RouterLink>
      <RouterLink class="nav-link" to="/import-jobs">导入任务</RouterLink>
      <RouterLink class="nav-link" to="/emails">邮件查询</RouterLink>
      <RouterLink class="nav-link" to="/clients">客户端凭证</RouterLink>
      <RouterLink class="nav-link" to="/rules">解析规则</RouterLink>
      <RouterLink class="nav-link" to="/webhooks">Webhook</RouterLink>
      <RouterLink class="nav-link" to="/audit-logs">审计日志</RouterLink>
    </aside>
    <main class="content">
      <div class="topbar">
        <div>
          <h2 style="margin: 0">{{ title }}</h2>
          <div class="muted">{{ subtitle }}</div>
        </div>
        <div class="toolbar" style="margin: 0">
          <span class="badge" :class="realtime.connected ? 'success' : 'warning'">
            {{ realtime.connected ? "WebSocket 已连接" : "WebSocket 未连接" }}
          </span>
          <button class="btn secondary" @click="logout">退出登录</button>
        </div>
      </div>
      <slot />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { useRealtimeStore } from "../stores/realtime";

defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ""
  }
});

const router = useRouter();
const authStore = useAuthStore();
const realtime = useRealtimeStore();

onMounted(() => {
  realtime.connect();
});

function logout() {
  authStore.logout();
  realtime.disconnect();
  router.push("/login");
}
</script>
