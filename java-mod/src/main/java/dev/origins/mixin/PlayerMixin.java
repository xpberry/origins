package dev.origins.mixin;

import dev.origins.Race;
import dev.origins.RacesMod;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerMixin {
    @Inject(method = "damage", at = @At("HEAD"))
    private void origins$weakness(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this; Race race = RacesMod.race(player);
        if (race == Race.DRAGONBORN && source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FREEZING)) player.setFrozenTicks(player.getFrozenTicks() + 40);
    }
}