package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class LFGPost {

    private String id;
    private String idUser;
    private String game;
    private String platform;     // PC, PS4, XBOX, Nintendo
    private String description;
    private int slotsTotal;
    private List<String> players; // UIDs de jugadores que se unieron
    private String schedule;      // Mañana, Tarde, Noche, Madrugada, Cualquier hora
    private long timestamp;

    public LFGPost() {
        players = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }

    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getSlotsTotal() { return slotsTotal; }
    public void setSlotsTotal(int slotsTotal) { this.slotsTotal = slotsTotal; }

    public List<String> getPlayers() { return players != null ? players : new ArrayList<>(); }
    public void setPlayers(List<String> players) { this.players = players; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
