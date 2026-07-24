package com.digitalheroes.Page.Pulse.service;

import com.digitalheroes.Page.Pulse.dto.AuditResponse;
import com.digitalheroes.Page.Pulse.exception.InvalidContentTypeException;
import com.digitalheroes.Page.Pulse.exception.InvalidUrlFormatException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuditService {

    public AuditResponse auditUrl(String urlString) throws URISyntaxException, IOException {

        if (urlString == null || urlString.isEmpty()) {
            throw new InvalidUrlFormatException("URL cannot be null or empty");
        }

        URL url = new URI(urlString).toURL();

        String protocol = url.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new InvalidUrlFormatException("URL must start with http or https");
        }

        Instant startTime = Instant.now();

        Connection.Response response = Jsoup.connect(urlString)
                .timeout(5000)  // 5 sec
                .execute();

        Instant endTime = Instant.now();

        validateContentType(response.contentType());

        Document document = response.parse();

        String title = document.title();

        Element metaDesc = document.selectFirst("meta[name=description]");
        String description = (metaDesc != null) ? metaDesc.attr("content") : "No meta description found";

        int h1Count = document.select("h1").size();

        Elements imagesMissingAlt = document.select("img:not([alt]), img[alt=]");
        int missingAltCount = imagesMissingAlt.size();

        String bodyText = document.body().text();
        int wordCount = bodyText.isEmpty() ? 0 : bodyText.split("\\s+").length;

        long responseTime = ChronoUnit.MILLIS.between(startTime, endTime);
        int statusCode = response.statusCode();

        return new AuditResponse(statusCode, responseTime, title, description, h1Count, missingAltCount, wordCount);
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !contentType.contains("text/html")) {
            throw new InvalidContentTypeException("Content-Type is not HTML");
        }
    }
}
