package dev.serverraces;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;

public final class GUIListener implements Listener {
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RaceMenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0 || event.getRawSlot() >= Race.values().length) return;
        Race race = Race.values()[event.getRawSlot()];
        ServerRacesPlugin plugin = ServerRacesPlugin.get();
        plugin.races().setRace(player, race);
        plugin.attributes().apply(player, race);
        player.sendMessage("Selected race: " + race.displayName());
        player.closeInventory();
    }

    @EventHandler public void drag(InventoryDragEvent event) { if (event.getInventory().getHolder() instanceof RaceMenuHolder) event.setCancelled(true); }
    @EventHandler public void close(InventoryCloseEvent event) { if (event.getInventory().getHolder() instanceof RaceMenuHolder) event.getPlayer().sendActionBar(net.kyori.adventure.text.Component.text("Race selection closed")); }
}