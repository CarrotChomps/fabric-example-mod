package com.carrotc.serverpatcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerPatcher implements ModInitializer {

    public static final String MOD_ID = "serverpatcher";

    public static final Identifier PAIR = new Identifier(MOD_ID, "pair");

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final List<PlayerPair> pairs = new ArrayList<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("link").then(argument("player1", StringArgumentType.string()).executes(context -> {
            context.getSource().sendFeedback(() -> Text.literal("Please use two targets."), false);
            return -1;
        }).then(argument("player2", StringArgumentType.string()).executes(context -> {

            String target1 = StringArgumentType.getString(context, "player1");
            String target2 = StringArgumentType.getString(context, "player2");

            ServerPlayerEntity player1 = context.getSource().getServer().getPlayerManager().getPlayer(target1);
            ServerPlayerEntity player2 = context.getSource().getServer().getPlayerManager().getPlayer(target2);

            if(player1 == player2) {
                context.getSource().sendFeedback(() -> Text.literal("Can't be the same Targets."), false);
                return -1;
            }

            if (player1 == null || player2 == null) {
                context.getSource().sendFeedback(() -> Text.literal("Invalid Targets."), false);
                return -1;
            }

            for (PlayerPair pair : pairs) {
                if (pair.has(player1)) {
                    context.getSource().sendFeedback(() -> Text.literal(player1.getName().getString() + " is already paired with someone."), false);
                    return 1;
                }
                if (pair.has(player2)) {
                    context.getSource().sendFeedback(() -> Text.literal(player2.getName().getString() + " is already paired with someone."), false);
                    return 1;
                }
            }


            PlayerPair pair = new PlayerPair(player1, player2);
            pairs.add(pair);

            context.getSource().sendFeedback(() -> Text.literal("Linked players."), false);

            return 1;
        })))));


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("unlink").then(argument("player", StringArgumentType.string()).executes(context -> {

            String target = StringArgumentType.getString(context, "player");

            ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(target);

            if (player == null) {
                context.getSource().sendFeedback(() -> Text.literal("Invalid Targets."), false);
                return -1;
            }

            PlayerPair removingPair = null;
            for (PlayerPair pair : pairs) {
                if (pair.has(player)) {
                    removingPair = pair;
                }
            }

            if (removingPair != null) {
                ServerPlayerEntity player1 = removingPair.getPlayer1();
                ServerPlayerEntity player2 = removingPair.getPlayer2();
                context.getSource().sendFeedback(() -> Text.literal(player1.getName().getString() + " is no longer paried with " + player2.getName().getString()), false);
                pairs.remove(removingPair);
                return 1;
            } else {
                context.getSource().sendFeedback(() -> Text.literal(player.getName().getString() + " was linked with no one."), false);
                return 1;
            }


        }))));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("list").executes(context -> {

            if (pairs.isEmpty()) {
                context.getSource().sendFeedback(() -> Text.literal("No one is paired at the moment"), false);
                return 1;
            }

            for (PlayerPair pair : pairs) {
                context.getSource().sendFeedback(() -> Text.literal(pair.getPlayer1().getName().getString() + " is paired with " + pair.getPlayer2().getName().getString()), false);
            }

            return 1;
        })));
    }
}