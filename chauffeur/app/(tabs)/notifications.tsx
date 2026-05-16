import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  RefreshControl,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  useColorScheme,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { BASE_URL } from "../../lib/api";
import { alertMessageFr } from "../../lib/alertMessages";
import { getToken } from "../../lib/storage";

type DriverNotificationType =
  | "MISSION_REASSIGNED"
  | "TRUCK_BREAKDOWN_HANDLED"
  | "SENSOR_BREAKDOWN_HANDLED"
  | "DELAY_DETECTED";

type DriverNotification = {
  id: number;
  type: DriverNotificationType;
  title: string;
  message: string;
  createdAt: string;
  read: boolean;
};

export default function NotificationsScreen() {
  const [notifications, setNotifications] = useState<DriverNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";

  const colors = isDark
    ? {
        container: "#0F172A",
        card: "#1A232D",
        text: "#F3F4F6",
        subtext: "#94A3B8",
        softText: "#CBD5E1",
        whiteBtn: "#1A232D",
        green: "#12905C",
        redSoft: "#2B1620",
        orangeSoft: "#3A2A12",
        blueSoft: "#172554",
        redText: "#FF6B6B",
        orangeText: "#FBBF24",
        blueText: "#60A5FA",
      }
    : {
        container: "#F3F5F7",
        card: "#FFFFFF",
        text: "#102A43",
        subtext: "#6B7C93",
        softText: "#7C8DA6",
        whiteBtn: "#FFFFFF",
        green: "#12905C",
        redSoft: "#FFF1F2",
        orangeSoft: "#FFF7ED",
        blueSoft: "#EEF4FF",
        redText: "#FF4D4F",
        orangeText: "#F59E0B",
        blueText: "#3B82F6",
      };

  useEffect(() => {
    loadNotifications();
  }, []);

  async function loadNotifications() {
    try {
      setLoading(true);
      const token = await getToken();

      const response = await fetch(`${BASE_URL}/api/driver/notifications`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Impossible de charger les notifications");
      }

      const data = await response.json();
      setNotifications(Array.isArray(data) ? data : []);
    } catch (error: any) {
      Alert.alert(
        "Erreur",
        alertMessageFr(error?.message, "Impossible de charger les notifications.")
      );
    } finally {
      setLoading(false);
    }
  }

  async function onRefresh() {
    try {
      setRefreshing(true);
      await loadNotifications();
    } finally {
      setRefreshing(false);
    }
  }

  function getTimeAgo(dateString: string) {
    const created = new Date(dateString).getTime();
    const now = Date.now();
    const diffMs = now - created;

    const minutes = Math.floor(diffMs / 60000);
    const hours = Math.floor(diffMs / 3600000);
    const days = Math.floor(diffMs / 86400000);

    if (minutes < 1) return "À l’instant";
    if (minutes < 60) return `Il y a ${minutes} min`;
    if (hours < 24) return `Il y a ${hours} h`;
    return `Il y a ${days} j`;
  }

  function getNotificationUI(type: DriverNotificationType) {
    switch (type) {
      case "MISSION_REASSIGNED":
        return {
          icon: "git-branch-outline" as const,
          iconColor: colors.blueText,
          bg: colors.blueSoft,
          label: "Mission",
        };
      case "TRUCK_BREAKDOWN_HANDLED":
        return {
          icon: "construct-outline" as const,
          iconColor: colors.orangeText,
          bg: colors.orangeSoft,
          label: "Camion",
        };
      case "SENSOR_BREAKDOWN_HANDLED":
        return {
          icon: "hardware-chip-outline" as const,
          iconColor: colors.orangeText,
          bg: colors.orangeSoft,
          label: "Capteur",
        };
      case "DELAY_DETECTED":
        return {
          icon: "time-outline" as const,
          iconColor: colors.redText,
          bg: colors.redSoft,
          label: "Retard",
        };
      default:
        return {
          icon: "notifications-outline" as const,
          iconColor: colors.blueText,
          bg: colors.blueSoft,
          label: "Notification",
        };
    }
  }

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.container }]}>
      <View style={styles.header}>
        <TouchableOpacity
          style={[styles.backButton, { backgroundColor: colors.whiteBtn }]}
          onPress={() => router.back()}
          activeOpacity={0.8}
        >
          <Ionicons name="arrow-back" size={20} color={colors.text} />
        </TouchableOpacity>

        <Text style={[styles.headerTitle, { color: colors.text }]}>Alertes</Text>

        <TouchableOpacity activeOpacity={0.8} onPress={onRefresh}>
          <Text style={[styles.refreshText, { color: colors.green }]}>Actualiser</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <View style={styles.loaderWrap}>
          <ActivityIndicator size="large" color="#12905C" />
          <Text style={[styles.loaderText, { color: colors.softText }]}>
            chargement alertes...
          </Text>
        </View>
      ) : (
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        >
          {notifications.length === 0 ? (
            <View style={[styles.emptyCard, { backgroundColor: colors.card }]}>
              <Ionicons name="checkmark-circle-outline" size={44} color="#10B981" />
              <Text style={[styles.emptyTitle, { color: colors.text }]}>Aucune alerte</Text>
              <Text style={[styles.emptyDesc, { color: colors.subtext }]}>
                Pas d&apos;alertes pour le moment.
              </Text>
            </View>
          ) : (
            notifications.map((item) => {
              const ui = getNotificationUI(item.type);

              return (
                <View key={item.id} style={[styles.card, { backgroundColor: colors.card }]}>
                  <View style={styles.left}>
                    <View style={[styles.iconWrap, { backgroundColor: ui.bg }]}>
                      <Ionicons name={ui.icon} size={20} color={ui.iconColor} />
                    </View>

                    <View style={styles.textWrap}>
                      <Text style={[styles.title, { color: colors.text }]}>
                        {item.title}
                      </Text>
                      <Text style={[styles.desc, { color: colors.subtext }]}>
                        {item.message}
                      </Text>

                      <View style={styles.metaRow}>
                        <Text
                          style={[
                            styles.badge,
                            { color: ui.iconColor, backgroundColor: ui.bg },
                          ]}
                        >
                          {ui.label}
                        </Text>
                        <Text style={[styles.time, { color: colors.softText }]}>
                          {getTimeAgo(item.createdAt)}
                        </Text>
                      </View>
                    </View>
                  </View>

                  {!item.read && (
                    <View style={[styles.greenDot, { backgroundColor: colors.green }]} />
                  )}
                </View>
              );
            })
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    paddingHorizontal: 18,
    paddingTop: 14,
    paddingBottom: 10,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  backButton: {
    width: 36,
    height: 36,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 3,
  },
  headerTitle: { fontSize: 20, fontWeight: "800" },
  refreshText: { fontWeight: "700", fontSize: 14 },
  content: { paddingHorizontal: 16, paddingBottom: 24 },
  loaderWrap: { flex: 1, justifyContent: "center", alignItems: "center" },
  loaderText: { marginTop: 12, fontSize: 14 },
  emptyCard: {
    borderRadius: 18,
    padding: 24,
    alignItems: "center",
    justifyContent: "center",
    marginTop: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.05,
    shadowRadius: 12,
    elevation: 4,
  },
  emptyTitle: { marginTop: 12, fontSize: 18, fontWeight: "800" },
  emptyDesc: { marginTop: 6, fontSize: 14, textAlign: "center" },
  card: {
    borderRadius: 18,
    padding: 16,
    marginBottom: 14,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.05,
    shadowRadius: 12,
    elevation: 4,
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
  },
  left: { flexDirection: "row", alignItems: "flex-start", flex: 1 },
  textWrap: { flex: 1 },
  iconWrap: {
    width: 38,
    height: 38,
    borderRadius: 19,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },
  title: { fontSize: 16, fontWeight: "800", marginBottom: 4 },
  desc: { fontSize: 14, marginBottom: 10, lineHeight: 20 },
  metaRow: { flexDirection: "row", alignItems: "center" },
  badge: {
    overflow: "hidden",
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    fontSize: 12,
    fontWeight: "800",
    marginRight: 8,
  },
  time: { fontSize: 12 },
  greenDot: {
    width: 8,
    height: 8,
    borderRadius: 99,
    marginLeft: 10,
    marginTop: 6,
  },
});
