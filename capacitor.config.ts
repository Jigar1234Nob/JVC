import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "au.jigar.jvc",
  appName: "JVC",
  webDir: "apps/web/dist",
  server: {
    allowNavigation: ["chat.jigar.au"]
  },
  android: { allowMixedContent: false }
};

export default config;
