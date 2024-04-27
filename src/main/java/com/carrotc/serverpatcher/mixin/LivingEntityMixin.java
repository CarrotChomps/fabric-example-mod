package com.carrotc.serverpatcher.mixin;

import com.carrotc.serverpatcher.ServerPatcher;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

//    @Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("HEAD"))
//    public void deathMessage(DamageSource damageSource, CallbackInfo ci) {
//        ServerPatcher.LOGGER.info("This got called yipee!!!!");
//        if ((Object) this instanceof PlayerEntity player) {
//            ServerPatcher.LOGGER.info(player.getName().getString() + " DIED LOL");
//        } else {
//            ServerPatcher.LOGGER.info("Something DIED LOL");
//        }
//    }
}
