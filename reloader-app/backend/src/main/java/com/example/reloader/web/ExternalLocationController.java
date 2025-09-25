package com.example.reloader.web;

import com.example.reloader.entity.ExternalEnvironment;
import com.example.reloader.entity.ExternalLocation;
import com.example.reloader.service.ExternalLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/environments")
public class ExternalLocationController {
    private final ExternalLocationService service;

    public ExternalLocationController(ExternalLocationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExternalEnvironment> listEnvironments() {
        return service.listEnvironments();
    }

    @GetMapping("/{envName}/locations")
    public List<ExternalLocation> listLocations(@PathVariable String envName) {
        return service.listLocationsForEnvironment(envName);
    }

    @PostMapping("/import")
    public ResponseEntity<String> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        service.importCsv(file.getInputStream());
        return ResponseEntity.ok("imported");
    }
}
