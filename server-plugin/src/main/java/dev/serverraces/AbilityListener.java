package dev.serverraces;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

public final class AbilityListener implements Listener {
    private static final String WEB_KEY = "web_trap";
    private final ServerRacesPlugin plugin;
    public AbilityListener(ServerRacesPlugin plugin) { this.plugin = plugin; }

    @EventHandler public void interact(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking() || event.getHand() == null || event.getHand().name().equals("OFF_HAND")) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() != null && event.getItem().getType() != Material.AIR) return;
        event.setCancelled(true);
        activate(event.getPlayer());
    }

    @EventHandler public void combat(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            Race race = plugin.races().getRace(victim);
            if (race == Race.DRAGONBORN && event.getCause() == EntityDamageEvent.DamageCause.FREEZE) event.setDamage(event.getDamage() * 1.5);
            if (race == Race.CELESTID && (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || event.getCause() == EntityDamageEvent.DamageCause.LAVA)) event.setDamage(event.getDamage() * 1.5);
            if (race == Race.ARACHNID && event.getDamager() instanceof Player attacker && attacker.getInventory().getItemInMainHand().containsEnchantment(Enchantment.BANE_OF_ARTHROPODS)) event.setDamage(event.getDamage() * 2);
        }
        Player attacker = attacker(event.getDamager());
        if (attacker == null) return;
        Race race = plugin.races().getRace(attacker);
        if (race == Race.WARLOCK && (event.getDamager() instanceof Projectile || event.getCause() == EntityDamageEvent.DamageCause.MAGIC)) attacker.setHealth(Math.min(attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), attacker.getHealth() + event.getFinalDamage() * .10));
        if (race == Race.IGNIS) event.setDamage(event.getDamage() + 2);
        if (race == Race.GOLEM && attacker.hasMetadata("serverraces_overcharge")) event.setDamage(event.getDamage() * 2);
        if (race == Race.CELESTID && event.getEntity() instanceof LivingEntity undead && undead.getType().name().matches(".*(ZOMBIE|SKELETON|PHANTOM|WITHER|DROWNED|HUSK|STRAY).*")) event.setDamage(event.getDamage() * 1.15);
        if (race == Race.SHADOWKIN) attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @EventHandler public void damage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Race race = plugin.races().getRace(player);
        if (race == Race.CELESTID && event.getCause() == EntityDamageEvent.DamageCause.FALL) event.setCancelled(true);
        if (race == Race.IGNIS && (event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)) event.setCancelled(true);
        if (race == Race.IGNIS && (event.getCause() == EntityDamageEvent.DamageCause.DROWNING || event.getCause() == EntityDamageEvent.DamageCause.CONTACT)) event.setDamage(1);
        if (race == Race.WITCH && event.getCause() == EntityDamageEvent.DamageCause.MAGIC) event.setCancelled(true);
        if (race == Race.WITCH && player.hasPotionEffect(PotionEffectType.POISON)) player.removePotionEffect(PotionEffectType.POISON);
        if (race == Race.WITCH && player.hasPotionEffect(PotionEffectType.WITHER)) player.removePotionEffect(PotionEffectType.WITHER);
    }

    @EventHandler public void potionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player) || plugin.races().getRace(player) != Race.WITCH) return;
        PotionEffectType type = event.getModifiedType();
        if (type.equals(PotionEffectType.POISON) || type.equals(PotionEffectType.WITHER)) event.setCancelled(true);
    }

    @EventHandler public void move(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.races().getRace(player) != Race.ARACHNID || !player.isSneaking()) return;
        if (player.getLocation().getBlock().getType().isSolid() && player.getVelocity().getY() < 0) player.setVelocity(player.getVelocity().setY(.2));
    }

    public void activate(Player player) {
        Race race = plugin.races().getRace(player);
        if (race == null) { player.sendActionBar("Choose a race with /race gui"); return; }
        long now = System.currentTimeMillis() / 50;
        long cooldown = switch (race) { case DRAGONBORN, GIANT, IGNIS -> 600; case DWARF -> 900; case WARLOCK, SHADOWKIN -> 400; case WITCH -> 500; case MERMAID -> 800; case FAIRY -> 700; case CELESTID -> 900; case ARACHNID -> 500; case GOLEM -> 1200; };
        if (!plugin.races().ready(player, race, now)) { player.sendActionBar("Cooldown: " + ((plugin.races().remaining(player, race, now) + 19) / 20) + "s"); return; }
        plugin.races().startCooldown(player, race, now, cooldown);
        switch (race) {
            case DRAGONBORN -> dragonBreath(player);
            case DWARF -> stoneStance(player);
            case GIANT -> groundSlam(player);
            case WARLOCK -> eldritchBlast(player);
            case WITCH -> brewingAlchemy(player);
            case MERMAID -> sirenSong(player);
            case FAIRY -> pixieDust(player);
            case CELESTID -> divineJudgement(player);
            case SHADOWKIN -> voidVeil(player);
            case ARACHNID -> webTrap(player);
            case IGNIS -> infernalBurst(player);
            case GOLEM -> overcharge(player);
        }
    }

    private void dragonBreath(Player player) {
        cone(player, 8, entity -> { entity.damage(6, player); entity.setFireTicks(120); });
        Vector direction = player.getLocation().getDirection().normalize();
        Vector right = basisRight(direction);
        Vector up = right.clone().crossProduct(direction).normalize();
        new BukkitRunnable() { int step;
            @Override public void run() {
                if (!player.isOnline() || step++ >= 5) { cancel(); return; }
                double distance = 1.2 + step * 1.25;
                double radius = .18 + step * .28;
                Location center = player.getEyeLocation().add(direction.clone().multiply(distance));
                for (int sample = 0; sample < 6; sample++) {
                    double angle = sample * Math.PI / 3 + step * .7;
                    Location point = center.clone().add(right.clone().multiply(Math.cos(angle) * radius)).add(up.clone().multiply(Math.sin(angle) * radius));
                    player.getWorld().spawnParticle(Particle.FLAME, point, 1, 0, 0, 0, 0);
                    if (sample % 2 == 0) player.getWorld().spawnParticle(Particle.SMOKE, point, 1, 0, 0, 0, .01);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    private void stoneStance(Player player) { player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1, true, false)); player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, true, false)); player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).addModifier(new AttributeModifier("serverraces:stone_stance", 1, AttributeModifier.Operation.ADD_NUMBER)); Bukkit.getScheduler().runTaskLater(plugin, () -> removeModifier(player, "serverraces:stone_stance"), 200); }
    private void groundSlam(Player player) {
        for (Entity entity : player.getNearbyEntities(5, 2, 5)) if (entity instanceof LivingEntity living) { living.damage(5, player); living.setVelocity(living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.4).setY(.7)); }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, .7f);
        org.bukkit.block.data.BlockData blockData = player.getLocation().getBlock().getRelative(0, -1, 0).getBlockData();
        new BukkitRunnable() { int step;
            @Override public void run() {
                if (!player.isOnline() || step++ >= 4) { cancel(); return; }
                for (int ring = 0; ring < 2; ring++) {
                    double radius = step * 1.05 + ring * .55;
                    for (int sample = 0; sample < 5; sample++) {
                        double angle = sample * Math.PI * 2 / 5 + step * .3;
                        Location point = player.getLocation().clone().add(Math.cos(angle) * radius, .08, Math.sin(angle) * radius);
                        player.getWorld().spawnParticle(Particle.BLOCK, point, 1, 0, 0, 0, 0, blockData);
                    }
                }
                if (step == 1) player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation().add(0, .2, 0), 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    private void eldritchBlast(Player player) {
        WitherSkull skull = player.launchProjectile(WitherSkull.class);
        skull.setVelocity(player.getLocation().getDirection().multiply(1.5));
        skull.setMetadata("serverraces_eldritch", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        Vector direction = player.getLocation().getDirection().normalize();
        Vector right = basisRight(direction);
        Vector up = right.clone().crossProduct(direction).normalize();
        new BukkitRunnable() { int step;
            @Override public void run() {
                if (!player.isOnline() || step++ >= 6) { cancel(); return; }
                double distance = step * 1.8;
                Location center = player.getEyeLocation().add(direction.clone().multiply(distance));
                for (int helix = 0; helix < 2; helix++) {
                    double angle = step * .9 + helix * Math.PI;
                    Location point = center.clone().add(right.clone().multiply(Math.cos(angle) * .22)).add(up.clone().multiply(Math.sin(angle) * .22));
                    player.getWorld().spawnParticle(Particle.WITCH, point, 1, 0, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.SQUID_INK, point, 1, 0, 0, 0, .01);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    private void brewingAlchemy(Player player) { ItemStack item = new ItemStack(Material.SPLASH_POTION); PotionMeta meta = (PotionMeta) item.getItemMeta(); boolean buff = ThreadLocalRandom.current().nextBoolean(); meta.addCustomEffect(new PotionEffect(buff ? PotionEffectType.SPEED : PotionEffectType.POISON, 120, 1), true); item.setItemMeta(meta); ThrownPotion potion = player.launchProjectile(ThrownPotion.class); potion.setItem(item); }
    private void sirenSong(Player player) { for (Entity entity : player.getNearbyEntities(10, 5, 10)) if (entity instanceof LivingEntity living && !(living instanceof Player)) { living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 3)); living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 1)); } }
    private void pixieDust(Player player) {
        for (Entity entity : player.getNearbyEntities(6, 3, 6)) if (entity instanceof Player target) { if (plugin.races().getRace(target) == Race.FAIRY) { target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1)); target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 0)); } else target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0)); }
        new BukkitRunnable() { int step;
            @Override public void run() {
                if (!player.isOnline() || step++ >= 5) { cancel(); return; }
                double radius = step * 1.2;
                for (int sample = 0; sample < 6; sample++) {
                    double angle = sample * Math.PI / 3 + step * .8;
                    Location point = player.getLocation().clone().add(Math.cos(angle) * radius, step * .45, Math.sin(angle) * radius);
                    Particle particle = switch (sample % 3) { case 0 -> Particle.END_ROD; case 1 -> Particle.GLOW; default -> Particle.SCRAPE; };
                    player.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, .01);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    private void divineJudgement(Player player) {
        Entity target = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(), 16, .4, entity -> entity != player).getHitEntity();
        if (target instanceof LivingEntity living) { living.damage(8, player); for (Entity entity : player.getNearbyEntities(6, 3, 6)) if (entity instanceof Player ally && plugin.races().getRace(ally) == Race.CELESTID) ally.setHealth(Math.min(ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), ally.getHealth() + 4)); beamParticles(player, living.getLocation()); }
    }
    private void voidVeil(Player player) {
        Location origin = player.getLocation().clone(); Vector direction = player.getLocation().getDirection().setY(0).normalize(); Location destination = origin.clone().add(direction.multiply(8));
        player.getWorld().spawnParticle(Particle.SQUID_INK, origin, 12, .35, .6, .35, .02); player.getWorld().spawnParticle(Particle.SMOKE, origin, 8, .35, .6, .35, .02); player.teleport(destination);
        for (int step = 1; step <= 8; step++) player.getWorld().spawnParticle(Particle.SQUID_INK, origin.clone().add(direction.clone().multiply(step)), 2, 0, .1, 0, .01);
        player.getWorld().spawnParticle(Particle.SMOKE, destination, 8, .35, .6, .35, .02);
    }
    private void webTrap(Player player) {
        Snowball snowball = player.launchProjectile(Snowball.class); snowball.setMetadata(WEB_KEY, new org.bukkit.metadata.FixedMetadataValue(plugin, true)); snowball.setVelocity(player.getLocation().getDirection().multiply(1.5));
        Location start = player.getEyeLocation().clone(); Vector velocity = player.getLocation().getDirection().normalize().multiply(1.5);
        new BukkitRunnable() { int step;
            @Override public void run() { if (!player.isOnline() || step++ >= 5) { cancel(); return; } Location point = start.clone().add(velocity.clone().multiply(step)).add(0, -.08 * step * step, 0); player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, point, 2, 0, 0, 0, .01); player.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, .01); }
        }.runTaskTimer(plugin, 0, 1);
    }
    private void infernalBurst(Player player) {
        for (Entity entity : player.getNearbyEntities(5, 2, 5)) if (entity instanceof LivingEntity living) { living.damage(4, player); living.setFireTicks(160); }
        new BukkitRunnable() { int step;
            @Override public void run() { if (!player.isOnline() || step++ >= 4) { cancel(); return; } double radius = step * 1.25; for (int sample = 0; sample < 10; sample++) { double angle = sample * Math.PI * 2 / 10; Location point = player.getLocation().clone().add(Math.cos(angle) * radius, .1, Math.sin(angle) * radius); Particle particle = switch (sample % 3) { case 0 -> Particle.LAVA; case 1 -> Particle.FLAME; default -> Particle.CAMPFIRE_COSY_SMOKE; }; player.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, .01); } }
        }.runTaskTimer(plugin, 0, 1);
    }
    private void overcharge(Player player) { player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 1, true, false)); player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 1, true, false)); player.setMetadata("serverraces_overcharge", new org.bukkit.metadata.FixedMetadataValue(plugin, true)); Bukkit.getScheduler().runTaskLater(plugin, () -> { player.removeMetadata("serverraces_overcharge", plugin); player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 4, true, false)); }, 160); }

    private void beamParticles(Player player, Location target) {
        Location start = target.clone().add(0, 12, 0);
        new BukkitRunnable() { int step;
            @Override public void run() {
                if (!player.isOnline() || step++ >= 5) { cancel(); return; }
                double y = start.getY() - step * 2.4;
                Location point = start.clone(); point.setY(y);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 8, .12, .25, .12, .02);
                if (step == 5) player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, target, 10, .35, .15, .35, .02);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private Vector basisRight(Vector direction) {
        Vector reference = Math.abs(direction.getY()) > .9 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        return direction.clone().crossProduct(reference).normalize();
    }

    @EventHandler public void projectileHit(ProjectileHitEvent event) { if (event.getEntity() instanceof WitherSkull skull && skull.hasMetadata("serverraces_eldritch") && event.getHitEntity() instanceof LivingEntity target) { target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1)); target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0)); } if (event.getEntity() instanceof Snowball snowball && snowball.hasMetadata(WEB_KEY)) { Location location = event.getHitBlock() == null ? snowball.getLocation() : event.getHitBlock().getLocation().add(0, 1, 0); if (location.getBlock().getType().isAir()) { location.getBlock().setType(Material.COBWEB); Bukkit.getScheduler().runTaskLater(plugin, () -> location.getBlock().setType(Material.AIR), 100); } } }

    private void cone(Player player, double radius, java.util.function.Consumer<LivingEntity> action) { Vector direction = player.getLocation().getDirection().normalize(); for (Entity entity : player.getNearbyEntities(radius, radius, radius)) if (entity instanceof LivingEntity living && direction.dot(living.getLocation().add(0, 1, 0).toVector().subtract(player.getEyeLocation().toVector()).normalize()) > .55) action.accept(living); }
    private Player attacker(Entity damager) { if (damager instanceof Player player) return player; if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player; return null; }
    private void removeModifier(Player player, String keyName) { AttributeModifier found = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getModifiers().stream().filter(modifier -> modifier.getName().equals(keyName)).findFirst().orElse(null); if (found != null) player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).removeModifier(found); }
}