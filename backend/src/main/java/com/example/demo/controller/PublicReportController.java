package com.example.demo.controller;

import com.example.demo.dto.AssignPublicReportRequest;
import com.example.demo.dto.CreatePublicReportRequest;
import com.example.demo.dto.PublicReportDecisionResponse;
import com.example.demo.dto.PublicReportResponse;
import com.example.demo.dto.QualifyPublicReportRequest;
import com.example.demo.dto.RejectPublicReportRequest;
import com.example.demo.dto.ResolvePublicReportRequest;
import com.example.demo.service.PublicReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicReportController {

    private final PublicReportService publicReportService;
    private final ObjectMapper objectMapper;

    public PublicReportController(PublicReportService publicReportService, ObjectMapper objectMapper) {
        this.publicReportService = publicReportService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/public-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublicReportResponse createPublicReport(
            @RequestPart("data") String data,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) throws Exception {
        CreatePublicReportRequest request =
                objectMapper.readValue(data, CreatePublicReportRequest.class);

        return publicReportService.create(request, photo);
    }

    @GetMapping("/municipality/public-reports")
    public List<PublicReportResponse> getAllReports() {
        return publicReportService.getAll();
    }

    @GetMapping("/municipality/public-reports/optimization")
    public List<PublicReportResponse> getValidatedReportsForOptimization() {
        return publicReportService.getValidatedForOptimization();
    }

    @GetMapping("/municipality/public-reports/{id}/history")
    public List<PublicReportDecisionResponse> getHistory(@PathVariable Long id) {
        return publicReportService.getDecisionHistory(id);
    }

    @PutMapping("/municipality/public-reports/{id}/validate")
    public PublicReportResponse validateReport(@PathVariable Long id) {
        return publicReportService.validate(id);
    }

    @PutMapping("/municipality/public-reports/{id}/reject")
    public PublicReportResponse rejectReport(
            @PathVariable Long id,
            @RequestBody RejectPublicReportRequest request
    ) {
        return publicReportService.reject(id, request.getReason());
    }

    @PutMapping("/municipality/public-reports/{id}/qualify")
    public PublicReportResponse qualifyReport(
            @PathVariable Long id,
            @RequestBody QualifyPublicReportRequest request
    ) {
        return publicReportService.qualify(
                id,
                request.getQualificationNote(),
                request.getDuplicateOfReportId()
        );
    }

    @PutMapping("/municipality/public-reports/{id}/assign")
    public PublicReportResponse assignReport(
            @PathVariable Long id,
            @RequestBody AssignPublicReportRequest request
    ) {
        return publicReportService.assign(id, request);
    }

    @PutMapping("/municipality/public-reports/{id}/resolve")
    public PublicReportResponse resolveReport(
            @PathVariable Long id,
            @RequestBody ResolvePublicReportRequest request
    ) {
        return publicReportService.resolve(id, request);
    }
}