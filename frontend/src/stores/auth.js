import { defineStore } from "pinia";
import http from "../api/http";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("admin_token") || "",
    loading: false,
    error: ""
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    async login(payload) {
      this.loading = true;
      this.error = "";
      try {
        const { data } = await http.post("/admin/login", payload);
        this.token = data.data.access_token;
        localStorage.setItem("admin_token", this.token);
      } catch (error) {
        this.error = error.response?.data?.message || "登录失败";
        throw error;
      } finally {
        this.loading = false;
      }
    },
    logout() {
      this.token = "";
      localStorage.removeItem("admin_token");
    }
  }
});
