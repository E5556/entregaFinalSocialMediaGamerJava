package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class Challenge {
    private String id;
    private String idChallenger;
    private String idChallenged;
    private String description;
    private String status; // "pending" | "accepted" | "rejected" | "voting" | "finished"
    private String evidenceChallenger;
    private String evidenceChallenged;
    private List<String> votesChallenger;
    private List<String> votesChallenged;
    private String winner; // uid
    private String winnerUsername;
    private long timestamp;

    public Challenge() {
        votesChallenger = new ArrayList<>();
        votesChallenged = new ArrayList<>();
        status = "pending";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdChallenger() { return idChallenger; }
    public void setIdChallenger(String v) { this.idChallenger = v; }
    public String getIdChallenged() { return idChallenged; }
    public void setIdChallenged(String v) { this.idChallenged = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getEvidenceChallenger() { return evidenceChallenger; }
    public void setEvidenceChallenger(String v) { this.evidenceChallenger = v; }
    public String getEvidenceChallenged() { return evidenceChallenged; }
    public void setEvidenceChallenged(String v) { this.evidenceChallenged = v; }
    public List<String> getVotesChallenger() { return votesChallenger; }
    public void setVotesChallenger(List<String> v) { this.votesChallenger = v; }
    public List<String> getVotesChallenged() { return votesChallenged; }
    public void setVotesChallenged(List<String> v) { this.votesChallenged = v; }
    public String getWinner() { return winner; }
    public void setWinner(String v) { this.winner = v; }
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String v) { this.winnerUsername = v; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long v) { this.timestamp = v; }
}
