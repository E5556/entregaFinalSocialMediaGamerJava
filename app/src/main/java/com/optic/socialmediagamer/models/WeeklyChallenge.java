package com.optic.socialmediagamer.models;

public class WeeklyChallenge {
    private String id;
    private String title;
    private String description;
    private long weekStart;
    private String status; // "active" | "voting" | "finished"
    private String winnerId;
    private String winnerUsername;

    public WeeklyChallenge() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getWeekStart() { return weekStart; }
    public void setWeekStart(long weekStart) { this.weekStart = weekStart; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
}
