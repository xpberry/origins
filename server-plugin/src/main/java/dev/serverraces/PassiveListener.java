package dev.serverraces;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PassiveListener implements Listener {
    private final Map<UUID, Integer> landTicks = new HashMap<>();
    private final ServerRacesPlugin plugin;
    public PassiveListener(ServerRacesPlugin plugin) { this.plugin = plugin; }

    @EventHandler public void join(PlayerJoinEvent event) { Race race = plugin.races().getRace(event.getPlayer()); if (race != null) plugin.attributes().apply(event.getPlayer(), race); }

    public void tick() {
        long now = System.currentTimeMillis() / 50;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Race race = plugin.races().getRace(player);
            if (race == null) continue;
            applyPassive(player, race);
            long remaining = plugin.races().remaining(player, race, now);
            if (remaining > 0) player.sendActionBar(race.displayName() + " ability: " + ((remaining + 19) / 20) + "s");
        }
    }

    private void applyPassive(Player player, Race race) {
        boolean inWater = player.isInWater();
        switch (race) {
            case DRAGONBORN -> effect(player, PotionEffectType.FIRE_RESISTANCE, 40, 0);
            case DWARF -> { if (player.getLocation().getBlockY() < 60) { effect(player, PotionEffectType.NIGHT_VISION, 40, 0); effect(player, PotionEffectType.HASTE, 40, 0); } }
            case MERMAID -> { if (inWater || player.getWorld().hasStorm() && player.getLocation().getBlock().getType() == Material.AIR) { effect(player, PotionEffectType.WATER_BREATHING, 40, 0); effect(player, PotionEffectType.DOLPHINS_GRACE, 40, 0); landTicks.remove(player.getUniqueId()); } else { int ticks = landTicks.merge(player.getUniqueId(), 1, Integer::sum); if (ticks > 600 && ticks % 40 == 0) player.damage(1); } }
            case FAIRY -> effect(player, PotionEffectType.SLOW_FALLING, 40, 0);
            case CELESTID -> { effect(player, PotionEffectType.GLOWING, 40, 0); }
            case SHADOWKIN -> { if (player.getLocation().getBlock().getLightFromBlocks() <= 3) { effect(player, PotionEffectType.INVISIBILITY, 40, 0); effect(player, PotionEffectType.SPEED, 40, 1); } }
            case ARACHNID -> { }
            case IGNIS -> { effect(player, PotionEffectType.FIRE_RESISTANCE, 40, 0); if ((player.getWorld().hasStorm() && player.getLocation().getBlock().getType() == Material.AIR || inWater) && player.getTicksLived() % 40 == 0) player.damage(1); }
            case GOLEM -> { if (inWater) effect(player, PotionEffectType.SLOWNESS, 40, 1); }
            default -> { }
        }
        if (race == Race.GIANT && player.getTicksLived() % 20 == 0) player.setExhaustion(player.getExhaustion() + .05f);
    }

    private void effect(Player player, PotionEffectType type, int duration, int amplifier) { player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false)); }
}