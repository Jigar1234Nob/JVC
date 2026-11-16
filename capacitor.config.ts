import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "au.jigar.jvc",
  appName: "JVC",
  webDir: "apps/web/dist",
  server: {
    url: "https://chat.jigar.au",
    androidScheme: "https"
  },
  android: {
    allowMixedContent: false
  }
};

export default config;
