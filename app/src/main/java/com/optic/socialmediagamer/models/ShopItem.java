package com.optic.socialmediagamer.models;

import java.util.ArrayList;
import java.util.List;

public class ShopItem {

    public static final String TYPE_FRAME      = "FRAME";
    public static final String TYPE_TITLE      = "TITLE";
    public static final String TYPE_BACKGROUND = "BACKGROUND";

    public static final String FRAME_NEON    = "frame_neon";
    public static final String FRAME_GOLD    = "frame_gold";
    public static final String FRAME_PURPLE  = "frame_purple";
    public static final String TITLE_PRO     = "title_pro";
    public static final String TITLE_SNIPER  = "title_sniper";
    public static final String TITLE_LEGEND  = "title_legend";
    public static final String BG_FIRE       = "bg_fire";
    public static final String BG_GALAXY     = "bg_galaxy";

    private String id;
    private String name;
    private String description;
    private long xpCost;
    private String type;
    private String emoji;
    private String value; // hex color for frames/bgs, display text for titles

    public ShopItem() {}

    public ShopItem(String id, String name, String description, long xpCost, String type, String emoji, String value) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.xpCost = xpCost;
        this.type = type;
        this.emoji = emoji;
        this.value = value;
    }

    public static List<ShopItem> getCatalog() {
        List<ShopItem> items = new ArrayList<>();
        items.add(new ShopItem(FRAME_NEON,   "Marco Neón",       "Borde cyan brillante en tu foto", 300,  TYPE_FRAME,      "💠", "#00F0FF"));
        items.add(new ShopItem(FRAME_GOLD,   "Marco Dorado",     "Borde dorado premium",             500,  TYPE_FRAME,      "🥇", "#FFD700"));
        items.add(new ShopItem(FRAME_PURPLE, "Marco Élite",      "Borde púrpura de élite",           700,  TYPE_FRAME,      "💜", "#BF00FF"));
        items.add(new ShopItem(TITLE_PRO,    "Pro Gamer",        "Título visible en tus posts",      200,  TYPE_TITLE,      "🎮", "Pro Gamer"));
        items.add(new ShopItem(TITLE_SNIPER, "Sniper de Élite",  "Título para los más precisos",     400,  TYPE_TITLE,      "🎯", "Sniper de Élite"));
        items.add(new ShopItem(TITLE_LEGEND, "El Inmortal",      "Título de leyenda",                600,  TYPE_TITLE,      "👑", "El Inmortal"));
        items.add(new ShopItem(BG_FIRE,      "Fondo Fire",       "Fondo rojo intenso en tu perfil",  250,  TYPE_BACKGROUND, "🔥", "#CC2200"));
        items.add(new ShopItem(BG_GALAXY,    "Fondo Galaxy",     "Fondo galaxia en tu perfil",       350,  TYPE_BACKGROUND, "🌌", "#001A66"));
        return items;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getXpCost() { return xpCost; }
    public void setXpCost(long xpCost) { this.xpCost = xpCost; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
