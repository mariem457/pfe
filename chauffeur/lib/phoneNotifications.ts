import * as Notifications from "expo-notifications";
import Constants from "expo-constants";
import { AppState, Platform } from "react-native";
import { BASE_URL } from "./api";
import {
  DriverNotification,
  getDriverNotificationText,
  isPhoneNotificationImportant,
} from "./driverNotifications";
import { getToken } from "./storage";

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldPlaySound: true,
    shouldSetBadge: true,
    shouldShowBanner: AppState.currentState !== "active",
    shouldShowList: AppState.currentState !== "active",
  }),
});

let permissionAsked = false;

export async function ensurePhoneNotificationPermission() {
  if (permissionAsked) return;
  permissionAsked = true;

  const current = await Notifications.getPermissionsAsync();
  if (current.granted) return;

  await Notifications.requestPermissionsAsync();

  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync("driver-important-alerts", {
      name: "Alertes chauffeur importantes",
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: "#EF4444",
    });
  }
}

async function getExpoProjectId() {
  return (
    Constants.easConfig?.projectId ||
    (Constants.expoConfig?.extra as { eas?: { projectId?: string } } | undefined)?.eas
      ?.projectId
  );
}

export async function registerDriverPushToken() {
  await ensurePhoneNotificationPermission();

  const permissions = await Notifications.getPermissionsAsync();
  if (!permissions.granted) {
    return;
  }

  const token = await getToken();
  if (!token) {
    return;
  }

  const projectId = await getExpoProjectId();
  const pushTokenResponse = projectId
    ? await Notifications.getExpoPushTokenAsync({ projectId })
    : await Notifications.getExpoPushTokenAsync();

  await fetch(`${BASE_URL}/api/drivers/me/push-token`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ expoPushToken: pushTokenResponse.data }),
  });
}

export async function showImportantPhoneNotification(notification: DriverNotification) {
  if (!isPhoneNotificationImportant(notification)) {
    return;
  }

  if (AppState.currentState === "active") {
    return;
  }

  await ensurePhoneNotificationPermission();

  const permissions = await Notifications.getPermissionsAsync();
  if (!permissions.granted) {
    return;
  }

  const text = getDriverNotificationText(notification);

  await Notifications.scheduleNotificationAsync({
    content: {
      title: text.title,
      body: text.message,
      sound: true,
      data: { notificationId: notification.id },
    },
    trigger: null,
  });
}
