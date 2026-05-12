package com.optic.socialmediagamer.models;

public class Badge {

    public static final String PRIMER_POST = "PRIMER_POST";
    public static final String VETERANO = "VETERANO";
    public static final String POPULAR = "POPULAR";
    public static final String SOCIAL = "SOCIAL";
    public static final String PC_FAN = "PC_FAN";
    public static final String PS_FAN = "PS_FAN";
    public static final String XBOX_FAN = "XBOX_FAN";
    public static final String NINTENDO_FAN = "NINTENDO_FAN";
    public static final String RACHA_7 = "RACHA_7";
    public static final String RACHA_30 = "RACHA_30";
    public static final String RACHA_100 = "RACHA_100";

    private String id;
    private String idUser;
    private String badgeKey;
    private long timestamp;

    public Badge() {}

    public Badge(String idUser, String badgeKey, long timestamp) {
        this.idUser = idUser;
        this.badgeKey = badgeKey;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }

    public String getBadgeKey() { return badgeKey; }
    public void setBadgeKey(String badgeKey) { this.badgeKey = badgeKey; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getEmoji() {
        switch (badgeKey != null ? badgeKey : "") {
            case PRIMER_POST:   return "🎮";
            case VETERANO:      return "⚔️";
            case POPULAR:       return "🌟";
            case SOCIAL:        return "🤝";
            case PC_FAN:        return "💻";
            case PS_FAN:        return "🎮";
            case XBOX_FAN:      return "🟢";
            case NINTENDO_FAN:  return "❤️";
            case RACHA_7:       return "🔥";
            case RACHA_30:      return "💥";
            case RACHA_100:     return "👑";
            default:            return "🏆";
        }
    }

    public String getTitle() {
        switch (badgeKey != null ? badgeKey : "") {
            case PRIMER_POST:   return "Primer Post";
            case VETERANO:      return "Veterano";
            case POPULAR:       return "Popular";
            case SOCIAL:        return "Social";
            case PC_FAN:        return "PC Master";
            case PS_FAN:        return "PS Fan";
            case XBOX_FAN:      return "Xbox Fan";
            case NINTENDO_FAN:  return "Nintendo Fan";
            case RACHA_7:       return "Racha 7 días";
            case RACHA_30:      return "Racha 30 días";
            case RACHA_100:     return "Racha 100 días";
            default:            return "Logro";
        }
    }
}