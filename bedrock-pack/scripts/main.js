import { world, system, EntityDamageCause } from "@minecraft/server";
import { ModalFormData } from "@minecraft/server-ui";

const cooldowns = new Map();
const races = ["dragonborn", "dwarf", "giant", "warlock", "witch", "mermaid", "fairy", "celestid", "shadowkin", "arachnid", "ignis", "golem"];
const tag = (race) => `race:${race}`;
const selected = (player) => races.find((race) => player.hasTag(tag(race)));
const nearby = (player, radius) => player.dimension.getEntities({ location: player.location, maxDistance: radius, excludeTypes: ["minecraft:item"] });
const cone = (player, radius) => {
  const view = player.getViewDirection();
  return nearby(player, radius).filter((entity) => {
    if (entity.id === player.id) return false;
    const dx = entity.location.x - player.location.x;
    const dy = entity.location.y - player.location.y;
    const dz = entity.location.z - player.location.z;
    const length = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1;
    return (dx * view.x + dy * view.y + dz * view.z) / length > 0.55;
  });
};

function openRaceMenu(player) {
  const current = selected(player);
  const menu = new ModalFormData()
    .title("Race selection")
    .dropdown("Choose a race", races.map((race) => race[0].toUpperCase() + race.slice(1)), Math.max(0, races.indexOf(current)))
    .submitButton("Select race");
  menu.show(player).then((response) => {
    if (!response.canceled) choose(player, races[response.formValues[0]]);
  }).catch(() => player.sendMessage("The race menu could not be opened."));
}

function choose(player, race) {
  if (!races.includes(race)) return player.sendMessage(`Use: ${races.join(", ")}`);
  for (const old of races) player.removeTag(tag(old));
  player.addTag(tag(race));
  player.triggerEvent(`origins:${race}`);
  player.sendMessage(`Race selected: ${race}`);
}

function ability(player) {
  const race = selected(player);
  if (!race) return player.sendMessage("Choose a race first.");
  const key = `${player.id}:ability`;
  if ((cooldowns.get(key) ?? 0) > system.currentTick) {
    return player.sendMessage(`Ability ready in ${Math.ceil((cooldowns.get(key) - system.currentTick) / 20)}s`, true);
  }
  cooldowns.set(key, system.currentTick + (race === "dragonborn" ? 600 : 200));
  const targets = nearby(player, race === "mermaid" ? 8 : 5).filter((entity) => entity.id !== player.id);
  if (race === "dragonborn") for (const entity of cone(player, 8)) { entity.applyDamage(6); entity.setOnFire(6); }
  if (race === "dwarf") player.runCommand("effect @s resistance 10 1 true");
  if (race === "giant") for (const entity of targets) { entity.applyDamage(6); entity.applyKnockback(entity.location.x - player.location.x, entity.location.z - player.location.z, 1.5, 0.5); }
  if (race === "warlock") for (const entity of targets.slice(0, 1)) { entity.runCommand("effect @s wither 10 1 true"); entity.runCommand("effect @s slowness 10 1 true"); player.addEffect("regeneration", 40, { amplifier: 0 }); }
  if (race === "witch") player.runCommand("give @s potion 1 5");
  if (race === "mermaid") for (const entity of targets) entity.addEffect("slowness", 160, { amplifier: 3 });
  if (race === "fairy") for (const entity of targets) {
    if (entity.typeId === "minecraft:player" && selected(entity) === "fairy") {
      entity.addEffect("regeneration", 160, { amplifier: 0 });
      entity.addEffect("speed", 160, { amplifier: 1 });
    } else entity.addEffect("blindness", 100, { amplifier: 0 });
  }
  if (race === "celestid") for (const entity of cone(player, 12)) { entity.applyDamage(8); entity.setOnFire(4); }
  if (race === "shadowkin") { const view = player.getViewDirection(); player.teleport({ x: player.location.x + view.x * 8, y: player.location.y, z: player.location.z + view.z * 8 }); }
  if (race === "arachnid") for (const entity of cone(player, 8)) entity.addEffect("slowness", 100, { amplifier: 3 });
  if (race === "ignis") for (const entity of targets) { entity.applyDamage(8); entity.setOnFire(8); }
  if (race === "golem") { player.addEffect("strength", 160, { amplifier: 1 }); player.addEffect("resistance", 160, { amplifier: 0 }); }
}

world.afterEvents.scriptEventReceive.subscribe((event) => {
  if (event.id === "origins:menu" && event.sourceEntity) openRaceMenu(event.sourceEntity);
  if (event.id === "origins:choose" && event.sourceEntity) choose(event.sourceEntity, event.message.trim().toLowerCase());
  if (event.id === "origins:ability" && event.sourceEntity) ability(event.sourceEntity);
});

world.beforeEvents.chatSend.subscribe((event) => {
  if (event.message.trim().toLowerCase() !== "!race") return;
  event.cancel = true;
  system.run(() => openRaceMenu(event.sender));
});

world.afterEvents.entityHurt.subscribe(({ hurtEntity, damageSource, damage }) => {
  if (hurtEntity.typeId !== "minecraft:player") return;
  const race = selected(hurtEntity);
  if (race === "warlock" && damageSource.cause === EntityDamageCause.magic) hurtEntity.addEffect("regeneration", 20, { amplifier: 0 });
  if (race === "mermaid" && !hurtEntity.isInWater) hurtEntity.addEffect("slowness", 40, { amplifier: 1 });
  if (race === "giant") hurtEntity.addEffect("hunger", 40, { amplifier: 1 });
  if (race === "celestid" && damageSource.damagingEntity?.typeId === "minecraft:player" && hurtEntity.getComponent("minecraft:type_family")?.hasTypeFamily("undead")) hurtEntity.applyDamage(4);
});

system.runInterval(() => {
  for (const player of world.getPlayers()) {
    const race = selected(player);
    if (race === "dragonborn") player.addEffect("fire_resistance", 220, { showParticles: false });
    if (race === "mermaid" && player.isInWater) { player.addEffect("water_breathing", 220, { showParticles: false }); player.addEffect("dolphins_grace", 40, { showParticles: false }); }
    if (race === "fairy") player.addEffect("slow_falling", 40, { showParticles: false });
    if (race === "mermaid" && !player.isInWater) player.addEffect("slowness", 40, { amplifier: 1, showParticles: false });
    if (race === "witch") { player.removeEffect("poison"); player.removeEffect("wither"); }
    if (race === "dwarf") player.addEffect("slowness", 40, { amplifier: 0, showParticles: false });
    if (race === "celestid" && player.isInWater === false) player.addEffect("strength", 40, { amplifier: 0, showParticles: false });
    if (race === "shadowkin") { player.addEffect("night_vision", 40, { showParticles: false }); player.addEffect("speed", 40, { showParticles: false }); if (player.isInWater === false && player.dimension.getSkyLightLevel?.(player.location) > 10) player.addEffect("weakness", 40, { amplifier: 1, showParticles: false }); }
    if (race === "arachnid") player.addEffect("speed", 40, { showParticles: false });
    if (race === "ignis" && player.isInWater) player.applyDamage(1);
    if (race === "golem" && player.isInWater) player.addEffect("slowness", 40, { amplifier: 1, showParticles: false });
  }
}, 20);