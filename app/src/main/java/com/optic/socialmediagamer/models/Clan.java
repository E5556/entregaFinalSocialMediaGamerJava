package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class Clan {
    private String id;
    private String name;
    private String tag;
    private String description;
    private String idLeader;
    private String imageUrl;
    private List<String> members;
    private long timestamp;

    public Clan() {
        members = new ArrayList<>();
        officers = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIdLeader() { return idLeader; }
    public void setIdLeader(String idLeader) { this.idLeader = idLeader; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    private List<String> officers;

    public List<String> getOfficers() { return officers; }
    public void setOfficers(List<String> officers) { this.officers = officers; }

    private long clanXp;
    public long getClanXp() { return clanXp; }
    public void setClanXp(long clanXp) { this.clanXp = clanXp; }
}
