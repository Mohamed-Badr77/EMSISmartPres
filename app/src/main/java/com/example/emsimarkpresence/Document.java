package com.example.emsimarkpresence;

import java.util.Date;

public class Document {
    private String id;
    private String name;
    private String type; // pdf, docx, jpg, etc.
    private String downloadUrl;
    private long size;
    private Date uploadDate;
    private String uploaderId;

    // Constructors, getters and setters
    public Document() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public Date getUploadDate() { return uploadDate; }
    public void setUploadDate(Date uploadDate) { this.uploadDate = uploadDate; }
    public String getUploaderId() { return uploaderId; }
    public void setUploaderId(String uploaderId) { this.uploaderId = uploaderId; }
}