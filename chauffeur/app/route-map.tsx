import { Ionicons } from "@expo/vector-icons";
import * as polyline from "@mapbox/polyline";
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

const BASE_URL = "http://192.168.1.115:8081";

/**
 * true  => soutenance demo: camion يتحرك على route optimisée في Paris
 * false => GPS réel
 */
const DEV_MODE_PARIS = true;

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

type Point = {
  latitude: number;
  longitude: number;
};

type MissionRoutePoint = {
  lat: number;
  lng: number;
};

const PARIS_DRIVER_LOCATION: Point = {
  latitude: 48.8411,
  longitude: 2.3003,
};

export default function RouteMap() {
  const mapRef = useRef<MapView | null>(null);
  const demoTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const [loading, setLoading] = useState(true);
  const [currentLocation, setCurrentLocation] = useState<Point | null>(null);
  const [bins, setBins] = useState<DriverBin[]>([]);
  const [routeCoords, setRouteCoords] = useState<Point[]>([]);
  const [distanceKm, setDistanceKm] = useState<number | null>(null);
  const [durationMin, setDurationMin] = useState<number | null>(null);

  async function sendPointToBackend(point: Point, speedKmh = 30, headingDeg = 0) {
    try {
      const token = await getToken();
      const userId = await getUserId();

      if (!token || !userId) {
        console.log("No token/userId, location not sent");
        return;
      }

      const response = await fetch(`${BASE_URL}/api/truck-locations`, {
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
      });

      if (!response.ok) {
        console.log("TRUCK LOCATION ERROR:", await response.text());
      }
    } catch (error) {
      console.log("Send truck location error:", error);
    }
  }

  async function sendRealLocationToBackend(location: Location.LocationObject) {
    const point: Point = {
      latitude: location.coords.latitude,
      longitude: location.coords.longitude,
    };

    await sendPointToBackend(
      point,
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

      const response = await fetch(`${BASE_URL}/api/drivers/${userId}/my-bins`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      const text = await response.text();

      if (!response.ok) {
        console.log("Load bins error:", response.status, text);
        setBins([]);
        return [];
      }

      const data = text ? JSON.parse(text) : [];

      const validBins = Array.isArray(data)
        ? data.filter(
            (bin) =>
              !bin.collected &&
              typeof bin.lat === "number" &&
              typeof bin.lng === "number"
          )
        : [];

      validBins.sort((a, b) => (a.visitOrder || 999) - (b.visitOrder || 999));

      setBins(validBins);
      return validBins;
    } catch (error) {
      console.log("Load bins exception:", error);
      setBins([]);
      return [];
    }
  }

  function getMissionIdFromBins(routeBins: DriverBin[]): number | null {
    if (!Array.isArray(routeBins) || routeBins.length === 0) return null;
    return routeBins[0]?.missionId ?? null;
  }

  async function loadOptimizedMissionRoute(missionId: number): Promise<Point[]> {
    try {
      const token = await getToken();
      if (!token) return [];

      const response = await fetch(`${BASE_URL}/api/missions/${missionId}/route`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      const text = await response.text();

      if (!response.ok) {
        console.log("Mission route error:", response.status, text);
        return [];
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

      if (coords.length >= 2) {
        setRouteCoords(coords);
        setDistanceKm(data?.totalDistanceKm ?? null);
        setDurationMin(data?.estimatedDurationMin ?? null);

        setTimeout(() => {
          mapRef.current?.fitToCoordinates(coords, {
            edgePadding: {
              top: 90,
              right: 60,
              bottom: 160,
              left: 60,
            },
            animated: true,
          });
        }, 500);
      }

      return coords;
    } catch (error) {
      console.log("Load optimized route exception:", error);
      return [];
    }
  }

  async function loadFallbackOsrmRoute(start: Point, routeBins: DriverBin[]) {
    if (routeBins.length === 0) {
      setRouteCoords([]);
      setDistanceKm(null);
      setDurationMin(null);
      return [];
    }

    try {
      const coords = [
        `${start.longitude},${start.latitude}`,
        ...routeBins.map((bin) => `${bin.lng},${bin.lat}`),
      ].join(";");

      const url =
        `https://router.project-osrm.org/route/v1/driving/${coords}` +
        `?overview=full&geometries=polyline&steps=false`;

      const response = await fetch(url);
      const data = await response.json();

      if (!data.routes || data.routes.length === 0) {
        console.log("No route found from OSRM:", data);
        setRouteCoords([]);
        setDistanceKm(null);
        setDurationMin(null);
        return [];
      }

      const route = data.routes[0];

      const decoded: Point[] = polyline.decode(route.geometry).map(([lat, lng]) => ({
        latitude: lat,
        longitude: lng,
      }));

      setRouteCoords(decoded);
      setDistanceKm(route.distance / 1000);
      setDurationMin(route.duration / 60);

      setTimeout(() => {
        mapRef.current?.fitToCoordinates(decoded, {
          edgePadding: {
            top: 90,
            right: 60,
            bottom: 160,
            left: 60,
          },
          animated: true,
        });
      }, 500);

      return decoded;
    } catch (error) {
      console.log("Route error:", error);
      setRouteCoords([]);
      setDistanceKm(null);
      setDurationMin(null);
      return [];
    }
  }

  function calculateHeading(from: Point, to: Point): number {
    const toRad = (deg: number) => (deg * Math.PI) / 180;
    const toDeg = (rad: number) => (rad * 180) / Math.PI;

    const lat1 = toRad(from.latitude);
    const lat2 = toRad(to.latitude);
    const dLng = toRad(to.longitude - from.longitude);

    const y = Math.sin(dLng) * Math.cos(lat2);
    const x =
      Math.cos(lat1) * Math.sin(lat2) -
      Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);

    return (toDeg(Math.atan2(y, x)) + 360) % 360;
  }

  function getDistanceMeters(a: Point, b: Point): number {
    const R = 6371000;
    const toRad = (value: number) => (value * Math.PI) / 180;

    const dLat = toRad(b.latitude - a.latitude);
    const dLng = toRad(b.longitude - a.longitude);

    const lat1 = toRad(a.latitude);
    const lat2 = toRad(b.latitude);

    const x =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1) *
        Math.cos(lat2) *
        Math.sin(dLng / 2) *
        Math.sin(dLng / 2);

    const c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    return R * c;
  }

  function isNearBin(
    point: Point,
    routeBins: DriverBin[],
    collectedBinKeys: Set<string>,
    thresholdMeters = 45
  ): DriverBin | null {
    for (const bin of routeBins) {
      if (typeof bin.lat !== "number" || typeof bin.lng !== "number") continue;

      const key = String(bin.missionBinId ?? bin.binId ?? bin.binCode);
      if (collectedBinKeys.has(key)) continue;

      const distance = getDistanceMeters(point, {
        latitude: bin.lat,
        longitude: bin.lng,
      });

      if (distance <= thresholdMeters) {
        return bin;
      }
    }

    return null;
  }

  function startParisDemoTrackingOnOptimizedRoute(path: Point[], routeBins: DriverBin[]) {
    if (demoTimerRef.current) {
      clearInterval(demoTimerRef.current);
      demoTimerRef.current = null;
    }

    if (!path || path.length < 2) {
      Alert.alert("Route manquante", "Aucune route optimisée trouvée pour la mission.");
      return;
    }

    let i = 0;
    let pauseTicks = 0;
    const collectedBinKeys = new Set<string>();

    setCurrentLocation(path[0]);
    sendPointToBackend(path[0], 0, 0);

    demoTimerRef.current = setInterval(async () => {
      const current = path[i % path.length];
      const next = path[(i + 1) % path.length];
      const headingDeg = calculateHeading(current, next);

      if (pauseTicks > 0) {
        pauseTicks--;

        setCurrentLocation(current);

        await sendPointToBackend(current, 0, headingDeg);

        mapRef.current?.animateCamera({
          center: current,
          zoom: 17,
          heading: headingDeg,
          pitch: 45,
        });

        return;
      }

      const nearBin = isNearBin(next, routeBins, collectedBinKeys, 45);

      if (nearBin) {
        const key = String(nearBin.missionBinId ?? nearBin.binId ?? nearBin.binCode);
        collectedBinKeys.add(key);

        pauseTicks = 2;

        console.log("Collecting bin:", nearBin.binCode || nearBin.binId);

        setBins((prev) =>
          prev.map((b) => {
            const bKey = String(b.missionBinId ?? b.binId ?? b.binCode);
            return bKey === key ? { ...b, collected: true } : b;
          })
        );

        setCurrentLocation(next);

        await sendPointToBackend(next, 0, headingDeg);

        mapRef.current?.animateCamera({
          center: next,
          zoom: 17,
          heading: headingDeg,
          pitch: 45,
        });

        i++;
        return;
      }

      const speedKmh = 25 + (i % 4) * 3;

      setCurrentLocation(next);

      await sendPointToBackend(next, speedKmh, headingDeg);

      mapRef.current?.animateCamera({
        center: next,
        zoom: 16,
        heading: headingDeg,
        pitch: 45,
      });

      i++;
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
            Alert.alert("Permission refusée", "Active la localisation GPS.");
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

          await sendRealLocationToBackend(position);
        } else {
          await sendPointToBackend(start, 0, 90);
        }

        setCurrentLocation(start);

        const routeBins = await loadBins();
        const missionId = getMissionIdFromBins(routeBins);

        let optimizedRoute: Point[] = [];

        if (missionId) {
          optimizedRoute = await loadOptimizedMissionRoute(missionId);
        }

        if (optimizedRoute.length < 2) {
          optimizedRoute = await loadFallbackOsrmRoute(start, routeBins);
        }

        if (DEV_MODE_PARIS) {
          const pathToSimulate = optimizedRoute.length >= 2 ? optimizedRoute : [start];

          setTimeout(() => {
            mapRef.current?.animateCamera({
              center: pathToSimulate[0],
              zoom: 16,
              pitch: 45,
            });
          }, 500);

          startParisDemoTrackingOnOptimizedRoute(pathToSimulate, routeBins);
        }

        if (!DEV_MODE_PARIS) {
          subscription = await Location.watchPositionAsync(
            {
              accuracy: Location.Accuracy.High,
              timeInterval: 3000,
              distanceInterval: 5,
            },
            async (location) => {
              const newPosition: Point = {
                latitude: location.coords.latitude,
                longitude: location.coords.longitude,
              };

              setCurrentLocation(newPosition);
              await sendRealLocationToBackend(location);

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

  return (
    <View style={styles.container}>
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
            title="Driver"
            description="Mode simulation sur route optimisée"
            pinColor="blue"
          />
        )}

        {routeCoords.length > 0 && (
          <Polyline
            coordinates={routeCoords}
            strokeColor="#2563EB"
            strokeWidth={6}
          />
        )}

        {bins.map((bin, index) => (
          <Marker
            key={bin.missionBinId ?? bin.binId ?? index}
            coordinate={{
              latitude: bin.lat!,
              longitude: bin.lng!,
            }}
            title={bin.binCode || `Bin ${index + 1}`}
            description={
              bin.collected
                ? "Collectée"
                : `Stop ${bin.visitOrder ?? index + 1}`
            }
            pinColor={bin.collected ? "green" : "red"}
          />
        ))}
      </MapView>

      <View style={styles.topBar}>
        <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
          <Ionicons name="chevron-back" size={24} color="#FFFFFF" />
        </TouchableOpacity>

        <View>
          <Text style={styles.title}>Navigation</Text>
          <Text style={styles.subtitle}>
            {DEV_MODE_PARIS
              ? "Démo active : route optimisée + pause collecte"
              : "GPS réel actif"}
          </Text>
        </View>
      </View>

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
  container: {
    flex: 1,
  },

  map: {
    flex: 1,
  },

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

  topBar: {
    position: "absolute",
    top: 50,
    left: 16,
    right: 16,
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(15,23,42,0.88)",
    borderRadius: 18,
    padding: 12,
  },

  backButton: {
    width: 42,
    height: 42,
    borderRadius: 14,
    backgroundColor: "#19C37D",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },

  title: {
    color: "#FFFFFF",
    fontSize: 18,
    fontWeight: "800",
  },

  subtitle: {
    color: "#CBD5E1",
    fontSize: 13,
    marginTop: 2,
  },

  noBinsCard: {
    position: "absolute",
    top: 125,
    left: 16,
    right: 16,
    backgroundColor: "rgba(239,68,68,0.92)",
    borderRadius: 16,
    padding: 14,
    alignItems: "center",
  },

  noBinsText: {
    color: "#FFFFFF",
    fontWeight: "800",
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

  bottomLabel: {
    color: "#94A3B8",
    fontSize: 12,
    marginBottom: 4,
  },

  bottomValue: {
    color: "#FFFFFF",
    fontSize: 18,
    fontWeight: "800",
  },

  centerButton: {
    width: 50,
    height: 50,
    borderRadius: 18,
    backgroundColor: "#19C37D",
    alignItems: "center",
    justifyContent: "center",
  },
});