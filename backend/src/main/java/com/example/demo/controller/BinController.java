package com.example.demo.controller;

import com.example.demo.dto.BinRequest;
import com.example.demo.dto.BinResponse;
import com.example.demo.dto.BinStatusDto;
import com.example.demo.service.BinService;
import com.example.demo.service.BinStatusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bins")
public class BinController {

    private final BinService service;
    private final BinStatusService binStatusService;

    public BinController(BinService service, BinStatusService binStatusService) {
        this.service = service;
        this.binStatusService = binStatusService;
    }

    @GetMapping
    public List<BinResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BinResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/status")
    public List<BinStatusDto> status() {
        return binStatusService.getBinsWithLatestTelemetry();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BinResponse create(@Valid @RequestBody BinRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public BinResponse update(@PathVariable Long id, @Valid @RequestBody BinRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}