<template>
  <LayoutShell title="总览面板" subtitle="查看账号池运行状态、实时导入进度与最新邮件命中。">
    <div class="cards">
      <div class="card" v-for="item in metricsCards" :key="item.key">
        <div class="muted">{{ item.label }}</div>
        <h3>{{ item.value }}</h3>
      </div>
    </div>

    <div class="grid-2" style="margin-top: 20px">
      <section class="panel">
        <h3>最近导入进度</h3>
        <div class="event-list">
          <div class="event-item" v-for="event in realtime.importEvents" :key="`${event.job_id}-${event.progress}`">
            任务 #{{ event.job_id }} - {{ event.status }} / {{ event.progress }}%
          </div>
          <div v-if="!realtime.importEvents.length" class="muted">暂无实时导入事件。</div>
        </div>
      </section>

      <section class="panel">
        <h3>最新邮件提醒</h3>
        <div class="event-list">
          <div class="event-item" v-for="event in realtime.mailEvents" :key="`${event.mail_id}-${event.subject}`">
            <strong>{{ event.account_email }}</strong> 收到邮件：{{ event.subject || "无主题" }}
          </div>
          <div v-if="!realtime.mailEvents.length" class="muted">暂无实时邮件事件。</div>
        </div>
      </section>
    </div>
  </LayoutShell>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import LayoutShell from "../components/LayoutShell.vue";
import http from "../api/http";
import { useRealtimeStore } from "../stores/realtime";

const realtime = useRealtimeStore();
const metrics = ref({});

const metricsCards = computed(() => [
  { key: "total_accounts", label: "账号总数", value: metrics.value.total_accounts || 0 },
  { key: "active_accounts", label: "可用账号", value: metrics.value.active_accounts || 0 },
  { key: "invalid_accounts", label: "失效账号", value: metrics.value.invalid_accounts || 0 },
  { key: "import_jobs", label: "导入任务", value: metrics.value.import_jobs || 0 },
  { key: "emails", label: "邮件总数", value: metrics.value.emails || 0 },
  { key: "webhooks", label: "Webhook", value: metrics.value.webhooks || 0 }
]);

onMounted(async () => {
  const { data } = await http.get("/overview");
  metrics.value = data.data;
});
</script>
