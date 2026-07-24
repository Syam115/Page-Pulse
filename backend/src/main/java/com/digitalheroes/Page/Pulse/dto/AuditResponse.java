package com.digitalheroes.Page.Pulse.dto;

public class AuditResponse {

    private int statusCode;

    private long responseTime;

    private String pageTitle;

    private String metaDescription;

    private int h1Count;

    private int missingAltCount;

    private int wordCount;

    public AuditResponse() {}

    public AuditResponse(int statusCode, long responseTime, String pageTitle, String metaDescription, int h1Count, int missingAltCount, int wordCount) {
        this.statusCode = statusCode;
        this.responseTime = responseTime;
        this.pageTitle = pageTitle;
        this.metaDescription = metaDescription;
        this.h1Count = h1Count;
        this.missingAltCount = missingAltCount;
        this.wordCount = wordCount;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public int getH1Count() {
        return h1Count;
    }

    public void setH1Count(int h1Count) {
        this.h1Count = h1Count;
    }

    public int getMissingAltCount() {
        return missingAltCount;
    }

    public void setMissingAltCount(int missingAltCount) {
        this.missingAltCount = missingAltCount;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }
}
