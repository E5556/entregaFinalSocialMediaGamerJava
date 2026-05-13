package com.optic.socialmediagamer.models;

public class Season {
    private String id;         // e.g. "2026-05"
    private String label;      // e.g. "Mayo 2026"
    private String championId;
    private String championUsername;
    private long seasonXp;
    private long closedAt;

    public Season() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getChampionId() { return championId; }
    public void setChampionId(String championId) { this.championId = championId; }
    public String getChampionUsername() { return championUsername; }
    public void setChampionUsername(String championUsername) { this.championUsername = championUsername; }
    public long getSeasonXp() { return seasonXp; }
    public void setSeasonXp(long seasonXp) { this.seasonXp = seasonXp; }
    public long getClosedAt() { return closedAt; }
    public void setClosedAt(long closedAt) { this.closedAt = closedAt; }
}
