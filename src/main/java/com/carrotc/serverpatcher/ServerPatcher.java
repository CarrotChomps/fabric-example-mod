package com.carrotc.serverpatcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerPatcher implements ModInitializer {

    public static final String MOD_ID = "serverpatcher";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        // link command (only works on online players)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("link").then(argument("player1", StringArgumentType.string()).executes(context -> {
            context.getSource().sendFeedback(() -> Text.literal("Please use two targets."), false);
            return -1;
        }).then(argument("player2", StringArgumentType.string()).executes(context -> {
            MinecraftServer server = context.getSource().getServer();

            String target1 = StringArgumentType.getString(context, "player1");
            String target2 = StringArgumentType.getString(context, "player2");

            // check if unique input
            if (target1.equalsIgnoreCase(target2)) {
                context.getSource().sendFeedback(() -> Text.literal("Can't be the same Targets."), false);
                return -1;
            }

            ServerPlayerEntity player1 = server.getPlayerManager().getPlayer(target1);
            ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(target2);

            try {
                PlayerPairManager.getInstance(server).addPair(player1, player2);
            } catch (NullPairingException e) {
                context.getSource().sendFeedback(() -> Text.literal("Those players don't exist."), false);
                return -1;
            } catch (AlreadyPairedException e) {
                context.getSource().sendFeedback(() -> Text.literal("Those players are already paired."), false);
                return -1;
            }

            context.getSource().sendFeedback(() -> Text.literal("Players linked"), false);
            return 0;
        })))));

        // unlink command (works on offline)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("unlink").then(argument("player", StringArgumentType.string()).executes(context -> {
            MinecraftServer server = context.getSource().getServer();
            String target = StringArgumentType.getString(context, "player");

            PlayerPair removedPair = PlayerPairManager.getInstance(server).removePair(target);
            if (removedPair == null) {
                context.getSource().sendFeedback(() -> Text.literal("That player doesn't exist"), false);
                return -1;
            }

            context.getSource().sendFeedback(() -> Text.literal("Player unlinked"), false);
            return 1;
        }))));

        // list command (displays offline players as well)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("list").executes(context -> {
            MinecraftServer server = context.getSource().getServer();
            List<PlayerPair> pairList = PlayerPairManager.getInstance(server).getPairs();

            if (pairList.isEmpty()) {
                context.getSource().sendFeedback(() -> Text.literal("No one is paired at the moment."), false);
            }

            PlayerPairManager.getInstance(server).getPairs().forEach(pair -> {
                context.getSource().sendFeedback(() -> Text.literal(pair.pairMessage()), false);
            });

            return 1;
        })));


        // on join event
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ServerPlayerEntity playerThatJustJoined = handler.getPlayer();
            if (PlayerPairManager.getInstance(server).isInAPair(playerThatJustJoined)) {
                PlayerPair joinedPair = PlayerPairManager.getInstance(server).getPair(playerThatJustJoined.getUuid());
                ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(joinedPair.getOtherPairUUID(playerThatJustJoined.getUuid()));
                if (player2 != null) {
                    playerThatJustJoined.setHealth(player2.getHealth()); // sync healths on join
                }
            }
        });

        // on leave event
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
        });
    }
}