import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";
import { LinearGradient } from "expo-linear-gradient";
import { router } from "expo-router";
import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  useColorScheme,
} from "react-native";
import MapView, { Geojson, Marker } from "react-native-maps";
import { alertMessageFr } from "../../lib/alertMessages";
import { getToken, getUserId, saveTruckId } from "../../lib/storage";
import { getMyTruckIncidents, sendTruckLocation } from "../../lib/truckApi";
import { formatWasteTypeFr } from "../../lib/wasteType";

type DriverBin = {
  missionId?: number;
  missionBinId?: number;
  binId?: number;
  binCode?: string;
  lat?: number;
  lng?: number;
  visitOrder?: number;
  collected?: boolean;
  assignmentStatus?: string;
  wasteType?: string;
};

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

type RouteStats = {
  totalDistanceKm: number | null;
  estimatedDurationMin: number | null;
};

const paris15: any = {
  type: "FeatureCollection",
  features: [],
};

const BASE_URL = "http://192.168.1.209:8081";

function formatTruckLabel(value: string) {
  return value.replace(/^TRUCK/i, "CAMION");
}

function formatDuration(minutes: number | null) {
  if (minutes == null) return "--";
  if (minutes < 60) return `${Math.round(minutes)} min`;

  const hours = Math.floor(minutes / 60);
  const rest = Math.round(minutes % 60);

  return rest > 0 ? `${hours} h ${rest} min` : `${hours} h`;
}

function formatDistance(km: number | null) {
  if (km == null) return "--";
  if (km < 1) return `${Math.round(km * 1000)} m`;
  return `${km.toFixed(1)} km`;
}

export default function Dashboard() {
  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";

  const [driverName, setDriverName] = useState("Chauffeur");
  const [truckId, setTruckId] = useState("Non assigné");
  const [bins, setBins] = useState<DriverBin[]>([]);
  const [loadingBins, setLoadingBins] = useState(true);
  const [routeStats, setRouteStats] = useState<RouteStats>({
    totalDistanceKm: null,
    estimatedDurationMin: null,
  });

  const [toast, setToast] = useState<DriverNotification | null>(null);
  const [hasUnreadNotifications, setHasUnreadNotifications] = useState(false);
  const lastNotificationIdRef = useRef<number | null>(null);
  const toastTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadDashboardHeader = useCallback(async () => {
    try {
      const token = await getToken();
      const userId = await getUserId();

      console.log("AUTH USER ID:", userId);
      console.log("TOKEN EXISTS:", !!token);

      if (!token || !userId) {
        setDriverName("Chauffeur");
        setTruckId("Non assigné");
        return;
      }

      const response = await fetch(`${BASE_URL}/api/drivers/${userId}/profile`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const text = await response.text();

      console.log("PROFILE STATUS:", response.status);
      console.log("PROFILE RAW RESPONSE:", text);

      if (!response.ok) {
        throw new Error(text);
      }

      const data = text ? JSON.parse(text) : {};
      console.log("PROFILE DATA:", data);
      console.log("ASSIGNED TRUCK:", data.assignedTruck);
      console.log("ASSIGNED TRUCK ID:", data.assignedTruckId);

      setDriverName(data.fullName || "Chauffeur");
      setTruckId(data.assignedTruck || "Non assigné");

      if (data.assignedTruckId) {
        await saveTruckId(data.assignedTruckId);
        console.log("TRUCK ID SAVED:", data.assignedTruckId);
      } else {
        console.log("NO assignedTruckId FOUND IN PROFILE");
      }
    } catch (error) {
      console.log("Erreur header:", error);
      setDriverName("Chauffeur");
      setTruckId("Non assigné");
    }
  }, []);

  const fetchMyBins = useCallback(async () => {
    try {
      setLoadingBins(true);

      const token = await getToken();
      const userId = await getUserId();

      if (!token || !userId) {
        setBins([]);
        return;
      }

      const response = await fetch(`${BASE_URL}/api/drivers/${userId}/my-bins`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      const text = await response.text();

      if (!response.ok) {
        throw new Error(`Erreur ${response.status}: ${text}`);
      }

      const data = text ? JSON.parse(text) : [];
      const list: DriverBin[] = Array.isArray(data) ? data : [];
      setBins(list);

      const missionId = list.find((bin) => !!bin.missionId)?.missionId;

      if (!missionId) {
        setRouteStats({ totalDistanceKm: null, estimatedDurationMin: null });
        return;
      }

      const routeResponse = await fetch(`${BASE_URL}/api/missions/${missionId}/route`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      const routeText = await routeResponse.text();

      if (!routeResponse.ok) {
        console.log("Erreur route mission:", routeText);
        setRouteStats({ totalDistanceKm: null, estimatedDurationMin: null });
        return;
      }

      const routeData = routeText ? JSON.parse(routeText) : {};
      setRouteStats({
        totalDistanceKm:
          typeof routeData.totalDistanceKm === "number"
            ? routeData.totalDistanceKm
            : null,
        estimatedDurationMin:
          typeof routeData.estimatedDurationMin === "number"
            ? routeData.estimatedDurationMin
            : null,
      });
    } catch (error) {
      console.log("Erreur bins:", error);
      setBins([]);
      setRouteStats({ totalDistanceKm: null, estimatedDurationMin: null });
    } finally {
      setLoadingBins(false);
    }
  }, []);

  function getToastIcon(type: DriverNotificationType) {
    switch (type) {
      case "MISSION_REASSIGNED":
        return "git-branch-outline" as const;
      case "TRUCK_BREAKDOWN_HANDLED":
        return "construct-outline" as const;
      case "SENSOR_BREAKDOWN_HANDLED":
        return "hardware-chip-outline" as const;
      case "DELAY_DETECTED":
        return "time-outline" as const;
      default:
        return "notifications-outline" as const;
    }
  }

  function getToastStyle(type: DriverNotificationType) {
    switch (type) {
      case "MISSION_REASSIGNED":
        return {
          bg: colors.blueSoft,
          iconColor: "#3B82F6",
        };
      case "TRUCK_BREAKDOWN_HANDLED":
      case "SENSOR_BREAKDOWN_HANDLED":
        return {
          bg: colors.warningSoft,
          iconColor: colors.orangeText,
        };
      case "DELAY_DETECTED":
        return {
          bg: colors.dangerSoft,
          iconColor: colors.redText,
        };
      default:
        return {
          bg: colors.blueSoft,
          iconColor: "#3B82F6",
        };
    }
  }

  function showDriverToast(notification: DriverNotification) {
    if (toastTimerRef.current) {
      clearTimeout(toastTimerRef.current);
    }

    setToast(notification);
    setHasUnreadNotifications(true);

    toastTimerRef.current = setTimeout(() => {
      setToast(null);
    }, 60000);
  }

  const loadDriverNotifications = useCallback(
    async (showToastIfNew = false) => {
      try {
        const token = await getToken();

        if (!token) return;

        const response = await fetch(`${BASE_URL}/api/driver/notifications`, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          const text = await response.text();
          console.log("Driver notifications response error:", text);
          return;
        }

        const data: DriverNotification[] = await response.json();
        const list = Array.isArray(data) ? data : [];

        setHasUnreadNotifications(list.some((item) => !item.read));

        if (list.length === 0) return;

        const newest = list[0];

        if (lastNotificationIdRef.current === null) {
          lastNotificationIdRef.current = newest.id;
          return;
        }

        if (showToastIfNew && newest.id !== lastNotificationIdRef.current) {
          lastNotificationIdRef.current = newest.id;
          showDriverToast(newest);
          fetchMyBins();
        }
      } catch (error) {
        console.log("Driver notifications error:", error);
      }
    },
    [fetchMyBins]
  );

  useEffect(() => {
    sendTruckLocation().catch(console.log);

    const interval = setInterval(() => {
      sendTruckLocation().catch(console.log);
    }, 10000);

    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    loadDriverNotifications(false);

    const interval = setInterval(() => {
      loadDriverNotifications(true);
    }, 10000);

    return () => {
      clearInterval(interval);
      if (toastTimerRef.current) {
        clearTimeout(toastTimerRef.current);
      }
    };
  }, [loadDriverNotifications]);

  useFocusEffect(
    useCallback(() => {
      fetchMyBins();
      loadDriverNotifications(false);

      loadDashboardHeader().then(() => {
        getMyTruckIncidents()
          .then((data) => console.log("MY TRUCK INCIDENTS:", data))
          .catch((err) => console.log("INCIDENTS ERROR:", err));
      });
    }, [fetchMyBins, loadDashboardHeader, loadDriverNotifications])
  );

  const colors = isDark
    ? {
      container: "#0F172A",
      card: "#1A232D",
      softCard: "#111827",
      text: "#F3F4F6",
      subtext: "#94A3B8",
      border: "#2A3642",
      whiteBorder: "#334155",
      dangerSoft: "#2B1620",
      warningSoft: "#3A2A12",
      greenSoft: "#123226",
      blueSoft: "#172554",
      redText: "#FF6B6B",
      orangeText: "#FBBF24",
      greenText: "#34D399",
    }
    : {
      container: "#F3F5F7",
      card: "#FFFFFF",
      softCard: "#F7F9FB",
      text: "#102A43",
      subtext: "#7C8DA6",
      border: "#BDE8D2",
      whiteBorder: "#D9DEE5",
      dangerSoft: "#FFF1F2",
      warningSoft: "#FFF7ED",
      greenSoft: "#E9FBF3",
      blueSoft: "#EEF4FF",
      redText: "#EF4444",
      orangeText: "#F59E0B",
      greenText: "#10B981",
    };

  const totalBins = bins.length;
  const collectedBins = bins.filter((bin) => bin.collected).length;
  const pendingBins = bins.filter((bin) => !bin.collected);
  const progressPercent =
    totalBins > 0 ? Math.round((collectedBins / totalBins) * 100) : 0;

  const mapLatitude = bins[0]?.lat || 48.8414;
  const mapLongitude = bins[0]?.lng || 2.3003;
  const toastUI = toast ? getToastStyle(toast.type) : null;
  const estimatedTimeText = loadingBins
    ? "--"
    : formatDuration(routeStats.estimatedDurationMin);
  const totalDistanceText = loadingBins
    ? "--"
    : formatDistance(routeStats.totalDistanceKm);

  async function startCurrentMissionAndOpenRoute() {
    try {
      const token = await getToken();

      if (!token) {
        Alert.alert("Erreur", "Session expirée. Veuillez vous reconnecter.");
        return;
      }

      const missionId = bins.find((b) => !!b.missionId)?.missionId;

      if (!missionId) {
        Alert.alert("Erreur", "Aucune mission active trouvée.");
        return;
      }

      const response = await fetch(`${BASE_URL}/api/missions/${missionId}/start`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({}),
      });

      const text = await response.text();

      if (!response.ok) {
        throw new Error(text || "Impossible de démarrer la mission.");
      }

      await fetchMyBins();

      router.push("/route-map");
    } catch (error: any) {
      console.log("START MISSION ERROR:", error);
      Alert.alert(
        "Erreur",
        alertMessageFr(error?.message, "Impossible de démarrer la mission."),
        [{ text: "D'accord" }]
      );
    }
  }

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.container }]}
      contentContainerStyle={styles.contentContainer}
      showsVerticalScrollIndicator={false}
    >
      <LinearGradient colors={["#0E8E63", "#19C37D"]} style={styles.header}>
        <View style={styles.headerTopRow}>
          <View style={styles.headerTextBlock}>
            <Text style={styles.name}>{driverName}</Text>
          </View>

          <View style={styles.headerIcons}>
            <TouchableOpacity
              activeOpacity={0.8}
              style={styles.iconButton}
              onPress={() => {
                setHasUnreadNotifications(false);
                router.push("/notifications");
              }}
            >
              <Ionicons name="notifications-outline" size={20} color="#FFFFFF" />
              {hasUnreadNotifications && <View style={styles.notificationDot} />}
            </TouchableOpacity>

            <TouchableOpacity
              activeOpacity={0.8}
              style={[styles.iconButton, styles.secondIconButton]}
              onPress={() => router.push("/profile")}
            >
              <Ionicons name="person-outline" size={20} color="#FFFFFF" />
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.truckCard}>
          <View style={styles.truckIconBox}>
            <Ionicons name="trash-outline" size={20} color="#fff" />
          </View>

          <View style={styles.truckTextWrap}>
            <Text style={styles.truckLabel}>ID camion</Text>
            <Text style={styles.truckId}>{formatTruckLabel(truckId)}</Text>
          </View>
        </View>
      </LinearGradient>


      {toast && toastUI && (
        <View style={[styles.liveToast, { backgroundColor: colors.card }]}>
          <View style={styles.alertLeft}>
            <View style={[styles.liveToastIcon, { backgroundColor: toastUI.bg }]}>
              <Ionicons
                name={getToastIcon(toast.type)}
                size={20}
                color={toastUI.iconColor}
              />
            </View>

            <View style={styles.alertTextWrap}>
              <Text style={[styles.alertTitle, { color: colors.text }]}>
                {toast.title}
              </Text>
              <Text style={[styles.alertTime, { color: colors.subtext }]}>
                {toast.message}
              </Text>
            </View>
          </View>

          <TouchableOpacity onPress={() => setToast(null)} activeOpacity={0.8}>
            <Ionicons name="close" size={18} color={colors.subtext} />
          </TouchableOpacity>
        </View>
      )}





      <View style={[styles.diagramCard, { backgroundColor: colors.card }]}>
        <View style={styles.diagramHeader}>
          <View style={styles.topCenterBadgeMini}>
            <View style={styles.badgeDot} />
            <Text style={styles.topCenterBadgeText}>Tournée active</Text>
          </View>

          <TouchableOpacity style={styles.progressButtonMini}>
            <Text style={styles.progressButtonText}>Progression</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.mapAreaMini}>
          <MapView
            style={styles.realMap}
            initialRegion={{
              latitude: mapLatitude,
              longitude: mapLongitude,
              latitudeDelta: 0.025,
              longitudeDelta: 0.025,
            }}
          >
            <Geojson
              geojson={paris15 as any}
              strokeColor="#22C55E"
              fillColor="rgba(34,197,94,0.12)"
              strokeWidth={2}
            />

            <Marker
              coordinate={{
                latitude: mapLatitude,
                longitude: mapLongitude,
              }}
              title={formatTruckLabel(truckId)}
              description="Position du camion"
              pinColor="blue"
            />

            {bins.map((bin, index) =>
              typeof bin.lat === "number" && typeof bin.lng === "number" ? (
                <Marker
                  key={bin.missionBinId ?? bin.binId ?? index}
                  coordinate={{
                    latitude: bin.lat,
                    longitude: bin.lng,
                  }}
                  title={bin.binCode || `Bin ${index + 1}`}
                  description={bin.collected ? "Collectée" : "En attente"}
                  pinColor={bin.collected ? "green" : "red"}
                />
              ) : null
            )}
          </MapView>
        </View>
      </View>

      <View style={[styles.card, { backgroundColor: colors.card }]}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>
          Tournée du Jour
        </Text>

        <View
          style={[
            styles.progressCard,
            {
              backgroundColor: colors.greenSoft,
              borderColor: colors.border,
            },
          ]}
        >
          <View style={styles.progressHeader}>
            <View>
              <Text style={[styles.progressLabel, { color: colors.subtext }]}>
                Progression de la tournée
              </Text>
              <Text style={[styles.progressValue, { color: colors.text }]}>
                {collectedBins} / {totalBins}
              </Text>
            </View>

            <View style={styles.progressRight}>
              <Text style={styles.progressPercent}>{progressPercent}%</Text>
              <Text style={[styles.progressSubText, { color: colors.subtext }]}>
                Terminée
              </Text>
            </View>
          </View>

          <View
            style={[
              styles.progressBar,
              { backgroundColor: isDark ? "#243244" : "#E5EAF0" },
            ]}
          >
            <View style={[styles.progressFill, { width: `${progressPercent}%` }]} />
          </View>
        </View>

        <View style={[styles.infoItem, { backgroundColor: colors.softCard }]}>
          <View style={styles.infoLeft}>
            <View style={[styles.infoIconGreen, { backgroundColor: colors.greenSoft }]}>
              <Ionicons name="trash-outline" size={18} color="#10B981" />
            </View>
            <View>
              <Text style={[styles.infoTitle, { color: colors.subtext }]}>
                Total des bacs
              </Text>
              <Text style={[styles.infoNumber, { color: colors.text }]}>
                {totalBins}
              </Text>
            </View>
          </View>

          <View style={styles.infoRight}>
            <Text style={[styles.infoTitle, { color: colors.subtext }]}>
              Collectées
            </Text>
            <Text style={[styles.infoNumber, { color: colors.text }]}>
              {collectedBins}
            </Text>
          </View>
        </View>

        <View style={[styles.infoItem, { backgroundColor: colors.softCard }]}>
          <View style={styles.infoLeft}>
            <View style={[styles.infoIconGreen, { backgroundColor: colors.greenSoft }]}>
              <Ionicons name="time-outline" size={18} color="#10B981" />
            </View>
            <View>
              <Text style={[styles.infoTitle, { color: colors.subtext }]}>
                Temps estimé
              </Text>
              <Text style={[styles.infoNumber, { color: colors.text }]}>
                {estimatedTimeText}
              </Text>
            </View>
          </View>
        </View>

        <View style={[styles.infoItem, { backgroundColor: colors.softCard }]}>
          <View style={styles.infoLeft}>
            <View style={[styles.infoIconBlue, { backgroundColor: colors.blueSoft }]}>
              <Ionicons name="git-network-outline" size={18} color="#3B82F6" />
            </View>
            <View>
              <Text style={[styles.infoTitle, { color: colors.subtext }]}>
                Distance totale
              </Text>
              <Text style={[styles.infoNumber, { color: colors.text }]}>
                {totalDistanceText}
              </Text>
            </View>
          </View>
        </View>

        <View
          style={[
            styles.statusRow,
            {
              backgroundColor: colors.greenSoft,
              borderColor: colors.border,
            },
          ]}
        >
          <View style={styles.statusLeft}>
            <View style={styles.statusDot} />
            <Text style={[styles.statusText, { color: colors.text }]}>
              Statut de la tournée
            </Text>
          </View>
          <Text style={styles.statusReady}>
            {loadingBins
              ? "Chargement..."
              : totalBins === 0
                ? "Aucun bac assigné"
                : collectedBins === totalBins
                  ? "Terminée"
                  : "Prête à démarrer"}
          </Text>
        </View>

        <TouchableOpacity
          style={styles.startButton}
          onPress={startCurrentMissionAndOpenRoute}
          activeOpacity={0.85}
          disabled={loadingBins || totalBins === 0 || collectedBins === totalBins}
        >
          <Ionicons name="play-outline" size={18} color="white" />
          <Text style={styles.startText}>Démarrer la tournée</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            styles.mapButton,
            {
              backgroundColor: colors.card,
              borderColor: colors.whiteBorder,
            },
          ]}
          activeOpacity={0.85}
          onPress={() =>
            router.push({
              pathname: "/declare-breakdown",
              params: { context: "truck" },
            })
          }
        >
          <Ionicons name="warning-outline" size={18} color="#EF4444" />
          <Text style={[styles.mapText, { color: colors.text }]}>
            Signaler problème camion
          </Text>
        </TouchableOpacity>
      </View>

      <View style={[styles.card, { backgroundColor: colors.card }]}>
        <View style={styles.rowBetween}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>
            Prochains bacs
          </Text>
        </View>

        {loadingBins ? (
          <Text style={[styles.binAddress, { color: colors.subtext }]}>
            Chargement...
          </Text>
        ) : pendingBins.length === 0 ? (
          <Text style={[styles.binAddress, { color: colors.subtext }]}>
            Aucun bac assigné aujourd’hui
          </Text>
        ) : (
          pendingBins.map((bin, index) => {
            const isFirst = index === 0;

            return (
              <View
                key={bin.missionBinId ?? bin.binId ?? index}
                style={[
                  isFirst ? styles.binCardActive : styles.binCard,
                  {
                    backgroundColor: isFirst ? colors.greenSoft : colors.softCard,
                    borderColor: isFirst ? colors.border : "transparent",
                  },
                ]}
              >
                <View style={styles.binLeft}>
                  <View
                    style={[
                      isFirst ? styles.binCircleRed : styles.binCircleGreen,
                      {
                        backgroundColor: isFirst
                          ? colors.dangerSoft
                          : colors.greenSoft,
                      },
                    ]}
                  >
                    <Text
                      style={[
                        isFirst
                          ? styles.binCircleTextRed
                          : styles.binCircleTextGreen,
                        { color: isFirst ? colors.redText : colors.greenText },
                      ]}
                    >
                      {bin.visitOrder ?? index + 1}
                    </Text>
                  </View>

                  <View style={styles.binContent}>
                    <Text style={[styles.binTitle, { color: colors.text }]}>
                      #{bin.binCode || "BIN"}
                    </Text>

                    <Text style={[styles.binAddress, { color: colors.subtext }]}>
                      {bin.wasteType ? `Déchet: ${formatWasteTypeFr(bin.wasteType)}` : "Bac assigné"}
                    </Text>

                    <View style={styles.binMetaRow}>
                      <Text style={[styles.binMetaGreen, { color: colors.orangeText }]}>
                        Planifiée
                      </Text>
                    </View>
                  </View>
                </View>

              </View>
            );
          })
        )}
      </View>

      <View style={styles.statsRow}>
        <View style={[styles.statCard, { backgroundColor: colors.card }]}>
          <Text style={[styles.statTitle, { color: colors.subtext }]}>
            Cette semaine
          </Text>
          <Text style={styles.statValue}>{collectedBins}</Text>
          <Text style={[styles.statSub, { color: colors.subtext }]}>
            Bacs collectés
          </Text>
        </View>

        <View
          style={[
            styles.statCard,
            styles.secondStatCard,
            { backgroundColor: colors.card },
          ]}
        >
          <Text style={[styles.statTitle, { color: colors.subtext }]}>
            Efficacité
          </Text>
          <Text style={styles.statValue}>{progressPercent}%</Text>
          <Text style={[styles.statSub, { color: colors.subtext }]}>
            Taux de réalisation
          </Text>
        </View>
      </View>

    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  contentContainer: { paddingBottom: 24 },

  header: {
    paddingHorizontal: 20,
    paddingTop: 24,
    paddingBottom: 26,
    borderBottomLeftRadius: 28,
    borderBottomRightRadius: 28,
  },
  headerTopRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    width: "100%",
    marginBottom: 18,
  },
  headerTextBlock: { flex: 1, paddingRight: 12 },
  name: { color: "#FFFFFF", fontSize: 22, fontWeight: "800" },
  headerIcons: { flexDirection: "row", alignItems: "center" },
  iconButton: {
    width: 42,
    height: 42,
    borderRadius: 14,
    backgroundColor: "rgba(255,255,255,0.14)",
    alignItems: "center",
    justifyContent: "center",
    position: "relative",
  },
  secondIconButton: { marginLeft: 10 },
  notificationDot: {
    position: "absolute",
    top: 9,
    right: 9,
    width: 8,
    height: 8,
    borderRadius: 99,
    backgroundColor: "#FF4D4F",
    borderWidth: 1.5,
    borderColor: "#16A34A",
  },

  truckCard: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(255,255,255,0.12)",
    borderRadius: 16,
    padding: 14,
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.18)",
  },
  truckIconBox: {
    width: 38,
    height: 38,
    borderRadius: 14,
    backgroundColor: "rgba(255,255,255,0.16)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  truckTextWrap: { flex: 1 },
  truckLabel: { color: "#D8FFF0", fontSize: 12, marginBottom: 2 },
  truckId: { color: "#FFFFFF", fontSize: 18, fontWeight: "700" },

  liveToast: {
    marginHorizontal: 16,
    marginTop: -12,
    marginBottom: 12,
    paddingVertical: 14,
    paddingHorizontal: 14,
    borderRadius: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 12,
    elevation: 8,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    zIndex: 999,
  },
  liveToastIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },

  alertCardRed: {
    marginHorizontal: 16,
    marginTop: -12,
    marginBottom: 12,
    paddingVertical: 14,
    paddingHorizontal: 14,
    borderRadius: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 4,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  alertCardOrange: {
    marginHorizontal: 16,
    marginBottom: 14,
    paddingVertical: 14,
    paddingHorizontal: 14,
    borderRadius: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 4,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  alertLeft: {
    flexDirection: "row",
    alignItems: "center",
    flex: 1,
    marginRight: 10,
  },
  alertTextWrap: { flex: 1 },
  alertIconRed: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  alertIconOrange: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  alertTitle: { fontSize: 13, fontWeight: "600", marginBottom: 3 },
  alertTime: { fontSize: 12 },

  diagramCard: {
    marginHorizontal: 16,
    marginBottom: 14,
    borderRadius: 20,
    padding: 14,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.05,
    shadowRadius: 12,
    elevation: 4,
  },
  diagramHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 10,
  },
  topCenterBadgeMini: {
    backgroundColor: "#FFFFFF",
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 10,
    flexDirection: "row",
    alignItems: "center",
    elevation: 2,
  },
  badgeDot: {
    width: 8,
    height: 8,
    borderRadius: 99,
    backgroundColor: "#10B981",
    marginRight: 8,
  },
  topCenterBadgeText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#1F2937",
  },
  progressButtonMini: {
    backgroundColor: "#159A61",
    borderRadius: 14,
    paddingHorizontal: 15,
    paddingVertical: 10,
  },
  progressButtonText: { color: "#FFFFFF", fontWeight: "700" },
  mapAreaMini: {
    height: 220,
    backgroundColor: "#EEF1F4",
    borderRadius: 18,
    position: "relative",
    overflow: "hidden",
  },
  realMap: { width: "100%", height: "100%" },

  card: {
    marginHorizontal: 16,
    marginBottom: 14,
    borderRadius: 20,
    padding: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.05,
    shadowRadius: 12,
    elevation: 4,
  },
  sectionTitle: { fontSize: 18, fontWeight: "700", marginBottom: 14 },
  progressCard: {
    borderRadius: 16,
    borderWidth: 1,
    padding: 14,
    marginBottom: 14,
  },
  progressHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-end",
    marginBottom: 12,
  },
  progressLabel: { fontSize: 13, marginBottom: 4 },
  progressValue: { fontSize: 18, fontWeight: "800" },
  progressRight: { alignItems: "flex-end" },
  progressPercent: { fontSize: 18, fontWeight: "800", color: "#0E8E63" },
  progressSubText: { fontSize: 12, marginTop: 2 },
  progressBar: { height: 10, borderRadius: 999, overflow: "hidden" },
  progressFill: {
    width: "0%",
    height: "100%",
    backgroundColor: "#19C37D",
    borderRadius: 999,
  },

  infoItem: {
    borderRadius: 16,
    padding: 14,
    marginBottom: 12,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  infoLeft: { flexDirection: "row", alignItems: "center" },
  infoRight: { alignItems: "flex-end" },
  infoIconGreen: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  infoIconBlue: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  infoTitle: { fontSize: 12, marginBottom: 3 },
  infoNumber: { fontSize: 16, fontWeight: "700" },

  statusRow: {
    borderRadius: 14,
    borderWidth: 1,
    paddingVertical: 12,
    paddingHorizontal: 14,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 14,
  },
  statusLeft: { flexDirection: "row", alignItems: "center" },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 99,
    backgroundColor: "#64748B",
    marginRight: 8,
  },
  statusText: { fontSize: 14 },
  statusReady: {
    color: "#0E8E63",
    fontWeight: "600",
    fontSize: 14,
  },

  startButton: {
    flexDirection: "row",
    backgroundColor: "#12905C",
    paddingVertical: 14,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 10,
  },
  startText: {
    color: "white",
    marginLeft: 6,
    fontSize: 16,
    fontWeight: "700",
  },
  mapButton: {
    flexDirection: "row",
    borderWidth: 1,
    paddingVertical: 14,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
  },
  mapText: {
    marginLeft: 5,
    fontSize: 15,
    fontWeight: "600",
  },

  rowBetween: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 4,
  },
  viewAll: {
    color: "#0E8E63",
    fontWeight: "600",
    fontSize: 14,
  },

  binCardActive: {
    borderWidth: 1,
    borderRadius: 16,
    padding: 12,
    marginTop: 8,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  binCard: {
    borderRadius: 16,
    padding: 12,
    marginTop: 10,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  binLeft: {
    flexDirection: "row",
    alignItems: "center",
    flex: 1,
    marginRight: 10,
  },
  binCircleRed: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },
  binCircleGreen: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },
  binCircleTextRed: { fontWeight: "800" },
  binCircleTextGreen: { fontWeight: "800" },
  binContent: { flex: 1 },
  binTitle: {
    fontSize: 15,
    fontWeight: "700",
    marginBottom: 4,
  },
  binAddress: { fontSize: 13, marginBottom: 6 },
  binMetaRow: { flexDirection: "row" },
  binMeta: { fontSize: 12, marginRight: 12 },
  binMetaGreen: {
    fontSize: 12,
    fontWeight: "700",
    marginRight: 12,
  },

  statsRow: {
    flexDirection: "row",
    marginHorizontal: 16,
    marginBottom: 20,
  },
  statCard: {
    flex: 1,
    borderRadius: 20,
    padding: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.05,
    shadowRadius: 12,
    elevation: 4,
  },
  secondStatCard: { marginLeft: 12 },
  statTitle: { fontSize: 13, marginBottom: 8 },
  statValue: {
    fontSize: 24,
    fontWeight: "800",
    color: "#0E8E63",
    marginBottom: 6,
  },
  statSub: { fontSize: 12 },

});
