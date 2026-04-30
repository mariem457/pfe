import React, { useState } from "react";
import {
  ActivityIndicator,
  Alert,
  SafeAreaView,
  
  Keyboard,
KeyboardAvoidingView,
Platform,
ScrollView,
TouchableWithoutFeedback,
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
import { router } from "expo-router";
import { BASE_URL } from "../lib/api";

export default function SignupScreen() {
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
        loginText: "#CBD5E1",
        loginLink: "#22C983",
        required: "#EF4444",
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
        loginText: "#64748B",
        loginLink: "#169A66",
        required: "#DC2626",
      };

  const [fullName, setFullName] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSignup() {
    const trimmedFullName = fullName.trim();
    const trimmedUsername = username.trim();
    const trimmedEmail = email.trim();
    const allowedEmailRegex =
  /^[A-Za-z0-9._%+-]+@(gmail|icloud|outlook|hotmail|yahoo|live|protonmail)\.(com|fr)$/i;


    const trimmedPhone = phone.trim();

    if (
      !trimmedFullName ||
      !trimmedUsername ||
      !trimmedEmail ||
      !trimmedPhone ||
      !password.trim() ||
      !confirmPassword.trim()
    ) {
      Alert.alert("Erreur", "Veuillez remplir tous les champs.");
      return;
    }
if (!allowedEmailRegex.test(trimmedEmail)) {
  Alert.alert(
    "Erreur",
    "Veuillez utiliser une adresse e-mail valide"
  );
  return;
}
      const frenchPhoneRegex = /^(?:\+33|0)[1-9](?:\d{8})$/;

  if (!frenchPhoneRegex.test(trimmedPhone.replace(/\s+/g, ""))) {
    Alert.alert("Erreur", "Veuillez saisir un numéro de téléphone français valide.");
    return;
  }
  const passwordRegex =
  /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>_\-\\/[\];'`~+=]).{8,}$/;

if (!passwordRegex.test(password)) {
  Alert.alert(
    "Erreur",
    "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial."
  );
  return;
}

    if (password !== confirmPassword) {
      Alert.alert("Erreur", "Les mots de passe ne correspondent pas.");
      return;
    }

    try {
      setLoading(true);

      const response = await fetch(`${BASE_URL}/api/auth/register-driver`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          fullName: trimmedFullName,
          username: trimmedUsername,
          email: trimmedEmail,
          phone: trimmedPhone,
          password,
          role: "DRIVER",
        }),
      });

      const raw = await response.text();

      let data: any = null;
      try {
        data = raw ? JSON.parse(raw) : null;
      } catch {
        data = null;
      }
      if (!response.ok) {
        const backendMessage =
          data?.message ||
          raw ||
          "Une erreur est survenue lors de l'inscription.";

        Alert.alert("Erreur", backendMessage);
        console.log("Erreur backend inscription :", data || raw);
        return;
      }

      const successMessage =
        data?.message ||
        raw ||
        "Compte créé. Un code de vérification a été envoyé par e-mail.";

      Alert.alert("Succès", successMessage, [
        {
          text: "OK",
          onPress: () =>
            router.push({
              pathname: "/verify-email",
              params: { email: trimmedEmail },
            }),
        },
      ]);
    } catch (error) {
      console.log("Erreur inscription :", error);
      Alert.alert("Erreur", "Une erreur est survenue.");
    } finally {
      setLoading(false);
    }
  }

  function Label({ text }: { text: string }) {
    return (
      <Text style={[styles.label, { color: colors.label }]}>
        {text}
        <Text style={{ color: colors.required }}> *</Text>
      </Text>
    );
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
          <Text style={[styles.backText, { color: colors.backText }]}>
            Retour
          </Text>
        </TouchableOpacity>

        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.scrollContent}
        >
          <View style={styles.centerContent}>
            <LinearGradient
              colors={["#14935F", "#1DBE7A"]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.logoBox}
            >
              <Ionicons name="person-add-outline" size={32} color="#FFFFFF" />
            </LinearGradient>

            <View style={[styles.card, { backgroundColor: colors.card }]}>
              <Text style={[styles.title, { color: colors.title }]}>
                Créer un compte
              </Text>

              <Text style={[styles.subtitle, { color: colors.subtitle }]}>
                Inscrivez-vous pour continuer
              </Text>

              <View style={styles.formGroup}>
                <Label text="Nom complet" />
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
                    name="person-outline"
                    size={18}
                    color={colors.icon}
                    style={styles.inputIcon}
                  />
                  <TextInput
                    value={fullName}
                    onChangeText={setFullName}
                    placeholder="Jean Dupont"
                    placeholderTextColor={colors.placeholder}
                    style={[styles.input, { color: colors.inputText }]}
                  />
                </View>
              </View>

              <View style={styles.formGroup}>
                <Label text="Nom d'utilisateur" />
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
                    name="at-outline"
                    size={18}
                    color={colors.icon}
                    style={styles.inputIcon}
                  />
                  <TextInput
                    value={username}
                    onChangeText={setUsername}
                    placeholder="votre_identifiant"
                    placeholderTextColor={colors.placeholder}
                    style={[styles.input, { color: colors.inputText }]}
                    autoCapitalize="none"
                  />
                </View>
              </View>

              <View style={styles.formGroup}>
                <Label text="Adresse e-mail" />
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
                    placeholder="votre@email.com"
                    placeholderTextColor={colors.placeholder}
                    style={[styles.input, { color: colors.inputText }]}
                    autoCapitalize="none"
                    keyboardType="email-address"
                  />
                </View>
              </View>

              <View style={styles.formGroup}>
                <Label text="Numéro de téléphone" />
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
                    name="call-outline"
                    size={18}
                    color={colors.icon}
                    style={styles.inputIcon}
                  />
                  <TextInput
                    value={phone}
                    onChangeText={setPhone}
                    placeholder="+216 12 345 678"
                    placeholderTextColor={colors.placeholder}
                    style={[styles.input, { color: colors.inputText }]}
                    keyboardType="phone-pad"
                  />
                </View>
              </View>

              <View style={styles.formGroup}>
                <Label text="Mot de passe" />
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
                    placeholder="Saisissez votre mot de passe"
                    placeholderTextColor={colors.placeholder}
                    secureTextEntry={!showPassword}
                    style={[styles.input, { color: colors.inputText }]}
                  />
                  <TouchableOpacity
                    onPress={() => setShowPassword(!showPassword)}
                  >
                    <Ionicons
                      name={showPassword ? "eye-outline" : "eye-off-outline"}
                      size={18}
                      color={colors.icon}
                    />
                  </TouchableOpacity>
                </View>
              </View>

              <View style={styles.formGroup}>
                <Label text="Confirmer le mot de passe" />
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
                    placeholder="Confirmez votre mot de passe"
                    placeholderTextColor={colors.placeholder}
                    secureTextEntry={!showConfirmPassword}
                    style={[styles.input, { color: colors.inputText }]}
                  />
                  <TouchableOpacity
                    onPress={() =>
                      setShowConfirmPassword(!showConfirmPassword)
                    }
                  >
                    <Ionicons
                      name={
                        showConfirmPassword ? "eye-outline" : "eye-off-outline"
                      }
                      size={18}
                      color={colors.icon}
                    />
                  </TouchableOpacity>
                </View>
              </View>

              <TouchableOpacity
                activeOpacity={0.9}
                style={styles.buttonShadow}
                onPress={handleSignup}
                disabled={loading}
              >
                <LinearGradient
                  colors={["#12905C", "#1CC37B"]}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 0 }}
                  style={styles.signupButton}
                >
                  {loading ? (
                    <ActivityIndicator color="#FFFFFF" />
                  ) : (
                    <Text style={styles.signupButtonText}>Créer un compte</Text>
                  )}
                </LinearGradient>
              </TouchableOpacity>

              <View style={styles.loginRow}>
                <Text style={[styles.loginText, { color: colors.loginText }]}>
                  Vous avez déjà un compte ?
                </Text>
                <TouchableOpacity onPress={() => router.push("/login")}>
                  <Text style={[styles.loginLink, { color: colors.loginLink }]}>
                    Se connecter
                  </Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        </ScrollView>
      </SafeAreaView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },

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

  scrollContent: {
    paddingBottom: 30,
  },

  centerContent: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingTop: 30,
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
    marginBottom: 16,
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
    marginTop: 6,
  },

  signupButton: {
    height: 50,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
  },

  signupButtonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "700",
  },

  loginRow: {
    marginTop: 18,
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
  },

  loginText: {
    fontSize: 13,
  },

  loginLink: {
    fontSize: 13,
    fontWeight: "700",
    marginLeft: 6,
  },
});