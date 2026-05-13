package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class WeeklyChallengeEntry {
    private String id;
    private String idUser;
    private String username;
    private String evidence;
    private List<String> votes;
    private long timestamp;

    public WeeklyChallengeEntry() { votes = new ArrayList<>(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public List<String> getVotes() { return votes; }
    public void setVotes(List<String> votes) { this.votes = votes; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
