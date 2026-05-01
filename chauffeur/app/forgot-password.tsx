import React, { useState } from "react";
import {
  ActivityIndicator,
  Alert,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Keyboard,
KeyboardAvoidingView,
Platform,
ScrollView,
TouchableWithoutFeedback,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useColorScheme,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import { router, useLocalSearchParams } from "expo-router";
import { BASE_URL } from "../lib/api";

export default function ForgotPasswordScreen() {
  const params = useLocalSearchParams<{ email?: string }>();
  const [email, setEmail] = useState(params.email || "");
  const [loading, setLoading] = useState(false);

  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";

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
        placeholder: "#6B7280",
        icon: "#94A3B8",
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
        placeholder: "#9CA3AF",
        icon: "#94A3B8",
      };

  async function handleSendCode() {
    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      Alert.alert("Erreur", "Veuillez saisir votre email.");
      return;
    }

    try {
      setLoading(true);

      const response = await fetch(`${BASE_URL}/api/auth/forgot-password`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: trimmedEmail,
        }),
      });

      const raw = await response.text();

      let data: any = null;
      try {
        data = raw ? JSON.parse(raw) : null;
      } catch {}

      if (!response.ok) {
        Alert.alert("Erreur", data?.message || "Impossible d’envoyer le code.");
        return;
      }

      // ✅ هنا التعديل المهم
      Alert.alert(
        "Succès",
        data?.message || "Code envoyé avec succès.",
        [
          {
            text: "OK",
            onPress: () =>
              router.push({
                pathname: "/verify-code", // 🔥 بدل reset-password
                params: { email: trimmedEmail },
              }),
          },
        ]
      );
    } catch (error) {
      Alert.alert("Erreur", "Une erreur est survenue.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <LinearGradient
      colors={colors.gradient}
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
            colors={["#14935F", "#1DBE7A"]}
            style={styles.logoBox}
          >
            <Ionicons name="mail-open-outline" size={34} color="#FFFFFF" />
          </LinearGradient>

          <View style={[styles.card, { backgroundColor: colors.card }]}>
            <Text style={[styles.title, { color: colors.title }]}>
              Forgot Password
            </Text>
            <Text style={[styles.subtitle, { color: colors.subtitle }]}>
              Enter your email to receive a reset code
            </Text>

            <View style={styles.formGroup}>
              <Text style={[styles.label, { color: colors.label }]}>
                Email
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
                  name="mail-outline"
                  size={18}
                  color={colors.icon}
                  style={styles.inputIcon}
                />

                <TextInput
                  value={email}
                  onChangeText={setEmail}
                  placeholder="Enter your email"
                  placeholderTextColor={colors.placeholder}
                  style={[styles.input, { color: colors.inputText }]}
                  autoCapitalize="none"
                  keyboardType="email-address"
                />
              </View>
            </View>

            <TouchableOpacity
              style={styles.buttonShadow}
              onPress={handleSendCode}
              disabled={loading}
            >
              <LinearGradient
                colors={["#12905C", "#1CC37B"]}
                style={styles.mainButton}
              >
                {loading ? (
                  <ActivityIndicator color="#FFFFFF" />
                ) : (
                  <Text style={styles.mainButtonText}>Send Code</Text>
                )}
              </LinearGradient>
            </TouchableOpacity>
          </View>
        </View>
      </SafeAreaView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  safeArea: { flex: 1, paddingHorizontal: 22, paddingTop: 10 },

  backRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginTop: 8,
  },

  backText: {
    fontSize: 15,
    fontWeight: "500",
  },

  centerContent: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },

  logoBox: {
    width: 62,
    height: 62,
    borderRadius: 16,
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 24,
  },

  card: {
    width: "100%",
    maxWidth: 340,
    borderRadius: 22,
    paddingHorizontal: 24,
    paddingVertical: 28,
  },

  title: {
    fontSize: 30,
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
    marginBottom: 8,
    fontWeight: "500",
  },

  inputContainer: {
    height: 48,
    borderWidth: 1,
    borderRadius: 14,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
  },

  inputIcon: {
    marginRight: 8,
  },

  input: {
    flex: 1,
    fontSize: 14,
  },

  buttonShadow: {
    borderRadius: 14,
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