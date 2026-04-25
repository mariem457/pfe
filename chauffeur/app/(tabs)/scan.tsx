import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  Alert,
  ActivityIndicator,
  TouchableOpacity,
} from "react-native";
import { CameraView, useCameraPermissions } from "expo-camera";
import { useLocalSearchParams, router } from "expo-router";
import { BASE_URL } from "../../lib/api";
import { getToken } from "../../lib/storage";

export default function ScanScreen() {
  const [permission, requestPermission] = useCameraPermissions();
  const [scanned, setScanned] = useState(false);
  const [loading, setLoading] = useState(false);
  const [lastCode, setLastCode] = useState<string | null>(null);

  const { expectedBinCode, missionBinId } = useLocalSearchParams<{
    expectedBinCode?: string;
    missionBinId?: string;
  }>();

  useEffect(() => {
    if (!permission) {
      requestPermission();
    }
  }, [permission, requestPermission]);

  const handleScan = async ({ data }: { data: string }) => {
    if (scanned || loading) return;

    setScanned(true);
    setLastCode(data);

    try {
      if (expectedBinCode && data !== expectedBinCode) {
        throw new Error(`QR incorrect. Attendu: ${expectedBinCode}`);
      }

      setLoading(true);

      const token = await getToken();

      if (!token) {
        throw new Error("Utilisateur non connecté");
      }

      const res = await fetch(`${BASE_URL}/api/drivers/bin-scan`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          missionBinId: missionBinId ? Number(missionBinId) : null,
          binCode: data,
          driverNote: "collecte effectuée",
          issueType: null,
        }),
      });

      const rawText = await res.text();

      let result: any = null;

      try {
        result = rawText ? JSON.parse(rawText) : null;
      } catch {
        throw new Error(rawText || "Réponse invalide du serveur");
      }

      if (!res.ok) {
        throw new Error(result?.message || "Erreur lors du scan");
      }

      Alert.alert(
        "Succès",
        result?.message || `Poubelle ${result?.binCode || data} collectée avec succès`,
        [
          {
            text: "OK",
            onPress: () => router.replace("/(tabs)/dashboard"),
          },
        ]
      );
    } catch (err: any) {
      Alert.alert("Erreur", err.message || "Une erreur est survenue");
      setScanned(false);
      setLastCode(null);
    } finally {
      setLoading(false);
    }
  };

  const resetScan = () => {
    setScanned(false);
    setLastCode(null);
  };

  if (!permission) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (!permission.granted) {
    return (
      <View style={styles.center}>
        <Text style={styles.infoText}>Permission caméra requise</Text>
        <TouchableOpacity style={styles.button} onPress={requestPermission}>
          <Text style={styles.buttonText}>Autoriser la caméra</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <CameraView
        style={StyleSheet.absoluteFillObject}
        barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
        onBarcodeScanned={handleScan}
      />

      <View style={styles.overlay}>
        <Text style={styles.title}>Scanner QR Code</Text>

        {expectedBinCode ? (
          <Text style={styles.codeText}>Bin attendu: {expectedBinCode}</Text>
        ) : null}

        {loading && <ActivityIndicator color="#fff" size="large" />}

        {lastCode && (
          <Text style={styles.codeText}>Code détecté: {lastCode}</Text>
        )}

        {scanned && !loading && (
          <TouchableOpacity style={styles.button} onPress={resetScan}>
            <Text style={styles.buttonText}>Scanner à nouveau</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  overlay: {
    position: "absolute",
    bottom: 40,
    left: 20,
    right: 20,
    backgroundColor: "rgba(0,0,0,0.6)",
    borderRadius: 16,
    padding: 20,
    alignItems: "center",
  },
  title: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "600",
    marginBottom: 12,
    textAlign: "center",
  },
  codeText: {
    color: "#fff",
    fontSize: 16,
    marginTop: 10,
    marginBottom: 12,
    textAlign: "center",
  },
  button: {
    backgroundColor: "#1E88E5",
    paddingVertical: 12,
    paddingHorizontal: 18,
    borderRadius: 10,
    marginTop: 10,
  },
  buttonText: {
    color: "#fff",
    fontWeight: "600",
  },
  center: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  infoText: {
    fontSize: 16,
    marginBottom: 16,
  },
});