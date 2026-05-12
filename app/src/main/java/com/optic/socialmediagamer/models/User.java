package com.optic.socialmediagamer.models;

import com.google.firebase.firestore.PropertyName;

public class User {

    private String id;
    private String email;
    private String username;
    private String phone;
    private String imageProfile;
    private String imageCover;
    private String bio;
    private long xp;
    private String twitchUsername;
    private String nowPlaying;
    private long timestamp;

    public User() {

    }

    public User(String id, String email, String username, String phone, long timestamp, String imageProfile, String imageCover) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.phone = phone;
        this.timestamp = timestamp;
        this.imageProfile = imageProfile;
        this.imageCover = imageCover;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = xp; }

    public String getTwitchUsername() { return twitchUsername; }
    public void setTwitchUsername(String twitchUsername) { this.twitchUsername = twitchUsername; }

    public String getNowPlaying() { return nowPlaying; }
    public void setNowPlaying(String nowPlaying) { this.nowPlaying = nowPlaying; }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @PropertyName("image_profile")
    public String getImageProfile() {
        return imageProfile;
    }

    @PropertyName("image_profile")
    public void setImageProfile(String imageProfile) {
        this.imageProfile = imageProfile;
    }

    @PropertyName("image_cover")
    public String getImageCover() {
        return imageCover;
    }

    @PropertyName("image_cover")
    public void setImageCover(String imageCover) {
        this.imageCover = imageCover;
    }
}
