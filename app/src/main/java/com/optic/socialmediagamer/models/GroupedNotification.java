package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class GroupedNotification {
    private String type;
    private String idPost;
    private String idFrom;
    private String body;
    private boolean read;
    private long timestamp;
    private int count;
    private List<String> ids;

    public GroupedNotification() {
        ids = new ArrayList<>();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIdPost() { return idPost; }
    public void setIdPost(String idPost) { this.idPost = idPost; }

    public String getIdFrom() { return idFrom; }
    public void setIdFrom(String idFrom) { this.idFrom = idFrom; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public List<String> getIds() { return ids; }
    public void setIds(List<String> ids) { this.ids = ids; }
}
