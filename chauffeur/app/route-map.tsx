import { Ionicons } from "@expo/vector-icons";
import * as polyline from "@mapbox/polyline";
import { BlurView } from "expo-blur";
import * as Location from "expo-location";
import { router, useLocalSearchParams } from "expo-router";
import * as Speech from "expo-speech";
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
import { formatWasteTypeFr } from "../lib/wasteType";
import AsyncStorage from "@react-native-async-storage/async-storage";

const BASE_URL = "http://192.168.1.209:8081";
const OSRM_URL = "http://192.168.1.209:5000";

const DEV_MODE_PARIS = true;
const LAST_ROUTE_INDEX_KEY = "wise_last_route_index";

type DriverBin = {
  missionBinId?: number;
  missionId?: number;
  binId?: number;
  binCode?: string;
  lat?: number;
  lng?: number;
  visitOrder?: number;
  collected?: boolean;
  wasteType?: string;
};

type Point = { latitude: number; longitude: number };

type MissionRoutePoint = {
  lat: number;
  lng: number;
};

type MissionRouteStop = {
  stopType?: string | null;
  binId?: number | null;
  lat?: number | null;
  lng?: number | null;
};

type ManeuverType =
  | "straight"
  | "turn-left"
  | "turn-right"
  | "slight-left"
  | "slight-right"
  | "arrive"
  | "depart";

type NavStep = {
  maneuver: ManeuverType;
  streetName: string;
  distanceM: number;
  location: Point;
};

const PARIS_DRIVER_LOCATION: Point = {
  latitude: 48.8411,
  longitude: 2.3003,
};

async function fetchWithTimeout(
  url: string,
  options: RequestInit = {},
  timeoutMs = 7000
) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timeout);
  }
}

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

      if (!m?.location || m.location.length < 2) continue;
      if (distanceM === 0 && maneuver !== "arrive") continue;

      steps.push({
        maneuver,
        streetName: step.name ?? "",
        distanceM,
        location: {
          latitude: m.location[1],
          longitude: m.location[0],
        },
      });
    }
  }

  return steps;
}

function Arrow({
  maneuver,
  size = 36,
  color = "#FFFFFF",
  opacity = 1,
}: {
  maneuver: ManeuverType;
  size?: number;
  color?: string;
  opacity?: number;
}) {
  const iconMap: Record<ManeuverType, any> = {
    straight: "arrow-up",
    depart: "arrow-up",
    "turn-left": "arrow-back",
    "turn-right": "arrow-forward",
    "slight-left": "arrow-up",
    "slight-right": "arrow-up",
    arrive: "flag",
  };

  const rotate =
    maneuver === "slight-left"
      ? "-35deg"
      : maneuver === "slight-right"
      ? "35deg"
      : "0deg";

  return (
    <View style={{ opacity, transform: [{ rotate }] }}>
      <Ionicons name={iconMap[maneuver]} size={size} color={color} />
    </View>
  );
}

function NavBanner({ steps }: { steps: NavStep[] }) {
  const hasSteps = steps.length > 0;
  const current = steps[0];
  const next1 = steps[1];
  const next2 = steps[2];

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
                <Text style={nb.street} numberOfLines={1}>
                  {current.streetName}
                </Text>
              )}
            </View>

            <View style={nb.nextSteps}>
              {next1 && (
                <View style={nb.nextItem}>
                  <Arrow
                    maneuver={next1.maneuver}
                    size={18}
                    color="rgba(255,255,255,0.55)"
                  />
                </View>
              )}
              {next2 && (
                <View style={nb.nextItem}>
                  <Arrow
                    maneuver={next2.maneuver}
                    size={14}
                    color="rgba(255,255,255,0.3)"
                  />
                </View>
              )}
            </View>
          </View>
        ) : (
          <View style={nb.content}>
            <Text style={nb.noRoute}>Calcul de l’itinéraire...</Text>
          </View>
        )}
      </BlurView>
    </View>
  );
}

const nb = StyleSheet.create({
  wrapper: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    zIndex: 20,
    borderBottomLeftRadius: 24,
    borderBottomRightRadius: 24,
    overflow: "hidden",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.35,
    shadowRadius: 16,
    elevation: 16,
  },
  blur: {
    paddingTop: 52,
    paddingBottom: 18,
    paddingHorizontal: 16,
    flexDirection: "row",
    alignItems: "center",
    gap: 14,
    backgroundColor: "rgba(15,23,42,0.82)",
  },
  backBtn: {
    width: 38,
    height: 38,
    borderRadius: 12,
    backgroundColor: "rgba(255,255,255,0.12)",
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.15)",
  },
  content: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
  },
  mainArrowCircle: {
    width: 52,
    height: 52,
    borderRadius: 16,
    backgroundColor: "#19C37D",
    alignItems: "center",
    justifyContent: "center",
  },
  textBlock: { flex: 1 },
  distance: { color: "#FFFFFF", fontSize: 26, fontWeight: "800" },
  street: {
    color: "rgba(255,255,255,0.65)",
    fontSize: 13,
    fontWeight: "600",
    marginTop: 2,
  },
  nextSteps: { flexDirection: "column", alignItems: "center", gap: 4 },
  nextItem: {
    width: 28,
    height: 28,
    borderRadius: 8,
    backgroundColor: "rgba(255,255,255,0.08)",
    alignItems: "center",
    justifyContent: "center",
  },
  noRoute: {
    color: "rgba(255,255,255,0.45)",
    fontSize: 14,
    fontStyle: "italic",
  },
});

export default function RouteMap() {
  const params = useLocalSearchParams<{
    actionDone?: string;
    collectedBinId?: string;
    reportedBinId?: string;
  }>();

  const mapRef = useRef<MapView | null>(null);
  const demoTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const lastSpokenRef = useRef<string>("");
  const isPausedRef = useRef(false);
  const routeIndexRef = useRef(0);

  const [loading, setLoading] = useState(true);
  const [currentLocation, setCurrentLocation] = useState<Point | null>(null);
  const [bins, setBins] = useState<DriverBin[]>([]);
  const [routeCoords, setRouteCoords] = useState<Point[]>([]);
  const [distanceKm, setDistanceKm] = useState<number | null>(null);
  const [durationMin, setDurationMin] = useState<number | null>(null);
  const [navSteps, setNavSteps] = useState<NavStep[]>([]);
  const [targetBin, setTargetBin] = useState<DriverBin | null>(null);
  const [isRoutePaused, setIsRoutePaused] = useState(false);
  const [awaitingContinue, setAwaitingContinue] = useState(false);

  useEffect(() => {
    isPausedRef.current = isRoutePaused || awaitingContinue;
  }, [isRoutePaused, awaitingContinue]);

  function speakText(text: string) {
    Speech.stop();
    Speech.speak(text, {
      language: "fr-FR",
      rate: 0.9,
      pitch: 1,
      volume: 1,
    });
  }

  function speakInstruction(step: NavStep) {
    const key = `${step.maneuver}-${step.streetName}-${step.distanceM}`;
    if (lastSpokenRef.current === key) return;

    lastSpokenRef.current = key;

    let text = "";
    if (step.maneuver === "turn-right") text = "Tournez à droite";
    else if (step.maneuver === "turn-left") text = "Tournez à gauche";
    else if (step.maneuver === "slight-right") text = "Tournez légèrement à droite";
    else if (step.maneuver === "slight-left") text = "Tournez légèrement à gauche";
    else if (step.maneuver === "arrive") text = "Vous êtes arrivé";
    else if (step.maneuver === "depart") text = "Démarrez";
    else text = "Continuez tout droit";

    if (step.streetName) text += ` sur ${step.streetName}`;
    speakText(text);
  }

  async function sendPointToBackend(point: Point, speedKmh = 30, headingDeg = 0) {
    try {
      const token = await getToken();
      const userId = await getUserId();
      if (!token || !userId) return;

      const res = await fetchWithTimeout(
        `${BASE_URL}/api/truck-locations`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            driverId: Number(userId),
            lat: point.latitude,
            lng: point.longitude,
            speedKmh,
            headingDeg,
            timestamp: new Date().toISOString(),
          }),
        },
        7000
      );

      if (!res.ok) console.log("TRUCK LOCATION ERROR:", await res.text());
    } catch (e) {
      console.log("sendPointToBackend error:", e);
    }
  }

  async function sendRealLocationToBackend(location: Location.LocationObject) {
    sendPointToBackend(
      {
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
      },
      location.coords.speed != null ? location.coords.speed * 3.6 : 0,
      location.coords.heading ?? 0
    );
  }

  async function loadBins(): Promise<DriverBin[]> {
    try {
      const token = await getToken();
      const userId = await getUserId();

      if (!token || !userId) {
        setBins([]);
        return [];
      }

      const res = await fetchWithTimeout(
        `${BASE_URL}/api/drivers/${userId}/my-bins`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        },
        7000
      );

      const text = await res.text();

      if (!res.ok) {
        console.log("LOAD BINS ERROR:", text);
        setBins([]);
        return [];
      }

      const data = text ? JSON.parse(text) : [];

      let validBins = Array.isArray(data)
        ? data.filter(
            (b) =>
              !b.collected &&
              typeof b.lat === "number" &&
              typeof b.lng === "number"
          )
        : [];

      const doneId = params.collectedBinId || params.reportedBinId;
      if (doneId) {
        validBins = validBins.filter(
          (b) =>
            String(b.missionBinId ?? b.binId ?? b.binCode) !== String(doneId)
        );
      }

      validBins.sort((a, b) => (a.visitOrder || 999) - (b.visitOrder || 999));

      setBins(validBins);
      return validBins;
    } catch (e) {
      console.log("loadBins error:", e);
      setBins([]);
      return [];
    }
  }

  function getMissionIdFromBins(routeBins: DriverBin[]): number | null {
    if (!Array.isArray(routeBins) || routeBins.length === 0) return null;
    return routeBins[0]?.missionId ?? null;
  }

  async function loadOptimizedMissionRoute(
    missionId: number,
    routeBins: DriverBin[]
  ): Promise<{ coords: Point[]; syncedBins: DriverBin[] }> {
    try {
      const token = await getToken();
      if (!token) return { coords: [], syncedBins: routeBins };

      const res = await fetchWithTimeout(
        `${BASE_URL}/api/missions/${missionId}/route`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        },
        7000
      );

      const text = await res.text();

      if (!res.ok) {
        console.log("LOAD MISSION ROUTE ERROR:", text);
        return { coords: [], syncedBins: routeBins };
      }

      const data = text ? JSON.parse(text) : null;

      const rawCoords: MissionRoutePoint[] =
        data?.collectionRouteCoordinates?.length >= 2
          ? data.collectionRouteCoordinates
          : data?.routeCoordinates?.length >= 2
          ? data.routeCoordinates
          : [];

      const coords: Point[] = rawCoords
        .filter((p) => typeof p.lat === "number" && typeof p.lng === "number")
        .map((p) => ({
          latitude: Number(p.lat),
          longitude: Number(p.lng),
        }));

      const routeStops: MissionRouteStop[] = Array.isArray(data?.routeStops)
        ? data.routeStops
        : [];

      const pickupStopByBinId = new Map<number, MissionRouteStop>();

      routeStops.forEach((stop) => {
        if (
          stop?.stopType === "BIN_PICKUP" &&
          typeof stop.binId === "number" &&
          typeof stop.lat === "number" &&
          typeof stop.lng === "number"
        ) {
          pickupStopByBinId.set(stop.binId, stop);
        }
      });

      const syncedBins = routeBins.map((bin) => {
        if (typeof bin.binId !== "number") return bin;

        const stop = pickupStopByBinId.get(bin.binId);

        if (!stop || typeof stop.lat !== "number" || typeof stop.lng !== "number") {
          return bin;
        }

        return {
          ...bin,
          lat: Number(stop.lat),
          lng: Number(stop.lng),
        };
      });

      setBins(syncedBins);

      if (coords.length >= 2) {
        setRouteCoords(coords);
        setDistanceKm(data?.totalDistanceKm ?? null);
        setDurationMin(data?.estimatedDurationMin ?? null);

        setTimeout(() => {
          mapRef.current?.fitToCoordinates(coords, {
            edgePadding: { top: 160, right: 60, bottom: 230, left: 60 },
            animated: true,
          });
        }, 500);
      }

      return { coords, syncedBins };
    } catch (e) {
      console.log("loadOptimizedMissionRoute error:", e);
      return { coords: [], syncedBins: routeBins };
    }
  }

  async function loadFallbackOsrmRoute(
    start: Point,
    routeBins: DriverBin[]
  ): Promise<Point[]> {
    if (routeBins.length === 0) {
      setRouteCoords([]);
      setDistanceKm(null);
      setDurationMin(null);
      setNavSteps([]);
      return [];
    }

    try {
      const coordStr = [
        `${start.longitude},${start.latitude}`,
        ...routeBins.map((b) => `${b.lng},${b.lat}`),
      ].join(";");

      const url = `${OSRM_URL}/route/v1/driving/${coordStr}?overview=full&geometries=polyline&steps=true`;
      const res = await fetchWithTimeout(url, {}, 7000);
      const data = await res.json();

      if (!data.routes || data.routes.length === 0) {
        setRouteCoords([]);
        setDistanceKm(null);
        setDurationMin(null);
        setNavSteps([]);
        return [];
      }

      const route = data.routes[0];

      const decoded: Point[] = polyline
        .decode(route.geometry)
        .map(([lat, lng]) => ({ latitude: lat, longitude: lng }));

      const steps = buildNavSteps(route);

      setRouteCoords(decoded);
      setDistanceKm(route.distance / 1000);
      setDurationMin(route.duration / 60);
      setNavSteps(steps);

      if (steps.length > 0 && !params.actionDone) speakInstruction(steps[0]);

      setTimeout(() => {
        mapRef.current?.fitToCoordinates(decoded, {
          edgePadding: { top: 160, right: 60, bottom: 230, left: 60 },
          animated: true,
        });
      }, 500);

      return decoded;
    } catch (e) {
      console.log("loadFallbackOsrmRoute error:", e);
      setRouteCoords([]);
      setDistanceKm(null);
      setDurationMin(null);
      setNavSteps([]);
      return [];
    }
  }

  async function loadNavStepsFromBins(start: Point, routeBins: DriverBin[]) {
    if (routeBins.length === 0) return;

    try {
      const coordStr = [
        `${start.longitude},${start.latitude}`,
        ...routeBins.map((b) => `${b.lng},${b.lat}`),
      ].join(";");

      const url = `${OSRM_URL}/route/v1/driving/${coordStr}?overview=false&steps=true`;
      const res = await fetchWithTimeout(url, {}, 7000);
      const data = await res.json();

      if (data.routes?.[0]) {
        const steps = buildNavSteps(data.routes[0]);
        setNavSteps(steps);
        if (steps.length > 0 && !params.actionDone) speakInstruction(steps[0]);
      }
    } catch (e) {
      console.log("loadNavStepsFromBins error:", e);
    }
  }

  function calculateHeading(from: Point, to: Point): number {
    const toRad = (d: number) => (d * Math.PI) / 180;
    const toDeg = (r: number) => (r * 180) / Math.PI;

    const lat1 = toRad(from.latitude);
    const lat2 = toRad(to.latitude);
    const dLng = toRad(to.longitude - from.longitude);

    return (
      (toDeg(
        Math.atan2(
          Math.sin(dLng) * Math.cos(lat2),
          Math.cos(lat1) * Math.sin(lat2) -
            Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
        )
      ) +
        360) %
      360
    );
  }

  function getDistanceMeters(a: Point, b: Point): number {
    const R = 6371000;
    const toRad = (v: number) => (v * Math.PI) / 180;

    const dLat = toRad(b.latitude - a.latitude);
    const dLng = toRad(b.longitude - a.longitude);

    const x =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(toRad(a.latitude)) *
        Math.cos(toRad(b.latitude)) *
        Math.sin(dLng / 2) ** 2;

    return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
  }

  function updateCurrentInstruction(position: Point) {
    setNavSteps((prev) => {
      if (prev.length <= 1) return prev;

      const nextStep = prev[1];
      const distanceToNextStep = getDistanceMeters(position, nextStep.location);

      if (distanceToNextStep <= 25) {
        speakInstruction(nextStep);
        return prev.slice(1);
      }

      return prev;
    });
  }

  function findNearBin(point: Point, routeBins: DriverBin[], threshold = 45) {
    for (const bin of routeBins) {
      if (bin.collected) continue;
      if (typeof bin.lat !== "number" || typeof bin.lng !== "number") continue;

      const d = getDistanceMeters(point, {
        latitude: bin.lat,
        longitude: bin.lng,
      });

      if (d <= threshold) return bin;
    }

    return null;
  }

  function stopRouteAtBin(bin: DriverBin, point: Point, headingDeg: number) {
    setTargetBin(bin);
    setIsRoutePaused(true);
    isPausedRef.current = true;

    if (demoTimerRef.current) {
      clearInterval(demoTimerRef.current);
      demoTimerRef.current = null;
    }

    setCurrentLocation(point);

    mapRef.current?.animateCamera({
      center: point,
      zoom: 18,
      heading: headingDeg,
      pitch: 45,
    });

    speakText(`Poubelle atteinte ${bin.binCode ?? ""}. Choisissez une action.`);
  }

  function openScanner() {
    if (!targetBin) return;

    router.push({
      pathname: "/scan",
      params: {
        missionBinId: String(targetBin.missionBinId ?? ""),
        expectedBinCode: targetBin.binCode ?? "",
      },
    });
  }

  function declareProblem() {
    if (!targetBin) return;

    router.push({
      pathname: "/declare-breakdown",
      params: {
        context: "bin",
        missionBinId: String(targetBin.missionBinId ?? ""),
        binCode: targetBin.binCode ?? "",
        resumeIndex: String(routeIndexRef.current ?? 0),
      },
    });
  }

  function continueRoute() {
    setAwaitingContinue(false);
    setIsRoutePaused(false);
    setTargetBin(null);
    isPausedRef.current = false;

    speakText("Reprise de l'itinéraire.");

    if (DEV_MODE_PARIS && routeCoords.length >= 2) {
      startParisDemoTrackingOnOptimizedRoute(routeCoords, bins);
    }
  }

  function trimRouteBehind(position: Point) {
    setRouteCoords((prev) => {
      if (prev.length < 2) return prev;

      let nearestIndex = 0;
      let nearestDistance = Number.MAX_VALUE;

      prev.forEach((p, index) => {
        const d = getDistanceMeters(position, p);

        if (d < nearestDistance) {
          nearestDistance = d;
          nearestIndex = index;
        }
      });

      return prev.slice(Math.max(0, nearestIndex));
    });
  }

  function startParisDemoTrackingOnOptimizedRoute(
    path: Point[],
    routeBins: DriverBin[]
  ) {
    if (demoTimerRef.current) {
      clearInterval(demoTimerRef.current);
      demoTimerRef.current = null;
    }

    if (!path || path.length < 2) return;

    let i = routeIndexRef.current || 0;

    const startIndex = currentLocation
      ? Math.max(
          0,
          path.reduce((best, p, idx) => {
            const bestDist = getDistanceMeters(currentLocation, path[best]);
            const d = getDistanceMeters(currentLocation, p);
            return d < bestDist ? idx : best;
          }, 0)
        )
      : 0;

    if (!routeIndexRef.current) {
      i = startIndex;
    }

    routeIndexRef.current = i;

    demoTimerRef.current = setInterval(() => {
      if (isPausedRef.current) return;

      const current = path[i % path.length];
      const next = path[(i + 1) % path.length];
      const headingDeg = calculateHeading(current, next);

      const near = findNearBin(next, routeBins, 45);
      if (near) {
        stopRouteAtBin(near, next, headingDeg);
        return;
      }

      setCurrentLocation(next);
      trimRouteBehind(next);
      updateCurrentInstruction(next);
      sendPointToBackend(next, 25, headingDeg);

      mapRef.current?.animateCamera({
        center: next,
        zoom: 16,
        heading: headingDeg,
        pitch: 45,
      });

      i++;
      routeIndexRef.current = i;
      AsyncStorage.setItem(LAST_ROUTE_INDEX_KEY, String(i));
    }, 2500);
  }

  useEffect(() => {
    let subscription: Location.LocationSubscription | null = null;

    async function initGps() {
      try {
        setLoading(true);

        let start: Point = PARIS_DRIVER_LOCATION;

        if (!DEV_MODE_PARIS) {
          const { status } = await Location.requestForegroundPermissionsAsync();

          if (status !== "granted") {
            Alert.alert("Permission refusée", "Activez la localisation GPS.");
            setLoading(false);
            return;
          }

          const position = await Location.getCurrentPositionAsync({
            accuracy: Location.Accuracy.High,
          });

          start = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          };

          sendRealLocationToBackend(position);
        } else {
          sendPointToBackend(start, 0, 90);
        }

        setCurrentLocation(start);

        const routeBins = await loadBins();
        const missionId = getMissionIdFromBins(routeBins);

        let optimizedRoute: Point[] = [];
        let syncedRouteBins = routeBins;

        if (missionId) {
          const result = await loadOptimizedMissionRoute(missionId, routeBins);
          optimizedRoute = result.coords;
          syncedRouteBins = result.syncedBins;
        }

        if (optimizedRoute.length < 2) {
          optimizedRoute = await loadFallbackOsrmRoute(start, syncedRouteBins);
        } else {
          await loadNavStepsFromBins(start, syncedRouteBins);
        }

        if (params.actionDone) {
          setAwaitingContinue(true);
          setIsRoutePaused(true);
          isPausedRef.current = true;
          speakText("Action terminée. Appuyez sur continuer route.");
          return;
        }

        if (DEV_MODE_PARIS && optimizedRoute.length >= 2) {
          const savedIndex = await AsyncStorage.getItem(LAST_ROUTE_INDEX_KEY);

          if (savedIndex) {
            const parsedIndex = Number(savedIndex);
            if (!Number.isNaN(parsedIndex)) {
              routeIndexRef.current = Math.min(
                Math.max(parsedIndex, 0),
                optimizedRoute.length - 1
              );
            }
          }

          setTimeout(() => {
            mapRef.current?.animateCamera({
              center: optimizedRoute[routeIndexRef.current] ?? optimizedRoute[0],
              zoom: 16,
              pitch: 45,
            });
          }, 500);

          startParisDemoTrackingOnOptimizedRoute(
            optimizedRoute,
            syncedRouteBins
          );
        }

        if (!DEV_MODE_PARIS) {
          subscription = await Location.watchPositionAsync(
            {
              accuracy: Location.Accuracy.High,
              timeInterval: 3000,
              distanceInterval: 5,
            },
            async (location) => {
              if (isPausedRef.current) return;

              const newPosition: Point = {
                latitude: location.coords.latitude,
                longitude: location.coords.longitude,
              };

              const near = findNearBin(newPosition, syncedRouteBins, 45);
              if (near) {
                stopRouteAtBin(
                  near,
                  newPosition,
                  location.coords.heading || 0
                );
                return;
              }

              setCurrentLocation(newPosition);
              trimRouteBehind(newPosition);
              updateCurrentInstruction(newPosition);
              sendRealLocationToBackend(location);

              mapRef.current?.animateCamera({
                center: newPosition,
                zoom: 17,
                heading: location.coords.heading || 0,
                pitch: 55,
              });
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
      Speech.stop();

      if (subscription) subscription.remove();

      if (demoTimerRef.current) {
        clearInterval(demoTimerRef.current);
        demoTimerRef.current = null;
      }
    };
  }, []);

  if (loading || !currentLocation) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#19C37D" />
        <Text style={styles.loadingText}>Chargement GPS...</Text>
      </View>
    );
  }

  const visibleBins = bins.filter((b) => !b.collected);

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
          <Marker
            coordinate={currentLocation}
            title="Chauffeur"
            description="Mode simulation"
            pinColor="blue"
          />
        )}

        {routeCoords.length > 0 && (
          <Polyline
            coordinates={routeCoords}
            strokeColor="#0000FF"
            strokeWidth={6}
          />
        )}

        {bins.map((bin, index) => (
          <Marker
            key={bin.missionBinId ?? bin.binId ?? index}
            coordinate={{
              latitude: Number(bin.lat),
              longitude: Number(bin.lng),
            }}
            title={bin.binCode || `Bac ${index + 1}`}
            description={
              bin.collected ? "Collectée" : `Arrêt ${bin.visitOrder ?? index + 1}`
            }
            pinColor={bin.collected ? "green" : "red"}
          />
        ))}
      </MapView>

      {visibleBins.length === 0 && (
        <View style={styles.noBinsCard}>
          <Text style={styles.noBinsText}>Tous les bins sont collectés</Text>
        </View>
      )}

      {targetBin && !awaitingContinue && (
        <View style={styles.stopCard}>
          <Text style={styles.stopBadge}>STOP</Text>
          <Text style={styles.stopTitle}>Poubelle atteinte</Text>
          <Text style={styles.stopCode}>{targetBin.binCode ?? "Bac"}</Text>
          <Text style={styles.stopSub}>Déchet: {formatWasteTypeFr(targetBin.wasteType)}</Text>

          <TouchableOpacity style={styles.collectBtn} onPress={openScanner}>
            <Ionicons name="scan-outline" size={20} color="#FFFFFF" />
            <Text style={styles.collectBtnText}>Collecter / Scanner QR</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.problemBtn} onPress={declareProblem}>
            <Ionicons name="warning-outline" size={19} color="#EF4444" />
            <Text style={styles.problemBtnText}>Signaler un problème</Text>
          </TouchableOpacity>
        </View>
      )}

      {awaitingContinue && (
        <View style={styles.continueCard}>
          <Ionicons name="checkmark-circle" size={42} color="#19C37D" />
          <Text style={styles.continueTitle}>Action terminée</Text>
          <Text style={styles.continueSub}>
            La mission a été mise à jour. Vous pouvez continuer la route.
          </Text>

          <TouchableOpacity style={styles.continueBtn} onPress={continueRoute}>
            <Ionicons name="play" size={18} color="#FFFFFF" />
            <Text style={styles.continueBtnText}>Continuer route</Text>
          </TouchableOpacity>
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

        <TouchableOpacity
          style={styles.centerButton}
          onPress={() => {
            mapRef.current?.animateCamera({
              center: currentLocation,
              zoom: 17,
              pitch: 55,
            });
          }}
        >
          <Ionicons name="navigate" size={20} color="#FFFFFF" />
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  map: { flex: 1 },

  loadingContainer: {
    flex: 1,
    backgroundColor: "#0F172A",
    alignItems: "center",
    justifyContent: "center",
  },
  loadingText: {
    color: "#FFFFFF",
    marginTop: 12,
    fontSize: 16,
    fontWeight: "600",
  },

  noBinsCard: {
    position: "absolute",
    top: 160,
    left: 16,
    right: 16,
    backgroundColor: "rgba(34,197,94,0.92)",
    borderRadius: 16,
    padding: 14,
    alignItems: "center",
    zIndex: 15,
  },
  noBinsText: { color: "#FFFFFF", fontWeight: "800" },

  stopCard: {
    position: "absolute",
    left: 24,
    right: 24,
    bottom: 178,
    backgroundColor: "rgba(255,255,255,0.98)",
    borderRadius: 26,
    paddingHorizontal: 20,
    paddingVertical: 18,
    zIndex: 50,
    alignItems: "center",
    shadowColor: "#0F172A",
    shadowOpacity: 0.16,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 10 },
    elevation: 12,
  },

  stopBadge: {
    backgroundColor: "#FFE4E6",
    color: "#DC2626",
    fontWeight: "900",
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 999,
    marginBottom: 8,
    fontSize: 10,
    letterSpacing: 1,
    textAlign: "center",
    overflow: "hidden",
  },

  stopTitle: {
    color: "#0F172A",
    fontSize: 18,
    fontWeight: "900",
    textAlign: "center",
    marginBottom: 3,
  },

  stopCode: {
    color: "#0F172A",
    fontSize: 24,
    fontWeight: "900",
    textAlign: "center",
    letterSpacing: -0.2,
  },

  stopSub: {
    color: "#64748B",
    fontSize: 13,
    fontWeight: "800",
    textAlign: "center",
    marginTop: 4,
    marginBottom: 16,
  },

  collectBtn: {
    width: "100%",
    height: 52,
    borderRadius: 16,
    backgroundColor: "#19C37D",
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 8,
  },

  collectBtnText: {
    color: "#FFFFFF",
    fontWeight: "900",
    fontSize: 15,
    textAlign: "center",
  },

  problemBtn: {
    width: "100%",
    height: 48,
    marginTop: 10,
    borderRadius: 16,
    backgroundColor: "#FFF1F2",
    borderWidth: 1,
    borderColor: "#FECACA",
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 7,
  },

  problemBtnText: {
    color: "#EF4444",
    fontWeight: "900",
    fontSize: 14,
    textAlign: "center",
  },

  continueCard: {
    position: "absolute",
    left: 16,
    right: 16,
    bottom: 145,
    backgroundColor: "#FFFFFF",
    borderRadius: 24,
    padding: 20,
    alignItems: "center",
    zIndex: 50,
    shadowColor: "#000",
    shadowOpacity: 0.22,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 8 },
    elevation: 16,
  },
  continueTitle: {
    color: "#0F172A",
    fontSize: 22,
    fontWeight: "900",
    marginTop: 8,
  },
  continueSub: {
    color: "#64748B",
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
    marginTop: 6,
    marginBottom: 16,
  },
  continueBtn: {
    width: "100%",
    height: 52,
    borderRadius: 16,
    backgroundColor: "#19C37D",
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 8,
  },
  continueBtnText: {
    color: "#FFFFFF",
    fontWeight: "900",
    fontSize: 16,
  },

  bottomCard: {
    position: "absolute",
    left: 16,
    right: 16,
    bottom: 30,
    backgroundColor: "rgba(15,23,42,0.94)",
    borderRadius: 22,
    padding: 16,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  bottomLabel: { color: "#94A3B8", fontSize: 12, marginBottom: 4 },
  bottomValue: { color: "#FFFFFF", fontSize: 18, fontWeight: "800" },
  centerButton: {
    width: 50,
    height: 50,
    borderRadius: 18,
    backgroundColor: "#19C37D",
    alignItems: "center",
    justifyContent: "center",
  },
});
