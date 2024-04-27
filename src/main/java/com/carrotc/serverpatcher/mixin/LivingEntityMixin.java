package com.carrotc.serverpatcher.mixin;

import com.carrotc.serverpatcher.PlayerPair;
import com.carrotc.serverpatcher.PlayerPairManager;
import com.carrotc.serverpatcher.ServerPatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

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
}
