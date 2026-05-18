import { DarkTheme, DefaultTheme, ThemeProvider } from "@react-navigation/native";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import "react-native-reanimated";

import { DriverImportantNotificationWatcher } from "@/components/DriverImportantNotificationWatcher";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { registerDriverPushToken } from "@/lib/phoneNotifications";
import { useEffect } from "react";

export default function RootLayout() {
  const colorScheme = useColorScheme();

  useEffect(() => {
    registerDriverPushToken().catch(console.log);
  }, []);

  return (
    <ThemeProvider value={colorScheme === "dark" ? DarkTheme : DefaultTheme}>
      <Stack>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen
          name="modal"
          options={{ presentation: "modal", title: "Modal" }}
        />
        <Stack.Screen name="login" options={{ headerShown: false }} />
      </Stack>

      <DriverImportantNotificationWatcher />
      <StatusBar style={colorScheme === "dark" ? "light" : "dark"} />
    </ThemeProvider>
  );
}
