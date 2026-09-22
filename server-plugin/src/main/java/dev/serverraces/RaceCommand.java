package dev.serverraces;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.List;

public final class RaceCommand implements CommandExecutor, TabCompleter {
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Only players can use this command."); return true; }
        if (command.getName().equalsIgnoreCase("ability") || (args.length > 0 && args[0].equalsIgnoreCase("ability"))) { ServerRacesPlugin.get().abilities().activate(player); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) { RaceGui.open(player); return true; }
        if (args[0].equalsIgnoreCase("info")) { Race race = ServerRacesPlugin.get().races().getRace(player); player.sendMessage(race == null ? "No race selected." : "Selected race: " + race.displayName()); return true; }
        if (args[0].equalsIgnoreCase("choose") && args.length > 1) { Race race = Race.parse(args[1]); if (race == null) player.sendMessage("Unknown race."); else { ServerRacesPlugin.get().races().setRace(player, race); ServerRacesPlugin.get().attributes().apply(player, race); player.sendMessage("Selected race: " + race.displayName()); } return true; }
        player.sendMessage("Usage: /race gui, /race choose <race>, /race ability, /race info"); return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("gui", "choose", "ability", "info").stream().filter(value -> value.startsWith(args[0].toLowerCase())).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("choose")) return java.util.Arrays.stream(Race.values()).map(race -> race.name().toLowerCase()).filter(value -> value.startsWith(args[1].toLowerCase())).toList();
        return List.of();
    }
}