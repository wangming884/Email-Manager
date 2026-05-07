<template>
  <LayoutShell title="账号详情" subtitle="更新标签、状态和协议配置，并支持手动连通性测试。">
    <section class="panel" v-if="account">
      <div class="grid-2">
        <div>
          <div class="muted">邮箱</div>
          <h3>{{ account.email }}</h3>
        </div>
        <div>
          <div class="muted">当前状态</div>
          <h3>{{ account.status }}</h3>
        </div>
      </div>

      <div class="form-row">
        <input v-model="form.tags" placeholder="标签" />
        <select v-model="form.status">
          <option value="">保持不变</option>
          <option>ACTIVE</option>
          <option>TESTING</option>
          <option>INVALID</option>
          <option>LOCKED</option>
        </select>
      </div>

      <div class="form-row">
        <input v-model="form.imap_host" placeholder="IMAP Host" />
        <input v-model.number="form.imap_port" type="number" placeholder="IMAP Port" />
      </div>

      <div class="form-row">
        <input v-model="form.smtp_host" placeholder="SMTP Host" />
        <input v-model.number="form.smtp_port" type="number" placeholder="SMTP Port" />
      </div>

      <div class="form-row">
        <input v-model.number="form.proxy_binding_id" type="number" placeholder="代理绑定 ID，可选" />
      </div>

      <div class="form-row">
        <button class="btn" @click="save">保存修改</button>
        <button class="btn success" @click="testAccount">测试账号</button>
      </div>

      <div v-if="account.last_error_message" class="badge danger">{{ account.last_error_message }}</div>
    </section>
  </LayoutShell>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import LayoutShell from "../components/LayoutShell.vue";
import http from "../api/http";

const route = useRoute();
const account = ref(null);
const form = reactive({
  tags: "",
  status: "",
  imap_host: "",
  imap_port: 993,
  smtp_host: "",
  smtp_port: 587,
  proxy_binding_id: null
});

onMounted(loadAccount);

async function loadAccount() {
  const { data } = await http.get(`/accounts/${route.params.id}`);
  account.value = data.data;
  Object.assign(form, {
    tags: account.value.tags || "",
    status: "",
    imap_host: account.value.imap_host,
    imap_port: account.value.imap_port,
    smtp_host: account.value.smtp_host,
    smtp_port: account.value.smtp_port,
    proxy_binding_id: account.value.proxy_binding_id
  });
}

async function save() {
  await http.patch(`/accounts/${route.params.id}`, form);
  await loadAccount();
}

async function testAccount() {
  await http.post(`/accounts/${route.params.id}/test`);
  await loadAccount();
}
</script>
