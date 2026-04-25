import { Ionicons } from "@expo/vector-icons";
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
import * as polyline from "@mapbox/polyline";
import { getToken, getUserId } from "../lib/storage";

const BASE_URL = "http://192.168.0.21:8081";

const DEV_MODE_PARIS = true;

const PARIS_DRIVER_LOCATION = {
  latitude: 48.8414,
  longitude: 2.3003,
};

type DriverBin = {
  missionBinId?: number;
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

export default function RouteMap() {
  const mapRef = useRef<MapView | null>(null);

  const [loading, setLoading] = useState(true);
  const [currentLocation, setCurrentLocation] = useState<Point | null>(null);
  const [bins, setBins] = useState<DriverBin[]>([]);
  const [routeCoords, setRouteCoords] = useState<Point[]>([]);
  const [distanceKm, setDistanceKm] = useState<number | null>(null);
  const [durationMin, setDurationMin] = useState<number | null>(null);

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

  async function loadRoute(start: Point, routeBins: DriverBin[]) {
    if (routeBins.length === 0) {
      setRouteCoords([]);
      setDistanceKm(null);
      setDurationMin(null);
      return;
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
        return;
      }

      const route = data.routes[0];

      const decoded = polyline.decode(route.geometry).map(([lat, lng]) => ({
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
    } catch (error) {
      console.log("Route error:", error);
      setRouteCoords([]);
      setDistanceKm(null);
      setDurationMin(null);
    }
  }

  async function sendLocationToBackend(location: Location.LocationObject) {
    try {
      const token = await getToken();
      if (!token) return;

      await fetch(`${BASE_URL}/api/driver-location`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          latitude: location.coords.latitude,
          longitude: location.coords.longitude,
          speed: location.coords.speed,
          heading: location.coords.heading,
          timestamp: new Date().toISOString(),
        }),
      });
    } catch (error) {
      console.log("Send location error:", error);
    }
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

          await sendLocationToBackend(position);
        }

        setCurrentLocation(start);

        const routeBins = await loadBins();
        await loadRoute(start, routeBins);

        if (DEV_MODE_PARIS) {
          setTimeout(() => {
            mapRef.current?.animateCamera({
              center: start,
              zoom: 15,
              pitch: 45,
            });
          }, 500);
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
              await sendLocationToBackend(location);

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
            coordinate={PARIS_DRIVER_LOCATION}
            title="Driver"
            description="Position simulation Paris 15"
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
            description={`Stop ${bin.visitOrder ?? index + 1}`}
            pinColor="red"
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
            {DEV_MODE_PARIS ? "Mode simulation Paris 15" : "GPS réel"}
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