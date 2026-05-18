import AsyncStorage from "@react-native-async-storage/async-storage";
import { router } from "expo-router";
import { useEffect, useRef } from "react";
import { Alert, AppState } from "react-native";
import { BASE_URL } from "../lib/api";
import { DriverNotification } from "../lib/driverNotifications";
import { showImportantPhoneNotification } from "../lib/phoneNotifications";
import { getToken } from "../lib/storage";

const LAST_IMPORTANT_NOTIFICATION_ID_KEY = "wise_last_important_notification_id";
const LAST_ROUTE_INDEX_KEY = "wise_last_route_index";
const LAST_DRIVER_POINT_KEY = "wise_last_driver_point";

export function DriverImportantNotificationWatcher() {
  const lastIdRef = useRef<number | null>(null);

  useEffect(() => {
    let mounted = true;

    async function handleNewNotification(notification: DriverNotification) {
      if (notification.type === "MISSION_CANCELLED") {
        await AsyncStorage.multiRemove([LAST_ROUTE_INDEX_KEY, LAST_DRIVER_POINT_KEY]);

        if (AppState.currentState === "active") {
          Alert.alert(
            "Mission annulee",
            notification.message || "Votre mission a ete annulee.",
            [
              {
                text: "OK",
                onPress: () => router.replace("/(tabs)/dashboard"),
              },
            ]
          );
          return;
        }
      }

      await showImportantPhoneNotification(notification);
    }

    async function loadLastId() {
      const saved = await AsyncStorage.getItem(LAST_IMPORTANT_NOTIFICATION_ID_KEY);
      const parsed = saved ? Number(saved) : null;
      if (!Number.isNaN(parsed)) {
        lastIdRef.current = parsed;
      }
    }

    async function checkNotifications() {
      try {
        const token = await getToken();
        if (!token || !mounted) return;

        const response = await fetch(`${BASE_URL}/api/driver/notifications`, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) return;

        const data: DriverNotification[] = await response.json();
        const list = Array.isArray(data) ? data : [];
        if (list.length === 0) return;

        const newest = list[0];

        if (lastIdRef.current === null) {
          lastIdRef.current = newest.id;
          await AsyncStorage.setItem(
            LAST_IMPORTANT_NOTIFICATION_ID_KEY,
            String(newest.id)
          );
          return;
        }

        if (newest.id !== lastIdRef.current) {
          lastIdRef.current = newest.id;
          await AsyncStorage.setItem(
            LAST_IMPORTANT_NOTIFICATION_ID_KEY,
            String(newest.id)
          );
          await handleNewNotification(newest);
        }
      } catch (error) {
        console.log("Important notification watcher error:", error);
      }
    }

    loadLastId().then(checkNotifications);
    const interval = setInterval(checkNotifications, 10000);

    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, []);

  return null;
}
