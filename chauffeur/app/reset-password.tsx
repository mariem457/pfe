import React, { useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Keyboard,
KeyboardAvoidingView,
Platform,
ScrollView,
TouchableWithoutFeedback,
  SafeAreaView,
  StatusBar,
  StyleSheet,
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

export default function ResetPasswordScreen() {
  const params = useLocalSearchParams<{ email?: string; code?: string }>();

  const [email] = useState(params.email || "");
  const [code] = useState(params.code || "");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);

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

  async function handleResetPassword() {
    if (!email.trim() || !code.trim()) {
      Alert.alert("Erreur", "Session invalide. Veuillez recommencer.");
      router.replace("/forgot-password");
      return;
    }

    if (!newPassword.trim() || !confirmPassword.trim()) {
      Alert.alert("Erreur", "Veuillez remplir tous les champs.");
      return;
    }

    if (newPassword !== confirmPassword) {
      Alert.alert("Erreur", "Les mots de passe ne correspondent pas.");
      return;
    }

    try {
      setLoading(true);

      const response = await fetch(`${BASE_URL}/api/auth/reset-password-by-code`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          identifier: email.trim(),
          code: code.trim(),
          newPassword: newPassword.trim(),
        }),
      });

      const raw = await response.text();
      let data: any = null;

      try {
        data = raw ? JSON.parse(raw) : null;
      } catch {}

      if (!response.ok) {
        Alert.alert(
          "Erreur",
          data?.message || "Impossible de réinitialiser le mot de passe."
        );
        return;
      }

      Alert.alert(
        "Succès",
        data?.message || "Mot de passe réinitialisé avec succès.",
        [{ text: "OK", onPress: () => router.replace("/login") }]
      );
    } catch {
      Alert.alert("Erreur", "Une erreur est survenue.");
    } finally {
      setLoading(false);
    }
  }

  return (
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
          <Text style={[styles.backText, { color: colors.backText }]}>Back</Text>
        </TouchableOpacity>

        <View style={styles.centerContent}>
          <LinearGradient
            colors={["#14935F", "#1DBE7A"] as const}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.logoBox}
          >
            <Ionicons name="key-outline" size={34} color="#FFFFFF" />
          </LinearGradient>

          <View style={[styles.card, { backgroundColor: colors.card }]}>
            <Text style={[styles.title, { color: colors.title }]}>
              Reset Password
            </Text>

            <Text style={[styles.subtitle, { color: colors.subtitle }]}>
              Choose a new password
            </Text>

            <View style={styles.formGroup}>
              <Text style={[styles.label, { color: colors.label }]}>
                New Password
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
                  name="lock-closed-outline"
                  size={18}
                  color={colors.icon}
                  style={styles.inputIcon}
                />
                <TextInput
                  value={newPassword}
                  onChangeText={setNewPassword}
                  placeholder="Enter new password"
                  placeholderTextColor={colors.placeholder}
                  secureTextEntry={!showPassword}
                  style={[styles.input, { color: colors.inputText }]}
                />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
                  <Ionicons
                    name={showPassword ? "eye-outline" : "eye-off-outline"}
                    size={18}
                    color={colors.icon}
                  />
                </TouchableOpacity>
              </View>
            </View>

            <View style={styles.formGroup}>
              <Text style={[styles.label, { color: colors.label }]}>
                Confirm Password
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
                  name="lock-closed-outline"
                  size={18}
                  color={colors.icon}
                  style={styles.inputIcon}
                />
                <TextInput
                  value={confirmPassword}
                  onChangeText={setConfirmPassword}
                  placeholder="Confirm new password"
                  placeholderTextColor={colors.placeholder}
                  secureTextEntry={!showConfirmPassword}
                  style={[styles.input, { color: colors.inputText }]}
                />
                <TouchableOpacity
                  onPress={() => setShowConfirmPassword(!showConfirmPassword)}
                >
                  <Ionicons
                    name={showConfirmPassword ? "eye-outline" : "eye-off-outline"}
                    size={18}
                    color={colors.icon}
                  />
                </TouchableOpacity>
              </View>
            </View>

            <TouchableOpacity
              activeOpacity={0.9}
              style={styles.buttonShadow}
              onPress={handleResetPassword}
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
                  <Text style={styles.mainButtonText}>Reset Password</Text>
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