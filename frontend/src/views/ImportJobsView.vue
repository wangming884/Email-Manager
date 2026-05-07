<template>
  <LayoutShell title="导入任务" subtitle="查看批量导入作业的执行进度与错误详情。">
    <section class="panel">
      <button class="btn secondary" @click="loadJobs">刷新</button>
      <div class="table-wrap" style="margin-top: 16px">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>状态</th>
              <th>来源</th>
              <th>进度</th>
              <th>成功/总数</th>
              <th>错误</th>
              <th>摘要</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="job in jobs" :key="job.id">
              <td>{{ job.id }}</td>
              <td>{{ job.status }}</td>
              <td>{{ job.source_type }}</td>
              <td>{{ job.progress }}%</td>
              <td>{{ job.success_count }}/{{ job.total_count }}</td>
              <td>{{ job.error_count }}</td>
              <td>{{ job.summary || "-" }}</td>
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

const jobs = ref([]);

onMounted(loadJobs);

async function loadJobs() {
  const { data } = await http.get("/import-jobs");
  jobs.value = data.data;
}
</script>
