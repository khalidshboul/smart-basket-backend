package com.smartbasket.backend.controller;

import com.smartbasket.backend.dto.BulkUploadResponseDto;
import com.smartbasket.backend.dto.BulkUploadResponseDto.RowError;
import com.smartbasket.backend.exception.BulkUploadValidationException;
import com.smartbasket.backend.exception.ResourceNotFoundException;
import com.smartbasket.backend.service.BulkUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/bulk-upload")
@RequiredArgsConstructor
@Slf4j
public class BulkUploadController {
    
    private final BulkUploadService bulkUploadService;
    
    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResponseDto> uploadItems(
            @RequestParam("file") MultipartFile file,
            @RequestParam("categoryId") String categoryId) {
        
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    BulkUploadResponseDto.builder()
                            .success(false)
                            .totalRows(0)
                            .successCount(0)
                            .errorCount(1)
                            .errors(List.of(RowError.builder()
                                    .row(0)
                                    .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                                    .message("File is empty")
                                    .build()))
                            .build()
            );
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(
                    BulkUploadResponseDto.builder()
                            .success(false)
                            .totalRows(0)
                            .successCount(0)
                            .errorCount(1)
                            .errors(List.of(RowError.builder()
                                    .row(0)
                                    .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                                    .message("File must be .xlsx format")
                                    .build()))
                            .build()
            );
        }
        
        try {
            log.info("Processing bulk upload file: {} for category: {}", filename, categoryId);
            BulkUploadResponseDto response = bulkUploadService.processExcelFile(file, categoryId);
            log.info("Bulk upload completed: {} success", response.getSuccessCount());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.error("Category not found: {}", categoryId);
            return ResponseEntity.badRequest().body(
                    BulkUploadResponseDto.builder()
                            .success(false)
                            .totalRows(0)
                            .successCount(0)
                            .errorCount(1)
                            .categoryId(categoryId)
                            .errors(List.of(RowError.builder()
                                    .row(0)
                                    .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                                    .message(e.getMessage())
                                    .build()))
                            .build()
            );
        } catch (BulkUploadValidationException e) {
            log.error("Validation failed: {} errors", e.getErrors().size());
            return ResponseEntity.badRequest().body(
                    BulkUploadResponseDto.builder()
                            .success(false)
                            .totalRows(e.getTotalRows())
                            .successCount(0)
                            .errorCount(e.getErrors().size())
                            .categoryId(e.getCategoryId())
                            .categoryName(e.getCategoryName())
                            .errors(e.getErrors())
                            .invalidStores(e.getInvalidStores())
                            .build()
            );
        } catch (IOException e) {
            log.error("Error reading Excel file: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    BulkUploadResponseDto.builder()
                            .success(false)
                            .totalRows(0)
                            .successCount(0)
                            .errorCount(1)
                            .categoryId(categoryId)
                            .errors(List.of(RowError.builder()
                                    .row(0)
                                    .errorType(BulkUploadResponseDto.ERROR_SYSTEM)
                                    .message("Error reading file: " + e.getMessage())
                                    .build()))
                            .build()
            );
        }
    }
}
