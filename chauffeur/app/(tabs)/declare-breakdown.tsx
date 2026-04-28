import { Ionicons } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import * as Location from "expo-location";
import { router } from "expo-router";
import React, { useEffect, useState } from "react";
import {
  Alert,
  Keyboard,
  KeyboardAvoidingView,
  Modal,
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
import { declareTruckIncident, getCurrentMissionId } from "../../lib/truckApi";

const incidentTypes = [
  { label: "Panne camion", value: "BREAKDOWN" },
  { label: "Trafic", value: "TRAFFIC_BLOCK" },
  { label: "Carburant faible", value: "FUEL_LOW" },
  { label: "Retard", value: "DELAY" },
  { label: "Manuel", value: "OTHER" },
];

const PRIMARY_GREEN = "#059669";
const SECONDARY_GREEN = "#10B981";

export default function DeclareBreakdownScreen() {
  const isDark = useColorScheme() === "dark";

  const [selectedType, setSelectedType] = useState("");
  const [dropdownVisible, setDropdownVisible] = useState(false);

  const [locationMode, setLocationMode] = useState<"AUTO" | "MANUAL">("AUTO");
  const [location, setLocation] = useState("");
  const [locationLoading, setLocationLoading] = useState(false);

  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);

  const selectedTypeLabel =
    incidentTypes.find((item) => item.value === selectedType)?.label || "";

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
        modalBg: "#1F2937",
      }
    : {
        gradient: ["#EEF3F2", "#F7F8FA"] as const,
        card: "#F8F8F9",
        text: "#1F2937",
        subtext: "#64748B",
        inputBg: "#FFFFFF",
        inputBorder: "#E5E7EB",
        placeholder: "#9CA3AF",
        backText: "#64748B",
        modalBg: "#FFFFFF",
      };

  useEffect(() => {
    if (locationMode === "AUTO") {
      fillCurrentLocation();
    }
  }, [locationMode]);

  async function fillCurrentLocation() {
    try {
      setLocationLoading(true);

      const { status } = await Location.requestForegroundPermissionsAsync();

      if (status !== "granted") {
        Alert.alert("Erreur", "Permission de localisation refusée.");
        return;
      }

      const currentLocation = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.High,
      });

      const latitude = currentLocation.coords.latitude;
      const longitude = currentLocation.coords.longitude;

      setLocation(`${latitude}, ${longitude}`);
    } catch (error) {
      console.log("Erreur localisation:", error);
      Alert.alert("Erreur", "Impossible de récupérer la localisation.");
    } finally {
      setLocationLoading(false);
    }
  }

  function parseLocation(value: string): { lat: number | null; lng: number | null } {
    if (!value.includes(",")) {
      return { lat: null, lng: null };
    }

    const parts = value.split(",");
    const lat = parseFloat(parts[0].trim());
    const lng = parseFloat(parts[1].trim());

    if (Number.isNaN(lat) || Number.isNaN(lng)) {
      return { lat: null, lng: null };
    }

    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      return { lat: null, lng: null };
    }

    return { lat, lng };
  }

  async function handleSubmit() {
    Keyboard.dismiss();

    if (!selectedType) {
      Alert.alert("Erreur", "Veuillez sélectionner le type d'incident.");
      return;
    }

    if (!location.trim()) {
      Alert.alert("Erreur", "Veuillez renseigner la localisation.");
      return;
    }

    if (!description.trim()) {
      Alert.alert("Erreur", "Veuillez écrire une description.");
      return;
    }

    try {
      setLoading(true);

      const missionId = await getCurrentMissionId();
      console.log("CURRENT MISSION ID:", missionId);

      const { lat, lng } = parseLocation(location);

      await declareTruckIncident({
        missionId,
        incidentType: selectedType,
        description: description.trim(),
        lat,
        lng,
      });

      Alert.alert("Succès", "Incident déclaré avec succès.", [
        {
          text: "OK",
          onPress: () => router.back(),
        },
      ]);
    } catch (error) {
      console.log("Erreur déclaration incident:", error);
      Alert.alert(
        "Erreur",
        error instanceof Error
          ? error.message
          : "Impossible de déclarer l'incident."
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
                <LinearGradient
                  colors={[PRIMARY_GREEN, SECONDARY_GREEN] as const}
                  style={styles.logoBox}
                >
                  <Ionicons name="warning-outline" size={34} color="#FFFFFF" />
                </LinearGradient>

                <View style={[styles.card, { backgroundColor: colors.card }]}>
                  <Text style={[styles.title, { color: colors.text }]}>
                    Déclarer un incident
                  </Text>

                  <Text style={[styles.subtitle, { color: colors.subtext }]}>
                    Remplissez les informations de l'incident
                  </Text>

                  <Text style={[styles.label, { color: colors.subtext }]}>
                    Type d'incident
                  </Text>

                  <TouchableOpacity
                    activeOpacity={0.85}
                    style={[
                      styles.dropdownButton,
                      {
                        backgroundColor: colors.inputBg,
                        borderColor: colors.inputBorder,
                      },
                    ]}
                    onPress={() => setDropdownVisible(true)}
                  >
                    <Text
                      style={[
                        styles.dropdownText,
                        {
                          color: selectedType ? colors.text : colors.placeholder,
                        },
                      ]}
                    >
                      {selectedTypeLabel || "Sélectionner un type d'incident"}
                    </Text>

                    <Ionicons
                      name="chevron-down"
                      size={20}
                      color={colors.subtext}
                    />
                  </TouchableOpacity>

                  <Text style={[styles.label, { color: colors.subtext }]}>
                    Mode de localisation
                  </Text>

                  <View style={styles.locationModeRow}>
                    <TouchableOpacity
                      activeOpacity={0.85}
                      style={[
                        styles.modeButton,
                        {
                          backgroundColor:
                            locationMode === "AUTO"
                              ? PRIMARY_GREEN
                              : colors.inputBg,
                          borderColor:
                            locationMode === "AUTO"
                              ? PRIMARY_GREEN
                              : colors.inputBorder,
                        },
                      ]}
                      onPress={() => setLocationMode("AUTO")}
                    >
                      <Text
                        style={[
                          styles.modeButtonText,
                          {
                            color:
                              locationMode === "AUTO" ? "#FFFFFF" : colors.text,
                          },
                        ]}
                      >
                        Automatique
                      </Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                      activeOpacity={0.85}
                      style={[
                        styles.modeButton,
                        {
                          backgroundColor:
                            locationMode === "MANUAL"
                              ? PRIMARY_GREEN
                              : colors.inputBg,
                          borderColor:
                            locationMode === "MANUAL"
                              ? PRIMARY_GREEN
                              : colors.inputBorder,
                        },
                      ]}
                      onPress={() => {
                        setLocationMode("MANUAL");
                        setLocation("");
                      }}
                    >
                      <Text
                        style={[
                          styles.modeButtonText,
                          {
                            color:
                              locationMode === "MANUAL" ? "#FFFFFF" : colors.text,
                          },
                        ]}
                      >
                        Manuelle
                      </Text>
                    </TouchableOpacity>
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
                      value={
                        locationLoading && locationMode === "AUTO"
                          ? "Récupération de la localisation..."
                          : location
                      }
                      onChangeText={setLocation}
                      editable={locationMode === "MANUAL"}
                      placeholder={
                        locationMode === "AUTO"
                          ? "Localisation automatique..."
                          : "Entrez votre localisation..."
                      }
                      placeholderTextColor={colors.placeholder}
                      style={[styles.input, { color: colors.text }]}
                    />

                    {locationMode === "AUTO" && (
                      <TouchableOpacity
                        onPress={fillCurrentLocation}
                        disabled={locationLoading}
                      >
                        <Ionicons
                          name="refresh-outline"
                          size={20}
                          color={PRIMARY_GREEN}
                        />
                      </TouchableOpacity>
                    )}
                  </View>

                  <Text style={[styles.label, { color: colors.subtext }]}>
                    Description
                  </Text>

                  <TextInput
                    value={description}
                    onChangeText={setDescription}
                    placeholder="Décrivez l'incident..."
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
                    style={[
                      styles.buttonShadow,
                      loading && styles.disabledButton,
                    ]}
                    onPress={handleSubmit}
                    disabled={loading}
                  >
                    <LinearGradient
                      colors={[PRIMARY_GREEN, SECONDARY_GREEN] as const}
                      style={styles.submitButton}
                    >
                      <Text style={styles.submitText}>
                        {loading ? "Envoi..." : "Envoyer la déclaration"}
                      </Text>
                    </LinearGradient>
                  </TouchableOpacity>
                </View>
              </View>
            </ScrollView>
          </SafeAreaView>
        </KeyboardAvoidingView>

        <Modal
          transparent
          visible={dropdownVisible}
          animationType="fade"
          onRequestClose={() => setDropdownVisible(false)}
        >
          <TouchableWithoutFeedback onPress={() => setDropdownVisible(false)}>
            <View style={styles.modalOverlay}>
              <TouchableWithoutFeedback>
                <View style={[styles.modalCard, { backgroundColor: colors.modalBg }]}>
                  <Text style={[styles.modalTitle, { color: colors.text }]}>
                    Choisir le type d'incident
                  </Text>

                  {incidentTypes.map((item) => (
                    <TouchableOpacity
                      key={item.value}
                      activeOpacity={0.85}
                      style={[
                        styles.modalOption,
                        {
                          backgroundColor:
                            selectedType === item.value
                              ? PRIMARY_GREEN
                              : colors.inputBg,
                          borderColor:
                            selectedType === item.value
                              ? PRIMARY_GREEN
                              : colors.inputBorder,
                        },
                      ]}
                      onPress={() => {
                        setSelectedType(item.value);
                        setDropdownVisible(false);
                      }}
                    >
                      <Text
                        style={[
                          styles.modalOptionText,
                          {
                            color:
                              selectedType === item.value ? "#FFFFFF" : colors.text,
                          },
                        ]}
                      >
                        {item.label}
                      </Text>

                      {selectedType === item.value && (
                        <Ionicons name="checkmark" size={20} color="#FFFFFF" />
                      )}
                    </TouchableOpacity>
                  ))}
                </View>
              </TouchableWithoutFeedback>
            </View>
          </TouchableWithoutFeedback>
        </Modal>
      </LinearGradient>
    </TouchableWithoutFeedback>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  keyboardView: { flex: 1 },
  safeArea: { flex: 1, paddingHorizontal: 22, paddingTop: 10 },
  scrollContent: { flexGrow: 1, paddingBottom: 80 },
  backRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginTop: 8,
    marginLeft: 4,
  },
  backText: { fontSize: 15, fontWeight: "500" },
  centerContent: {
    flexGrow: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingVertical: 40,
  },
  logoBox: {
    width: 68,
    height: 68,
    borderRadius: 18,
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 24,
    shadowColor: PRIMARY_GREEN,
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.25,
    shadowRadius: 16,
    elevation: 8,
  },
  card: {
    width: "100%",
    maxWidth: 360,
    borderRadius: 22,
    paddingHorizontal: 22,
    paddingVertical: 28,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 18 },
    shadowOpacity: 0.12,
    shadowRadius: 30,
    elevation: 10,
  },
  title: {
    fontSize: 28,
    fontWeight: "800",
    textAlign: "center",
    marginBottom: 8,
  },
  subtitle: { fontSize: 14, textAlign: "center", marginBottom: 26 },
  label: { fontSize: 13, fontWeight: "700", marginBottom: 10, marginTop: 4 },
  dropdownButton: {
    height: 50,
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 14,
    marginBottom: 18,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  dropdownText: { fontSize: 14, fontWeight: "700" },
  locationModeRow: { flexDirection: "row", gap: 10, marginBottom: 18 },
  modeButton: {
    flex: 1,
    height: 42,
    borderRadius: 12,
    borderWidth: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  modeButtonText: { fontSize: 13, fontWeight: "800" },
  inputContainer: {
    height: 48,
    borderWidth: 1,
    borderRadius: 14,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    marginBottom: 18,
  },
  inputIcon: { marginRight: 8 },
  input: { flex: 1, fontSize: 14 },
  textArea: {
    minHeight: 110,
    borderWidth: 1,
    borderRadius: 14,
    padding: 12,
    textAlignVertical: "top",
    fontSize: 14,
    marginBottom: 22,
  },
  buttonShadow: {
    borderRadius: 14,
    shadowColor: PRIMARY_GREEN,
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.22,
    shadowRadius: 12,
    elevation: 6,
  },
  disabledButton: { opacity: 0.7 },
  submitButton: {
    height: 52,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
  },
  submitText: { color: "#FFFFFF", fontSize: 16, fontWeight: "800" },
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.45)",
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: 24,
  },
  modalCard: {
    width: "100%",
    maxWidth: 340,
    borderRadius: 20,
    padding: 18,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: "800",
    marginBottom: 14,
    textAlign: "center",
  },
  modalOption: {
    height: 48,
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 14,
    marginBottom: 10,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  modalOptionText: { fontSize: 14, fontWeight: "800" },
});