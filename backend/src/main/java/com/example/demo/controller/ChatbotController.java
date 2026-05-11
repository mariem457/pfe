package com.example.demo.controller;

import com.example.demo.dto.BinResponse;
import com.example.demo.service.BinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin("*")
public class ChatbotController {

    private final BinService binService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> binAddressCache = new ConcurrentHashMap<>();

    public ChatbotController(BinService binService) {
        this.binService = binService;
    }

    @PostMapping
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {
        String raw = String.valueOf(body.getOrDefault("message", "")).trim();
        String locationLabel = String.valueOf(body.getOrDefault("locationLabel", "")).trim();
        Set<String> requestedWasteTypes = extractWasteTypes(body.get("wasteTypes"));

        if (raw.isBlank() || !looksLikeLocation(raw)) {
            return Map.of(
                    "reply",
                    "Bonjour 👋 S’il vous plaît, donnez-moi votre localisation pour vous indiquer la poubelle disponible la plus proche.",
                    "bins",
                    List.of()
            );
        }

        Optional<GeoPoint> geoOpt = geocode(raw);

        if (geoOpt.isEmpty()) {
            return Map.of(
                    "reply",
                    "Je n’ai pas reconnu cette adresse. Essayez par exemple : Rue de Javel, Avenue Émile Zola, Convention, Vaugirard.",
                    "bins",
                    List.of()
            );
        }

        GeoPoint userLocation = geoOpt.get();

        if (!"75015".equals(userLocation.postcode)) {
            return Map.of(
                    "reply",
                    "Désolé 🙏 ce service couvre actuellement uniquement les zones de Paris 15.",
                    "bins",
                    List.of()
            );
        }

        String requestedNeighborhood = detectNeighborhood(raw + " " + locationLabel + " " + userLocation.label);

        List<BinResponse> baseAvailableBins = binService.findAll().stream()
                .filter(this::isAvailable)
                .filter(b -> matchesWasteType(b, requestedWasteTypes))
                .toList();

        List<BinResponse> neighborhoodBins = requestedNeighborhood == null
                ? List.of()
                : baseAvailableBins.stream()
                .filter(b -> matchesNeighborhood(b, requestedNeighborhood))
                .toList();

        List<BinResponse> binsToRank = neighborhoodBins.isEmpty()
                ? baseAvailableBins
                : neighborhoodBins;

        List<BinResponse> availableBins = binsToRank.stream()
                .sorted(Comparator.comparingDouble(
                        b -> distance(userLocation.lat, userLocation.lng, safeLat(b), safeLng(b))
                ))
                .limit(10)
                .toList();

        if (availableBins.isEmpty()) {
            return Map.of(
                    "reply",
                    "⚠️ Toutes les poubelles disponibles proches de " + userLocation.label + " sont actuellement remplies.",
                    "bins",
                    List.of()
            );
        }

        return Map.of(
                "reply",
                "✅ Poubelles disponibles proches de " + userLocation.label + " :",
                "bins",
                availableBins.stream().map(b -> Map.of(
                        "code", safe(b.binCode),
                        "zone", buildPreciseZone(b),
                        "fillLevel", b.fillLevel,
                        "distance", formatDistance(distance(userLocation.lat, userLocation.lng, safeLat(b), safeLng(b))),
                        "lat", safeLat(b),
                        "lng", safeLng(b),
                        "latitude", safeLat(b),
                        "longitude", safeLng(b)
                )).toList()
        );
    }

    private Optional<GeoPoint> geocode(String address) {
        Optional<GeoPoint> coordinates = parseCoordinates(address);
        if (coordinates.isPresent()) {
            return coordinates;
        }

        Optional<GeoPoint> geoPoint = geocodeWithFrenchApi(address);
        if (geoPoint.isPresent()) {
            return geoPoint;
        }

        geoPoint = geocodeWithNominatim(address);
        if (geoPoint.isPresent()) {
            return geoPoint;
        }

        if (mentionsParis15(address)) {
            return Optional.of(new GeoPoint(48.8412, 2.3003, "Paris 15", "75015"));
        }

        return Optional.empty();
    }

    private Optional<GeoPoint> geocodeWithFrenchApi(String address) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://data.geopf.fr/geocodage/search")
                    .queryParam("q", address + " Paris 15")
                    .queryParam("limit", 1)
                    .queryParam("postcode", "75015")
                    .build()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();

            if (body == null || !(body.get("features") instanceof List<?> features) || features.isEmpty()) {
                return Optional.empty();
            }

            Map feature = (Map) features.get(0);
            Map geometry = (Map) feature.get("geometry");
            Map properties = (Map) feature.get("properties");

            List<?> coordinates = (List<?>) geometry.get("coordinates");

            double lng = ((Number) coordinates.get(0)).doubleValue();
            double lat = ((Number) coordinates.get(1)).doubleValue();

            String label = String.valueOf(properties.getOrDefault("label", address));
            String postcode = String.valueOf(properties.getOrDefault("postcode", ""));

            return Optional.of(new GeoPoint(lat, lng, label, postcode));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<GeoPoint> geocodeWithNominatim(String address) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                    .queryParam("format", "jsonv2")
                    .queryParam("q", address + ", Paris 15, France")
                    .queryParam("limit", 1)
                    .build()
                    .toUriString();

            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List<?> body = response.getBody();

            if (body == null || body.isEmpty() || !(body.get(0) instanceof Map result)) {
                return Optional.empty();
            }

            double lat = Double.parseDouble(String.valueOf(result.get("lat")));
            double lng = Double.parseDouble(String.valueOf(result.get("lon")));
            String label = String.valueOf(result.getOrDefault("display_name", address));
            String postcode = mentionsParis15(label) || isInsideParis15Approx(lat, lng) ? "75015" : "";

            return Optional.of(new GeoPoint(lat, lng, label, postcode));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<GeoPoint> parseCoordinates(String raw) {
        String[] parts = raw.split(",");

        if (parts.length != 2) {
            return Optional.empty();
        }

        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());

            if (!isInsideParis15Approx(lat, lng)) {
                return Optional.of(new GeoPoint(lat, lng, "Position sélectionnée", ""));
            }

            return Optional.of(new GeoPoint(lat, lng, "adresse sélectionnée dans Paris 15", "75015"));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private boolean isInsideParis15Approx(double lat, double lng) {
        return lat >= 48.819
                && lat <= 48.857
                && lng >= 2.257
                && lng <= 2.323;
    }

    private boolean looksLikeLocation(String msg) {
        String m = normalize(msg);

        List<String> notLocation = List.of(
                "bonjour", "bonsoir", "salut", "salem", "salam", "hello", "hi", "hey",
                "cc", "coucou", "cv", "ca va", "merci", "ok"
        );

        if (notLocation.contains(m)) return false;

        return m.length() >= 3;
    }

    private boolean isAvailable(BinResponse b) {
        return b.fillLevel != null
                && b.fillLevel < 80
                && Boolean.TRUE.equals(b.isActive)
                && b.lat != null
                && b.lng != null;
    }

    private Set<String> extractWasteTypes(Object rawWasteTypes) {
        if (!(rawWasteTypes instanceof Collection<?> values)) {
            return Set.of();
        }

        Set<String> wasteTypes = new HashSet<>();

        for (Object value : values) {
            if (value != null) {
                wasteTypes.add(String.valueOf(value).trim().toUpperCase(Locale.ROOT));
            }
        }

        return wasteTypes;
    }

    private boolean matchesWasteType(BinResponse b, Set<String> requestedWasteTypes) {
        if (requestedWasteTypes.isEmpty()) {
            return true;
        }

        return b.wasteType != null
                && requestedWasteTypes.contains(b.wasteType.trim().toUpperCase(Locale.ROOT));
    }

    private String detectNeighborhood(String text) {
        String normalized = normalize(text);

        List<String> neighborhoods = List.of(
                "javel",
                "grenelle",
                "saint lambert",
                "saintlambert",
                "necker"
        );

        for (String neighborhood : neighborhoods) {
            if (normalized.contains(neighborhood)) {
                return neighborhood;
            }
        }

        return null;
    }

    private boolean matchesNeighborhood(BinResponse b, String neighborhood) {
        String binText = normalize(
                safe(b.zoneName) + " " + safe(b.notes)
        );

        return binText.contains(neighborhood);
    }

    private String buildPreciseZone(BinResponse b) {
        if (b.lat != null && b.lng != null) {
            return reverseGeocodeBinAddress(b.lat, b.lng)
                    .orElseGet(() -> fallbackBinZone(b));
        }

        return fallbackBinZone(b);
    }

    private Optional<String> reverseGeocodeBinAddress(double lat, double lng) {
        String cacheKey = String.format(Locale.US, "%.6f,%.6f", lat, lng);

        if (binAddressCache.containsKey(cacheKey)) {
            return Optional.of(binAddressCache.get(cacheKey));
        }

        Optional<String> frenchAddress = reverseGeocodeWithFrenchApi(lat, lng);
        if (frenchAddress.isPresent()) {
            binAddressCache.put(cacheKey, frenchAddress.get());
            return frenchAddress;
        }

        Optional<String> osmAddress = reverseGeocodeWithNominatim(lat, lng);
        osmAddress.ifPresent(address -> binAddressCache.put(cacheKey, address));
        return osmAddress;
    }

    private Optional<String> reverseGeocodeWithFrenchApi(double lat, double lng) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api-adresse.data.gouv.fr/reverse/")
                    .queryParam("lat", lat)
                    .queryParam("lon", lng)
                    .queryParam("limit", 1)
                    .build()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();

            if (body == null || !(body.get("features") instanceof List<?> features) || features.isEmpty()) {
                return Optional.empty();
            }

            Map feature = (Map) features.get(0);
            Map properties = (Map) feature.get("properties");

            if (properties == null) {
                return Optional.empty();
            }

            String name = firstNonBlank(properties.get("name"), properties.get("label"));

            if (name.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(name);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> reverseGeocodeWithNominatim(double lat, double lng) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://nominatim.openstreetmap.org/reverse")
                    .queryParam("format", "jsonv2")
                    .queryParam("lat", lat)
                    .queryParam("lon", lng)
                    .queryParam("addressdetails", 1)
                    .build()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();

            if (body == null) {
                return Optional.empty();
            }

            String address = extractStreetAddress(body)
                    .orElse(String.valueOf(body.getOrDefault("display_name", "")).trim());

            if (address.isBlank() || "null".equalsIgnoreCase(address)) {
                return Optional.empty();
            }

            return Optional.of(address);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String fallbackBinZone(BinResponse b) {
        return "Rue indisponible";
    }

    private Optional<String> extractStreetAddress(Map body) {
        if (!(body.get("address") instanceof Map address)) {
            return Optional.empty();
        }

        String road = firstNonBlank(
                address.get("road"),
                address.get("pedestrian"),
                address.get("footway"),
                address.get("path"),
                address.get("cycleway")
        );

        if (road.isBlank()) {
            return Optional.empty();
        }

        String houseNumber = firstNonBlank(address.get("house_number"));

        if (!houseNumber.isBlank()) {
            return Optional.of(houseNumber + " " + road);
        }

        return Optional.of(road);
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) continue;

            String text = String.valueOf(value).trim();
            if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }

        return "";
    }

    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2)
                        * Math.sin(dLng / 2);

        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String formatDistance(double km) {
        if (km < 1) {
            return Math.round(km * 1000) + " m";
        }
        return String.format(Locale.US, "%.2f km", km);
    }

    private double safeLat(BinResponse b) {
        return b.lat == null ? 0 : b.lat;
    }

    private double safeLng(BinResponse b) {
        return b.lng == null ? 0 : b.lng;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("ç", "c")
                .replace("’", " ")
                .replace("'", " ")
                .replace("-", " ")
                .replace(",", " ")
                .replace(".", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean mentionsParis15(String address) {
        String normalized = normalize(address);
        return normalized.contains("75015")
                || normalized.contains("paris 15")
                || normalized.contains("paris 15e")
                || normalized.contains("15 arrondissement")
                || normalized.contains("15e arrondissement");
    }

    private record GeoPoint(double lat, double lng, String label, String postcode) {}
}
