package com.carrotc.serverpatcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerPatcher implements ModInitializer {

    public static final String MOD_ID = "serverpatcher";

    public static final Identifier PAIR = new Identifier(MOD_ID, "pair");

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final List<PlayerPair> pairs = new ArrayList<>();

    @Override
    public void onInitialize() {

        // link command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("link").then(argument("player1", StringArgumentType.string()).executes(context -> {
            context.getSource().sendFeedback(() -> Text.literal("Please use two targets."), false);
            return -1;
        }).then(argument("player2", StringArgumentType.string()).executes(context -> {

            String target1 = StringArgumentType.getString(context, "player1");
            String target2 = StringArgumentType.getString(context, "player2");

            ServerPlayerEntity player1 = context.getSource().getServer().getPlayerManager().getPlayer(target1);
            ServerPlayerEntity player2 = context.getSource().getServer().getPlayerManager().getPlayer(target2);

            // check if unique input
            if (player1 == player2) {
                context.getSource().sendFeedback(() -> Text.literal("Can't be the same Targets."), false);
                return -1;
            }

            // check input
            if (player1 == null || player2 == null) {
                context.getSource().sendFeedback(() -> Text.literal("Invalid Targets."), false);
                return -1;
            }

            // check if there is already a pair
            for (PlayerPair pair : pairs) {
                if (pair.has(player1.getUuid())) {
                    context.getSource().sendFeedback(() -> Text.literal(player1.getName().getString() + " is already paired with someone."), false);
                    return 1;
                }
                if (pair.has(player2.getUuid())) {
                    context.getSource().sendFeedback(() -> Text.literal(player2.getName().getString() + " is already paired with someone."), false);
                    return 1;
                }
            }

            // save to player
            PlayerData player1State = StateSaverAndLoader.getPlayerState(player1);
            PlayerData player2State = StateSaverAndLoader.getPlayerState(player2);

            player1State.setPair(player2.getUuid());
            player2State.setPair(player1.getUuid());

            PlayerPair pair = new PlayerPair(player1.getUuid(), player2.getUuid());
            pairs.add(pair);

            context.getSource().sendFeedback(() -> Text.literal("Linked players."), false);

            return 1;
        })))));

        // unlink command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("unlink").then(argument("player", StringArgumentType.string()).executes(context -> {

            String target = StringArgumentType.getString(context, "player");

            ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(target);
            // check input
            if (player == null) {
                context.getSource().sendFeedback(() -> Text.literal("Invalid Targets."), false);
                return -1;
            }


            PlayerPair removingPair = null;
            for (PlayerPair pair : pairs) {
                if (pair.has(player.getUuid())) {
                    removingPair = pair;
                }
            }


            // remove from memory
            if (removingPair != null) {
                PlayerManager pM = context.getSource().getServer().getPlayerManager();
                ServerPlayerEntity player1 = pM.getPlayer(removingPair.getPlayer1());
                ServerPlayerEntity player2 = pM.getPlayer(removingPair.getPlayer2());
                if (player1 != null && player2 != null) {
                    context.getSource().sendFeedback(() -> Text.literal(player1.getName().getString() + " is no longer paried with " + player2.getName().getString()), false);


                    PlayerData player1State = StateSaverAndLoader.getPlayerState(player1);
                    PlayerData player2State = StateSaverAndLoader.getPlayerState(player2);

                    // remove from playerData
                    player1State.removePair();
                    player2State.removePair();

                    pairs.remove(removingPair);
                }
            } else {
                context.getSource().sendFeedback(() -> Text.literal(player.getName().getString() + " was linked with no one."), false);
            }

            return 1;
        }))));

        // list command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("list").executes(context -> {

            if (pairs.isEmpty()) {
                context.getSource().sendFeedback(() -> Text.literal("No one is paired at the moment"), false);
                return 1;
            }

            for (PlayerPair pair : pairs) {
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
            UUID pair = playerData.getPair();

            if (pair == null) {
                LOGGER.info(playerEntity.getName().getString() + " has no pair, no need to worry!.");
                return;
            }

            // if the pair is already in memory
            for (PlayerPair playerPair : pairs) {
                if (playerPair.has(pair) || playerPair.has(playerEntity.getUuid())) {
                    UUID p1 = playerPair.getPlayer1();
                    UUID p2 = playerPair.getPlayer2();
                    if (p1 == playerEntity.getUuid()) {
                        playerData.setPair(p2);
                    } else {
                        playerData.setPair(p1);
                    }
                    LOGGER.info(playerEntity.getName().getString() + " already has a pair in memory.");
                    return;
                }
            }

            // if not create a new pair in memory
            PlayerPair playerPair = new PlayerPair(pair, playerEntity.getUuid());
            pairs.add(playerPair);
            LOGGER.info(pairs + " is the new pair list.");
        });

        // on leave event
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            LOGGER.info(handler.getPlayer().getName().getString() + " left the server.");
            ServerPlayerEntity playerEntity = handler.getPlayer();
            PlayerData playerData = StateSaverAndLoader.getPlayerState(playerEntity);

            // if they have a pair in memory, save it to data.
            for (PlayerPair pair : pairs) {
                if (pair.has(playerEntity.getUuid())) {
                    UUID p1 = pair.getPlayer1();
                    UUID p2 = pair.getPlayer2();
                    if (p1 == playerEntity.getUuid()) {
                        playerData.setPair(p2);
                    } else {
                        playerData.setPair(p1);
                    }
                }
            }
            LOGGER.info(pairs + " is the new pair list.");
        });
    }
}