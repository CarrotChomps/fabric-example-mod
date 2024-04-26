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

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerPatcher implements ModInitializer {

    public static final String MOD_ID = "serverpatcher";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        // link command
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

            // check input
            UUID uuid1 = player1 != null ? player1.getUuid() : StateSaverAndLoader.getOfflineUUID(target1);
            UUID uuid2 = player2 != null ? player2.getUuid() : StateSaverAndLoader.getOfflineUUID(target2);

            if (uuid1 == null || uuid2 == null) {
                context.getSource().sendFeedback(() -> Text.literal("Invalid Targets."), false);
                return -1;
            }

            try {
                // save to memory
                PlayerPairManager.INSTANCE.addPair(uuid1, uuid2);
                // save to player data
                PlayerData player1State = StateSaverAndLoader.getPlayerState(uuid1, server);
                PlayerData player2State = StateSaverAndLoader.getPlayerState(uuid2, server);

                player1State.setPair(uuid1);
                player2State.setPair(uuid2);

                context.getSource().sendFeedback(() -> Text.literal("Linked players."), false);

                return 1;
            } catch (NullPairingException e) {
                String source = e.getSource();
                context.getSource().sendFeedback(() -> Text.literal(source + " was null!"), false);
                return -1;
            } catch (AlreadyPairedException e) {
                UUID u = e.getAttemptedPair();
                context.getSource().sendFeedback(() -> Text.literal(u + " is already paired with someone!"), false);
                return -1;
            }
        })))));

        // unlink command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("unlink").then(argument("player", StringArgumentType.string()).executes(context -> {
            MinecraftServer server = context.getSource().getServer();
            String target = StringArgumentType.getString(context, "player");
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(target);
            UUID uuid = player != null ? player.getUuid() : StateSaverAndLoader.getOfflineUUID(target);

            // check input
            if (uuid == null) {
                context.getSource().sendFeedback(() -> Text.literal("Invalid Targets."), false);
                return -1;
            }

            // remove from memory
            PlayerPair removedPair = PlayerPairManager.INSTANCE.removePair(uuid);
            // remove from playerData
            PlayerData player1Data = StateSaverAndLoader.getPlayerState(removedPair.getPlayer1(), server);
            PlayerData player2Data = StateSaverAndLoader.getPlayerState(removedPair.getPlayer2(), server);

            // remove from playerData
            player1Data.removePair();
            player2Data.removePair();

            StateSaverAndLoader.getServerState(server).savePlayerData(removedPair.getPlayer1(), player1Data);
            StateSaverAndLoader.getServerState(server).savePlayerData(removedPair.getPlayer2(), player2Data);

            return 1;
        }))));

        // list command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("list").executes(context -> {

            if (PlayerPairManager.pairs.isEmpty()) {
                context.getSource().sendFeedback(() -> Text.literal("No one is paired at the moment"), false);
                return 1;
            }

            for (PlayerPair pair : PlayerPairManager.pairs) {
                PlayerManager pM = context.getSource().getServer().getPlayerManager();
                if (pair.getPlayer1() != null) {
                    context.getSource().sendFeedback(() -> Text.literal(pM.getPlayer(pair.getPlayer1()).getName().getString() + " is paired with " + pM.getPlayer(pair.getPlayer2()).getName().getString()), false);
                } else {
                    LOGGER.error("Someone is paired with a null!!");
                }
            }

            return 1;
        })));


        // on join event
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            LOGGER.info(handler.getPlayer().getName().getString() + " joined the server.");
            ServerPlayerEntity playerEntity = handler.getPlayer();

            PlayerData playerData = StateSaverAndLoader.getPlayerState(playerEntity);
            UUID p1 = playerEntity.getUuid();
            UUID p2 = playerData.getPair();

            // if the player has no pair, don't do anything
            if (p2 == null) {
                LOGGER.info(playerEntity.getName().getString() + " has no pair, no need to worry!.");
                return;
            }

            // loading pair
            try {
                PlayerPairManager.INSTANCE.addPair(p1, p2);
            } catch (NullPairingException e) {
                LOGGER.error("Null Pairing BAD!! D:<");
                return;
            } catch (AlreadyPairedException e) {
                LOGGER.info(playerEntity.getName().getString() + " already has a pair in memory.");
                return;
            }

            LOGGER.info("Player joined, this is the new list " + PlayerPairManager.INSTANCE);
        });

        // on leave event
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            LOGGER.info(handler.getPlayer().getName().getString() + " left the server.");
            ServerPlayerEntity playerEntity = handler.getPlayer();
            PlayerData playerData = StateSaverAndLoader.getPlayerState(playerEntity);
            // if they have a pair in memory, save it to data.
            PlayerPair pair = PlayerPairManager.INSTANCE.getPair(playerEntity.getUuid());

            assert pair != null;
            UUID p1 = pair.getPlayer1();
            UUID p2 = pair.getPlayer2();

            if (p1 == playerEntity.getUuid()) {
                playerData.setPair(p2);
                StateSaverAndLoader.getServerState(server).savePlayerData(p1, playerData);
            } else {
                playerData.setPair(p1);
                StateSaverAndLoader.getServerState(server).savePlayerData(p2, playerData);
            }

            LOGGER.info("Player left, this is the new list " + PlayerPairManager.INSTANCE);
        });
    }
}