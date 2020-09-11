package com.github.hcsp;

import java.time.Instant;

public class News {
    private Integer id;
    private String url;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public News(){
    }

    public News(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
    }

    public News(News old) {
        this.id = old.id;
        this.title = old.getTitle();
        this.url = old.getUrl();
        this.content = old.getContent();
        this.createdAt = old.getCreatedAt();
        this.updatedAt = old.getUpdatedAt();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
