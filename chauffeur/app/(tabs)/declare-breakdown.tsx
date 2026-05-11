import { Ionicons } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import * as Location from "expo-location";
import { router, useLocalSearchParams } from "expo-router";
import React, { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  TouchableWithoutFeedback,
  View,
  useColorScheme,
} from "react-native";
import { BASE_URL } from "../../lib/api";
import { getToken, getUserId } from "../../lib/storage";
import { declareTruckIncident, getCurrentMissionId } from "../../lib/truckApi";

const truckProblemTypes = [
  "Panne camion",
  "Panne moteur",
  "Problème carburant",
  "Pneu crevé",
  "Problème GPS",
  "QR code invalide alors qu'il est valide",
  "Accident",
  "Autre",
];

const binProblemTypes = [
  "QR code pas clair",
  "Poubelle en panne",
  "Poubelle bloquée",
  "Poubelle endommagée",
  "Accès impossible",
  "Capteur défectueux",
  "Autre",
];

export default function DeclareBreakdownScreen() {
  const isDark = useColorScheme() === "dark";

  const { missionBinId, binCode, resumeIndex, context } = useLocalSearchParams<{
    missionBinId?: string;
    binCode?: string;
    resumeIndex?: string;
    context?: "truck" | "bin";
  }>();

  const isBinProblem = context === "bin" || !!missionBinId;
  const problemTypes = isBinProblem ? binProblemTypes : truckProblemTypes;
  const accentColor = isBinProblem ? "#F97316" : "#EF4444";
  const accentDark = isBinProblem ? "#EA580C" : "#DC2626";
  const accentSoft = isBinProblem ? "#FFF7ED" : "#FFF1F2";
  const accentBorder = isBinProblem ? "#FED7AA" : "#FECACA";
  const accentText = isBinProblem ? "#C2410C" : "#B91C1C";

  const [selectedType, setSelectedType] = useState("");
  const [location, setLocation] = useState("");
  const [coords, setCoords] = useState<{ lat: number | null; lng: number | null }>({
    lat: null,
    lng: null,
  });
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingLocation, setLoadingLocation] = useState(false);

  const colors = isDark
    ? {
        gradient: ["#0F1720", "#111827"] as const,
        card: "#1A232D",
        text: "#F3F4F6",
        subtext: "#94A3B8",
        inputBg: "#111827",
        inputBorder: "#2A3642",
        placeholder: "#6B7280",
        backText: "#CBD5E1",
        softDanger: isBinProblem ? "#2D2114" : "#2B1620",
      }
    : {
        gradient: ["#EEF3F2", "#F7F8FA"] as const,
        card: "#FFFFFF",
        text: "#102A43",
        subtext: "#64748B",
        inputBg: "#FFFFFF",
        inputBorder: "#E5E7EB",
        placeholder: "#9CA3AF",
        backText: "#64748B",
        softDanger: accentSoft,
      };

  const screenCopy = useMemo(
    () =>
      isBinProblem
        ? {
            title: "Signaler une poubelle",
            subtitle: binCode
              ? `Poubelle concernée: ${binCode}`
              : "Signalez le problème détecté sur cette poubelle.",
            icon: "trash-outline" as const,
            typeLabel: "Type de problème poubelle",
            placeholder: "Décrivez le problème de la poubelle...",
            success: "Problème poubelle signalé avec succès.",
          }
        : {
            title: "Signaler un problème camion",
            subtitle: "Panne, GPS, QR valide refusé ou autre anomalie camion.",
            icon: "construct-outline" as const,
            typeLabel: "Type de problème camion",
            placeholder: "Décrivez le problème du camion...",
            success: "Problème camion signalé avec succès.",
          },
    [binCode, isBinProblem]
  );

  function formatAddress(address: Location.LocationGeocodedAddress): string {
    const parts = [
      address.name,
      address.street,
      address.district,
      address.city,
      address.region,
      address.country,
    ].filter(Boolean);

    return parts.join(", ");
  }

  async function loadCurrentLocation() {
    try {
      setLoadingLocation(true);

      const { status } = await Location.requestForegroundPermissionsAsync();

      if (status !== "granted") {
        setLocation("");
        Alert.alert(
          "Localisation refusée",
          "Activez la localisation pour remplir l'adresse automatiquement."
        );
        return;
      }

      const position = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.High,
      });

      setCoords({
        lat: position.coords.latitude,
        lng: position.coords.longitude,
      });

      const reverse = await Location.reverseGeocodeAsync({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      });

      const addressText =
        reverse.length > 0
          ? formatAddress(reverse[0])
          : `Lat: ${position.coords.latitude.toFixed(6)}, Lng: ${position.coords.longitude.toFixed(6)}`;

      setLocation(addressText);
    } catch (error) {
      console.log("Erreur localisation:", error);
      setLocation("");
      Alert.alert("Erreur", "Impossible de récupérer la localisation.");
    } finally {
      setLoadingLocation(false);
    }
  }

  useEffect(() => {
    loadCurrentLocation();
  }, []);

  function mapTruckIncidentType(type: string) {
    if (
      type === "Panne camion" ||
      type === "Panne moteur" ||
      type === "Pneu crevé"
    ) {
      return "BREAKDOWN";
    }

    if (type === "Problème carburant") return "FUEL_LOW";
    if (type === "Problème GPS") return "GPS_LOST";
    return "OTHER";
  }

  async function submitBinProblem(token: string) {
    const response = await fetch(`${BASE_URL}/api/drivers/bin-scan`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        missionBinId: missionBinId ? Number(missionBinId) : null,
        binCode: binCode ?? null,
        driverNote: `${selectedType} - ${description.trim()} - ${
          location.trim() || "Localisation non détectée"
        }`,
        issueType: selectedType,
      }),
    });

    const text = await response.text();
    if (!response.ok) {
      throw new Error(text || "Impossible de signaler cette poubelle.");
    }
  }

  async function submitTruckProblem() {
    const missionId = await getCurrentMissionId();

    await declareTruckIncident({
      missionId,
      incidentType: mapTruckIncidentType(selectedType),
      description: `${selectedType} - ${description.trim()} - ${
        location.trim() || "Localisation non détectée"
      }`,
      lat: coords.lat,
      lng: coords.lng,
    });
  }

  async function handleSubmit() {
    Keyboard.dismiss();

    if (!selectedType) {
      Alert.alert("Erreur", "Veuillez sélectionner le type de problème.");
      return;
    }

    if (!description.trim()) {
      Alert.alert("Erreur", "Veuillez écrire une description.");
      return;
    }

    try {
      setLoading(true);

      const token = await getToken();
      const userId = await getUserId();

      if (!token || !userId) {
        Alert.alert("Erreur", "Session expirée. Veuillez vous reconnecter.");
        router.replace("/login");
        return;
      }

      if (isBinProblem) {
        await submitBinProblem(token);
      } else {
        await submitTruckProblem();
      }

      Alert.alert("Succès", screenCopy.success, [
        {
          text: isBinProblem ? "Continuer route" : "Quitter",
          onPress: () => {
            if (isBinProblem) {
              router.replace({
                pathname: "/route-map",
                params: {
                  actionDone: "report",
                  reportedBinId: missionBinId ?? "",
                  resumeIndex: resumeIndex ?? "0",
                },
              });
              return;
            }

            router.replace("/(tabs)/dashboard");
          },
        },
      ]);
    } catch (error: any) {
      console.log("Erreur déclaration problème:", error);
      Alert.alert(
        "Erreur",
        error?.message || "Impossible d'envoyer le signalement."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <TouchableWithoutFeedback onPress={Keyboard.dismiss}>
      <LinearGradient colors={colors.gradient} style={styles.container}>
        <StatusBar barStyle={isDark ? "light-content" : "dark-content"} />

        <KeyboardAvoidingView
          style={styles.keyboardView}
          behavior={Platform.OS === "ios" ? "padding" : "height"}
          keyboardVerticalOffset={80}
        >
          <SafeAreaView style={styles.safeArea}>
            <ScrollView
              showsVerticalScrollIndicator={false}
              keyboardShouldPersistTaps="handled"
              keyboardDismissMode="on-drag"
              contentContainerStyle={styles.scrollContent}
            >
              <TouchableOpacity style={styles.backRow} onPress={() => router.back()}>
                <Ionicons name="arrow-back" size={18} color={colors.backText} />
                <Text style={[styles.backText, { color: colors.backText }]}>
                  Retour
                </Text>
              </TouchableOpacity>

              <View style={styles.centerContent}>
                <View style={styles.heroRow}>
                  <LinearGradient
                    colors={[accentDark, accentColor] as const}
                    style={styles.logoBox}
                  >
                    <Ionicons name={screenCopy.icon} size={32} color="#FFFFFF" />
                  </LinearGradient>
                  <View style={styles.heroText}>
                    <Text style={[styles.title, { color: colors.text }]}>
                      {screenCopy.title}
                    </Text>
                    <Text style={[styles.subtitle, { color: colors.subtext }]}>
                      {screenCopy.subtitle}
                    </Text>
                  </View>
                </View>

                <View style={[styles.card, { backgroundColor: colors.card }]}>
                  <Text style={[styles.label, { color: colors.subtext }]}>
                    {screenCopy.typeLabel}
                  </Text>

                  <View style={styles.typesGrid}>
                    {problemTypes.map((type) => (
                      <TouchableOpacity
                        key={type}
                        activeOpacity={0.85}
                        style={[
                          styles.typeButton,
                          {
                            backgroundColor:
                              selectedType === type ? accentColor : colors.softDanger,
                            borderColor:
                              selectedType === type ? accentColor : accentBorder,
                          },
                        ]}
                        onPress={() => setSelectedType(type)}
                      >
                        {selectedType === type && (
                          <Ionicons name="checkmark-circle" size={15} color="#FFFFFF" />
                        )}
                        <Text
                          style={[
                            styles.typeText,
                            {
                              color:
                                selectedType === type ? "#FFFFFF" : accentText,
                            },
                          ]}
                        >
                          {type}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>

                  <Text style={[styles.label, { color: colors.subtext }]}>
                    Localisation
                  </Text>

                  <View
                    style={[
                      styles.inputContainer,
                      {
                        backgroundColor: colors.inputBg,
                        borderColor: colors.inputBorder,
                      },
                    ]}
                  >
                    <Ionicons
                      name="location-outline"
                      size={18}
                      color={colors.subtext}
                      style={styles.inputIcon}
                    />

                    <TextInput
                      value={location}
                      onChangeText={setLocation}
                      placeholder={
                        loadingLocation
                          ? "Récupération de la localisation..."
                          : "Localisation automatique..."
                      }
                      placeholderTextColor={colors.placeholder}
                      style={[styles.input, { color: colors.text }]}
                    />

                    <TouchableOpacity
                      activeOpacity={0.8}
                      onPress={loadCurrentLocation}
                      disabled={loadingLocation}
                    >
                      <Ionicons
                        name={loadingLocation ? "sync-outline" : "locate-outline"}
                        size={20}
                        color={loadingLocation ? colors.placeholder : accentColor}
                      />
                    </TouchableOpacity>
                  </View>

                  <Text style={[styles.label, { color: colors.subtext }]}>
                    Description
                  </Text>

                  <TextInput
                    value={description}
                    onChangeText={setDescription}
                    placeholder={screenCopy.placeholder}
                    placeholderTextColor={colors.placeholder}
                    multiline
                    style={[
                      styles.textArea,
                      {
                        backgroundColor: colors.inputBg,
                        borderColor: colors.inputBorder,
                        color: colors.text,
                      },
                    ]}
                  />

                  <TouchableOpacity
                    activeOpacity={0.9}
                    style={styles.buttonShadow}
                    onPress={handleSubmit}
                    disabled={loading}
                  >
                    <LinearGradient
                      colors={[accentDark, accentColor] as const}
                      style={styles.submitButton}
                    >
                      <Ionicons name="send-outline" size={18} color="#FFFFFF" />
                      <Text style={styles.submitText}>
                        {loading ? "Envoi..." : "Envoyer le signalement"}
                      </Text>
                    </LinearGradient>
                  </TouchableOpacity>
                </View>
              </View>
            </ScrollView>
          </SafeAreaView>
        </KeyboardAvoidingView>
      </LinearGradient>
    </TouchableWithoutFeedback>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  keyboardView: { flex: 1 },
  safeArea: {
    flex: 1,
    paddingHorizontal: 0,
    paddingTop: 6,
  },
  scrollContent: {
    flexGrow: 1,
    paddingBottom: 118,
  },
  backRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginTop: 8,
    marginHorizontal: 24,
    alignSelf: "flex-start",
    paddingVertical: 8,
  },
  backText: {
    fontSize: 15,
    fontWeight: "700",
  },
  centerContent: {
    flexGrow: 1,
    paddingTop: 28,
  },
  heroRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 16,
    marginHorizontal: 24,
    marginBottom: 26,
  },
  heroText: {
    flex: 1,
  },
  logoBox: {
    width: 72,
    height: 72,
    borderRadius: 22,
    justifyContent: "center",
    alignItems: "center",
    shadowColor: "#EF4444",
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.25,
    shadowRadius: 16,
    elevation: 8,
  },
  card: {
    width: "92%",
    alignSelf: "center",
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    borderBottomLeftRadius: 30,
    borderBottomRightRadius: 30,
    paddingHorizontal: 24,
    paddingTop: 30,
    paddingBottom: 34,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 18 },
    shadowOpacity: 0.12,
    shadowRadius: 30,
    elevation: 10,
  },
  title: {
    fontSize: 28,
    fontWeight: "900",
    marginBottom: 6,
    lineHeight: 32,
  },
  subtitle: {
    fontSize: 15,
    lineHeight: 22,
    fontWeight: "700",
  },
  label: {
    fontSize: 14,
    fontWeight: "800",
    marginBottom: 12,
    marginTop: 6,
  },
  typesGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10,
    marginBottom: 24,
  },
  typeButton: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 16,
    paddingVertical: 11,
    minHeight: 44,
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  typeText: {
    fontSize: 13,
    fontWeight: "800",
  },
  inputContainer: {
    minHeight: 56,
    borderWidth: 1,
    borderRadius: 18,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    marginBottom: 24,
  },
  inputIcon: {
    marginRight: 8,
  },
  input: {
    flex: 1,
    fontSize: 15,
    fontWeight: "700",
    minHeight: 52,
  },
  textArea: {
    minHeight: 132,
    borderWidth: 1,
    borderRadius: 18,
    padding: 16,
    textAlignVertical: "top",
    fontSize: 15,
    marginBottom: 26,
    fontWeight: "700",
  },
  buttonShadow: {
    borderRadius: 16,
    shadowColor: "#EF4444",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.22,
    shadowRadius: 12,
    elevation: 6,
  },
  submitButton: {
    height: 58,
    borderRadius: 18,
    justifyContent: "center",
    alignItems: "center",
    flexDirection: "row",
    gap: 8,
  },
  submitText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "900",
  },
});
