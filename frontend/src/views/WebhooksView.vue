<template>
  <LayoutShell title="Webhook 管理" subtitle="配置命中邮件后的回调地址、签名密钥和过滤条件。">
    <section class="panel">
      <div class="form-row">
        <input v-model="form.name" placeholder="名称" />
        <input v-model="form.url" placeholder="https://example.com/webhook" />
      </div>
      <div class="form-row">
        <input v-model="form.event_type" placeholder="mail.received" />
        <input v-model="form.subject_keyword" placeholder="主题关键字，可选" />
      </div>
      <div class="form-row">
        <input v-model="form.shared_secret" placeholder="共享密钥" />
        <button class="btn" @click="createWebhook">新增 Webhook</button>
      </div>
    </section>

    <section class="panel">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>URL</th>
              <th>事件</th>
              <th>主题关键字</th>
              <th>启用</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="webhook in webhooks" :key="webhook.id">
              <td>{{ webhook.id }}</td>
              <td>{{ webhook.name }}</td>
              <td>{{ webhook.url }}</td>
              <td>{{ webhook.event_type }}</td>
              <td>{{ webhook.subject_keyword || "-" }}</td>
              <td>{{ webhook.active ? "是" : "否" }}</td>
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

const webhooks = ref([]);
const form = reactive({
  name: "",
  url: "",
  event_type: "mail.received",
  subject_keyword: "",
  shared_secret: ""
});

onMounted(loadWebhooks);

async function loadWebhooks() {
  const { data } = await http.get("/webhooks");
  webhooks.value = data.data;
}

async function createWebhook() {
  await http.post("/webhooks", form);
  Object.assign(form, { name: "", url: "", event_type: "mail.received", subject_keyword: "", shared_secret: "" });
  await loadWebhooks();
}
</script>
