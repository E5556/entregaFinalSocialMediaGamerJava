package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class Tournament {
    private String id;
    private String idUser;
    private String game;
    private String format;
    private long dateTimestamp;
    private int maxPlayers;
    private List<String> players;
    private String status; // "open" | "started" | "finished"
    private List<String> bracket; // "uid1|uid2" pairs
    private String winner;
    private String winnerUsername;
    private long timestamp;

    public Tournament() {
        players = new ArrayList<>();
        bracket = new ArrayList<>();
        status  = "open";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public long getDateTimestamp() { return dateTimestamp; }
    public void setDateTimestamp(long dateTimestamp) { this.dateTimestamp = dateTimestamp; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getBracket() { return bracket; }
    public void setBracket(List<String> bracket) { this.bracket = bracket; }
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
