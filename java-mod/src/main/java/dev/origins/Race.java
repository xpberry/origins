package dev.origins;

public enum Race {
    DRAGONBORN, DWARF, GIANT, WARLOCK, WITCH, MERMAID, FAIRY,
    CELESTID, SHADOWKIN, ARACHNID, IGNIS, GOLEM;

    public static Race parse(String value) {
        return value == null ? null : switch (value.toLowerCase()) {
            case "dragonborn" -> DRAGONBORN; case "dwarf" -> DWARF; case "giant" -> GIANT;
            case "warlock" -> WARLOCK; case "witch" -> WITCH; case "mermaid" -> MERMAID;
            case "fairy" -> FAIRY; case "celestid" -> CELESTID; case "shadowkin" -> SHADOWKIN;
            case "arachnid" -> ARACHNID; case "ignis" -> IGNIS; case "golem" -> GOLEM; default -> null;
        };
    }
}