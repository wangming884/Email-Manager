import { defineStore } from "pinia";
import SockJS from "sockjs-client/dist/sockjs";
import { Client } from "@stomp/stompjs";

export const useRealtimeStore = defineStore("realtime", {
  state: () => ({
    connected: false,
    importEvents: [],
    mailEvents: [],
    client: null
  }),
  actions: {
    connect() {
      if (this.client || !localStorage.getItem("admin_token")) {
        return;
      }
      const client = new Client({
        webSocketFactory: () => new SockJS("/ws/events"),
        reconnectDelay: 4000,
        onConnect: () => {
          this.connected = true;
          client.subscribe("/topic/import-progress", (frame) => {
            try {
              this.importEvents.unshift(JSON.parse(frame.body));
              this.importEvents = this.importEvents.slice(0, 10);
            } catch (error) {
              console.error("Failed to parse import progress event:", error);
            }
          });
          client.subscribe("/topic/mail-received", (frame) => {
            try {
              this.mailEvents.unshift(JSON.parse(frame.body));
              this.mailEvents = this.mailEvents.slice(0, 10);
            } catch (error) {
              console.error("Failed to parse mail received event:", error);
            }
          });
        },
        onDisconnect: () => {
          this.connected = false;
        },
        onStompError: (frame) => {
          console.error("STOMP error:", frame.headers.message);
          this.connected = false;
        },
        onWebSocketError: (event) => {
          console.error("WebSocket error:", event);
          this.connected = false;
        }
      });
      client.activate();
      this.client = client;
    },
    disconnect() {
      if (this.client) {
        try {
          this.client.deactivate();
        } catch (error) {
          console.error("Error disconnecting WebSocket:", error);
        }
      }
      this.client = null;
      this.connected = false;
      this.importEvents = [];
      this.mailEvents = [];
    }
  }
});
