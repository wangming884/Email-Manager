<template>
  <LayoutShell title="解析规则" subtitle="配置验证码、激活链接和自定义正则提取逻辑。">
    <section class="panel">
      <div class="form-row">
        <input v-model="form.name" placeholder="规则名称" />
        <select v-model="form.type">
          <option>VERIFICATION_CODE</option>
          <option>ACTIVATION_LINK</option>
          <option>CUSTOM_REGEX</option>
        </select>
      </div>
      <div class="form-row">
        <input v-model="form.subject_keyword" placeholder="主题关键字，可选" />
        <input v-model="form.regex_pattern" placeholder="正则表达式" />
      </div>
      <div class="form-row">
        <input v-model="form.description" placeholder="描述" />
        <button class="btn" @click="createRule">新增规则</button>
      </div>
    </section>

    <section class="panel">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>类型</th>
              <th>主题关键字</th>
              <th>正则</th>
              <th>启用</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.id">
              <td>{{ rule.id }}</td>
              <td>{{ rule.name }}</td>
              <td>{{ rule.type }}</td>
              <td>{{ rule.subject_keyword || "-" }}</td>
              <td>{{ rule.regex_pattern }}</td>
              <td>{{ rule.enabled ? "是" : "否" }}</td>
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

const rules = ref([]);
const form = reactive({
  name: "",
  type: "VERIFICATION_CODE",
  subject_keyword: "",
  regex_pattern: "",
  description: ""
});

onMounted(loadRules);

async function loadRules() {
  const { data } = await http.get("/parser-rules");
  rules.value = data.data;
}

async function createRule() {
  await http.post("/parser-rules", form);
  Object.assign(form, { name: "", type: "VERIFICATION_CODE", subject_keyword: "", regex_pattern: "", description: "" });
  await loadRules();
}
</script>
