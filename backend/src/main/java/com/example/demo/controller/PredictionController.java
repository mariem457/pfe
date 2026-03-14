package com.example.demo.controller;

import com.example.demo.service.PredictionResult;
import com.example.demo.service.PythonPredictionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prediction")
public class PredictionController {

    private final PythonPredictionService pythonPredictionService;

    public PredictionController(PythonPredictionService pythonPredictionService) {
        this.pythonPredictionService = pythonPredictionService;
    }

    @GetMapping("/bin/{binId}")
    public PredictionResult predictForBin(@PathVariable Long binId) {
        return pythonPredictionService.runPredictionForBin(binId);
    }
}