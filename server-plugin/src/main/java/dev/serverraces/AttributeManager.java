package dev.serverraces;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public final class AttributeManager {
    private final String scale = "serverraces:race_scale";
    private final String health = "serverraces:race_health";
    private final String reach = "serverraces:race_reach";
    private final String speed = "serverraces:race_speed";
    private final String armor = "serverraces:race_armor";
    private final String knockback = "serverraces:race_knockback";

    /** Applies the selected race's attribute modifiers and restores the player's health cap. */
    public void apply(Player player, Race race) {
        removeAll(player);
        modifier(player, Attribute.GENERIC_MAX_HEALTH, health, race == Race.GIANT ? 10 : race == Race.WARLOCK || race == Race.ARACHNID ? -4 : race == Race.FAIRY ? -8 : 0, AttributeModifier.Operation.ADD_NUMBER);
        if (race == Race.GIANT) {
            modifier(player, Attribute.GENERIC_SCALE, scale, .35, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            modifier(player, Attribute.PLAYER_BLOCK_INTERACTION_RANGE, reach, 1.5, AttributeModifier.Operation.ADD_NUMBER);
        }
        if (race == Race.FAIRY) modifier(player, Attribute.GENERIC_SCALE, scale, -.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        if (race == Race.DWARF) modifier(player, Attribute.GENERIC_MOVEMENT_SPEED, speed, -.1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        if (race == Race.GOLEM) {
            modifier(player, Attribute.GENERIC_ARMOR, armor, 4, AttributeModifier.Operation.ADD_NUMBER);
            modifier(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockback, 1, AttributeModifier.Operation.ADD_NUMBER);
        }
        if (race == Race.GOLEM) modifier(player, Attribute.GENERIC_MOVEMENT_SPEED, speed, -.2, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        clampHealth(player);
    }

    private void modifier(Player player, Attribute attribute, String key, double amount, AttributeModifier.Operation operation) {
        if (amount == 0) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.addModifier(new AttributeModifier(key, amount, operation));
    }

    private void removeAll(Player player) {
        for (Attribute attribute : new Attribute[]{Attribute.GENERIC_MAX_HEALTH, Attribute.GENERIC_SCALE, Attribute.PLAYER_BLOCK_INTERACTION_RANGE, Attribute.GENERIC_MOVEMENT_SPEED, Attribute.GENERIC_ARMOR, Attribute.GENERIC_KNOCKBACK_RESISTANCE}) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) instance.getModifiers().stream().filter(modifier -> modifier.getName().startsWith("serverraces:")).toList().forEach(instance::removeModifier);
        }
    }

    private void clampHealth(Player player) {
        double maximum = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        if (player.getHealth() > maximum) player.setHealth(maximum);
    }
}