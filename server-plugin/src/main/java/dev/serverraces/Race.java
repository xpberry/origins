package dev.serverraces;

import java.util.Locale;

public enum Race {
    DRAGONBORN("Dragonborn"), DWARF("Dwarf"), GIANT("Giant"), WARLOCK("Warlock"),
    WITCH("Witch"), MERMAID("Mermaid"), FAIRY("Fairy"), CELESTID("Celestid"),
    SHADOWKIN("Shadowkin"), ARACHNID("Arachnid"), IGNIS("Ignis"), GOLEM("Golem");

    private final String displayName;
    Race(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    public static Race parse(String value) {
        if (value == null) return null;
        for (Race race : values()) if (race.name().equals(value.toUpperCase(Locale.ROOT))) return race;
        return null;
    }
}