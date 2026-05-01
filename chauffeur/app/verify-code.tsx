import React, { useRef, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Keyboard,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  
  Text,
  TextInput,
  TouchableOpacity,
  TouchableWithoutFeedback,
  View,
  useColorScheme,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import { router, useLocalSearchParams } from "expo-router";
import { BASE_URL } from "../lib/api";

export default function VerifyCodeScreen() {
  const { email } = useLocalSearchParams<{ email?: string }>();
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);

  const hiddenInputRef = useRef<TextInput>(null);
  const codeDigits = Array.from({ length: 6 }, (_, index) => code[index] || "");

  const isDark = useColorScheme() === "dark";

  const colors = isDark
    ? {
        gradient: ["#0F1720", "#111827"] as const,
        backText: "#CBD5E1",
        card: "#1A232D",
        title: "#F3F4F6",
        subtitle: "#94A3B8",
        label: "#CBD5E1",
        inputBg: "#111827",
        inputBorder: "#2A3642",
        inputText: "#F9FAFB",
      }
    : {
        gradient: ["#EEF3F2", "#F7F8FA"] as const,
        backText: "#64748B",
        card: "#F8F8F9",
        title: "#1F2937",
        subtitle: "#64748B",
        label: "#64748B",
        inputBg: "#FFFFFF",
        inputBorder: "#E5E7EB",
        inputText: "#111827",
      };

  function handleCodeChange(text: string) {
    const clean = text.replace(/\D/g, "").slice(0, 6);
    setCode(clean);

    if (clean.length === 6) {
      Keyboard.dismiss();
    }
  }

  async function handleVerify() {
    Keyboard.dismiss();

    if (!email || code.length !== 6) {
      Alert.alert("Erreur", "Veuillez saisir le code à 6 chiffres.");
      return;
    }

    try {
      setLoading(true);

      const res = await fetch(`${BASE_URL}/api/auth/verify-reset-code`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ identifier: email, code }),
      });

      const raw = await res.text();
      let data: any = null;

      try {
        data = raw ? JSON.parse(raw) : null;
      } catch {}

      if (!res.ok) {
        Alert.alert("Erreur", data?.message || "Code invalide ou expiré.");
        return;
      }

      router.push({
        pathname: "/reset-password",
        params: { email, code },
      });
    } catch {
      Alert.alert("Erreur", "Une erreur est survenue.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <TouchableWithoutFeedback onPress={Keyboard.dismiss}>
      <LinearGradient
        colors={colors.gradient}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={styles.container}
      >
        <StatusBar barStyle={isDark ? "light-content" : "dark-content"} />

        <SafeAreaView style={styles.safeArea}>
          <TouchableOpacity style={styles.backRow} onPress={() => router.back()}>
            <Ionicons name="arrow-back" size={18} color={colors.backText} />
            <Text style={[styles.backText, { color: colors.backText }]}>
              Back
            </Text>
          </TouchableOpacity>

          <View style={styles.centerContent}>
            <LinearGradient
              colors={["#14935F", "#1DBE7A"] as const}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.logoBox}
            >
              <Ionicons
                name="shield-checkmark-outline"
                size={34}
                color="#FFFFFF"
              />
            </LinearGradient>

            <View style={[styles.card, { backgroundColor: colors.card }]}>
              <Text style={[styles.title, { color: colors.title }]}>
                Vérification du code
              </Text>

              <Text style={[styles.subtitle, { color: colors.subtitle }]}>
                Entrez le code envoyé par e-mail
              </Text>

              <View style={styles.formGroup}>
                <Text style={[styles.label, { color: colors.label }]}>
                  Code
                </Text>

                <TouchableOpacity
                  activeOpacity={1}
                  style={styles.codeBoxesRow}
                  onPress={() => hiddenInputRef.current?.focus()}
                >
                  {codeDigits.map((digit, index) => (
                    <View
                      key={index}
                      style={[
                        styles.codeBox,
                        {
                          backgroundColor: colors.inputBg,
                          borderColor: digit ? "#169A66" : colors.inputBorder,
                        },
                      ]}
                    >
                      <Text
                        style={[
                          styles.codeBoxText,
                          { color: colors.inputText },
                        ]}
                      >
                        {digit}
                      </Text>
                    </View>
                  ))}
                </TouchableOpacity>

                <TextInput
                  ref={hiddenInputRef}
                  value={code}
                  onChangeText={handleCodeChange}
                  keyboardType="number-pad"
                  maxLength={6}
                  style={styles.hiddenInput}
                  blurOnSubmit={false}
                />
              </View>

              <TouchableOpacity
                activeOpacity={0.9}
                style={styles.buttonShadow}
                onPress={handleVerify}
                disabled={loading}
              >
                <LinearGradient
                  colors={["#12905C", "#1CC37B"] as const}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 0 }}
                  style={styles.mainButton}
                >
                  {loading ? (
                    <ActivityIndicator color="#FFFFFF" />
                  ) : (
                    <Text style={styles.mainButtonText}>Vérifier</Text>
                  )}
                </LinearGradient>
              </TouchableOpacity>
            </View>
          </View>
        </SafeAreaView>
      </LinearGradient>
    </TouchableWithoutFeedback>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },

  safeArea: {
    flex: 1,
    paddingHorizontal: 22,
    paddingTop: 10,
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
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingBottom: 40,
  },

  logoBox: {
    width: 62,
    height: 62,
    borderRadius: 16,
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 24,
    shadowColor: "#119660",
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.25,
    shadowRadius: 16,
    elevation: 8,
  },

  card: {
    width: "100%",
    maxWidth: 340,
    borderRadius: 22,
    paddingHorizontal: 24,
    paddingVertical: 28,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 18 },
    shadowOpacity: 0.12,
    shadowRadius: 30,
    elevation: 10,
  },

  title: {
    fontSize: 28,
    fontWeight: "700",
    textAlign: "center",
    marginBottom: 8,
  },

  subtitle: {
    fontSize: 14,
    textAlign: "center",
    marginBottom: 26,
  },

  formGroup: {
    marginBottom: 18,
  },

  label: {
    fontSize: 13,
    marginBottom: 10,
    fontWeight: "500",
  },

  codeBoxesRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 18,
  },

  codeBox: {
    width: 42,
    height: 52,
    borderWidth: 1,
    borderRadius: 12,
    justifyContent: "center",
    alignItems: "center",
  },

  codeBoxText: {
    fontSize: 22,
    fontWeight: "700",
  },

  hiddenInput: {
    position: "absolute",
    opacity: 0,
    width: 1,
    height: 1,
  },

  buttonShadow: {
    borderRadius: 14,
    shadowColor: "#169A66",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.22,
    shadowRadius: 12,
    elevation: 6,
  },

  mainButton: {
    height: 50,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
  },

  mainButtonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "700",
  },
});