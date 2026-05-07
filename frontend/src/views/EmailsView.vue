<template>
  <LayoutShell title="邮件查询" subtitle="按收件人、主题、账号和解析类型检索同步后的邮件。">
    <section class="panel">
      <div class="toolbar">
        <input v-model="filters.to_email" placeholder="收件邮箱" />
        <input v-model="filters.subject_keyword" placeholder="主题关键词" />
        <input v-model="filters.received_after" placeholder="received_after，例如 2026-05-01T00:00:00+08:00" />
        <input v-model.number="filters.account_id" type="number" placeholder="账号ID" />
        <input v-model="filters.parsed_type" placeholder="解析类型" />
        <button class="btn secondary" @click="loadEmails">查询</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>账号</th>
              <th>主题</th>
              <th>解析类型</th>
              <th>规则</th>
              <th>接收时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="email in emails" :key="email.id">
              <td>{{ email.id }}</td>
              <td>{{ email.account_email }}</td>
              <td>{{ email.subject || "-" }}</td>
              <td>{{ email.parsed_type || "-" }}</td>
              <td>{{ email.matched_rule_name || "-" }}</td>
              <td>{{ email.received_at }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </LayoutShell>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import LayoutShell from "../components/LayoutShell.vue";
import http from "../api/http";

const emails = ref([]);
const filters = reactive({
  to_email: "",
  subject_keyword: "",
  received_after: "",
  account_id: null,
  parsed_type: ""
});

onMounted(loadEmails);

async function loadEmails() {
  const params = { ...filters };
  if (!params.account_id) {
    delete params.account_id;
  }
  const { data } = await http.get("/emails/query", { params });
  emails.value = data.data;
}
</script>
