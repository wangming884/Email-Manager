<template>
  <LayoutShell title="审计日志" subtitle="查看所有 API 调用链路、调用主体和请求结果。">
    <section class="panel">
      <button class="btn secondary" @click="loadLogs">刷新</button>
      <div class="table-wrap" style="margin-top: 16px">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Trace ID</th>
              <th>主体</th>
              <th>方法</th>
              <th>路径</th>
              <th>状态码</th>
              <th>IP</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>{{ log.id }}</td>
              <td>{{ log.trace_id }}</td>
              <td>{{ log.actor_type }} / {{ log.actor_id }}</td>
              <td>{{ log.method }}</td>
              <td>{{ log.path }}</td>
              <td>{{ log.status_code }}</td>
              <td>{{ log.client_ip }}</td>
              <td>{{ log.created_at }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </LayoutShell>
</template>

<script setup>
import { onMounted, ref } from "vue";
import LayoutShell from "../components/LayoutShell.vue";
import http from "../api/http";

const logs = ref([]);

onMounted(loadLogs);

async function loadLogs() {
  const { data } = await http.get("/audit-logs");
  logs.value = data.data;
}
</script>
