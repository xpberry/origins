package dev.serverraces;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;

public final class RaceManager {
    public static final NamespacedKey SELECTED_RACE = new NamespacedKey("serverraces", "selected_race");
    private final Map<UUID, EnumMap<Race, Long>> cooldowns = new HashMap<>();

    /** Reads the player's persisted race, or returns null when no race is selected. */
    public Race getRace(Player player) {
        String value = player.getPersistentDataContainer().get(SELECTED_RACE, PersistentDataType.STRING);
        return Race.parse(value);
    }

    /** Persists a race choice on the player entity. */
    public void setRace(Player player, Race race) {
        player.getPersistentDataContainer().set(SELECTED_RACE, PersistentDataType.STRING, race.name());
    }

    public boolean ready(Player player, Race race, long now) { return now >= cooldowns.getOrDefault(player.getUniqueId(), new EnumMap<>(Race.class)).getOrDefault(race, 0L); }
    public long remaining(Player player, Race race, long now) { return Math.max(0, cooldowns.getOrDefault(player.getUniqueId(), new EnumMap<>(Race.class)).getOrDefault(race, 0L) - now); }
    public void startCooldown(Player player, Race race, long now, long ticks) { cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(Race.class)).put(race, now + ticks); }
    public void clear(Player player) { cooldowns.remove(player.getUniqueId()); }
}