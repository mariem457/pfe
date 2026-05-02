package com.example.demo.controller;

import com.example.demo.entity.Bin;
import com.example.demo.repository.BinRepository;
import com.example.demo.service.QRCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bins")
@CrossOrigin("*")
public class QRCodeController {

    private final BinRepository binRepository;
    private final QRCodeService qrCodeService;

    public QRCodeController(BinRepository binRepository, QRCodeService qrCodeService) {
        this.binRepository = binRepository;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> getQRCode(@PathVariable Long id) {
        Bin bin = binRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + id));

        byte[] qrImage = qrCodeService.generateQRCodeImage(bin.getBinCode());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"qr-" + bin.getBinCode() + ".png\"")
                .body(qrImage);
    }

    @PostMapping("/{id}/qrcode/regenerate")
    public ResponseEntity<byte[]> regenerateQRCode(@PathVariable Long id) {
        Bin bin = binRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + id));

        byte[] qrImage = qrCodeService.generateQRCodeImage(bin.getBinCode());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"qr-" + bin.getBinCode() + ".png\"")
                .body(qrImage);
    }
}