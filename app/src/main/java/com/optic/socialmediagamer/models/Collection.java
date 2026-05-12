package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class Collection {

    private String id;
    private String idUser;
    private String name;
    private List<String> postIds;
    private long timestamp;

    public Collection() {
        postIds = new ArrayList<>();
    }

    public Collection(String idUser, String name, long timestamp) {
        this.idUser = idUser;
        this.name = name;
        this.timestamp = timestamp;
        this.postIds = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getPostIds() { return postIds != null ? postIds : new ArrayList<>(); }
    public void setPostIds(List<String> postIds) { this.postIds = postIds; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
