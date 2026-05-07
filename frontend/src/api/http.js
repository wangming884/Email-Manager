import axios from "axios";

const http = axios.create({
  baseURL: "/api/v1"
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("admin_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Add response interceptor for error handling
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Handle 401 Unauthorized - token expired or invalid
      if (error.response.status === 401) {
        localStorage.removeItem("admin_token");
        // Redirect to login page using window.location to avoid router context issues
        window.location.href = "/login";
      }
      // Handle 403 Forbidden
      else if (error.response.status === 403) {
        console.error("Access forbidden:", error.response.data);
      }
      // Handle 500 Internal Server Error
      else if (error.response.status >= 500) {
        console.error("Server error:", error.response.data);
      }
    } else if (error.request) {
      // Network error - no response received
      console.error("Network error: Unable to reach server");
    }
    return Promise.reject(error);
  }
);

export default http;

