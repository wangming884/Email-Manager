<template>
  <LayoutShell title="账号管理" subtitle="导入、筛选、测试和同步邮箱账号。">
    <section class="panel">
      <h3>批量导入</h3>
      <div class="form-row">
        <textarea v-model="importLines" rows="5" placeholder="每行一个账号，默认格式 user@example.com:password[:proxyName]" />
      </div>
      <div class="form-row">
        <input v-model="regexPattern" placeholder="自定义正则，可选。需包含命名组 email/password/proxy" />
      </div>
      <div class="form-row">
        <input type="file" @change="onFileChange" />
        <button class="btn" @click="submitImport">开始导入</button>
        <button class="btn secondary" @click="exportAccounts">导出 CSV</button>
      </div>
      <div v-if="importMessage" class="badge success">{{ importMessage }}</div>
      <div v-if="error" class="badge danger">{{ error }}</div>
      <div v-if="loading" class="badge">处理中...</div>
    </section>

    <section class="panel">
      <h3>账号筛选</h3>
      <div class="toolbar">
        <input v-model="filters.status" placeholder="状态，如 ACTIVE" />
        <input v-model="filters.provider" placeholder="服务商，如 gmail.com" />
        <input v-model="filters.tag" placeholder="标签" />
        <button class="btn secondary" @click="loadAccounts">查询</button>
        <button class="btn success" @click="triggerSync">同步活跃账号</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>邮箱</th>
              <th>状态</th>
              <th>服务商</th>
              <th>标签</th>
              <th>最近测试</th>
              <th>最近同步</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="account in accounts" :key="account.id">
              <td>{{ account.id }}</td>
              <td>{{ account.email }}</td>
              <td><span class="badge" :class="badgeClass(account.status)">{{ account.status }}</span></td>
              <td>{{ account.provider }}</td>
              <td>{{ account.tags || "-" }}</td>
              <td>{{ account.last_tested_at || "-" }}</td>
              <td>{{ account.last_synced_at || "-" }}</td>
              <td>
                <div class="toolbar" style="margin: 0">
                  <RouterLink class="btn secondary" :to="`/accounts/${account.id}`">详情</RouterLink>
                  <button class="btn" @click="testAccount(account.id)">测试</button>
                </div>
              </td>
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

const accounts = ref([]);
const selectedFile = ref(null);
const importLines = ref("");
const regexPattern = ref("");
const importMessage = ref("");
const loading = ref(false);
const error = ref("");
const filters = reactive({
  status: "",
  provider: "",
  tag: ""
});

onMounted(loadAccounts);

async function loadAccounts() {
  try {
    loading.value = true;
    error.value = "";
    const { data } = await http.get("/accounts", { params: filters });
    accounts.value = data.data;
  } catch (err) {
    error.value = err.response?.data?.message || "加载账号失败";
    console.error("Failed to load accounts:", err);
  } finally {
    loading.value = false;
  }
}

function onFileChange(event) {
  selectedFile.value = event.target.files?.[0] || null;
}

async function submitImport() {
  try {
    loading.value = true;
    error.value = "";
    importMessage.value = "";
    
    const formData = new FormData();
    if (selectedFile.value) {
      formData.append("file", selectedFile.value);
    } else if (importLines.value.trim()) {
      const blob = new Blob([importLines.value], { type: "text/plain" });
      formData.append("file", blob, "accounts.txt");
    } else {
      error.value = "请输入账号数据或选择文件";
      return;
    }
    
    if (regexPattern.value) {
      formData.append("regex_pattern", regexPattern.value);
    }
    
    const { data } = await http.post("/accounts/import", formData);
    importMessage.value = `导入任务 #${data.data.job_id} 已创建`;
    importLines.value = "";
    selectedFile.value = null;
    setTimeout(() => loadAccounts(), 1000);
  } catch (err) {
    error.value = err.response?.data?.message || "导入失败";
    console.error("Failed to import accounts:", err);
  } finally {
    loading.value = false;
  }
}

async function testAccount(id) {
  try {
    await http.post(`/accounts/${id}/test`);
    importMessage.value = "测试已启动";
    setTimeout(() => loadAccounts(), 2000);
  } catch (err) {
    error.value = err.response?.data?.message || "测试失败";
    console.error("Failed to test account:", err);
  }
}

async function triggerSync() {
  try {
    loading.value = true;
    await http.post("/sync/jobs", { account_ids: [] });
    importMessage.value = "同步任务已创建";
  } catch (err) {
    error.value = err.response?.data?.message || "同步失败";
    console.error("Failed to trigger sync:", err);
  } finally {
    loading.value = false;
  }
}

async function exportAccounts() {
  try {
    loading.value = true;
    const response = await http.get("/accounts/export", { responseType: "blob" });
    const blobUrl = URL.createObjectURL(response.data);
    const anchor = document.createElement("a");
    anchor.href = blobUrl;
    anchor.download = "accounts.csv";
    anchor.click();
    URL.revokeObjectURL(blobUrl);
    importMessage.value = "导出成功";
  } catch (err) {
    error.value = err.response?.data?.message || "导出失败";
    console.error("Failed to export accounts:", err);
  } finally {
    loading.value = false;
  }
}

function badgeClass(status) {
  if (status === "ACTIVE") return "success";
  if (status === "INVALID") return "danger";
  if (status === "LOCKED") return "warning";
  return "";
}
</script>
