package com.pmh.controller;

import com.pmh.model.dto.TransactionLogSearchDTO;
import com.pmh.model.entity.ApiResponse;
import com.pmh.model.entity.TransactionLog;
import com.pmh.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction-logs")
@RequiredArgsConstructor
public class TransactionLogController {

    private final TransactionLogService transactionLogService;

    /**
     * Tìm kiếm transaction logs
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER', 'CHECKER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<TransactionLog>>> search(
            @RequestBody(required = false) TransactionLogSearchDTO searchDTO,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        if (searchDTO == null) {
            searchDTO = new TransactionLogSearchDTO();
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TransactionLog> result = transactionLogService.search(searchDTO, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<TransactionLog>>builder()
                .success(true)
                .message("Tìm kiếm thành công")
                .data(result)
                .build());
    }

    /**
     * Xuất file
     */
    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER', 'CHECKER')")
    public ResponseEntity<ApiResponse<String>> exportFile(
            @RequestBody(required = false) TransactionLogSearchDTO searchDTO
    ) {
        if (searchDTO == null) {
            searchDTO = new TransactionLogSearchDTO();
        }

        String message = transactionLogService.exportToFile(searchDTO);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message(message)
                .data(null)
                .build());
    }
}
