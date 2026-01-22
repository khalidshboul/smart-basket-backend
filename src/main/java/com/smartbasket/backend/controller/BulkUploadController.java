package com.smartbasket.backend.controller;

import com.smartbasket.backend.dto.BulkUploadResponseDto;
import com.smartbasket.backend.service.BulkUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/bulk-upload")
@RequiredArgsConstructor
@Slf4j
public class BulkUploadController {
    
    private final BulkUploadService bulkUploadService;
    
    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResponseDto> uploadItems(
            @RequestParam("file") MultipartFile file) {
        
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    BulkUploadResponseDto.builder()
                            .totalSheets(0)
                            .totalRows(0)
                            .successCount(0)
                            .errorCount(1)
                            .build()
            );
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(
                    BulkUploadResponseDto.builder()
                            .totalSheets(0)
                            .totalRows(0)
                            .successCount(0)
                            .errorCount(1)
                            .build()
            );
        }
        
        try {
            log.info("Processing bulk upload file: {}", filename);
            BulkUploadResponseDto response = bulkUploadService.processExcelFile(file);
            log.info("Bulk upload completed: {} success, {} errors",
                    response.getSuccessCount(), response.getErrorCount());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error reading Excel file: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    BulkUploadResponseDto.builder()
                            .totalSheets(0)
                            .totalRows(0)
                            .successCount(0)
                            .errorCount(1)
                            .build()
            );
        }
    }
}
