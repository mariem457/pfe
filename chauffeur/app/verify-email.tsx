import React, { useRef, useState } from "react";
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
import { router, useLocalSearchParams } from "expo-router";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { BASE_URL } from "../lib/api";

const CODE_LENGTH = 6;

export default function VerifyEmailScreen() {
  const { email } = useLocalSearchParams<{ email: string }>();
  const isDark = useColorScheme() === "dark";

  const [codeDigits, setCodeDigits] = useState<string[]>(
    Array(CODE_LENGTH).fill("")
  );

  const inputRefs = useRef<Array<TextInput | null>>([]);

  const colors = isDark
    ? {
        gradient: ["#0F1720", "#111827"] as const,
        card: "#1A232D",
        title: "#F3F4F6",
        subtitle: "#94A3B8",
        label: "#CBD5E1",
        inputBg: "#111827",
        inputBorder: "#2A3642",
        inputBorderActive: "#1CC37B",
        inputText: "#F9FAFB",
      }
    : {
        gradient: ["#EEF3F2", "#F7F8FA"] as const,
        card: "#F8F8F9",
        title: "#1F2937",
        subtitle: "#64748B",
        label: "#64748B",
        inputBg: "#FFFFFF",
        inputBorder: "#E5E7EB",
        inputBorderActive: "#169A66",
        inputText: "#111827",
      };

  const code = codeDigits.join("");

  function handleChangeDigit(value: string, index: number) {
    const cleaned = value.replace(/\D/g, "");
    const newDigits = [...codeDigits];

    if (cleaned.length === 0) {
      newDigits[index] = "";
      setCodeDigits(newDigits);
      return;
    }

    if (cleaned.length === 1) {
      newDigits[index] = cleaned;
      setCodeDigits(newDigits);

      if (index < CODE_LENGTH - 1) {
        inputRefs.current[index + 1]?.focus();
      } else {
        Keyboard.dismiss();
      }

      return;
    }

    const pasted = cleaned.slice(0, CODE_LENGTH).split("");
    const updated = Array(CODE_LENGTH).fill("");

    for (let i = 0; i < pasted.length; i++) {
      updated[i] = pasted[i];
    }

    setCodeDigits(updated);

    if (pasted.length === CODE_LENGTH) {
      Keyboard.dismiss();
    } else {
      inputRefs.current[pasted.length]?.focus();
    }
  }

  function handleKeyPress(
    e: { nativeEvent: { key: string } },
    index: number
  ) {
    if (e.nativeEvent.key === "Backspace" && !codeDigits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();

      const newDigits = [...codeDigits];
      newDigits[index - 1] = "";
      setCodeDigits(newDigits);
    }
  }

  async function handleVerify() {
    Keyboard.dismiss();

    if (code.length !== CODE_LENGTH) {
      Alert.alert("Erreur", "Veuillez saisir le code complet.");
      return;
    }

    try {
      const response = await fetch(`${BASE_URL}/api/auth/verify-email`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code }),
      });

      const raw = await response.text();

      let data: any = null;
      try {
        data = raw ? JSON.parse(raw) : null;
      } catch {}

      if (!response.ok) {
        Alert.alert("Erreur", data?.message || "Code incorrect.");
        return;
      }

      Alert.alert(
        "Succès",
        data?.message || raw || "E-mail vérifié avec succès.",
        [{ text: "OK", onPress: () => router.replace("/login") }]
      );
    } catch {
      Alert.alert("Erreur", "Une erreur est survenue.");
    }
  }

  async function handleResend() {
    Keyboard.dismiss();

    try {
      const response = await fetch(`${BASE_URL}/api/auth/resend-verification`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });

      const raw = await response.text();

      let data: any = null;
      try {
        data = raw ? JSON.parse(raw) : null;
      } catch {}

      if (!response.ok) {
        Alert.alert("Erreur", data?.message || "Impossible de renvoyer le code.");
        return;
      }

      Alert.alert("Succès", data?.message || raw || "Code renvoyé avec succès.");
    } catch {
      Alert.alert("Erreur", "Une erreur est survenue.");
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
              <View style={styles.centerContent}>
                <LinearGradient
                  colors={["#14935F", "#1DBE7A"] as const}
                  style={styles.logoBox}
                >
                  <Ionicons name="mail-open-outline" size={32} color="#fff" />
                </LinearGradient>

                <View style={[styles.card, { backgroundColor: colors.card }]}>
                  <Text style={[styles.title, { color: colors.title }]}>
                    Vérification de l’e-mail
                  </Text>

                  <Text style={[styles.subtitle, { color: colors.subtitle }]}>
                    Saisissez le code envoyé à {email}
                  </Text>

                  <View style={styles.formGroup}>
                    <Text style={[styles.label, { color: colors.label }]}>
                      Code de vérification
                    </Text>

                    <View style={styles.codeRow}>
                      {codeDigits.map((digit, index) => (
                        <TextInput
                          key={index}
                          ref={(ref) => {
                            inputRefs.current[index] = ref;
                          }}
                          value={digit}
                          onChangeText={(value) =>
                            handleChangeDigit(value, index)
                          }
                          onKeyPress={(e) => handleKeyPress(e, index)}
                          keyboardType="number-pad"
                          maxLength={index === 0 ? CODE_LENGTH : 1}
                          textAlign="center"
                          style={[
                            styles.codeInput,
                            {
                              backgroundColor: colors.inputBg,
                              borderColor: digit
                                ? colors.inputBorderActive
                                : colors.inputBorder,
                              color: colors.inputText,
                            },
                          ]}
                        />
                      ))}
                    </View>
                  </View>

                  <TouchableOpacity
                    activeOpacity={0.9}
                    style={styles.buttonShadow}
                    onPress={handleVerify}
                  >
                    <LinearGradient
                      colors={["#12905C", "#1CC37B"] as const}
                      style={styles.button}
                    >
                      <Text style={styles.buttonText}>Vérifier</Text>
                    </LinearGradient>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={styles.resendBtn}
                    onPress={handleResend}
                  >
                    <Text style={styles.resendText}>Renvoyer le code</Text>
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
  container: {
    flex: 1,
  },

  keyboardView: {
    flex: 1,
  },

  safeArea: {
    flex: 1,
    paddingHorizontal: 22,
    paddingTop: 10,
  },

  scrollContent: {
    flexGrow: 1,
    paddingBottom: 90,
  },

  centerContent: {
    flexGrow: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingVertical: 40,
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

  codeRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    gap: 8,
  },

  codeInput: {
    width: 42,
    height: 52,
    borderWidth: 1,
    borderRadius: 12,
    fontSize: 20,
    fontWeight: "700",
  },

  buttonShadow: {
    borderRadius: 14,
    marginTop: 10,
  },

  button: {
    height: 50,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
  },

  buttonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "700",
  },

  resendBtn: {
    marginTop: 18,
    alignItems: "center",
  },

  resendText: {
    color: "#169A66",
    fontWeight: "700",
  },
});