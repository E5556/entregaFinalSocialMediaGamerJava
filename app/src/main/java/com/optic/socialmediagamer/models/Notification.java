package com.optic.socialmediagamer.models;

public class Notification {
    private String id;
    private String type;      // "like" | "comment" | "follow"
    private String idFrom;    // quien genera la notificación
    private String idTo;      // quien la recibe
    private String idPost;    // post relacionado (puede ser null en follow)
    private String title;
    private String body;      // texto de la notificación
    private boolean read;
    private long timestamp;

    public Notification() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getIdFrom() { return idFrom; }
    public void setIdFrom(String idFrom) { this.idFrom = idFrom; }
    public String getIdTo() { return idTo; }
    public void setIdTo(String idTo) { this.idTo = idTo; }
    public String getIdPost() { return idPost; }
    public void setIdPost(String idPost) { this.idPost = idPost; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
