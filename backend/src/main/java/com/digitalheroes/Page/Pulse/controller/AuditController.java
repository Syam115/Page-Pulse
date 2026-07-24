package com.digitalheroes.Page.Pulse.controller;

import com.digitalheroes.Page.Pulse.dto.AuditRequest;
import com.digitalheroes.Page.Pulse.dto.AuditResponse;
import com.digitalheroes.Page.Pulse.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/audit")
    public ResponseEntity<AuditResponse> audit(@RequestBody AuditRequest request) throws Exception {
        return ResponseEntity.ok(auditService.auditUrl(request.getUrl()));
    }
}
