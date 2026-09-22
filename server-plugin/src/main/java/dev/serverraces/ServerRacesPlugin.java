package dev.serverraces;

import org.bukkit.plugin.java.JavaPlugin;

public final class ServerRacesPlugin extends JavaPlugin {
    private static ServerRacesPlugin instance;
    private RaceManager races;
    private AttributeManager attributes;
    private AbilityListener abilities;
    private PassiveListener passives;

    @Override public void onEnable() {
        instance = this; races = new RaceManager(); attributes = new AttributeManager(); abilities = new AbilityListener(this);
        RaceCommand command = new RaceCommand(); getCommand("race").setExecutor(command); getCommand("race").setTabCompleter(command); getCommand("ability").setExecutor(command);
        passives = new PassiveListener(this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this); getServer().getPluginManager().registerEvents(passives, this); getServer().getPluginManager().registerEvents(abilities, this);
        getServer().getScheduler().runTaskTimer(this, passives::tick, 1, 1);
    }
    @Override public void onDisable() { if (races != null) getServer().getOnlinePlayers().forEach(races::clear); }
    public static ServerRacesPlugin get() { return instance; }
    public RaceManager races() { return races; }
    public AttributeManager attributes() { return attributes; }
    public AbilityListener abilities() { return abilities; }
}