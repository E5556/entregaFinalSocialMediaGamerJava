package com.optic.socialmediagamer.models;

public class GameRating {
    private String id;
    private String idFrom;
    private String idTo;
    private String game;
    private float communication;
    private float skill;
    private float attitude;
    private long timestamp;

    public GameRating() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdFrom() { return idFrom; }
    public void setIdFrom(String idFrom) { this.idFrom = idFrom; }
    public String getIdTo() { return idTo; }
    public void setIdTo(String idTo) { this.idTo = idTo; }
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    public float getCommunication() { return communication; }
    public void setCommunication(float communication) { this.communication = communication; }
    public float getSkill() { return skill; }
    public void setSkill(float skill) { this.skill = skill; }
    public float getAttitude() { return attitude; }
    public void setAttitude(float attitude) { this.attitude = attitude; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
