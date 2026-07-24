package com.digitalheroes.Page.Pulse.controller;

import com.digitalheroes.Page.Pulse.dto.AuditRequest;
import com.digitalheroes.Page.Pulse.dto.AuditResponse;
import com.digitalheroes.Page.Pulse.exception.InvalidUrlFormatException;
import com.digitalheroes.Page.Pulse.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    @Test
    void testAudit_Success() throws Exception {
        AuditRequest request = new AuditRequest("https://example.com");
        AuditResponse expectedResponse = new AuditResponse(200, 150, "Test Page", "Test description", 1, 2, 100);

        when(auditService.auditUrl(anyString())).thenReturn(expectedResponse);

        ResponseEntity<AuditResponse> response = auditController.audit(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Test Page", response.getBody().getPageTitle());
        assertEquals("Test description", response.getBody().getMetaDescription());
    }

    @Test
    void testAudit_InvalidUrl() throws Exception {
        AuditRequest request = new AuditRequest("");

        when(auditService.auditUrl(anyString()))
                .thenThrow(new InvalidUrlFormatException("URL cannot be null or empty"));

        assertThrows(InvalidUrlFormatException.class, () -> {
            auditController.audit(request);
        });
    }
}
