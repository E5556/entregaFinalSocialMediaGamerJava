package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class Poll {
    private String postId;
    private String optionA;
    private String optionB;
    private List<String> votesA;
    private List<String> votesB;

    public Poll() {
        votesA = new ArrayList<>();
        votesB = new ArrayList<>();
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public List<String> getVotesA() { return votesA != null ? votesA : new ArrayList<>(); }
    public void setVotesA(List<String> votesA) { this.votesA = votesA; }

    public List<String> getVotesB() { return votesB != null ? votesB : new ArrayList<>(); }
    public void setVotesB(List<String> votesB) { this.votesB = votesB; }
}
