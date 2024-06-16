package com.carrotc.serverpatcher.mixin;

import com.carrotc.serverpatcher.PlayerPair;
import com.carrotc.serverpatcher.PlayerPairManager;
import com.carrotc.serverpatcher.ServerPatcher;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public abstract boolean addStatusEffect(StatusEffectInstance effect);

    @Shadow
    public abstract void heal(float amount);

    @Shadow
    protected abstract float modifyAppliedDamage(DamageSource source, float amount);

    @Inject(method = "heal", at = @At("HEAD"))
    public void heal0(float amount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player1) {
            MinecraftServer server = player1.getServer();
            if (server != null) {
                PlayerPair healPair = PlayerPairManager.getInstance(server).getPair(player1.getUuid());
                if (healPair != null) {
                    if (!healPair.isBeenHealed()) {
                        ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(healPair.getOtherPairUUID(player1.getUuid()));
                        if (player2 != null) {
                            // ServerPatcher.LOGGER.info(player1.getName().getString() + " died so... so killing " + player2.getName().getString() + " as well!");
                            healPair.setBeenHealed(true);
                            player2.heal(amount);
                            healPair.setBeenHealed(false);
                        }
                    }
                } else {
                    // ServerPatcher.LOGGER.error("Null healPair!");
                }
            }
        }
    }

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("TAIL"), cancellable = true)
    public void damage0(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity playerBeingHurt) {
            // ServerPatcher.LOGGER.info("[!D!] Calling Damage Method from Mixin for " + playerBeingHurt.getName().getString());
            MinecraftServer server = playerBeingHurt.getServer();
            if (server != null) {
                PlayerPair hurtPair = PlayerPairManager.getInstance(server).getPair(playerBeingHurt.getUuid());
                if (hurtPair != null) {
//                if (hurtPair.isUsedTotem()) {
//                    hurtPair.setUsedTotem(false);
//                    cir.setReturnValue(false);
//                    ServerPatcher.LOGGER.info("[!!] Pair used a totem of Undying, canceling for hurting Player: " + playerGainingAbsorption.getName().getString());
//                    return;
//                }
                    if (!hurtPair.isBeenDamaged()) { // if the pair hasn't been damaged, damage the pair
                        ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(hurtPair.getOtherPairUUID(playerBeingHurt.getUuid()));
                        if (player2 != null) {
                            // ServerPatcher.LOGGER.info(playerDying.getName().getString() + " died so... so killing " + player2.getName().getString() + " as well!");
                            hurtPair.setBeenDamaged(true);

                            // stupid totem of undying edge case
                            if (amount >= player2.getHealth()) {
                                if (hurtPair.isUsedTotem()) {
                                    player2.setHealth(1);
                                    hurtPair.setBeenDamaged(false);
                                    hurtPair.setUsedTotem(false);
                                    // ServerPatcher.LOGGER.info("[!!] Pair used a totem of undying " + playerBeingHurt.getName().getString() + ", not dealing damage, and setting " + player2.getName().getString() + "'s HP to 1.");
                                    return;
                                }
                                for (ItemStack hand : player2.getHandItems()) {
                                    if (hand.isOf(Items.TOTEM_OF_UNDYING)) {
                                        playerBeingHurt.setHealth(1);
                                        player2.setHealth(1);
                                        hurtPair.setBeenDamaged(false);
                                        // ServerPatcher.LOGGER.info("[!!] " + player2.getName().getString() + " has a totem of undying, so we won't deal damage to" + playerBeingHurt.getName().getString());
                                        return;
                                    }
                                }
                            }

                            // deal damage to the pair based on the amount of damage the other half just took (note, this may kill them)
                            float actualAmount = hurtPair.getRecentDamage() > 0 ? hurtPair.getRecentDamage() : amount;
                            // ServerPatcher.LOGGER.info("[!] " + playerBeingHurt.getName().getString() + " took damage, so dealing " + actualAmount + " to " + player2.getName().getString());
                            player2.damage(player2.getDamageSources().generic(), actualAmount);

                            // emergency health sync
                            if (playerBeingHurt.getHealth() != player2.getHealth()) {
                                if (playerBeingHurt.isAlive() && player2.isAlive()) {
                                    float healthCheck = Math.max(playerBeingHurt.getHealth(), player2.getHealth());
                                    ServerPatcher.LOGGER.warn(String.format("Pair-Health DeSync Detected[%s{%s},%s{%s}] setting to %s", playerBeingHurt.getName().getString(), playerBeingHurt.getHealth(), player2.getName().getString(), player2.getHealth(), healthCheck));
                                    playerBeingHurt.setHealth(healthCheck);
                                    player2.setHealth(healthCheck);
                                }
                            }
                            hurtPair.setBeenDamaged(false);
                        }
                    }
                } else {
                    // ServerPatcher.LOGGER.error("Null hurtPair!");
                }
            }
        }
    }

    @Inject(method = "modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F", at = @At("TAIL"))
    public void modifyAppliedDamage0(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity playerBeingHurt) {
            PlayerPair pair = PlayerPairManager.getInstance(playerBeingHurt.getServer()).getPair(playerBeingHurt.getUuid());
            if (pair != null) {
                // ServerPatcher.LOGGER.info("[!] REAL DAMAGE BEING DEALT TO " + playerBeingHurt.getName().getString() + " IS " + amount);
                pair.setRecentDamage(amount);
            }
        }
    }

    @Inject(method = "tryUseTotem(Lnet/minecraft/entity/damage/DamageSource;)Z", at = @At("HEAD"))
    public void tryUseTotem0(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity playerBeingHurt) {
            PlayerPair pair = PlayerPairManager.getInstance(playerBeingHurt.getServer()).getPair(playerBeingHurt.getUuid());
            if (pair != null) {
                for (Hand hand : Hand.values()) {
                    ItemStack itemStack2 = playerBeingHurt.getStackInHand(hand);
                    if (itemStack2.isOf(Items.TOTEM_OF_UNDYING)) {
                        // ServerPatcher.LOGGER.info("[!] Someone used a Totem :>!");
                        pair.setUsedTotem(true);
                    }
                }
            }
        }
    }
}
