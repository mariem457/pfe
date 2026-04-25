import { Ionicons } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import { router } from "expo-router";
import React, { useState } from "react";
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

const panneTypes = [
  "Panne camion",
  "Panne moteur",
  "Problème carburant",
  "Pneu crevé",
  "Problème GPS",
  "Problème frein",
  "Accident",
  "Autre",
];

export default function DeclareBreakdownScreen() {
  const isDark = useColorScheme() === "dark";

  const [selectedType, setSelectedType] = useState("");
  const [location, setLocation] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);

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
      };

  async function handleSubmit() {
    Keyboard.dismiss();

    if (!selectedType) {
      Alert.alert("Erreur", "Veuillez sélectionner le type de panne.");
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

      const payload = {
        driverUserId: userId,
        type: selectedType,
        location: location.trim(),
        description: description.trim(),
        createdAt: new Date().toISOString(),
      };

      console.log("TRUCK BREAKDOWN:", payload);

      /*
      await fetch(`${BASE_URL}/api/incidents/truck-breakdown`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });
      */

      Alert.alert("Succès", "Panne déclarée avec succès.", [
        {
          text: "OK",
          onPress: () => router.back(),
        },
      ]);
    } catch (error) {
      console.log("Erreur déclaration panne:", error);
      Alert.alert("Erreur", "Impossible de déclarer la panne.");
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
                  Back
                </Text>
              </TouchableOpacity>

              <View style={styles.centerContent}>
                <LinearGradient
                  colors={["#DC2626", "#EF4444"] as const}
                  style={styles.logoBox}
                >
                  <Ionicons name="warning-outline" size={34} color="#FFFFFF" />
                </LinearGradient>

                <View style={[styles.card, { backgroundColor: colors.card }]}>
                  <Text style={[styles.title, { color: colors.text }]}>
                    Déclarer une panne
                  </Text>

                  <Text style={[styles.subtitle, { color: colors.subtext }]}>
                    Remplissez les informations de la panne
                  </Text>

                  <Text style={[styles.label, { color: colors.subtext }]}>
                    Type de panne
                  </Text>

                  <View style={styles.typesGrid}>
                    {panneTypes.map((type) => (
                      <TouchableOpacity
                        key={type}
                        activeOpacity={0.85}
                        style={[
                          styles.typeButton,
                          {
                            backgroundColor:
                              selectedType === type ? "#EF4444" : colors.inputBg,
                            borderColor:
                              selectedType === type ? "#EF4444" : colors.inputBorder,
                          },
                        ]}
                        onPress={() => setSelectedType(type)}
                      >
                        <Text
                          style={[
                            styles.typeText,
                            {
                              color:
                                selectedType === type ? "#FFFFFF" : colors.text,
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
                      placeholder="Ex: Rue principale, zone 15..."
                      placeholderTextColor={colors.placeholder}
                      style={[styles.input, { color: colors.text }]}
                    />
                  </View>

                  <Text style={[styles.label, { color: colors.subtext }]}>
                    Description
                  </Text>

                  <TextInput
                    value={description}
                    onChangeText={setDescription}
                    placeholder="Décrivez la panne..."
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
                      colors={["#DC2626", "#EF4444"] as const}
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
      </LinearGradient>
    </TouchableWithoutFeedback>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },

  keyboardView: { flex: 1 },

  safeArea: {
    flex: 1,
    paddingHorizontal: 22,
    paddingTop: 10,
  },

  scrollContent: {
    flexGrow: 1,
    paddingBottom: 80,
  },

  backRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginTop: 8,
    marginLeft: 4,
  },

  backText: {
    fontSize: 15,
    fontWeight: "500",
  },

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
    shadowColor: "#EF4444",
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

  subtitle: {
    fontSize: 14,
    textAlign: "center",
    marginBottom: 26,
  },

  label: {
    fontSize: 13,
    fontWeight: "700",
    marginBottom: 10,
    marginTop: 4,
  },

  typesGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginBottom: 18,
  },

  typeButton: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 13,
    paddingVertical: 10,
  },

  typeText: {
    fontSize: 13,
    fontWeight: "700",
  },

  inputContainer: {
    height: 48,
    borderWidth: 1,
    borderRadius: 14,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    marginBottom: 18,
  },

  inputIcon: {
    marginRight: 8,
  },

  input: {
    flex: 1,
    fontSize: 14,
  },

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
    shadowColor: "#EF4444",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.22,
    shadowRadius: 12,
    elevation: 6,
  },

  submitButton: {
    height: 52,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
  },

  submitText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "800",
  },
});