package com.carrotc.serverpatcher.mixin;

import com.carrotc.serverpatcher.PlayerPair;
import com.carrotc.serverpatcher.PlayerPairManager;
import com.carrotc.serverpatcher.ServerPatcher;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("HEAD"))
    public void onDeath0(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity player1 = (ServerPlayerEntity) (Object) this;
        MinecraftServer server = player1.getServer();
        if (server != null) {
            PlayerPair deadPair = PlayerPairManager.getInstance(server).getPair(player1.getUuid());
            if (deadPair != null) {
                ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(deadPair.getOtherPairUUID(player1.getUuid()));
                if (player2 != null) {
                    // ServerPatcher.LOGGER.info(player1.getName().getString() + " died so... so killing " + player2.getName().getString() + " as well!");
                    player2.kill();
                }
            } else {
                ServerPatcher.LOGGER.error("Null hurtPair!");
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
    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"))
    public void damage0(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player1 = (ServerPlayerEntity) (Object) this;
        MinecraftServer server = player1.getServer();
        if (server != null) {
            PlayerPair hurtPair = PlayerPairManager.getInstance(server).getPair(player1.getUuid());
            if (hurtPair != null) {
                if (!hurtPair.isBeenDamaged()) {
                    ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(hurtPair.getOtherPairUUID(player1.getUuid()));
                    if (player2 != null) {
                        // ServerPatcher.LOGGER.info(player1.getName().getString() + " died so... so killing " + player2.getName().getString() + " as well!");
                        hurtPair.setBeenDamaged(true);
                        player2.damage(player2.getDamageSources().generic(), amount);
                        hurtPair.setBeenDamaged(false);
                    }
                }
            } else {
                ServerPatcher.LOGGER.error("Null hurtPair!");
            }
        }
    }
}
