package com.example.demo.service;

import com.example.demo.dto.LatLngPoint;
import com.example.demo.dto.ZoneCreateRequest;
import com.example.demo.dto.ZoneResponse;
import com.example.demo.dto.ZoneUpdateRequest;
import com.example.demo.entity.Zone;
import com.example.demo.repository.ZoneRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ZoneService {

    private final ZoneRepository repo;
    private final ObjectMapper objectMapper;

    public ZoneService(ZoneRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    public List<ZoneResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public ZoneResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public ZoneResponse create(ZoneCreateRequest req) {
        if (req == null || req.name == null || req.name.isBlank()) {
            throw new IllegalArgumentException("zone name is required");
        }

        boolean exists = repo.findAll().stream()
                .anyMatch(z -> req.name.equalsIgnoreCase(z.getShapeName()));

        if (exists) {
            throw new IllegalArgumentException("zone name already exists");
        }

        Zone z = new Zone();
        z.setShapeName(req.name);
        z.setShapeId(generateShapeId(req.name));
        z.setShapeType("ADM3");
        z.setShapeGroup("TUN");

        if (req.polygon != null && !req.polygon.isEmpty()) {
            z.setGeometryJson(toGeoJson(req.polygon));
        } else {
            throw new IllegalArgumentException("polygon is required");
        }

        return toResponse(repo.save(z));
    }

    @Transactional
    public ZoneResponse update(Long id, ZoneUpdateRequest req) {
        Zone z = getOrThrow(id);

        if (req.name != null && !req.name.isBlank()
                && !req.name.equalsIgnoreCase(z.getShapeName())) {

            boolean exists = repo.findAll().stream()
                    .anyMatch(other ->
                            !other.getId().equals(z.getId())
                                    && req.name.equalsIgnoreCase(other.getShapeName())
                    );

            if (exists) {
                throw new IllegalArgumentException("zone name already exists");
            }

            z.setShapeName(req.name);
        }

        if (req.polygon != null) {
            if (req.polygon.isEmpty()) {
                z.setGeometryJson(null);
            } else {
                z.setGeometryJson(toGeoJson(req.polygon));
            }
        }

        return toResponse(repo.save(z));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("zone not found");
        }
        repo.deleteById(id);
    }

    public Optional<Zone> findZoneContainingPoint(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return Optional.empty();
        }

        List<Zone> zones = repo.findAll();

        for (Zone zone : zones) {
            List<LatLngPoint> polygon = parsePolygon(zone.getGeometryJson());
            if (polygon.size() < 3) continue;

            if (isPointInsidePolygon(lat, lng, polygon)) {
                return Optional.of(zone);
            }
        }

        return Optional.empty();
    }

    private Zone getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("zone not found"));
    }

    private ZoneResponse toResponse(Zone z) {
        ZoneResponse r = new ZoneResponse();
        r.id = z.getId();
        r.name = z.getShapeName();
        r.description = null;

        List<LatLngPoint> polygon = parsePolygon(z.getGeometryJson());
        r.polygon = polygon;

        LatLngPoint center = computeCenter(polygon);
        r.centerLat = center != null ? center.getLat() : null;
        r.centerLng = center != null ? center.getLng() : null;

        r.createdAt = z.getCreatedAt() != null
                ? z.getCreatedAt().atOffset(java.time.ZoneOffset.UTC)
                : null;
        return r;
    }

    private String toGeoJson(List<LatLngPoint> polygon) {
        try {
            if (polygon == null || polygon.size() < 3) {
                throw new IllegalArgumentException("polygon must contain at least 3 points");
            }

            List<List<Double>> ring = new ArrayList<>();

            for (LatLngPoint point : polygon) {
                if (point.getLat() == null || point.getLng() == null) {
                    throw new IllegalArgumentException("invalid polygon point");
                }
                ring.add(List.of(point.getLng(), point.getLat()));
            }

            List<Double> first = ring.get(0);
            List<Double> last = ring.get(ring.size() - 1);

            if (!first.get(0).equals(last.get(0)) || !first.get(1).equals(last.get(1))) {
                ring.add(List.of(first.get(0), first.get(1)));
            }

            var geoJson = java.util.Map.of(
                    "type", "Polygon",
                    "coordinates", List.of(ring)
            );

            return objectMapper.writeValueAsString(geoJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid polygon");
        }
    }

    private List<LatLngPoint> parsePolygon(String geometryJson) {
        if (geometryJson == null || geometryJson.isBlank()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(geometryJson);
            JsonNode coordinates = root.get("coordinates");

            if (coordinates == null || !coordinates.isArray() || coordinates.isEmpty()) {
                return Collections.emptyList();
            }

            JsonNode firstRing = coordinates.get(0);
            if (firstRing == null || !firstRing.isArray()) {
                return Collections.emptyList();
            }

            List<LatLngPoint> polygon = new ArrayList<>();

            for (JsonNode pointNode : firstRing) {
                if (pointNode.isArray() && pointNode.size() >= 2) {
                    Double lng = pointNode.get(0).asDouble();
                    Double lat = pointNode.get(1).asDouble();
                    polygon.add(new LatLngPoint(lat, lng));
                }
            }

            if (polygon.size() > 1) {
                LatLngPoint first = polygon.get(0);
                LatLngPoint last = polygon.get(polygon.size() - 1);

                if (first.getLat().equals(last.getLat()) && first.getLng().equals(last.getLng())) {
                    polygon.remove(polygon.size() - 1);
                }
            }

            return polygon;
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid geometry json");
        }
    }

    private LatLngPoint computeCenter(List<LatLngPoint> polygon) {
        if (polygon == null || polygon.isEmpty()) {
            return null;
        }

        double latSum = 0.0;
        double lngSum = 0.0;

        for (LatLngPoint p : polygon) {
            latSum += p.getLat();
            lngSum += p.getLng();
        }

        return new LatLngPoint(latSum / polygon.size(), lngSum / polygon.size());
    }

    private boolean isPointInsidePolygon(Double lat, Double lng, List<LatLngPoint> polygon) {
        boolean inside = false;
        int n = polygon.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Double xi = polygon.get(i).getLng();
            Double yi = polygon.get(i).getLat();
            Double xj = polygon.get(j).getLng();
            Double yj = polygon.get(j).getLat();

            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / ((yj - yi) + 0.0) + xi);

            if (intersect) {
                inside = !inside;
            }
        }

        return inside;
    }

    private String generateShapeId(String name) {
        return "CUSTOM_" + Math.abs(name.trim().toLowerCase().hashCode()) + "_" + System.currentTimeMillis();
    }
}