package com.mido.backend.controller;

import com.mido.backend.storage.FileStorageService;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@Validated
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") @NotNull MultipartFile file) {
        String url = fileStorageService.storeImage(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}

