package com.carrotc.serverpatcher.mixin;

import com.carrotc.serverpatcher.PlayerPair;
import com.carrotc.serverpatcher.PlayerPairManager;
import com.carrotc.serverpatcher.ServerPatcher;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Shadow
    public abstract void readCustomDataFromNbt(NbtCompound nbt);

    @Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("HEAD"), cancellable = true)
    public void onDeath0(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity playerDying = (ServerPlayerEntity) (Object) this;
        ServerPatcher.LOGGER.info("Calling Death Method from Mixin for " + playerDying.getName().getString());
        MinecraftServer server = playerDying.getServer();
        if (server != null) {
            PlayerPair deadPair = PlayerPairManager.getInstance(server).getPair(playerDying.getUuid());
            if (deadPair != null) {
                ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(deadPair.getOtherPairUUID(playerDying.getUuid()));
                if (player2 != null) {
                    // ServerPatcher.LOGGER.info(playerDying.getName().getString() + " died so... so killing " + player2.getName().getString() + " as well!");


                    // if either players hold a totem of undying, dont kill them but deal damage to proc the totem;
//                    for (ItemStack hand : playerDying.getHandItems()) {
//                        if (hand.isOf(Items.TOTEM_OF_UNDYING)) {
//                            playerDying.setHealth(1);
//                            player2.setHealth(1);
//
//                            playerDying.damage(playerDying.getDamageSources().generic(), 10);
//                            player2.damage(player2.getDamageSources().generic(), 10);
//                            return;
//                        }
//                    }

                    // CarrotC takes the damage (DMG-MXN)
                    //      NotCarrotC takes the damage (DMG-MXN)
                    //      NotCarrotC dies, because he takes the damage [he dies???]
                    // CarrotC dies ()

                    for (ItemStack hand : player2.getHandItems()) {
                        if (hand.isOf(Items.TOTEM_OF_UNDYING)) {
                            playerDying.setHealth(1);
                            player2.damage(player2.getDamageSources().generic(), (player2.getHealth() + player2.getAbsorptionAmount()) + 1);
                            ServerPatcher.LOGGER.info("[!!] Pair has a totem of Undying, canceling for dying Player: " + playerDying.getName().getString());
                            ci.cancel();
                            return;
                        }
                    }
                    ServerPatcher.LOGGER.info("[!] killing " + player2.getName().getString());
                    player2.kill();
                }
            } else {
                ServerPatcher.LOGGER.error("Null deadPair!");
            }
        }
    }
}
