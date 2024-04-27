package com.carrotc.serverpatcher.mixin;

import com.carrotc.serverpatcher.PlayerPair;
import com.carrotc.serverpatcher.PlayerPairManager;
import com.carrotc.serverpatcher.ServerPatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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
                    ServerPatcher.LOGGER.error("Null healPair!");
                }
            }
        }
    }


    /**
     * totems of undying don't work
     *
     * @param source
     * @param amount
     * @param cir
     */
    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("TAIL"), cancellable = true)
    public void damage0(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity playerBeingHurt) {
            ServerPatcher.LOGGER.info("Calling Damage Method from Mixin for " + playerBeingHurt.getName().getString());
            MinecraftServer server = playerBeingHurt.getServer();
            if (server != null) {
                PlayerPair hurtPair = PlayerPairManager.getInstance(server).getPair(playerBeingHurt.getUuid());
                if (hurtPair != null) {
//                if (hurtPair.isUsedTotem()) {
//                    hurtPair.setUsedTotem(false);
//                    cir.setReturnValue(false);
//                    ServerPatcher.LOGGER.info("[!!] Pair used a totem of Undying, canceling for hurting Player: " + playerBeingHurt.getName().getString());
//                    return;
//                }
                    if (!hurtPair.isBeenDamaged()) {
                        ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(hurtPair.getOtherPairUUID(playerBeingHurt.getUuid()));
                        if (player2 != null) {
                            // ServerPatcher.LOGGER.info(playerDying.getName().getString() + " died so... so killing " + player2.getName().getString() + " as well!");
                            hurtPair.setBeenDamaged(true);

                            // stupid totem of undying edge case
                            if (amount >= player2.getHealth()) {
//                            for (ItemStack hand : playerBeingHurt.getHandItems()) {
//                                if (hand.isOf(Items.TOTEM_OF_UNDYING)) {
//                                    player2.setHealth(1);
//                                    hurtPair.setBeenDamaged(false);
//                                    ServerPatcher.LOGGER.info("[!!] PlayerBeingHurt has a totem of undying " + playerBeingHurt.getName().getString());
//                                    return;
//                                }
//                            }
                                for (ItemStack hand : player2.getHandItems()) {
                                    if (hand.isOf(Items.TOTEM_OF_UNDYING)) {
                                        playerBeingHurt.setHealth(1);
                                        player2.setHealth(1);
                                        hurtPair.setBeenDamaged(false);
                                        ServerPatcher.LOGGER.info("[!!] PairedPlayer has a totem of undying " + playerBeingHurt.getName().getString());
                                        return;
                                    }
                                }
                            }
                            float actualAmount = hurtPair.getRecentDamage();
                            ServerPatcher.LOGGER.info("[!] dealing " + actualAmount + " to " + player2.getName().getString());
                            player2.damage(player2.getDamageSources().generic(), actualAmount);
                            hurtPair.setBeenDamaged(false);
                        }
                    }
                } else {
                    ServerPatcher.LOGGER.error("Null hurtPair!");
                }
            }
        }
    }

    @Inject(method = "modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F", at = @At("TAIL"), cancellable = true)
    public void modifyAppliedDamage0(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity playerBeingHurt) {
            PlayerPair pair = PlayerPairManager.getInstance(playerBeingHurt.getServer()).getPair(playerBeingHurt.getUuid());
            if (pair != null) {
                ServerPatcher.LOGGER.info("[!] REAL DAMAGE BEING DEALT TO " + playerBeingHurt.getName().getString() + " IS " + amount);
                pair.setRecentDamage(amount);
            }
        }
    }
}
