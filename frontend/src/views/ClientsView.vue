<template>
  <LayoutShell title="客户端凭证" subtitle="为外部核心平台分配 client_id、secret 和可用 scope。">
    <section class="panel">
      <h3>新建客户端</h3>
      <div class="form-row">
        <input v-model="form.client_id" placeholder="client_id" />
        <input v-model="form.name" placeholder="客户端名称" />
      </div>
      <div class="form-row">
        <input v-model="form.client_secret" placeholder="client_secret" />
        <input v-model="form.scopes" placeholder="ACCOUNT_READ,ACCOUNT_WRITE,EMAIL_READ,SYNC_TRIGGER" />
      </div>
      <button class="btn" @click="createClient">创建客户端</button>
    </section>

    <section class="panel">
      <h3>客户端列表</h3>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Client ID</th>
              <th>名称</th>
              <th>Scopes</th>
              <th>启用</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="client in clients" :key="client.id">
              <td>{{ client.id }}</td>
              <td>{{ client.client_id }}</td>
              <td>{{ client.name }}</td>
              <td>{{ client.scopes }}</td>
              <td>{{ client.enabled ? "是" : "否" }}</td>
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

const clients = ref([]);
const form = reactive({
  client_id: "",
  name: "",
  client_secret: "",
  scopes: "ACCOUNT_READ,ACCOUNT_WRITE,EMAIL_READ,SYNC_TRIGGER"
});

onMounted(loadClients);

async function loadClients() {
  const { data } = await http.get("/client-apps");
  clients.value = data.data;
}

async function createClient() {
  await http.post("/client-apps", form);
  Object.assign(form, { client_id: "", name: "", client_secret: "" });
  await loadClients();
}
</script>
