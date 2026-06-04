import { Tabs } from "expo-router";
import { router } from "expo-router";
import React, { useEffect, useRef } from "react";
import { Ionicons } from "@expo/vector-icons";
import { Alert, AppState, Linking } from "react-native";

import { HapticTab } from "@/components/haptic-tab";
import { AppColors } from "@/constants/app-colors";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { requireDriverLocation } from "@/lib/locationRequirement";
import { removeAuth } from "@/lib/storage";

export default function TabLayout() {
  const colorScheme = useColorScheme();
  const scheme = colorScheme ?? "light";
  const colors = AppColors[scheme];
  const isDark = scheme === "dark";
  const alertVisibleRef = useRef(false);

  useEffect(() => {
    let cancelled = false;

    async function verifyLocationAccess() {
      const requirement = await requireDriverLocation();

      if (cancelled || requirement.ok || alertVisibleRef.current) return;

      alertVisibleRef.current = true;
      await removeAuth();
      router.replace("/login");

      Alert.alert(
        "Localisation obligatoire",
        requirement.message ?? "Activez la localisation pour continuer.",
        [
          {
            text: "OK",
            style: "cancel",
            onPress: () => {
              alertVisibleRef.current = false;
            },
          },
          {
            text: "Paramètres",
            onPress: () => {
              alertVisibleRef.current = false;
              Linking.openSettings();
            },
          },
        ]
      );
    }

    verifyLocationAccess();

    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "active") {
        verifyLocationAccess();
      }
    });

    return () => {
      cancelled = true;
      subscription.remove();
    };
  }, []);

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarButton: HapticTab,
        tabBarActiveTintColor: colors.primary2,
        tabBarInactiveTintColor: isDark ? "#94A3B8" : "#6B7280",
        tabBarStyle: {
          backgroundColor: colors.tabBg,
          borderTopColor: colors.tabBorder,
          height: 64,
          paddingBottom: 8,
          paddingTop: 8,
        },
        tabBarLabelStyle: {
          fontSize: 12,
          fontWeight: "600",
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "Accueil",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="home-outline" size={size} color={color} />
          ),
        }}
      />

      <Tabs.Screen
        name="dashboard"
        options={{
          title: "Tableau",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="grid-outline" size={size} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="scan"
        options={{
          title: "Scan",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="scan-outline" size={size} color={color} />
          ),
        }}
      />
            <Tabs.Screen
        name="profile"
        options={{
          title: "Profil",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="person-outline" size={size} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="notifications"
        options={{
          title: "Alertes",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="notifications-outline" size={size} color={color} />
          ),
        }}
      />



      <Tabs.Screen
        name="map"
        options={{
          title: "Carte",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="map-outline" size={size} color={color} />
          ),
        }}
      />

      <Tabs.Screen
        name="declare-breakdown"
        options={{
          href: null,
        }}
      />

      <Tabs.Screen
        name="explore"
        options={{
          href: null,
        }}
      />
    </Tabs>
  );
}
