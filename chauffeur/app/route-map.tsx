import { Ionicons } from "@expo/vector-icons";
import * as polyline from "@mapbox/polyline";
import { BlurView } from "expo-blur";
import * as Location from "expo-location";
import { router } from "expo-router";
import React, { useEffect, useRef, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import MapView, { Marker, Polyline, PROVIDER_GOOGLE } from "react-native-maps";
import { getToken, getUserId } from "../lib/storage";

const BASE_URL = "http://10.221.127.114:8081";
const OSRM_URL = "http://10.221.127.114:5000";
const DEV_MODE_PARIS = true;

// ─── Types ───────────────────────────────────────────────────────────────────

type DriverBin = {
  missionBinId?: number; missionId?: number; binId?: number; binCode?: string;
  lat?: number; lng?: number; visitOrder?: number; collected?: boolean; wasteType?: string;
};
type Point = { latitude: number; longitude: number };
type MissionRoutePoint = { lat: number; lng: number };
type ManeuverType = "straight"|"turn-left"|"turn-right"|"slight-left"|"slight-right"|"arrive"|"depart";
type NavStep = { maneuver: ManeuverType; streetName: string; distanceM: number };

const PARIS_DRIVER_LOCATION: Point = { latitude: 48.8411, longitude: 2.3003 };

// ─── OSRM parsing ────────────────────────────────────────────────────────────

function parseManeuver(type?: string, modifier?: string): ManeuverType {
  if (type === "arrive") return "arrive";
  if (type === "depart") return "depart";
  if (!modifier) return "straight";
  if (modifier === "left") return "turn-left";
  if (modifier === "right") return "turn-right";
  if (modifier === "slight left") return "slight-left";
  if (modifier === "slight right") return "slight-right";
  return "straight";
}

function buildNavSteps(osrmRoute: any): NavStep[] {
  const steps: NavStep[] = [];
  if (!osrmRoute?.legs) return steps;
  for (const leg of osrmRoute.legs) {
    for (const step of leg.steps ?? []) {
      const m = step.maneuver;
      const maneuver = parseManeuver(m?.type, m?.modifier);
      const distanceM = Math.round(step.distance ?? 0);
      if (distanceM === 0 && maneuver !== "arrive") continue;
      steps.push({ maneuver, streetName: step.name ?? "", distanceM });
    }
  }
  return steps;
}

// ─── Arrow ────────────────────────────────────────────────────────────────────

function Arrow({ maneuver, size = 36, color = "#FFFFFF", opacity = 1 }: {
  maneuver: ManeuverType; size?: number; color?: string; opacity?: number;
}) {
  const iconMap: Record<ManeuverType, any> = {
    straight: "arrow-up", depart: "arrow-up",
    "turn-left": "arrow-back", "turn-right": "arrow-forward",
    "slight-left": "arrow-up", "slight-right": "arrow-up", arrive: "flag",
  };
  const rotate =
    maneuver === "slight-left" ? "-35deg"
    : maneuver === "slight-right" ? "35deg"
    : "0deg";
  return (
    <View style={{ opacity, transform: [{ rotate }] }}>
      <Ionicons name={iconMap[maneuver]} size={size} color={color} />
    </View>
  );
}

// ─── NavBanner ────────────────────────────────────────────────────────────────

function NavBanner({ steps }: { steps: NavStep[] }) {
  const hasSteps = steps.length > 0;
  const current  = steps[0];
  const next1    = steps[1];
  const next2    = steps[2];

  const distText = hasSteps
    ? current.distanceM >= 1000
      ? `${(current.distanceM / 1000).toFixed(1)} km`
      : `${current.distanceM} m`
    : "";

  return (
    <View style={nb.wrapper}>
      <BlurView intensity={80} tint="dark" style={nb.blur}>

        <TouchableOpacity style={nb.backBtn} onPress={() => router.back()}>
          <Ionicons name="chevron-back" size={22} color="#FFFFFF" />
        </TouchableOpacity>

        {hasSteps ? (
          <View style={nb.content}>
            <View style={nb.mainArrowCircle}>
              <Arrow maneuver={current.maneuver} size={30} color="#FFFFFF" />
            </View>
            <View style={nb.textBlock}>
              <Text style={nb.distance}>{distText}</Text>
              {current.streetName !== "" && (
                <Text style={nb.street} numberOfLines={1}>{current.streetName}</Text>
              )}
            </View>
            <View style={nb.nextSteps}>
              {next1 && (
                <View style={nb.nextItem}>
                  <Arrow maneuver={next1.maneuver} size={18} color="rgba(255,255,255,0.55)" />
                </View>
              )}
              {next2 && (
                <View style={nb.nextItem}>
                  <Arrow maneuver={next2.maneuver} size={14} color="rgba(255,255,255,0.3)" />
                </View>
              )}
            </View>
          </View>
        ) : (
          <View style={nb.content}>
            <Text style={nb.noRoute}>Calcul de litinéraire…</Text>
          </View>
        )}

      </BlurView>
    </View>
  );
}

const nb = StyleSheet.create({
  wrapper: {
    position: "absolute", top: 0, left: 0, right: 0, zIndex: 20,
    borderBottomLeftRadius: 24, borderBottomRightRadius: 24,
    overflow: "hidden",
    shadowColor: "#000", shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.35, shadowRadius: 16, elevation: 16,
  },
  blur: {
    paddingTop: 52, paddingBottom: 18, paddingHorizontal: 16,
    flexDirection: "row", alignItems: "center", gap: 14,
    backgroundColor: "rgba(15,23,42,0.82)",
  },
  backBtn: {
    width: 38, height: 38, borderRadius: 12,
    backgroundColor: "rgba(255,255,255,0.12)",
    alignItems: "center", justifyContent: "center",
    borderWidth: 1, borderColor: "rgba(255,255,255,0.15)",
  },
  content: { flex: 1, flexDirection: "row", alignItems: "center", gap: 12 },
  mainArrowCircle: {
    width: 52, height: 52, borderRadius: 16,
    backgroundColor: "#19C37D",
    alignItems: "center", justifyContent: "center",
    shadowColor: "#19C37D", shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.5, shadowRadius: 8, elevation: 6,
  },
  textBlock: { flex: 1 },
  distance: { color: "#FFFFFF", fontSize: 26, fontWeight: "800", letterSpacing: -0.5 },
  street: { color: "rgba(255,255,255,0.65)", fontSize: 13, fontWeight: "600", marginTop: 2 },
  nextSteps: { flexDirection: "column", alignItems: "center", gap: 4 },
  nextItem: {
    width: 28, height: 28, borderRadius: 8,
    backgroundColor: "rgba(255,255,255,0.08)",
    alignItems: "center", justifyContent: "center",
  },
  noRoute: { color: "rgba(255,255,255,0.45)", fontSize: 14, fontStyle: "italic" },
});

// ─── Main component ───────────────────────────────────────────────────────────

export default function RouteMap() {
  const mapRef       = useRef<MapView | null>(null);
  const demoTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const [loading, setLoading]                 = useState(true);
  const [currentLocation, setCurrentLocation] = useState<Point | null>(null);
  const [bins, setBins]                       = useState<DriverBin[]>([]);
  const [routeCoords, setRouteCoords]         = useState<Point[]>([]);
  const [distanceKm, setDistanceKm]           = useState<number | null>(null);
  const [durationMin, setDurationMin]         = useState<number | null>(null);
  const [navSteps, setNavSteps]               = useState<NavStep[]>([]);

  // ── backend ────────────────────────────────────────────────────────────

  async function sendPointToBackend(point: Point, speedKmh = 30, headingDeg = 0) {
    try {
      const token = await getToken(); const userId = await getUserId();
      if (!token || !userId) return;
      const res = await fetch(`${BASE_URL}/api/truck-locations`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({ driverId: Number(userId), lat: point.latitude, lng: point.longitude, speedKmh, headingDeg, timestamp: new Date().toISOString() }),
      });
      if (!res.ok) console.log("TRUCK LOCATION ERROR:", await res.text());
    } catch (e) { console.log("sendPointToBackend error:", e); }
  }

  async function sendRealLocationToBackend(location: Location.LocationObject) {
    await sendPointToBackend(
      { latitude: location.coords.latitude, longitude: location.coords.longitude },
      location.coords.speed != null ? location.coords.speed * 3.6 : 0,
      location.coords.heading ?? 0
    );
  }

  // ── bins ───────────────────────────────────────────────────────────────

  async function loadBins(): Promise<DriverBin[]> {
    try {
      const token = await getToken(); const userId = await getUserId();
      if (!token || !userId) { setBins([]); return []; }
      const res = await fetch(`${BASE_URL}/api/drivers/${userId}/my-bins`, {
        method: "GET", headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      });
      const text = await res.text();
      if (!res.ok) { setBins([]); return []; }
      const data = text ? JSON.parse(text) : [];
      const validBins = Array.isArray(data)
        ? data.filter(b => !b.collected && typeof b.lat === "number" && typeof b.lng === "number")
        : [];
      validBins.sort((a, b) => (a.visitOrder || 999) - (b.visitOrder || 999));
      setBins(validBins); return validBins;
    } catch { setBins([]); return []; }
  }

  function getMissionIdFromBins(routeBins: DriverBin[]): number | null {
    if (!Array.isArray(routeBins) || routeBins.length === 0) return null;
    return routeBins[0]?.missionId ?? null;
  }

  // ── optimized route (sans steps — steps chargés séparément) ───────────

  async function loadOptimizedMissionRoute(missionId: number): Promise<Point[]> {
    try {
      const token = await getToken(); if (!token) return [];
      const res = await fetch(`${BASE_URL}/api/missions/${missionId}/route`, {
        method: "GET", headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      });
      const text = await res.text(); if (!res.ok) return [];
      const data = text ? JSON.parse(text) : null;
      const rawCoords: MissionRoutePoint[] =
        data?.collectionRouteCoordinates?.length >= 2 ? data.collectionRouteCoordinates
        : data?.routeCoordinates?.length >= 2 ? data.routeCoordinates : [];
      const coords: Point[] = rawCoords
        .filter(p => typeof p.lat === "number" && typeof p.lng === "number")
        .map(p => ({ latitude: Number(p.lat), longitude: Number(p.lng) }));
      if (coords.length >= 2) {
        setRouteCoords(coords);
        setDistanceKm(data?.totalDistanceKm ?? null);
        setDurationMin(data?.estimatedDurationMin ?? null);
        setTimeout(() => {
          mapRef.current?.fitToCoordinates(coords, { edgePadding: { top: 160, right: 60, bottom: 160, left: 60 }, animated: true });
        }, 500);
      }
      return coords;
    } catch { return []; }
  }

  // ── OSRM fallback (avec steps intégrés) ───────────────────────────────

  async function loadFallbackOsrmRoute(start: Point, routeBins: DriverBin[]): Promise<Point[]> {
    if (routeBins.length === 0) {
      setRouteCoords([]); setDistanceKm(null); setDurationMin(null); setNavSteps([]);
      return [];
    }
    try {
      const coordStr = [
        `${start.longitude},${start.latitude}`,
        ...routeBins.map(b => `${b.lng},${b.lat}`),
      ].join(";");
      const url = `${OSRM_URL}/route/v1/driving/${coordStr}?overview=full&geometries=polyline&steps=true`;
      const res = await fetch(url);
      const data = await res.json();
      if (!data.routes || data.routes.length === 0) {
        setRouteCoords([]); setDistanceKm(null); setDurationMin(null); setNavSteps([]);
        return [];
      }
      const route = data.routes[0];
      const decoded: Point[] = polyline.decode(route.geometry).map(([lat, lng]) => ({ latitude: lat, longitude: lng }));
      setRouteCoords(decoded);
      setDistanceKm(route.distance / 1000);
      setDurationMin(route.duration / 60);
      setNavSteps(buildNavSteps(route));
      setTimeout(() => {
        mapRef.current?.fitToCoordinates(decoded, { edgePadding: { top: 160, right: 60, bottom: 160, left: 60 }, animated: true });
      }, 500);
      return decoded;
    } catch {
      setRouteCoords([]); setDistanceKm(null); setDurationMin(null); setNavSteps([]);
      return [];
    }
  }

  // ── Charger les steps OSRM depuis start + bins (toujours correct) ──────

  async function loadNavStepsFromBins(start: Point, routeBins: DriverBin[]) {
    if (routeBins.length === 0) return;
    try {
      const coordStr = [
        `${start.longitude},${start.latitude}`,
        ...routeBins.map(b => `${b.lng},${b.lat}`),
      ].join(";");
      const url = `${OSRM_URL}/route/v1/driving/${coordStr}?overview=false&steps=true`;
      const res = await fetch(url);
      const data = await res.json();
      if (data.routes?.[0]) {
        const steps = buildNavSteps(data.routes[0]);
        setNavSteps(steps);
        console.log(`Guide: ${steps.length} steps chargés`);
      }
    } catch (e) {
      console.log("loadNavStepsFromBins error:", e);
    }
  }

  // ── demo helpers ───────────────────────────────────────────────────────

  function calculateHeading(from: Point, to: Point): number {
    const toRad = (d: number) => d * Math.PI / 180;
    const toDeg = (r: number) => r * 180 / Math.PI;
    const lat1 = toRad(from.latitude), lat2 = toRad(to.latitude);
    const dLng = toRad(to.longitude - from.longitude);
    return (toDeg(Math.atan2(Math.sin(dLng) * Math.cos(lat2), Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng))) + 360) % 360;
  }

  function getDistanceMeters(a: Point, b: Point): number {
    const R = 6371000, toRad = (v: number) => v * Math.PI / 180;
    const dLat = toRad(b.latitude - a.latitude), dLng = toRad(b.longitude - a.longitude);
    const x = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(a.latitude)) * Math.cos(toRad(b.latitude)) * Math.sin(dLng / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
  }

  function isNearBin(point: Point, routeBins: DriverBin[], collectedKeys: Set<string>, threshold = 45): DriverBin | null {
    for (const bin of routeBins) {
      if (typeof bin.lat !== "number" || typeof bin.lng !== "number") continue;
      const key = String(bin.missionBinId ?? bin.binId ?? bin.binCode);
      if (collectedKeys.has(key)) continue;
      if (getDistanceMeters(point, { latitude: bin.lat, longitude: bin.lng }) <= threshold) return bin;
    }
    return null;
  }

  function advanceNavStep() {
    setNavSteps(prev => prev.length > 1 ? prev.slice(1) : prev);
  }

  function startParisDemoTrackingOnOptimizedRoute(path: Point[], routeBins: DriverBin[]) {
    if (demoTimerRef.current) { clearInterval(demoTimerRef.current); demoTimerRef.current = null; }
    if (!path || path.length < 2) { Alert.alert("Route manquante", "Aucune route optimisée trouvée."); return; }

    let i = 0, pauseTicks = 0, lastAdvance = -1;
    const collectedBinKeys = new Set<string>();
    setCurrentLocation(path[0]);
    sendPointToBackend(path[0], 0, 0);

    demoTimerRef.current = setInterval(async () => {
      const current = path[i % path.length];
      const next    = path[(i + 1) % path.length];
      const headingDeg = calculateHeading(current, next);

      if (pauseTicks > 0) {
        pauseTicks--;
        setCurrentLocation(current);
        await sendPointToBackend(current, 0, headingDeg);
        mapRef.current?.animateCamera({ center: current, zoom: 17, heading: headingDeg, pitch: 45 });
        return;
      }

      const nearBin = isNearBin(next, routeBins, collectedBinKeys, 45);
      if (nearBin) {
        const key = String(nearBin.missionBinId ?? nearBin.binId ?? nearBin.binCode);
        collectedBinKeys.add(key);
        pauseTicks = 2;
        setBins(prev => prev.map(b => {
          const bKey = String(b.missionBinId ?? b.binId ?? b.binCode);
          return bKey === key ? { ...b, collected: true } : b;
        }));
        setCurrentLocation(next);
        await sendPointToBackend(next, 0, headingDeg);
        mapRef.current?.animateCamera({ center: next, zoom: 17, heading: headingDeg, pitch: 45 });
        i++; return;
      }

      if (i > 0 && i % 10 === 0 && i !== lastAdvance) {
        lastAdvance = i;
        advanceNavStep();
      }

      const speedKmh = 25 + (i % 4) * 3;
      setCurrentLocation(next);
      await sendPointToBackend(next, speedKmh, headingDeg);
      mapRef.current?.animateCamera({ center: next, zoom: 16, heading: headingDeg, pitch: 45 });
      i++;
    }, 2500);
  }

  // ── init ───────────────────────────────────────────────────────────────

  useEffect(() => {
    let subscription: Location.LocationSubscription | null = null;

    async function initGps() {
      try {
        setLoading(true);
        let start: Point = PARIS_DRIVER_LOCATION;

        if (!DEV_MODE_PARIS) {
          const { status } = await Location.requestForegroundPermissionsAsync();
          if (status !== "granted") { Alert.alert("Permission refusée", "Active la localisation GPS."); setLoading(false); return; }
          const position = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
          start = { latitude: position.coords.latitude, longitude: position.coords.longitude };
          await sendRealLocationToBackend(position);
        } else {
          await sendPointToBackend(start, 0, 90);
        }

        setCurrentLocation(start);
        const routeBins = await loadBins();
        const missionId = getMissionIdFromBins(routeBins);

        let optimizedRoute: Point[] = [];
        if (missionId) optimizedRoute = await loadOptimizedMissionRoute(missionId);
        if (optimizedRoute.length < 2) {
          // fallback: OSRM direct — steps déjà chargés dans loadFallbackOsrmRoute
          optimizedRoute = await loadFallbackOsrmRoute(start, routeBins);
        } else {
          // Route vient du backend → charger les steps depuis OSRM avec start + bins
          await loadNavStepsFromBins(start, routeBins);
        }

        if (DEV_MODE_PARIS) {
          const pathToSimulate = optimizedRoute.length >= 2 ? optimizedRoute : [start];
          setTimeout(() => { mapRef.current?.animateCamera({ center: pathToSimulate[0], zoom: 16, pitch: 45 }); }, 500);
          startParisDemoTrackingOnOptimizedRoute(pathToSimulate, routeBins);
        }

        if (!DEV_MODE_PARIS) {
          subscription = await Location.watchPositionAsync(
            { accuracy: Location.Accuracy.High, timeInterval: 3000, distanceInterval: 5 },
            async (location) => {
              const newPosition: Point = { latitude: location.coords.latitude, longitude: location.coords.longitude };
              setCurrentLocation(newPosition);
              await sendRealLocationToBackend(location);
              mapRef.current?.animateCamera({ center: newPosition, zoom: 17, heading: location.coords.heading || 0, pitch: 55 });
            }
          );
        }
      } catch (error) {
        console.log("Init GPS error:", error);
        Alert.alert("Erreur", "Impossible de charger le GPS.");
      } finally {
        setLoading(false);
      }
    }

    initGps();
    return () => {
      if (subscription) subscription.remove();
      if (demoTimerRef.current) { clearInterval(demoTimerRef.current); demoTimerRef.current = null; }
    };
  }, []);

  // ── render ─────────────────────────────────────────────────────────────

  if (loading || !currentLocation) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#19C37D" />
        <Text style={styles.loadingText}>Chargement GPS...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>

      <NavBanner steps={navSteps} />

      <MapView
        ref={mapRef}
        provider={PROVIDER_GOOGLE}
        style={styles.map}
        showsUserLocation={!DEV_MODE_PARIS}
        showsMyLocationButton={!DEV_MODE_PARIS}
        followsUserLocation={!DEV_MODE_PARIS}
        initialRegion={{
          latitude: currentLocation.latitude,
          longitude: currentLocation.longitude,
          latitudeDelta: 0.025,
          longitudeDelta: 0.025,
        }}
      >
        {DEV_MODE_PARIS && (
          <Marker coordinate={currentLocation} title="Driver" description="Mode simulation" pinColor="blue" />
        )}
        {routeCoords.length > 0 && (
          <Polyline coordinates={routeCoords} strokeColor="#2563EB" strokeWidth={6} />
        )}
        {bins.map((bin, index) => (
          <Marker
            key={bin.missionBinId ?? bin.binId ?? index}
            coordinate={{ latitude: bin.lat!, longitude: bin.lng! }}
            title={bin.binCode || `Bin ${index + 1}`}
            description={bin.collected ? "Collectée" : `Stop ${bin.visitOrder ?? index + 1}`}
            pinColor={bin.collected ? "green" : "red"}
          />
        ))}
      </MapView>

      {bins.length === 0 && (
        <View style={styles.noBinsCard}>
          <Text style={styles.noBinsText}>No bins assigned</Text>
        </View>
      )}

      <View style={styles.bottomCard}>
        <View>
          <Text style={styles.bottomLabel}>Trajet</Text>
          <Text style={styles.bottomValue}>
            {distanceKm !== null ? `${distanceKm.toFixed(1)} km` : "-- km"}
          </Text>
        </View>
        <View>
          <Text style={styles.bottomLabel}>Temps estimé</Text>
          <Text style={styles.bottomValue}>
            {durationMin !== null ? `${Math.round(durationMin)} min` : "-- min"}
          </Text>
        </View>
        <TouchableOpacity style={styles.centerButton} onPress={() => {
          mapRef.current?.animateCamera({ center: currentLocation, zoom: 17, pitch: 55 });
        }}>
          <Ionicons name="navigate" size={20} color="#FFFFFF" />
        </TouchableOpacity>
      </View>

    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  map: { flex: 1 },
  loadingContainer: { flex: 1, backgroundColor: "#0F172A", alignItems: "center", justifyContent: "center" },
  loadingText: { color: "#FFFFFF", marginTop: 12, fontSize: 16, fontWeight: "600" },
  noBinsCard: { position: "absolute", top: 160, left: 16, right: 16, backgroundColor: "rgba(239,68,68,0.92)", borderRadius: 16, padding: 14, alignItems: "center", zIndex: 15 },
  noBinsText: { color: "#FFFFFF", fontWeight: "800" },
  bottomCard: { position: "absolute", left: 16, right: 16, bottom: 30, backgroundColor: "rgba(15,23,42,0.94)", borderRadius: 22, padding: 16, flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  bottomLabel: { color: "#94A3B8", fontSize: 12, marginBottom: 4 },
  bottomValue: { color: "#FFFFFF", fontSize: 18, fontWeight: "800" },
  centerButton: { width: 50, height: 50, borderRadius: 18, backgroundColor: "#19C37D", alignItems: "center", justifyContent: "center" },
});