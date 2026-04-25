import { Ionicons } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import * as Location from "expo-location";
import { router } from "expo-router";
import { useFocusEffect } from "@react-navigation/native";
import React, { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Modal,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useColorScheme,
} from "react-native";
import MapView, { Geojson, Marker } from "react-native-maps";
import { getToken, getUserId } from "../../lib/storage";

type DriverBin = {
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

const paris15: any = {
  type: "FeatureCollection",
  features: [],
};

const BASE_URL = "http://192.168.0.21:8081";

export default function Dashboard() {
  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";

  const [driverName, setDriverName] = useState("Driver");
  const [truckId, setTruckId] = useState("Not assigned");
  const [bins, setBins] = useState<DriverBin[]>([]);
  const [loadingBins, setLoadingBins] = useState(true);

  const [incidentModalVisible, setIncidentModalVisible] = useState(false);
  const [selectedBin, setSelectedBin] = useState<DriverBin | null>(null);
  const [selectedIncidentType, setSelectedIncidentType] = useState("");
  const [incidentComment, setIncidentComment] = useState("");

  const incidentTypes = [
    "Panne capteur",
    "Poubelle bloquée",
    "Poubelle endommagée",
    "Accès impossible",
    "QR code illisible",
    "Autre",
  ];

  async function sendLocationToBackend() {
    try {
      const token = await getToken();
      if (!token) return;

      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== "granted") return;

      const location = await Location.getCurrentPositionAsync({});

      await fetch(`${BASE_URL}/api/driver-location`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          latitude: location.coords.latitude,
          longitude: location.coords.longitude,
          speed: location.coords.speed,
          heading: location.coords.heading,
          timestamp: new Date().toISOString(),
        }),
      });
    } catch (error) {
      console.log("Erreur location:", error);
    }
  }

  const loadDashboardHeader = useCallback(async () => {
    try {
      const token = await getToken();

      if (!token) {
        setDriverName("Driver");
        setTruckId("Not assigned");
        return;
      }

      const response = await fetch(`${BASE_URL}/api/settings/profile`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const text = await response.text();

      if (!response.ok) throw new Error(text);

      const data = text ? JSON.parse(text) : {};

      setDriverName(data.fullName || "Driver");
      setTruckId(data.assignedTruck || "Not assigned");
    } catch (error) {
      console.log("Erreur header:", error);
      setDriverName("Driver");
      setTruckId("Not assigned");
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
      setBins(Array.isArray(data) ? data : []);
    } catch (error) {
      console.log("Erreur bins:", error);
      setBins([]);
    } finally {
      setLoadingBins(false);
    }
  }, []);

  useEffect(() => {
    sendLocationToBackend();

    const interval = setInterval(() => {
      sendLocationToBackend();
    }, 10000);

    return () => clearInterval(interval);
  }, []);

  useFocusEffect(
    useCallback(() => {
      fetchMyBins();
      loadDashboardHeader();
    }, [fetchMyBins, loadDashboardHeader])
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

  function openIncidentModal(bin: DriverBin) {
    setSelectedBin(bin);
    setSelectedIncidentType("");
    setIncidentComment("");
    setIncidentModalVisible(true);
  }

  async function submitBinIncident() {
    if (!selectedBin) return;

    if (!selectedIncidentType) {
      Alert.alert("Erreur", "Veuillez sélectionner le type de panne.");
      return;
    }

    try {
      const token = await getToken();

      if (!token) {
        Alert.alert("Erreur", "Session expirée.");
        return;
      }

      const payload = {
        missionBinId: selectedBin.missionBinId,
        binId: selectedBin.binId,
        binCode: selectedBin.binCode,
        type: selectedIncidentType,
        description: incidentComment,
        latitude: selectedBin.lat,
        longitude: selectedBin.lng,
        createdAt: new Date().toISOString(),
      };

      console.log("BIN INCIDENT:", payload);

      /*
      await fetch(`${BASE_URL}/api/incidents/bin`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });
      */

      Alert.alert("Succès", "Panne déclarée avec succès.");
      setIncidentModalVisible(false);
    } catch (error) {
      console.log("Erreur déclaration panne bin:", error);
      Alert.alert("Erreur", "Impossible de déclarer la panne.");
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
              onPress={() => router.push("/notifications")}
            >
              <Ionicons name="notifications-outline" size={20} color="#FFFFFF" />
              <View style={styles.notificationDot} />
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
            <Text style={styles.truckLabel}>Truck ID</Text>
            <Text style={styles.truckId}>{truckId}</Text>
          </View>
        </View>
      </LinearGradient>

      <View style={[styles.alertCardRed, { backgroundColor: colors.card }]}>
        <View style={styles.alertLeft}>
          <View style={[styles.alertIconRed, { backgroundColor: colors.dangerSoft }]}>
            <Ionicons name="warning-outline" size={18} color={colors.redText} />
          </View>

          <View style={styles.alertTextWrap}>
            <Text style={[styles.alertTitle, { color: colors.text }]}>
              Driver dashboard connected
            </Text>
            <Text style={[styles.alertTime, { color: colors.subtext }]}>
              Mission bins loaded from database
            </Text>
          </View>
        </View>

        <Ionicons name="close" size={18} color={colors.subtext} />
      </View>

      <View style={[styles.alertCardOrange, { backgroundColor: colors.card }]}>
        <View style={styles.alertLeft}>
          <View style={[styles.alertIconOrange, { backgroundColor: colors.warningSoft }]}>
            <Ionicons name="paper-plane-outline" size={18} color={colors.orangeText} />
          </View>

          <View style={styles.alertTextWrap}>
            <Text style={[styles.alertTitle, { color: colors.text }]}>
              Route data synchronized
            </Text>
            <Text style={[styles.alertTime, { color: colors.subtext }]}>
              Real mission data
            </Text>
          </View>
        </View>

        <Ionicons name="close" size={18} color={colors.subtext} />
      </View>

      <View style={[styles.diagramCard, { backgroundColor: colors.card }]}>
        <View style={styles.diagramHeader}>
          <View style={styles.topCenterBadgeMini}>
            <View style={styles.badgeDot} />
            <Text style={styles.topCenterBadgeText}>Active Route</Text>
          </View>

          <TouchableOpacity style={styles.progressButtonMini}>
            <Text style={styles.progressButtonText}>Progress</Text>
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
              title={truckId}
              description="Truck position"
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
                  description={bin.collected ? "Collected" : "Pending"}
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
                Route Progress
              </Text>
              <Text style={[styles.progressValue, { color: colors.text }]}>
                {collectedBins} / {totalBins}
              </Text>
            </View>

            <View style={styles.progressRight}>
              <Text style={styles.progressPercent}>{progressPercent}%</Text>
              <Text style={[styles.progressSubText, { color: colors.subtext }]}>
                Completed
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
                Total Bins
              </Text>
              <Text style={[styles.infoNumber, { color: colors.text }]}>
                {totalBins}
              </Text>
            </View>
          </View>

          <View style={styles.infoRight}>
            <Text style={[styles.infoTitle, { color: colors.subtext }]}>
              Collected
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
                Estimated Time
              </Text>
              <Text style={[styles.infoNumber, { color: colors.text }]}>--</Text>
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
                Total Distance
              </Text>
              <Text style={[styles.infoNumber, { color: colors.text }]}>--</Text>
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
              Route Status
            </Text>
          </View>
          <Text style={styles.statusReady}>
            {loadingBins
              ? "Loading..."
              : totalBins === 0
              ? "No Bins Assigned"
              : collectedBins === totalBins
              ? "Completed"
              : "Ready To Start"}
          </Text>
        </View>

        <TouchableOpacity
          style={styles.startButton}
          onPress={() => router.push("/route-map")}
          activeOpacity={0.85}
        >
          <Ionicons name="play-outline" size={18} color="white" />
          <Text style={styles.startText}>Start Route</Text>
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
          onPress={() => router.push("/declare-breakdown")}
        >
          <Ionicons name="warning-outline" size={18} color="#EF4444" />
          <Text style={[styles.mapText, { color: colors.text }]}>
            Déclarer une panne
          </Text>
        </TouchableOpacity>
      </View>

      <View style={[styles.card, { backgroundColor: colors.card }]}>
        <View style={styles.rowBetween}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>
            Next Bins
          </Text>
          <TouchableOpacity activeOpacity={0.8}>
            <Text style={styles.viewAll}>View All</Text>
          </TouchableOpacity>
        </View>

        {loadingBins ? (
          <Text style={[styles.binAddress, { color: colors.subtext }]}>
            Chargement...
          </Text>
        ) : pendingBins.length === 0 ? (
          <Text style={[styles.binAddress, { color: colors.subtext }]}>
            Aucune bins assignée aujourd’hui
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
                      {bin.wasteType ? `Waste: ${bin.wasteType}` : "Bin assigné"}
                    </Text>

                    <View style={styles.binMetaRow}>
                      <Text style={[styles.binMetaGreen, { color: colors.orangeText }]}>
                        Planned
                      </Text>

                     
                    </View>
                  </View>
                </View>

                <View style={styles.binActions}>
                  

                  <TouchableOpacity
                    style={styles.collectButton}
                    activeOpacity={0.85}
                    onPress={() =>
                      router.push({
                        pathname: "/scan",
                        params: {
                          expectedBinCode: bin.binCode || "",
                          missionBinId: String(bin.missionBinId || ""),
                        },
                      })
                    }
                  >
                    <Text style={styles.collectButtonText}>Collecter</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={styles.reportBinButton}
                    activeOpacity={0.85}
                    onPress={() => openIncidentModal(bin)}
                  >
                    <Ionicons name="alert-circle-outline" size={19} color="#FFFFFF" />
                  </TouchableOpacity>
                </View>
              </View>
            );
          })
        )}
      </View>

      <View style={styles.statsRow}>
        <View style={[styles.statCard, { backgroundColor: colors.card }]}>
          <Text style={[styles.statTitle, { color: colors.subtext }]}>
            This Week
          </Text>
          <Text style={styles.statValue}>{collectedBins}</Text>
          <Text style={[styles.statSub, { color: colors.subtext }]}>
            Bins Collected
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
            Efficiency
          </Text>
          <Text style={styles.statValue}>{progressPercent}%</Text>
          <Text style={[styles.statSub, { color: colors.subtext }]}>
            Completion Rate
          </Text>
        </View>
      </View>

      <Modal
        visible={incidentModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setIncidentModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalCard, { backgroundColor: colors.card }]}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>
              Déclarer une panne
            </Text>

            <Text style={[styles.modalSubtitle, { color: colors.subtext }]}>
              Bin: #{selectedBin?.binCode || "BIN"}
            </Text>

            {incidentTypes.map((type) => (
              <TouchableOpacity
                key={type}
                style={[
                  styles.incidentOption,
                  {
                    backgroundColor:
                      selectedIncidentType === type ? "#EF4444" : colors.softCard,
                    borderColor:
                      selectedIncidentType === type
                        ? "#EF4444"
                        : colors.whiteBorder,
                  },
                ]}
                onPress={() => setSelectedIncidentType(type)}
              >
                <Text
                  style={[
                    styles.incidentOptionText,
                    {
                      color:
                        selectedIncidentType === type ? "#FFFFFF" : colors.text,
                    },
                  ]}
                >
                  {type}
                </Text>
              </TouchableOpacity>
            ))}

            <TextInput
              value={incidentComment}
              onChangeText={setIncidentComment}
              placeholder="Description optionnelle..."
              placeholderTextColor={colors.subtext}
              multiline
              style={[
                styles.commentInput,
                {
                  backgroundColor: colors.softCard,
                  borderColor: colors.whiteBorder,
                  color: colors.text,
                },
              ]}
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.cancelButton}
                onPress={() => setIncidentModalVisible(false)}
              >
                <Text style={styles.cancelButtonText}>Annuler</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.submitIncidentButton}
                onPress={submitBinIncident}
              >
                <Text style={styles.submitIncidentText}>Envoyer</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
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

  binActions: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  reportBinButton: {
    width: 35,
    height: 35,
    borderRadius: 20,
    backgroundColor: "#EF4444",
    alignItems: "center",
    justifyContent: "center",
  },
  collectButton: {
    backgroundColor: "#19C37D",
    paddingHorizontal: 14,
    paddingVertical: 9,
    borderRadius: 999,
  },
  collectButtonText: {
    color: "white",
    fontWeight: "700",
    fontSize: 13,
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

  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.55)",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  modalCard: {
    width: "100%",
    borderRadius: 22,
    padding: 20,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: "800",
    marginBottom: 6,
  },
  modalSubtitle: {
    fontSize: 13,
    marginBottom: 16,
  },
  incidentOption: {
    borderWidth: 1,
    borderRadius: 14,
    paddingVertical: 12,
    paddingHorizontal: 14,
    marginBottom: 10,
  },
  incidentOptionText: {
    fontSize: 14,
    fontWeight: "700",
  },
  commentInput: {
    minHeight: 86,
    borderWidth: 1,
    borderRadius: 14,
    padding: 12,
    textAlignVertical: "top",
    fontSize: 14,
    marginTop: 4,
  },
  modalButtons: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 16,
    gap: 10,
  },
  cancelButton: {
    flex: 1,
    height: 46,
    borderRadius: 14,
    backgroundColor: "#64748B",
    alignItems: "center",
    justifyContent: "center",
  },
  cancelButtonText: {
    color: "#FFFFFF",
    fontWeight: "700",
  },
  submitIncidentButton: {
    flex: 1,
    height: 46,
    borderRadius: 14,
    backgroundColor: "#EF4444",
    alignItems: "center",
    justifyContent: "center",
  },
  submitIncidentText: {
    color: "#FFFFFF",
    fontWeight: "800",
  },
});