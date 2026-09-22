package dev.origins;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.command.CommandManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RacesMod implements ModInitializer {
    public static final String MOD_ID = "origins_races";
    public static final EntityAttribute SCALE = register("scale", new ClampedEntityAttribute("attribute.name.origins_races.scale", 1.0, 0.1, 4.0).setTracked(true));
    public static final EntityAttribute REACH = register("reach", new ClampedEntityAttribute("attribute.name.origins_races.reach", 0.0, 0.0, 8.0).setTracked(true));
    private static final UUID SCALE_ID = UUID.fromString("4b8d2b4a-1d1d-4b9c-8a61-1d7b1b6d0001");
    private static final UUID REACH_ID = UUID.fromString("4b8d2b4a-1d1d-4b9c-8a61-1d7b1b6d0002");
    private static final UUID ARMOR_ID = UUID.fromString("4b8d2b4a-1d1d-4b9c-8a61-1d7b1b6d0003");
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private static EntityAttribute register(String name, EntityAttribute attribute) { return Registry.register(Registries.ATTRIBUTE, id(name), attribute); }
    public static Identifier id(String path) { return new Identifier(MOD_ID, path); }

    @Override public void onInitialize() {
        FabricDefaultAttributeRegistry.register(EntityType.PLAYER, PlayerEntity.createPlayerAttributes().add(SCALE, 1.0).add(REACH, 0.0));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("race")
            .then(CommandManager.literal("choose").then(CommandManager.argument("race", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow(); Race race = Race.parse(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "race"));
                if (race == null) { player.sendMessage(Text.literal("Unknown race."), false); return 0; }
                player.getCommandTags().removeIf(tag -> tag.startsWith("race:")); player.addCommandTag("race:" + race.name().toLowerCase()); applyBase(player, race); player.sendMessage(Text.literal("Race selected: " + race.name()), false); return 1;
            })))
            .then(CommandManager.literal("ability").executes(ctx -> { useAbility(ctx.getSource().getPlayerOrThrow()); return 1; }))));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerManager().getPlayerList().forEach(RacesMod::tick));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> { if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) onAttack(serverPlayer); return ActionResult.PASS; });
    }

    public static Race race(PlayerEntity player) { return player.getCommandTags().stream().filter(t -> t.startsWith("race:")).map(t -> Race.parse(t.substring(6))).findFirst().orElse(null); }
    private static void applyBase(ServerPlayerEntity player, Race race) {
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(race == Race.GIANT ? 30 : race == Race.WARLOCK || race == Race.ARACHNID ? 16 : race == Race.FAIRY ? 12 : 20);
        EntityAttributeInstance scale = player.getAttributeInstance(SCALE), reach = player.getAttributeInstance(REACH); scale.removeModifier(SCALE_ID); reach.removeModifier(REACH_ID);
        player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).removeModifier(ARMOR_ID);
        if (race == Race.GIANT) { scale.addPersistentModifier(new EntityAttributeModifier(SCALE_ID, "Giant scale", .5, EntityAttributeModifier.Operation.MULTIPLY_TOTAL)); reach.addPersistentModifier(new EntityAttributeModifier(REACH_ID, "Giant reach", 2, EntityAttributeModifier.Operation.ADDITION)); }
        if (race == Race.FAIRY) scale.addPersistentModifier(new EntityAttributeModifier(SCALE_ID, "Fairy scale", -.5, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        if (race == Race.GOLEM) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).addPersistentModifier(new EntityAttributeModifier(ARMOR_ID, "Golem armor", 4, EntityAttributeModifier.Operation.ADDITION));
    }
    private static void tick(ServerPlayerEntity p) {
        Race r = race(p); if (r == null) return; int y = p.getBlockY();
        if (r == Race.DWARF && y < 60) p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 220, 0, true, false));
        if (r == Race.DWARF) p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 0, true, false));
        if (r == Race.DRAGONBORN) p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 220, 0, true, false));
        if (r == Race.MERMAID && p.isSubmergedInWater()) { p.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 220, 0, true, false)); p.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 40, 0, true, false)); }
        if (r == Race.FAIRY) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, true, false));
        if (r == Race.MERMAID && !p.isSubmergedInWater()) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, true, false));
        if (r == Race.GIANT && p.age % 30 == 0) p.getHungerManager().addExhaustion(1.0f);
        if (r == Race.DWARF) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false));
        if (r == Race.WITCH) { p.removeStatusEffect(StatusEffects.POISON); p.removeStatusEffect(StatusEffects.WITHER); }
        if (r == Race.CELESTID) { p.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, true, false)); if (p.getWorld().isDay() && p.getWorld().isSkyVisible(p.getBlockPos())) p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0, true, false)); }
        if (r == Race.SHADOWKIN) { p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 40, 0, true, false)); if (!p.getWorld().isDay() || !p.getWorld().isSkyVisible(p.getBlockPos())) p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, true, false)); p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false)); }
        if (r == Race.ARACHNID) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false));
        if (r == Race.IGNIS) p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40, 0, true, false));
        if (r == Race.GOLEM) { p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false)); if (p.isWet()) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, true, false)); }
        if (r == Race.IGNIS && p.isWet()) p.damage(p.getDamageSources().drown(), 1.0f);
    }
    private static void useAbility(ServerPlayerEntity p) {
        Race r = race(p); if (r == null) return;
        long readyAt = COOLDOWNS.getOrDefault(p.getUuid(), 0L);
        if (p.age < readyAt) { p.sendMessage(Text.literal("Ability ready in " + ((readyAt - p.age + 19) / 20) + "s"), true); return; }
        COOLDOWNS.put(p.getUuid(), (long) p.age + (r == Race.DRAGONBORN ? 600 : 200));
        switch (r) { case DRAGONBORN -> dragonBreath(p); case DWARF -> { p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1)); p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0)); }
            case GIANT -> p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(p.getBlockPos()).expand(4), e -> e != p).forEach(e -> { e.damage(p.getDamageSources().playerAttack(p), 6); e.takeKnockback(1.5, p.getX() - e.getX(), p.getZ() - e.getZ()); });
            case WARLOCK -> p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(p.getBlockPos()).expand(12), e -> e != p).stream().findFirst().ifPresent(e -> { e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 200, 1)); e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1)); });
            case WITCH -> { if (p.getRandom().nextBoolean()) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1)); else p.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0)); }
            case MERMAID -> p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(p.getBlockPos()).expand(8), e -> e != p).forEach(e -> { e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 4)); e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 3)); });
            case FAIRY -> p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(p.getBlockPos()).expand(8), e -> e != p).forEach(e -> { if (e instanceof ServerPlayerEntity ally && race(ally) == Race.FAIRY) { e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 0)); e.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 160, 1)); } else e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0)); });
            case CELESTID -> p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(p.getBlockPos()).expand(12), e -> e != p).forEach(e -> { e.damage(p.getDamageSources().magic(), 8); e.setOnFireFor(4); });
            case SHADOWKIN -> { Vec3d direction = p.getRotationVec(1.0f); p.requestTeleport(p.getX() + direction.x * 8, p.getY(), p.getZ() + direction.z * 8); }
            case ARACHNID -> p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(p.getBlockPos()).expand(8), e -> e != p).forEach(e -> e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 3)));
            case IGNIS -> p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(p.getBlockPos()).expand(4), e -> e != p).forEach(e -> { e.damage(p.getDamageSources().playerAttack(p), 8); e.setOnFireFor(8); });
            case GOLEM -> { p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 160, 1)); p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 160, 0)); } }
    }
    private static void dragonBreath(ServerPlayerEntity p) {
        Vec3d direction = p.getRotationVec(1.0f);
        p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(p.getBlockPos()).expand(8), e -> e != p).forEach(e -> {
            Vec3d offset = e.getPos().subtract(p.getPos()).normalize();
            if (direction.dotProduct(offset) > 0.55) { e.damage(p.getDamageSources().playerAttack(p), 6); e.setOnFireFor(6); }
        });
    }
    private static void onAttack(ServerPlayerEntity p) {
        if (race(p) == Race.WARLOCK) p.heal(1.0f);
        if (race(p) == Race.CELESTID && p.getAttacking() != null && p.getAttacking().getGroup() == EntityGroup.UNDEAD) p.getAttacking().damage(p.getDamageSources().playerAttack(p), 4.0f);
    }
}