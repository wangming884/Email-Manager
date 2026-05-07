import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";
import LoginView from "../views/LoginView.vue";
import DashboardView from "../views/DashboardView.vue";
import AccountsView from "../views/AccountsView.vue";
import AccountDetailView from "../views/AccountDetailView.vue";
import ImportJobsView from "../views/ImportJobsView.vue";
import EmailsView from "../views/EmailsView.vue";
import ClientsView from "../views/ClientsView.vue";
import RulesView from "../views/RulesView.vue";
import WebhooksView from "../views/WebhooksView.vue";
import AuditLogsView from "../views/AuditLogsView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "login", component: LoginView },
    { path: "/", name: "dashboard", component: DashboardView },
    { path: "/accounts", name: "accounts", component: AccountsView },
    { path: "/accounts/:id", name: "account-detail", component: AccountDetailView },
    { path: "/import-jobs", name: "import-jobs", component: ImportJobsView },
    { path: "/emails", name: "emails", component: EmailsView },
    { path: "/clients", name: "clients", component: ClientsView },
    { path: "/rules", name: "rules", component: RulesView },
    { path: "/webhooks", name: "webhooks", component: WebhooksView },
    { path: "/audit-logs", name: "audit-logs", component: AuditLogsView }
  ]
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.name !== "login" && !authStore.isAuthenticated) {
    return { name: "login" };
  }
  if (to.name === "login" && authStore.isAuthenticated) {
    return { name: "dashboard" };
  }
});

export default router;
