import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  SafeAreaView,
  ScrollView,
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
import { BASE_URL } from "../../lib/api";
import { getToken, removeAuth } from "../../lib/storage";

type DriverProfile = {
  fullName?: string;
  email?: string;
  phone?: string;
  username?: string;
  driverId?: string;
  assignedTruck?: string;
  shiftSchedule?: string;
  binsCollected?: number;
  efficiency?: number;
  kmDriven?: number;
  routesDone?: number;
};

export default function ProfileScreen() {
  const isDark = useColorScheme() === "dark";

  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [loading, setLoading] = useState(true);
  const [savingPassword, setSavingPassword] = useState(false);
  const [profile, setProfile] = useState<DriverProfile | null>(null);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    try {
      setLoading(true);

      const token = await getToken();

      if (!token) {
        Alert.alert("Erreur", "Session expirée. Veuillez vous reconnecter.");
        router.replace("/login");
        return;
      }

      const response = await fetch(`${BASE_URL}/api/settings/profile`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      const raw = await response.text();
      console.log("PROFILE RAW:", raw);

      let data: any = null;
      try {
        data = raw ? JSON.parse(raw) : null;
      } catch {
        data = null;
      }

      if (!response.ok) {
        console.log("Erreur chargement profil:", data || raw);
        Alert.alert("Erreur", "Impossible de charger le profil.");
        return;
      }

      setProfile(data || {});
    } catch (error) {
      console.log("Erreur profile:", error);
      Alert.alert("Erreur", "Une erreur est survenue.");
    } finally {
      setLoading(false);
    }
  }

  async function handleUpdatePassword() {
    if (!currentPassword.trim() || !newPassword.trim() || !confirmPassword.trim()) {
      Alert.alert("Erreur", "Veuillez remplir tous les champs.");
      return;
    }

    if (newPassword !== confirmPassword) {
      Alert.alert("Erreur", "Les mots de passe ne correspondent pas.");
      return;
    }

    try {
      setSavingPassword(true);

      const token = await getToken();

      if (!token) {
        Alert.alert("Erreur", "Session expirée. Veuillez vous reconnecter.");
        router.replace("/login");
        return;
      }

      const response = await fetch(`${BASE_URL}/api/settings/password`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          currentPassword,
          newPassword,
        }),
      });

      const raw = await response.text();
      console.log("PASSWORD RAW:", raw);

      let data: any = null;
      try {
        data = raw ? JSON.parse(raw) : null;
      } catch {
        data = null;
      }

      if (!response.ok) {
        console.log("Erreur changement mot de passe:", data || raw);
        Alert.alert("Erreur", "Impossible de modifier le mot de passe.");
        return;
      }

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");

      Alert.alert("Succès", data?.message || "Mot de passe mis à jour.");
    } catch (error) {
      console.log("Erreur update password:", error);
      Alert.alert("Erreur", "Une erreur est survenue.");
    } finally {
      setSavingPassword(false);
    }
  }

  async function handleLogout() {
    try {
      await removeAuth();
      router.replace("/login");
    } catch (error) {
      console.log("Erreur logout:", error);
      Alert.alert("Erreur", "Impossible de se déconnecter.");
    }
  }

  const theme = isDark
    ? {
        background: "#081120",
        card: "#121C2E",
        softCard: "#0D1728",
        text: "#FFFFFF",
        subText: "#94A3B8",
        border: "#243041",
        inputBg: "#0B1530",
        inputBorder: "#243041",
        backButton: "#132033",
      }
    : {
        background: "#F3F5F7",
        card: "#FFFFFF",
        softCard: "#F7F9FB",
        text: "#102A43",
        subText: "#7C8DA6",
        border: "#E5E7EB",
        inputBg: "#FFFFFF",
        inputBorder: "#E5E7EB",
        backButton: "#FFFFFF",
      };

  if (loading) {
    return (
      <SafeAreaView
        style={[styles.container, styles.loaderContainer, { backgroundColor: theme.background }]}
      >
        <ActivityIndicator size="large" color="#19C37D" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={isDark ? "light-content" : "dark-content"} />

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <TouchableOpacity
            style={[styles.backButton, { backgroundColor: theme.backButton }]}
            onPress={() => router.back()}
            activeOpacity={0.8}
          >
            <Ionicons
              name="arrow-back"
              size={20}
              color={isDark ? "#FFFFFF" : "#1F2937"}
            />
          </TouchableOpacity>

          <Text style={[styles.headerTitle, { color: theme.text }]}>Profile</Text>

          <View style={styles.headerSpacer} />
        </View>

        <LinearGradient colors={["#12905C", "#1CC37B"]} style={styles.heroCard}>
          <View style={styles.avatarCircle}>
            <Ionicons name="person-outline" size={46} color="#FFFFFF" />
          </View>

          <Text style={styles.heroName}>
            {profile?.fullName || "Nom non disponible"}
          </Text>
          <Text style={styles.heroId}>Driver ID: {profile?.driverId || "-"}</Text>
        </LinearGradient>

        <View style={[styles.card, { backgroundColor: theme.card }]}>
          <Text style={[styles.sectionTitle, { color: theme.text }]}>
            Driver Information
          </Text>

          <InfoRow
            theme={theme}
            icon="person-outline"
            iconColor="#10B981"
            green
            label="Full Name"
            value={profile?.fullName || "-"}
          />

          <InfoRow
            theme={theme}
            icon="mail-outline"
            iconColor="#64748B"
            label="Email"
            value={profile?.email || "-"}
          />

          <InfoRow
            theme={theme}
            icon="call-outline"
            iconColor="#64748B"
            label="Phone"
            value={profile?.phone || "-"}
          />

          <InfoRow
            theme={theme}
            icon="bus-outline"
            iconColor="#64748B"
            label="Assigned Truck"
            value={profile?.assignedTruck || "-"}
          />

          <InfoRow
            theme={theme}
            icon="calendar-outline"
            iconColor="#10B981"
            green
            label="Shift Schedule"
            value={profile?.shiftSchedule || "-"}
          />
        </View>

        <View style={[styles.card, { backgroundColor: theme.card }]}>
          <View style={styles.sectionHeader}>
            <Ionicons name="lock-closed-outline" size={18} color="#12905C" />
            <Text style={[styles.sectionTitleInline, { color: theme.text }]}>
              Security Settings
            </Text>
          </View>

          <PasswordInput
            label="Current Password"
            value={currentPassword}
            onChangeText={setCurrentPassword}
            show={showCurrent}
            onToggleShow={() => setShowCurrent(!showCurrent)}
            placeholder="Enter current password"
            theme={theme}
          />

          <PasswordInput
            label="New Password"
            value={newPassword}
            onChangeText={setNewPassword}
            show={showNew}
            onToggleShow={() => setShowNew(!showNew)}
            placeholder="Enter new password"
            theme={theme}
          />

          <PasswordInput
            label="Confirm New Password"
            value={confirmPassword}
            onChangeText={setConfirmPassword}
            show={showConfirm}
            onToggleShow={() => setShowConfirm(!showConfirm)}
            placeholder="Confirm new password"
            theme={theme}
          />

          <TouchableOpacity
            style={styles.updateButton}
            activeOpacity={0.85}
            onPress={handleUpdatePassword}
            disabled={savingPassword}
          >
            <Text style={styles.updateButtonText}>
              {savingPassword ? "Updating..." : "Update Password"}
            </Text>
          </TouchableOpacity>
        </View>

        <View style={[styles.card, { backgroundColor: theme.card }]}>
          <Text style={[styles.sectionTitle, { color: theme.text }]}>This Month</Text>

          <View style={styles.statsGrid}>
            <View style={[styles.statBox, styles.statGreen]}>
              <Text style={[styles.statNumber, { color: "#0E8E63" }]}>
                {profile?.binsCollected ?? 0}
              </Text>
              <Text style={styles.statText}>Bins Collected</Text>
            </View>

            <View style={[styles.statBox, styles.statGreen]}>
              <Text style={[styles.statNumber, { color: "#19C37D" }]}>
                {profile?.efficiency ?? 0}%
              </Text>
              <Text style={styles.statText}>Efficiency</Text>
            </View>

            <View style={[styles.statBox, styles.statBlue]}>
              <Text style={[styles.statNumber, { color: "#3B82F6" }]}>
                {profile?.kmDriven ?? 0}
              </Text>
              <Text style={styles.statText}>km Driven</Text>
            </View>

            <View style={[styles.statBox, styles.statYellow]}>
              <Text style={[styles.statNumber, { color: "#F59E0B" }]}>
                {profile?.routesDone ?? 0}
              </Text>
              <Text style={styles.statText}>Routes Done</Text>
            </View>
          </View>
        </View>

        <TouchableOpacity
          style={styles.logoutButton}
          activeOpacity={0.85}
          onPress={handleLogout}
        >
          <Ionicons name="log-out-outline" size={18} color="#FF5A5F" />
          <Text style={styles.logoutText}>Logout</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

function InfoRow({
  theme,
  icon,
  iconColor,
  green = false,
  label,
  value,
}: {
  theme: any;
  icon: keyof typeof Ionicons.glyphMap;
  iconColor: string;
  green?: boolean;
  label: string;
  value: string;
}) {
  return (
    <View style={[styles.infoRow, { backgroundColor: theme.softCard }]}>
      <View style={green ? styles.infoIconGreen : styles.infoIconGray}>
        <Ionicons name={icon} size={18} color={iconColor} />
      </View>
      <View style={{ flex: 1 }}>
        <Text style={[styles.infoLabel, { color: theme.subText }]}>{label}</Text>
        <Text style={[styles.infoValue, { color: theme.text }]}>{value}</Text>
      </View>
    </View>
  );
}

function PasswordInput({
  label,
  value,
  onChangeText,
  show,
  onToggleShow,
  placeholder,
  theme,
}: {
  label: string;
  value: string;
  onChangeText: (text: string) => void;
  show: boolean;
  onToggleShow: () => void;
  placeholder: string;
  theme: any;
}) {
  return (
    <>
      <Text style={[styles.inputLabel, { color: theme.subText }]}>{label}</Text>
      <View
        style={[
          styles.inputWrap,
          {
            backgroundColor: theme.inputBg,
            borderColor: theme.inputBorder,
          },
        ]}
      >
        <Ionicons name="lock-closed-outline" size={18} color="#94A3B8" />
        <TextInput
          style={[styles.input, { color: theme.text }]}
          placeholder={placeholder}
          placeholderTextColor="#9CA3AF"
          secureTextEntry={!show}
          value={value}
          onChangeText={onChangeText}
        />
        <TouchableOpacity onPress={onToggleShow}>
          <Ionicons
            name={show ? "eye-outline" : "eye-off-outline"}
            size={18}
            color="#94A3B8"
          />
        </TouchableOpacity>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },

  loaderContainer: {
    justifyContent: "center",
    alignItems: "center",
  },

  content: {
    padding: 16,
    paddingBottom: 28,
  },

  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 16,
  },

  backButton: {
    width: 36,
    height: 36,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 3,
  },

  headerTitle: {
    fontSize: 20,
    fontWeight: "700",
  },

  headerSpacer: {
    width: 36,
  },

  heroCard: {
    borderRadius: 22,
    paddingVertical: 26,
    paddingHorizontal: 20,
    alignItems: "center",
    marginBottom: 18,
  },

  avatarCircle: {
    width: 90,
    height: 90,
    borderRadius: 45,
    borderWidth: 3,
    borderColor: "rgba(255,255,255,0.28)",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 14,
    backgroundColor: "rgba(255,255,255,0.12)",
  },

  heroName: {
    color: "#FFFFFF",
    fontSize: 18,
    fontWeight: "800",
    marginBottom: 4,
  },

  heroId: {
    color: "#D8FFF0",
    fontSize: 14,
  },

  card: {
    borderRadius: 20,
    padding: 16,
    marginBottom: 18,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.05,
    shadowRadius: 12,
    elevation: 4,
  },

  sectionTitle: {
    fontSize: 18,
    fontWeight: "700",
    marginBottom: 14,
  },

  sectionHeader: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 14,
  },

  sectionTitleInline: {
    fontSize: 18,
    fontWeight: "700",
    marginLeft: 8,
  },

  infoRow: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 16,
    padding: 14,
    marginBottom: 10,
  },

  infoIconGreen: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: "#E9FBF3",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },

  infoIconGray: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: "#EEF2F7",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },

  infoLabel: {
    fontSize: 12,
    marginBottom: 3,
  },

  infoValue: {
    fontSize: 15,
    fontWeight: "700",
  },

  inputLabel: {
    fontSize: 13,
    marginBottom: 8,
    marginTop: 4,
  },

  inputWrap: {
    height: 48,
    borderWidth: 1,
    borderRadius: 14,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    marginBottom: 14,
  },

  input: {
    flex: 1,
    marginLeft: 8,
    fontSize: 14,
  },

  updateButton: {
    backgroundColor: "#19C37D",
    height: 48,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
    marginTop: 4,
  },

  updateButtonText: {
    color: "#FFFFFF",
    fontSize: 15,
    fontWeight: "700",
  },

  statsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
  },

  statBox: {
    width: "48.5%",
    borderRadius: 16,
    paddingVertical: 18,
    paddingHorizontal: 12,
    alignItems: "center",
    marginBottom: 12,
  },

  statGreen: {
    backgroundColor: "#EEF8F3",
  },

  statBlue: {
    backgroundColor: "#EEF4FF",
  },

  statYellow: {
    backgroundColor: "#FFF7E8",
  },

  statNumber: {
    fontSize: 22,
    fontWeight: "800",
    marginBottom: 4,
  },

  statText: {
    fontSize: 13,
    color: "#7C8DA6",
  },

  logoutButton: {
    height: 48,
    borderRadius: 14,
    backgroundColor: "#FFF1F2",
    borderWidth: 1,
    borderColor: "#FECACA",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
  },

  logoutText: {
    color: "#FF5A5F",
    fontWeight: "700",
    marginLeft: 6,
  },
});