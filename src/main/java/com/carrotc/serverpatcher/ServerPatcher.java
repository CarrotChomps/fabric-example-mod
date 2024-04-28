package com.carrotc.serverpatcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerPatcher implements ModInitializer {

    public static final String MOD_ID = "serverpatcher";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // link command (only works on online players)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("link").then(argument("player1", StringArgumentType.string()).executes(context -> {
            // context.getSource().sendFeedback(() -> Text.literal("Please use two targets."), false);
            context.getSource().sendMessage(Text.literal("Please use two targets."));
            return -1;
        }).then(argument("player2", StringArgumentType.string()).executes(context -> {
            MinecraftServer server = context.getSource().getServer();

            String target1 = StringArgumentType.getString(context, "player1");
            String target2 = StringArgumentType.getString(context, "player2");

            // check if unique input
            if (target1.equalsIgnoreCase(target2)) {
                // context.getSource().sendFeedback(() -> Text.literal("Can't be the same Targets."), false);
                context.getSource().sendMessage(Text.literal("Can't be the same Targets."));
                return -1;
            }

            ServerPlayerEntity player1 = server.getPlayerManager().getPlayer(target1);
            ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(target2);

            try {
                PlayerPairManager.getInstance(server).addPair(player1, player2);
            } catch (NullPairingException e) {
                // context.getSource().sendFeedback(() -> Text.literal("Those players don't exist."), false);
                context.getSource().sendMessage(Text.literal("That player doesn't exist"));
                return -1;
            } catch (AlreadyPairedException e) {
                // context.getSource().sendFeedback(() -> Text.literal("Those players are already paired."), false);
                context.getSource().sendMessage(Text.literal("Those players are already paired."));
                return -1;
            }

            // context.getSource().sendFeedback(() -> Text.literal("Players linked"), false);
            context.getSource().sendMessage(Text.literal("Players linked"));
            return 0;
        })))));

        // unlink command (works on offline)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("unlink")
                .then(argument("player", StringArgumentType.string())
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            String target = StringArgumentType.getString(context, "player");

                            PlayerPair removedPair = PlayerPairManager.getInstance(server).removePair(target);
                            if (removedPair == null) {
                                // context.getSource().sendFeedback(() -> Text.literal("That player doesn't exist"), false);
                                context.getSource().sendMessage(Text.literal("That player doesn't exist"));
                                return -1;
                            }

                            // context.getSource().sendFeedback(() -> Text.literal("Player unlinked"), false);
                            context.getSource().sendMessage(Text.literal("Player unlinked"));
                            return 1;
                        }))));

        // list command (displays offline players as well)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("list").executes(context -> {
            MinecraftServer server = context.getSource().getServer();
            List<PlayerPair> pairList = PlayerPairManager.getInstance(server).getPairs();

            if (pairList.isEmpty()) {
                // context.getSource().sendFeedback(() -> Text.literal("No one is paired at the moment."), false);
                context.getSource().sendMessage(Text.literal("No one is paired at the moment."));
            }

            PlayerPairManager.getInstance(server).getPairs().forEach(pair -> {
                context.getSource().sendMessage(Text.literal(pair.pairMessage()));
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

        ServerPlayerEvents.AFTER_RESPAWN.register(((oldPlayer, playerThatJustRespawned, alive) -> {
            MinecraftServer server = playerThatJustRespawned.getServer();
            if (server != null) {
                if (PlayerPairManager.getInstance(server).isInAPair(playerThatJustRespawned)) {
                    PlayerPair joinedPair = PlayerPairManager.getInstance(server).getPair(playerThatJustRespawned.getUuid());
                    ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(joinedPair.getOtherPairUUID(playerThatJustRespawned.getUuid()));
                    if (player2 != null && player2.isAlive()) {
                        playerThatJustRespawned.setHealth(player2.getHealth()); // sync healths on respawn
                    }
                }
            }
        }));

        // randomize command (pick random players, excludes online players from given list, and excludes user if remaining players after exclusion are uneven)
        // needs the names to be connected by a hypen i.e. "NotCarrotC-CarrotC"
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("randomize")
                .executes(context -> randomizePairs(context, ""))
                .then(argument("excludeList", StringArgumentType.string())
                .executes(context -> randomizePairs(context, StringArgumentType.getString(context, "excludeList"))))));
    }

    private int randomizePairs(CommandContext<ServerCommandSource> context, String excludeInput) {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayerEntity user = context.getSource().getPlayer();
        PlayerPairManager.getInstance(server).clearPairs();
        // split the exclude input into a list, then get a list of the online players & remove any that are wanting to be excluding
        List<String> excludeList = List.of(excludeInput.split("-"));
        List<ServerPlayerEntity> onlinePlayers = server.getPlayerManager().getPlayerList();
        List<ServerPlayerEntity> targetPlayers = new ArrayList<>();
        for (ServerPlayerEntity onlinePlayer : onlinePlayers) {
            if (!excludeList.contains(onlinePlayer.getName().getString())) {
                targetPlayers.add(onlinePlayer);
            }
        }

        // check if even players. if not, remove user from target list to make sure everyone has fun
        if (targetPlayers.size() % 2 != 0) {
            // ServerPatcher.LOGGER.info("Uneven players, removing user form list");
            if (user != null) {
                targetPlayers.remove(user);
            } else {
                // context.getSource().sendFeedback(() -> Text.literal("User wasn't a player, uhoh!"), false);
                context.getSource().sendMessage(Text.literal("User wasn't a player, uhoh!"));
                return -1;
            }
        }

        // can't pair if there is only 1 player in the list
        if (targetPlayers.size() == 1) {
            // context.getSource().sendFeedback(() -> Text.literal("Not enough players to make random pairs..."), false);
            context.getSource().sendMessage(Text.literal("Not enough players to make random pairs..."));
            return -1;
        }

        Collections.shuffle(targetPlayers); // randomize list

        // iterate i over targetPlayers list
        // p1 gets assigned an even number
        // p2 gets assigned an odd number
        // when p1 and p2 have an assignment they are paired and then cleared. for the list to repeat till end.
        ServerPlayerEntity p1 = null;
        ServerPlayerEntity p2 = null;
        for (int i = 0; i < targetPlayers.size(); i++) {
            if (i % 2 == 0) p1 = targetPlayers.get(i);
            if (i % 2 == 1) p2 = targetPlayers.get(i);

            if (p1 != null && p2 != null) {
                try {
                    PlayerPairManager.getInstance(server).addPair(p1, p2);
                } catch (NullPairingException | AlreadyPairedException ignored) {
                }
                p1 = null;
                p2 = null;
            }
        }

        // context.getSource().sendFeedback(() -> Text.literal("Players randomized"), false);
        context.getSource().sendMessage(Text.literal("Players randomized"));
        return 1;
    }
}