package com.example.demo.service;

import com.example.demo.dto.ZoneCreateRequest;
import com.example.demo.dto.ZoneResponse;
import com.example.demo.dto.ZoneUpdateRequest;
import com.example.demo.entity.Zone;
import com.example.demo.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ZoneService {

    private final ZoneRepository repo;

    public ZoneService(ZoneRepository repo) {
        this.repo = repo;
    }

    public List<ZoneResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public ZoneResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public ZoneResponse create(ZoneCreateRequest req) {
        if (repo.existsByName(req.name)) {
            throw new IllegalArgumentException("zone name already exists");
        }

        Zone z = new Zone();
        z.setName(req.name);
        z.setDescription(req.description);
        z.setCenterLat(req.centerLat);
        z.setCenterLng(req.centerLng);

        return toResponse(repo.save(z));
    }

    @Transactional
    public ZoneResponse update(Long id, ZoneUpdateRequest req) {
        Zone z = getOrThrow(id);

        if (req.name != null && !req.name.equals(z.getName())) {
            if (repo.existsByName(req.name)) {
                throw new IllegalArgumentException("zone name already exists");
            }
            z.setName(req.name);
        }

        if (req.description != null) z.setDescription(req.description);
        if (req.centerLat != null) z.setCenterLat(req.centerLat);
        if (req.centerLng != null) z.setCenterLng(req.centerLng);

        return toResponse(repo.save(z));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new IllegalArgumentException("zone not found");
        repo.deleteById(id);
    }

    private Zone getOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("zone not found"));
    }

    private ZoneResponse toResponse(Zone z) {
        ZoneResponse r = new ZoneResponse();
        r.id = z.getId();
        r.name = z.getName();
        r.description = z.getDescription();
        r.centerLat = z.getCenterLat();
        r.centerLng = z.getCenterLng();
        r.createdAt = z.getCreatedAt();
        return r;
    }
}