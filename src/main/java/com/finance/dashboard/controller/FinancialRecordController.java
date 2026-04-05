package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.FinancialRecordRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.FinancialRecordResponse;
import com.finance.dashboard.dto.response.PagedResponse;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
@RestController
@RequestMapping("/records")
@Tag(name = "Financial Records", description = "CRUD operations for financial transactions")
public class FinancialRecordController {

    private final FinancialRecordService recordService;
    public FinancialRecordController(FinancialRecordService recordService) {
        this.recordService = recordService;
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "List records",
               description = "Returns a paginated list of records. Supports filtering by type, category, and date range.")
    public ResponseEntity<PagedResponse<FinancialRecordResponse>> getAllRecords(
            @Parameter(description = "Filter by type: INCOME or EXPENSE")
            @RequestParam(required = false) TransactionType type,

            @Parameter(description = "Filter by category (partial match)")
            @RequestParam(required = false) String category,

            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(recordService.getAllRecords(type, category, from, to, pageable));
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get record by ID")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(recordService.getRecordById(id)));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create record", description = "Creates a new financial record. ADMIN only.")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> create(
            @Valid @RequestBody FinancialRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        FinancialRecordResponse created = recordService.createRecord(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Record created successfully", created));
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update record", description = "Updates an existing financial record. ADMIN only.")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody FinancialRecordRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Record updated successfully",
                recordService.updateRecord(id, request)));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete record",
               description = "Soft-deletes a record (hidden from queries, preserved for audit). ADMIN only.")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.ok(ApiResponse.success("Record deleted successfully", null));
    }
}