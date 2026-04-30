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
import { getToken } from "../../lib/storage";

type AlertItem = {
  id: number;
  binId: number;
  binCode: string;
  telemetryId: number | null;
  alertType: string;
  severity: string;
  title: string;
  message: string;
  createdAt: string;
  resolved: boolean;
  resolvedAt: string | null;
  resolvedByUserId: number | null;
};

export default function NotificationsScreen() {
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [resolvingId, setResolvingId] = useState<number | null>(null);

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
        redSoft: "#2B1620",
        orangeSoft: "#3A2A12",
        blueSoft: "#172554",
        redText: "#FF6B6B",
        orangeText: "#FBBF24",
        blueText: "#60A5FA",
        green: "#12905C",
        borderLeft: "#12905C",
      }
    : {
        container: "#F3F5F7",
        card: "#FFFFFF",
        text: "#102A43",
        subtext: "#6B7C93",
        softText: "#7C8DA6",
        whiteBtn: "#FFFFFF",
        redSoft: "#FFF1F2",
        orangeSoft: "#FFF7ED",
        blueSoft: "#EEF4FF",
        redText: "#FF4D4F",
        orangeText: "#F59E0B",
        blueText: "#3B82F6",
        green: "#12905C",
        borderLeft: "#12905C",
      };

  useEffect(() => {
    loadAlerts();
  }, []);

  async function loadAlerts() {
    try {
      setLoading(true);

      const token = await getToken();

      const response = await fetch(`${BASE_URL}/api/alerts/open`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Failed to load alerts");
      }

      const data = await response.json();
      setAlerts(data);
    } catch (error: any) {
      Alert.alert(
        "Erreur",
        error?.message || "Impossible de charger les alertes."
      );
    } finally {
      setLoading(false);
    }
  }

  async function onRefresh() {
    try {
      setRefreshing(true);

      const token = await getToken();

      const response = await fetch(`${BASE_URL}/api/alerts/open`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Failed to load alerts");
      }

      const data = await response.json();
      setAlerts(data);
    } catch (error: any) {
      Alert.alert(
        "Erreur",
        error?.message || "Impossible de rafraîchir les alertes."
      );
    } finally {
      setRefreshing(false);
    }
  }

  async function resolveAlert(alertId: number) {
    try {
      setResolvingId(alertId);

      const token = await getToken();

      const response = await fetch(`${BASE_URL}/api/alerts/${alertId}/resolve`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Failed to resolve alert");
      }

      Alert.alert("Succès", "Alerte résolue.");
      await loadAlerts();
    } catch (error: any) {
      Alert.alert(
        "Erreur",
        error?.message || "Impossible de résoudre l’alerte."
      );
    } finally {
      setResolvingId(null);
    }
  }

  function getTimeAgo(dateString: string) {
    const created = new Date(dateString).getTime();
    const now = Date.now();
    const diffMs = now - created;

    const minutes = Math.floor(diffMs / 60000);
    const hours = Math.floor(diffMs / 3600000);
    const days = Math.floor(diffMs / 86400000);

    if (minutes < 1) return "Just now";
    if (minutes < 60) return `${minutes} min ago`;
    if (hours < 24) return `${hours} hour${hours > 1 ? "s" : ""} ago`;
    return `${days} day${days > 1 ? "s" : ""} ago`;
  }

  function getAlertIcon(alert: AlertItem) {
    const severity = alert.severity?.toUpperCase();
    const type = alert.alertType?.toUpperCase();

    if (severity === "HIGH") {
      return {
        icon: "warning-outline" as const,
        iconColor: colors.redText,
        wrapStyle: [styles.iconWrapRed, { backgroundColor: colors.redSoft }],
        cardStyle: [
          styles.cardImportant,
          { backgroundColor: colors.card, borderLeftColor: colors.borderLeft },
        ],
      };
    }

    if (type === "MAINTENANCE") {
      return {
        icon: "build-outline" as const,
        iconColor: colors.orangeText,
        wrapStyle: [styles.iconWrapOrange, { backgroundColor: colors.orangeSoft }],
        cardStyle: [
          styles.cardImportant,
          { backgroundColor: colors.card, borderLeftColor: colors.borderLeft },
        ],
      };
    }

    if (type === "SYSTEM") {
      return {
        icon: "alert-circle-outline" as const,
        iconColor: colors.orangeText,
        wrapStyle: [styles.iconWrapOrange, { backgroundColor: colors.orangeSoft }],
        cardStyle: [
          styles.cardImportant,
          { backgroundColor: colors.card, borderLeftColor: colors.borderLeft },
        ],
      };
    }

    return {
      icon: "notifications-outline" as const,
      iconColor: colors.blueText,
      wrapStyle: [styles.iconWrapBlue, { backgroundColor: colors.blueSoft }],
      cardStyle: [styles.card, { backgroundColor: colors.card }],
    };
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

        <Text style={[styles.headerTitle, { color: colors.text }]}>
          Notifications
        </Text>

        <TouchableOpacity activeOpacity={0.8} onPress={onRefresh}>
          <Text style={[styles.clearAll, { color: colors.green }]}>Refresh</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <View style={styles.loaderWrap}>
          <ActivityIndicator size="large" color="#12905C" />
          <Text style={[styles.loaderText, { color: colors.softText }]}>
            Loading alerts...
          </Text>
        </View>
      ) : (
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.content}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
        >
          {alerts.length === 0 ? (
            <View style={[styles.emptyCard, { backgroundColor: colors.card }]}>
              <Ionicons
                name="checkmark-circle-outline"
                size={40}
                color="#10B981"
              />
              <Text style={[styles.emptyTitle, { color: colors.text }]}>
                No open alerts
              </Text>
              <Text style={[styles.emptyDesc, { color: colors.subtext }]}>
                All alerts are resolved for now.
              </Text>
            </View>
          ) : (
            alerts.map((item) => {
              const alertUI = getAlertIcon(item);

              return (
                <View key={item.id} style={alertUI.cardStyle}>
                  <View style={styles.left}>
                    <View style={alertUI.wrapStyle}>
                      <Ionicons
                        name={alertUI.icon}
                        size={18}
                        color={alertUI.iconColor}
                      />
                    </View>

                    <View style={styles.textWrap}>
                      <Text style={[styles.title, { color: colors.text }]}>
                        {item.title}
                      </Text>

                      <Text style={[styles.desc, { color: colors.subtext }]}>
                        {item.message}
                        {item.binCode ? ` (${item.binCode})` : ""}
                      </Text>

                      <Text style={[styles.metaText, { color: colors.softText }]}>
                        Severity: {item.severity} • Type: {item.alertType}
                      </Text>

                      <Text style={[styles.time, { color: colors.softText }]}>
                        {getTimeAgo(item.createdAt)}
                      </Text>

                      <TouchableOpacity
                        style={styles.resolveButton}
                        activeOpacity={0.85}
                        onPress={() => resolveAlert(item.id)}
                        disabled={resolvingId === item.id}
                      >
                        {resolvingId === item.id ? (
                          <ActivityIndicator color="#FFFFFF" size="small" />
                        ) : (
                          <>
                            <Ionicons
                              name="checkmark-circle-outline"
                              size={16}
                              color="#FFFFFF"
                            />
                            <Text style={styles.resolveButtonText}>
                              Resolve
                            </Text>
                          </>
                        )}
                      </TouchableOpacity>
                    </View>
                  </View>

                  <View style={[styles.greenDot, { backgroundColor: colors.green }]} />
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
  container: {
    flex: 1,
  },

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

  headerTitle: {
    fontSize: 20,
    fontWeight: "700",
  },

  clearAll: {
    fontWeight: "600",
    fontSize: 14,
  },

  content: {
    paddingHorizontal: 16,
    paddingBottom: 24,
  },

  loaderWrap: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },

  loaderText: {
    marginTop: 12,
    fontSize: 14,
  },

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

  emptyTitle: {
    marginTop: 12,
    fontSize: 18,
    fontWeight: "700",
  },

  emptyDesc: {
    marginTop: 6,
    fontSize: 14,
    textAlign: "center",
  },

  cardImportant: {
    borderRadius: 18,
    padding: 16,
    marginBottom: 14,
    borderLeftWidth: 3,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.05,
    shadowRadius: 12,
    elevation: 4,
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
  },

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

  left: {
    flexDirection: "row",
    alignItems: "flex-start",
    flex: 1,
  },

  textWrap: {
    flex: 1,
  },

  iconWrapRed: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },

  iconWrapOrange: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },

  iconWrapBlue: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },

  title: {
    fontSize: 16,
    fontWeight: "700",
    marginBottom: 4,
  },

  desc: {
    fontSize: 14,
    marginBottom: 8,
    lineHeight: 20,
  },

  metaText: {
    fontSize: 12,
    marginBottom: 6,
  },

  time: {
    fontSize: 12,
    marginBottom: 10,
  },

  resolveButton: {
    marginTop: 4,
    alignSelf: "flex-start",
    backgroundColor: "#12905C",
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 8,
    flexDirection: "row",
    alignItems: "center",
  },

  resolveButtonText: {
    color: "#FFFFFF",
    fontWeight: "700",
    marginLeft: 6,
    fontSize: 13,
  },

  greenDot: {
    width: 7,
    height: 7,
    borderRadius: 99,
    marginLeft: 10,
    marginTop: 6,
  },
});