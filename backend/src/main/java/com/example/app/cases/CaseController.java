package com.example.app.cases;

import com.example.app.cases.dto.CreateCaseRequest;
import com.example.app.cases.dto.CaseResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * CASE CONTROLLER - Updated with Microservices Support
 * 
 * 🎯 NEW: Accept assigneeId parameter for microservices flow
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {
    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    /**
     * Create case với microservices support
     * - Accept assigneeId để validate qua People Service
     */
    @PostMapping
    public ResponseEntity<CaseResponse> createCase(
            @Valid @RequestBody CreateCaseRequest request,
            @RequestParam(required = false) Long assigneeId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        CaseResponse response = caseService.createCaseWithMicroservices(request, assigneeId, idempotencyKey);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Get case by ID - WITH READ-THROUGH CACHE
     */
    @GetMapping("/{id}")
    public ResponseEntity<CaseResponse> getCaseById(@PathVariable Long id) {
        return caseService.getCaseById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Update case status - WITH CACHE INVALIDATION
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CaseResponse> updateCaseStatus(@PathVariable Long id, 
                                                        @RequestParam String status) {
        try {
            CaseResponse updated = caseService.updateCaseStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }
}