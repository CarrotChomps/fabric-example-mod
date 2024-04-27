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

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("HEAD"))
    public void onDeath0(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        MinecraftServer server = player.getServer();
        if (server != null) {
            PlayerPair deadPair = PlayerPairManager.getInstance(server).getPair(player.getUuid());
            if (deadPair != null) {
                ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(deadPair.getOtherPairUUID(player.getUuid()));
                if (player2 != null) {
                    ServerPatcher.LOGGER.info(player.getName().getString() + " died so... so killing " + player2.getName().getString() + " as well!");
                    player2.kill();
                }
            } else {
                ServerPatcher.LOGGER.error("Null deadPair!");
            }
        }
    }

//    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"))
//    public void dropItem0(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
//        PlayerEntity player = (PlayerEntity) (Object) this;
//
//        ServerPatcher.LOGGER.info(player.getName().getString() + " Dropped an item LOL");
//    }
}
