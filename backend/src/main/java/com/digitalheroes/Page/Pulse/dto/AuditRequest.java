package com.digitalheroes.Page.Pulse.dto;

public class AuditRequest {

    private String url;

    public AuditRequest() {}

    public AuditRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
