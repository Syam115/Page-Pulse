package com.digitalheroes.Page.Pulse.service;

import com.digitalheroes.Page.Pulse.dto.AuditResponse;
import com.digitalheroes.Page.Pulse.exception.InvalidContentTypeException;
import com.digitalheroes.Page.Pulse.exception.InvalidUrlFormatException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private AuditService auditService;

    @BeforeEach
    void init() { this.auditService = new AuditService(); }

    @Test
    void testAuditUrl_Success() throws URISyntaxException, IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);
            Connection.Response mockResponse = mock(Connection.Response.class);
            Document mockDocument = mock(Document.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(5000)).thenReturn(mockConnection);
            when(mockConnection.execute()).thenReturn(mockResponse);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.contentType()).thenReturn("text/html; charset=UTF-8");
            when(mockResponse.parse()).thenReturn(mockDocument);

            when(mockDocument.title()).thenReturn("Test Page");
            Element metaDesc = mock(Element.class);
            when(mockDocument.selectFirst("meta[name=description]")).thenReturn(metaDesc);
            when(metaDesc.attr("content")).thenReturn("Test description");

            Elements h1Elements = mock(Elements.class);
            when(h1Elements.size()).thenReturn(2);
            when(mockDocument.select("h1")).thenReturn(h1Elements);

            Elements imgElements = mock(Elements.class);
            when(imgElements.size()).thenReturn(1);
            when(mockDocument.select("img:not([alt]), img[alt=]")).thenReturn(imgElements);

            Element bodyElement = mock(Element.class);
            when(mockDocument.body()).thenReturn(bodyElement);
            when(bodyElement.text()).thenReturn("This is a test page with some words");

            AuditResponse response = auditService.auditUrl("https://example.com");

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
            assertEquals("Test Page", response.getPageTitle());
            assertEquals("Test description", response.getMetaDescription());
            assertEquals(2, response.getH1Count());
            assertEquals(1, response.getMissingAltCount());
            assertEquals(8, response.getWordCount());
            assertTrue(response.getResponseTime() >= 0);
        }
    }

    @Test
    void testAuditUrl_InvalidUrlFormat_Null() {
        assertThrows(InvalidUrlFormatException.class, () -> {
            auditService.auditUrl(null);
        });
    }

    @Test
    void testAuditUrl_InvalidUrlFormat_Empty() {
        assertThrows(InvalidUrlFormatException.class, () -> {
            auditService.auditUrl("");
        });
    }

    @Test
    void testAuditUrl_InvalidUrlFormat_NoProtocol() {
        assertThrows(InvalidUrlFormatException.class, () -> {
            auditService.auditUrl("ftp://example.com");
        });
    }

    @Test
    void testAuditUrl_NonHtmlContent() throws URISyntaxException, IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);
            Connection.Response mockResponse = mock(Connection.Response.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(5000)).thenReturn(mockConnection);
            when(mockConnection.execute()).thenReturn(mockResponse);
            when(mockResponse.contentType()).thenReturn("application/json");

            assertThrows(InvalidContentTypeException.class, () -> {
                auditService.auditUrl("https://example.com/api");
            });
        }
    }

    @Test
    void testAuditUrl_Timeout() throws URISyntaxException, IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(5000)).thenReturn(mockConnection);
            when(mockConnection.execute()).thenThrow(new SocketTimeoutException("Connection timed out"));

            assertThrows(SocketTimeoutException.class, () -> {
                auditService.auditUrl("https://slow-example.com");
            });
        }
    }

    @Test
    void testAuditUrl_NoMetaDescription() throws URISyntaxException, IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);
            Connection.Response mockResponse = mock(Connection.Response.class);
            Document mockDocument = mock(Document.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(5000)).thenReturn(mockConnection);
            when(mockConnection.execute()).thenReturn(mockResponse);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.contentType()).thenReturn("text/html");
            when(mockResponse.parse()).thenReturn(mockDocument);

            when(mockDocument.title()).thenReturn("Test Page");
            when(mockDocument.selectFirst("meta[name=description]")).thenReturn(null);

            Elements h1Elements = mock(Elements.class);
            when(h1Elements.size()).thenReturn(0);
            when(mockDocument.select("h1")).thenReturn(h1Elements);

            Elements imgElements = mock(Elements.class);
            when(imgElements.size()).thenReturn(0);
            when(mockDocument.select("img:not([alt]), img[alt=]")).thenReturn(imgElements);

            Element bodyElement = mock(Element.class);
            when(mockDocument.body()).thenReturn(bodyElement);
            when(bodyElement.text()).thenReturn("Test content");

            AuditResponse response = auditService.auditUrl("https://example.com");

            assertEquals("No meta description found", response.getMetaDescription());
        }
    }

    @Test
    void testAuditUrl_EmptyBody() throws URISyntaxException, IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);
            Connection.Response mockResponse = mock(Connection.Response.class);
            Document mockDocument = mock(Document.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(5000)).thenReturn(mockConnection);
            when(mockConnection.execute()).thenReturn(mockResponse);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.contentType()).thenReturn("text/html");
            when(mockResponse.parse()).thenReturn(mockDocument);

            when(mockDocument.title()).thenReturn("Empty Page");
            when(mockDocument.selectFirst("meta[name=description]")).thenReturn(null);

            Elements h1Elements = mock(Elements.class);
            when(h1Elements.size()).thenReturn(0);
            when(mockDocument.select("h1")).thenReturn(h1Elements);

            Elements imgElements = mock(Elements.class);
            when(imgElements.size()).thenReturn(0);
            when(mockDocument.select("img:not([alt]), img[alt=]")).thenReturn(imgElements);

            Element bodyElement = mock(Element.class);
            when(mockDocument.body()).thenReturn(bodyElement);
            when(bodyElement.text()).thenReturn("");

            AuditResponse response = auditService.auditUrl("https://example.com");

            assertEquals(0, response.getWordCount());
        }
    }
}
