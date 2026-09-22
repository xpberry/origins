package dev.serverraces;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RaceGui {
    private RaceGui() { }

    public static void open(Player player) {
        RaceMenuHolder holder = new RaceMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("ServerRaces: Choose a Race", NamedTextColor.GOLD));
        holder.setInventory(inventory);
        Race selected = ServerRacesPlugin.get().races().getRace(player);
        Race[] races = Race.values();
        for (int index = 0; index < races.length; index++) inventory.setItem(index, icon(races[index], races[index] == selected));
        player.openInventory(inventory);
    }

    private static ItemStack icon(Race race, boolean selected) {
        Material material = switch (race) {
            case DRAGONBORN, IGNIS -> Material.BLAZE_POWDER;
            case DWARF, GOLEM -> Material.IRON_INGOT;
            case GIANT -> Material.OBSIDIAN;
            case WARLOCK, SHADOWKIN -> Material.ENDER_PEARL;
            case WITCH -> Material.BREWING_STAND;
            case MERMAID -> Material.HEART_OF_THE_SEA;
            case FAIRY -> Material.FEATHER;
            case CELESTID -> Material.GLOWSTONE_DUST;
            case ARACHNID -> Material.STRING;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text((selected ? "Selected: " : "") + race.displayName(), selected ? NamedTextColor.GREEN : NamedTextColor.WHITE));
        meta.lore(java.util.List.of(Component.text("Click to select", NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }
}