import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Image,
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
import { Ionicons } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import { router } from "expo-router";
import { BASE_URL } from "../lib/api";
import { alertMessageFr } from "../lib/alertMessages";
import { registerDriverPushToken } from "../lib/phoneNotifications";
import {
  getRememberedAccounts,
  removeRememberedEmail,
  saveRememberedAccount,
  saveAuth,
  saveRememberedEmail,
  type RememberedAccount,
} from "../lib/storage";

type LoginResponse = {
  token: string;
  role: string;
  userId: number;
  username: string;
  email?: string;
  mustChangePassword?: boolean;
};

export default function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [rememberedAccounts, setRememberedAccounts] = useState<RememberedAccount[]>([]);
  const [showEmailSuggestions, setShowEmailSuggestions] = useState(false);

  const isDark = useColorScheme() === "dark";

  useEffect(() => {
    async function loadRememberedData() {
      const accounts = await getRememberedAccounts();

      setRememberedAccounts(accounts);
    }

    loadRememberedData();
  }, []);

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
        checkboxBorder: "#64748B",
        checkboxBg: "#111827",
        checkboxChecked: "#169A66",
        rememberText: "#CBD5E1",
        forgotText: "#22C983",
        signupText: "#CBD5E1",
        signupLink: "#22C983",
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
        checkboxBorder: "#9CA3AF",
        checkboxBg: "#FFFFFF",
        checkboxChecked: "#169A66",
        rememberText: "#64748B",
        forgotText: "#169A66",
        signupText: "#64748B",
        signupLink: "#169A66",
      };

  async function handleLogin() {
    Keyboard.dismiss();

    const trimmedEmail = email.trim();

    if (!trimmedEmail || !password.trim()) {
      Alert.alert("Erreur", "Veuillez saisir email et mot de passe.");
      return;
    }

    try {
      setLoading(true);

      const response = await fetch(`${BASE_URL}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: trimmedEmail,
          password,
          rememberMe: remember,
        }),
      });

      if (!response.ok) {
        let message = "Impossible de se connecter.";
        try {
          const errorText = await response.text();
          if (errorText?.trim()) message = errorText;
        } catch {}
        throw new Error(message);
      }

      const data: LoginResponse = await response.json();

      await saveAuth({
        token: data.token,
        userId: data.userId,
        role: data.role,
        username: data.username,
        email: data.email ?? trimmedEmail,
        mustChangePassword: data.mustChangePassword ?? false,
      });

      if (remember) {
        await saveRememberedEmail(trimmedEmail);
        await saveRememberedAccount(trimmedEmail, password);
      } else {
        await removeRememberedEmail();
      }

      if (data.mustChangePassword) {
        router.replace("/reset-password");
        return;
      }

      registerDriverPushToken().catch(console.log);
      router.replace("/(tabs)/dashboard");
    } catch (error: any) {
      Alert.alert(
        "Échec de connexion",
        alertMessageFr(error?.message, "Impossible de se connecter.")
      );
    } finally {
      setLoading(false);
    }
  }

  const emailSuggestions = rememberedAccounts.filter((account) => {
    const query = email.trim().toLowerCase();
    if (!query) return true;
    return account.email.toLowerCase().includes(query);
  });

  function selectRememberedAccount(account: RememberedAccount) {
    setEmail(account.email);
    setPassword(account.password);
    setRemember(true);
    setShowEmailSuggestions(false);
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
                <Text style={[styles.backText, { color: colors.backText }]}>Retour</Text>
              </TouchableOpacity>

              <View style={styles.centerContent}>
                <View style={styles.logoBox}>
                  <Image
                    source={require("../assets/images/wise_trash.png")}
                    style={styles.logoImage}
                    resizeMode="contain"
                  />
                </View>

                <View style={[styles.card, { backgroundColor: colors.card }]}>
                  <Text style={[styles.title, { color: colors.title }]}>
                    Bienvenue
                  </Text>

                  <Text style={[styles.subtitle, { color: colors.subtitle }]}>
                    se connecter pour continuer 
                  </Text>

                  <View style={styles.formGroup}>
                    <Text style={[styles.label, { color: colors.label }]}>Email</Text>
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
                        onChangeText={(value) => {
                          setEmail(value);
                          setShowEmailSuggestions(true);
                        }}
                        placeholder="Entrer votre email"
                        placeholderTextColor={colors.placeholder}
                        style={[styles.input, { color: colors.inputText }]}
                        autoCapitalize="none"
                        keyboardType="email-address"
                        onFocus={() => setShowEmailSuggestions(true)}
                      />
                    </View>
                    {showEmailSuggestions && emailSuggestions.length > 0 && (
                      <View
                        style={[
                          styles.suggestionsBox,
                          {
                            backgroundColor: colors.inputBg,
                            borderColor: colors.inputBorder,
                          },
                        ]}
                      >
                        {emailSuggestions.map((account) => (
                          <TouchableOpacity
                            key={account.email}
                            style={styles.suggestionItem}
                            activeOpacity={0.85}
                            onPress={() => selectRememberedAccount(account)}
                          >
                            <Ionicons name="mail-outline" size={16} color={colors.icon} />
                            <Text
                              style={[
                                styles.suggestionText,
                                { color: colors.inputText },
                              ]}
                              numberOfLines={1}
                            >
                              {account.email}
                            </Text>
                          </TouchableOpacity>
                        ))}
                      </View>
                    )}
                  </View>

                  <View style={styles.formGroup}>
                    <Text style={[styles.label, { color: colors.label }]}>Mot de passe</Text>
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
                        value={password}
                        onChangeText={setPassword}
                        placeholder="Entrer votre mot de passe"
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

                  <View style={styles.optionsRow}>
                    <TouchableOpacity
                      style={styles.rememberRow}
                      onPress={() => setRemember(!remember)}
                    >
                      <View
                        style={[
                          styles.checkbox,
                          {
                            borderColor: remember
                              ? colors.checkboxChecked
                              : colors.checkboxBorder,
                            backgroundColor: remember
                              ? colors.checkboxChecked
                              : colors.checkboxBg,
                          },
                        ]}
                      >
                        {remember && <Ionicons name="checkmark" size={12} color="#fff" />}
                      </View>

                      <Text style={[styles.rememberText, { color: colors.rememberText }]}>
                        se souvenir de moi
                      </Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                      activeOpacity={0.8}
                      onPress={() =>
                        router.push({
                          pathname: "/forgot-password",
                          params: { email: email.trim() },
                        })
                      }
                    >
                      <Text style={[styles.forgotText, { color: colors.forgotText }]}>
                        Mot de passe oublié?
                      </Text>
                    </TouchableOpacity>
                  </View>

                  <TouchableOpacity
                    activeOpacity={0.9}
                    style={styles.buttonShadow}
                    onPress={handleLogin}
                    disabled={loading}
                  >
                    <LinearGradient
                      colors={["#12905C", "#1CC37B"] as const}
                      start={{ x: 0, y: 0 }}
                      end={{ x: 1, y: 0 }}
                      style={styles.loginButton}
                    >
                      {loading ? (
                        <ActivityIndicator color="#FFFFFF" />
                      ) : (
                        <Text style={styles.loginButtonText}>se connecter</Text>
                      )}
                    </LinearGradient>
                  </TouchableOpacity>

                  <View style={styles.signupRow}>
                    <Text style={[styles.signupText, { color: colors.signupText }]}>
                      je n&apos;ai pas de compte?
                    </Text>
                    <TouchableOpacity onPress={() => router.push("/signup")}>
                      <Text style={[styles.signupLink, { color: colors.signupLink }]}>
                        S&apos;inscrire
                      </Text>
                    </TouchableOpacity>
                  </View>
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

  logoImage: {
    width: 200,
    height: 350,
    ...(Platform.OS === "android"
      ? {
          width: 148,
          height: 148,
        }
      : {}),
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
    width: 102,
    height: 102,
    borderRadius: 16,
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 24,
    ...(Platform.OS === "ios"
      ? {
          shadowColor: "#119660",
          shadowOffset: { width: 0, height: 10 },
          shadowOpacity: 0.25,
          shadowRadius: 16,
        }
      : {
          elevation: 0,
        }),
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

  suggestionsBox: {
    borderWidth: 1,
    borderTopWidth: 0,
    borderBottomLeftRadius: 14,
    borderBottomRightRadius: 14,
    marginTop: -2,
    overflow: "hidden",
  },

  suggestionItem: {
    minHeight: 44,
    paddingHorizontal: 12,
    flexDirection: "row",
    alignItems: "center",
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(148,163,184,0.25)",
  },

  suggestionText: {
    flex: 1,
    marginLeft: 8,
    fontSize: 13,
    fontWeight: "700",
  },

  optionsRow: {
    marginTop: 2,
    marginBottom: 22,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },

  rememberRow: {
    flexDirection: "row",
    alignItems: "center",
  },

  checkbox: {
    width: 16,
    height: 16,
    borderWidth: 1.5,
    borderRadius: 4,
    marginRight: 8,
    alignItems: "center",
    justifyContent: "center",
  },

  rememberText: {
    fontSize: 13,
  },

  forgotText: {
    fontSize: 13,
    fontWeight: "600",
  },

  buttonShadow: {
    borderRadius: 14,
    shadowColor: "#169A66",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.22,
    shadowRadius: 12,
    elevation: 6,
  },

  loginButton: {
    height: 50,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
  },

  loginButtonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "700",
  },

  signupRow: {
    marginTop: 18,
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
  },

  signupText: {
    fontSize: 13,
  },

  signupLink: {
    fontSize: 13,
    fontWeight: "700",
    marginLeft: 6,
  },
});
