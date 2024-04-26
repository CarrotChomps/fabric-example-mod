package com.carrotc.serverpatcher;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.HashMap;
import java.util.UUID;

public class StateSaverAndLoader extends PersistentState {

    public static final String PLAYER_NAME_MAP_KEY = "playerNameMap";
    public static HashMap<String, UUID> nameToUUIDMap = new HashMap<>();

    public HashMap<UUID, PlayerData> players = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // writing to server
        NbtCompound mapTag = new NbtCompound();
        nameToUUIDMap.forEach((s, u) -> {
            mapTag.putString(s, u.toString());
        });
        nbt.put(PLAYER_NAME_MAP_KEY, mapTag);

        // writing to playerData
        NbtCompound playersNbt = new NbtCompound();
        players.forEach((uuid, playerData) -> {
            NbtCompound playerNbt = new NbtCompound();

            playerNbt.putUuid("pair", playerData.pair);

            playersNbt.put(uuid.toString(), playerNbt);
        });
        nbt.put("players", playersNbt);

        return nbt;
    }

    public static StateSaverAndLoader createFromNbt(NbtCompound tag) {
        StateSaverAndLoader state = new StateSaverAndLoader();
        // reading to server
        NbtCompound mapTag = tag.getCompound(PLAYER_NAME_MAP_KEY);
        mapTag.getKeys().forEach(playerName -> {
            UUID uuid = UUID.fromString(mapTag.getString(playerName));
            nameToUUIDMap.put(playerName, uuid);
        });

        // reading to playerData
        NbtCompound playersNbt = tag.getCompound("players");
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData();

            playerData.pair = playersNbt.getCompound(key).getUuid("pair");

            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        return state;
    }

    public static UUID getOfflineUUID(String name) {
        return nameToUUIDMap.get(name);
    }

    /**
     * This function gets the 'PersistentStateManager' and creates or returns the filled in 'StateSaveAndLoader'.
     * It does this by calling 'StateSaveAndLoader::createFromNbt' passing it the previously saved 'NbtCompound' we wrote in 'writeNbt'.
     */
    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        StateSaverAndLoader state = persistentStateManager.getOrCreate(
                StateSaverAndLoader::createFromNbt,
                StateSaverAndLoader::new,
                ServerPatcher.MOD_ID
        );

        // If state is not marked dirty, when Minecraft closes, 'writeNbt' won't be called and therefore nothing will be saved.
        // Technically it's 'cleaner' if you only mark state as dirty when there was actually a change, but the vast majority
        // of mod writers are just going to be confused when their data isn't being saved, and so it's best just to 'markDirty' for them.
        // Besides, it's literally just setting a bool to true, and the only time there's a 'cost' is when the file is written to disk when
        // there were no actual change to any of the mods state (INCREDIBLY RARE).
        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        return getPlayerState(player.getUuid(), player.getServer());
    }

    public static PlayerData getPlayerState(UUID u, MinecraftServer server) {
        StateSaverAndLoader serverState = getServerState(server);

        // Either get the player by the uuid, or we don't have data for him yet, make a new player state
        return serverState.players.computeIfAbsent(u, uuid -> new PlayerData());
    }

    public void savePlayerData(UUID u, PlayerData data) {
        players.put(u, data);
    }
}
