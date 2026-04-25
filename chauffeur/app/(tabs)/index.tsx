import { Ionicons } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import { router } from "expo-router";
import React, { useEffect, useMemo, useRef } from "react";
import {
  Animated,
  Dimensions,
  Easing,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  useColorScheme,
} from "react-native";

const { width, height } = Dimensions.get("window");

type ParticleProps = {
  left: number;
  top: number;
  size: number;
  opacity: number;
  duration: number;
  delay: number;
  color: string;
};

function Particle({
  left,
  top,
  size,
  opacity,
  duration,
  delay,
  color,
}: ParticleProps) {
  const translateY = useRef(new Animated.Value(0)).current;
  const translateX = useRef(new Animated.Value(0)).current;
  const scale = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    const animation = Animated.loop(
      Animated.parallel([
        Animated.sequence([
          Animated.timing(translateY, {
            toValue: -20,
            duration,
            delay,
            easing: Easing.inOut(Easing.sin),
            useNativeDriver: true,
          }),
          Animated.timing(translateY, {
            toValue: 0,
            duration,
            easing: Easing.inOut(Easing.sin),
            useNativeDriver: true,
          }),
        ]),
        Animated.sequence([
          Animated.timing(translateX, {
            toValue: 8,
            duration: duration + 500,
            delay,
            easing: Easing.inOut(Easing.sin),
            useNativeDriver: true,
          }),
          Animated.timing(translateX, {
            toValue: -8,
            duration: duration + 500,
            easing: Easing.inOut(Easing.sin),
            useNativeDriver: true,
          }),
          Animated.timing(translateX, {
            toValue: 0,
            duration: duration + 500,
            easing: Easing.inOut(Easing.sin),
            useNativeDriver: true,
          }),
        ]),
        Animated.sequence([
          Animated.timing(scale, {
            toValue: 1.18,
            duration: duration / 2,
            delay,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: true,
          }),
          Animated.timing(scale, {
            toValue: 1,
            duration: duration / 2,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: true,
          }),
        ]),
      ])
    );

    animation.start();

    return () => animation.stop();
  }, [delay, duration, scale, translateX, translateY]);

  return (
    <Animated.View
      style={[
        styles.particle,
        {
          left,
          top,
          width: size,
          height: size,
          borderRadius: size / 2,
          opacity,
          backgroundColor: color,
          transform: [{ translateY }, { translateX }, { scale }],
        },
      ]}
    />
  );
}

export default function HomeScreen() {
  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";

  const theme = isDark
    ? {
        gradient: ["#0b200e", "#09140b", "#0F172A"] as const,
        bg: "#030303",
        blur: "#1f8a6f2a",
        particle: "#34d39959",
        title: "#01f098",
        subtitle: "#CBD5E1",
        buttonShadow: "#11925F",
        statusBar: "light-content" as const,
      }
    : {
        gradient: ["#F9FCFB", "#EEF7F4", "#F8FBFA"] as const,
        bg: "#F8FBFA",
        blur: "#BFEFD9",
        particle: "#22C983",
        title: "#0E8E63",
        subtitle: "#6B7280",
        buttonShadow: "#11925F",
        statusBar: "dark-content" as const,
      };

  const logoScale = useRef(new Animated.Value(1)).current;
  const textOpacity = useRef(new Animated.Value(0)).current;
  const buttonTranslateY = useRef(new Animated.Value(20)).current;
  const buttonOpacity = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.loop(
        Animated.sequence([
          Animated.timing(logoScale, {
            toValue: 1.06,
            duration: 1600,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: true,
          }),
          Animated.timing(logoScale, {
            toValue: 1,
            duration: 1600,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: true,
          }),
        ])
      ),
      Animated.timing(textOpacity, {
        toValue: 1,
        duration: 900,
        easing: Easing.out(Easing.ease),
        useNativeDriver: true,
      }),
      Animated.parallel([
        Animated.timing(buttonTranslateY, {
          toValue: 0,
          duration: 900,
          easing: Easing.out(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(buttonOpacity, {
          toValue: 1,
          duration: 900,
          easing: Easing.out(Easing.ease),
          useNativeDriver: true,
        }),
      ]),
    ]).start();
  }, [buttonOpacity, buttonTranslateY, logoScale, textOpacity]);

  const particles = useMemo(() => {
    return Array.from({ length: 40 }).map((_, i) => ({
      id: i,
      left: Math.random() * (width - 20),
      top: Math.random() * (height - 140),
      size: Math.random() * 7 + 3,
      opacity: Math.random() * 0.35 + 0.15,
      duration: Math.random() * 2800 + 2200,
      delay: Math.random() * 1400,
    }));
  }, []);

  return (
    <LinearGradient
      colors={theme.gradient}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={[styles.container, { backgroundColor: theme.bg }]}
    >
      <StatusBar barStyle={theme.statusBar} />

      <View style={[styles.blurCircle, styles.circle1, { backgroundColor: theme.blur }]} />
      <View style={[styles.blurCircle, styles.circle2, { backgroundColor: theme.blur }]} />
      <View style={[styles.blurCircle, styles.circle3, { backgroundColor: theme.blur }]} />
      <View style={[styles.blurCircle, styles.circle4, { backgroundColor: theme.blur }]} />
      <View style={[styles.blurCircle, styles.circle5, { backgroundColor: theme.blur }]} />

      {particles.map((item) => (
        <Particle
          key={item.id}
          left={item.left}
          top={item.top}
          size={item.size}
          opacity={item.opacity}
          duration={item.duration}
          delay={item.delay}
          color={theme.particle}
        />
      ))}

      <View style={styles.content}>
        <Animated.View style={{ transform: [{ scale: logoScale }] }}>
          <LinearGradient
            colors={["#0F8E63", "#1CC97F"]}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.logoBox}
          >
            <Ionicons name="trash-outline" size={42} color="#FFFFFF" />
          </LinearGradient>
        </Animated.View>

        <Animated.View style={[styles.textWrapper, { opacity: textOpacity }]}>
          <Text style={[styles.title, { color: theme.title }]}>Intello Trash</Text>
          <Text style={[styles.subtitle, { color: theme.subtitle }]}>
            Smart Waste Collection
          </Text>
        </Animated.View>

        <Animated.View
          style={{
            opacity: buttonOpacity,
            transform: [{ translateY: buttonTranslateY }],
          }}
        >
          <TouchableOpacity
            activeOpacity={0.88}
            style={[styles.buttonShadow, { shadowColor: theme.buttonShadow }]}
            onPress={() => router.push("/login")}
          >
            <LinearGradient
              colors={["#0E8E63", "#19C37D"]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 0 }}
              style={styles.button}
            >
              <Text style={styles.buttonText}>Login</Text>
            </LinearGradient>
          </TouchableOpacity>
        </Animated.View>
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    position: "relative",
  },

  content: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: 24,
  },

  textWrapper: {
    alignItems: "center",
  },

  blurCircle: {
    position: "absolute",
    borderRadius: 999,
  },

  circle1: {
    width: 240,
    height: 240,
    top: 70,
    left: -70,
    opacity: 0.22,
  },

  circle2: {
    width: 170,
    height: 170,
    bottom: 120,
    right: -35,
    opacity: 0.2,
  },

  circle3: {
    width: 120,
    height: 120,
    top: 240,
    right: 35,
    opacity: 0.14,
  },

  circle4: {
    width: 190,
    height: 190,
    bottom: 250,
    left: -70,
    opacity: 0.16,
  },

  circle5: {
    width: 140,
    height: 140,
    top: 115,
    right: -45,
    opacity: 0.14,
  },

  particle: {
    position: "absolute",
  },

  logoBox: {
    width: 98,
    height: 98,
    borderRadius: 26,
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 24,
    shadowColor: "#11925F",
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.24,
    shadowRadius: 16,
    elevation: 10,
  },

  title: {
    fontSize: 34,
    fontWeight: "800",
    marginBottom: 8,
    letterSpacing: 0.3,
  },

  subtitle: {
    fontSize: 16,
    marginBottom: 30,
    fontWeight: "500",
  },

  buttonShadow: {
    borderRadius: 16,
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.22,
    shadowRadius: 14,
    elevation: 8,
  },

  button: {
    minWidth: 165,
    height: 52,
    borderRadius: 16,
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: 30,
  },

  buttonText: {
    color: "#FFFFFF",
    fontSize: 17,
    fontWeight: "700",
    letterSpacing: 0.2,
  },
});