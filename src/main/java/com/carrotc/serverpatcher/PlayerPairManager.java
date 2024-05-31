package com.carrotc.serverpatcher;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the memory side of pairs
 */
public class PlayerPairManager extends PersistentState {

    public static final String PAIRS_KEY = "pairs";
    private static final List<PlayerPair> pairs = new ArrayList<>();

    private PlayerPairManager() {
    }

    public static PlayerPairManager getInstance(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        PlayerPairManager instance = persistentStateManager.getOrCreate(
                PlayerPairManager::deserialize,
                PlayerPairManager::new,
                ServerPatcher.MOD_ID);

        instance.markDirty();

        return instance;
    }

    // You can only link online players.
    public void addPair(ServerPlayerEntity p1, ServerPlayerEntity p2) throws NullPairingException, AlreadyPairedException {
        if (p1 == null) {
            throw new NullPairingException("player1");
        }
        if (p2 == null) {
            throw new NullPairingException("player2");
        }

        // if p1 and p2 are not in a pair, we can make a pair with them
        if (!isInAPair(p1)) {
            if (!isInAPair(p2)) {
                PlayerPair pair = new PlayerPair(p1, p2);

                pairs.add(pair);
            } else {
                throw new AlreadyPairedException(p2);
            }
        } else {
            throw new AlreadyPairedException(p1);
        }
    }

    public PlayerPair removePair(String target) {
        PlayerPair removingPair = null;
        for (PlayerPair pair : pairs) {
            if (pair.has(target)) {
                removingPair = pair;
            }
        }
        pairs.remove(removingPair);
        return removingPair;
    }

    public boolean isInAPair(ServerPlayerEntity p) {
        for (PlayerPair pair : pairs) {
            if (pair.has(p.getUuid())) {
                return true;
            }
        }
        return false;
    }

    public PlayerPair getPair(UUID p1) {
        // ServerPatcher.LOGGER.info("Looking for a pair that has \"" + p1 + "\"");
        for (PlayerPair pair : pairs) {
            // ServerPatcher.LOGGER.info("\"" + p1 + "\" vs. \"" + pair.getUUID1() + "\"");
            // ServerPatcher.LOGGER.info("\"" + p1 + "\" vs. \"" + pair.getUUID2() + "\"");
            // ServerPatcher.LOGGER.info("==============================================");
            if (pair.has(p1)) {
                return pair;
            }
        }
        return null;
    }

    public PlayerPair getPair(String name) {
        for (PlayerPair pair : pairs) {
            if (pair.has(name)) {
                return pair;
            }
        }
        return null;
    }

    public List<PlayerPair> getPairs() {
        return pairs;
    }

    public void setPairMaxHealth(MinecraftServer server, PlayerPair pair, float amount) {
        if (pair == null) {
            return;
        }
        pair.setMaxHealth(amount);
        ServerPlayerEntity p1 = server.getPlayerManager().getPlayer(pair.getUUID1());
        ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(pair.getUUID2());

        if (p1 != null) updateMaxHealthAttribute(p1, amount);
        if (p2 != null) updateMaxHealthAttribute(p2, amount);
    }

    public void updateMaxHealthAttribute(ServerPlayerEntity p, float amount) {
        if (p != null) {
            // clear the health to set the player back to 20.0 health
            // TODO: you can probably clear the modifiers based on a key to avoid collisions in case there is other sources that change max health...
            p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).clearModifiers();

            float healthOperation = amount - p.getMaxHealth();

            // then update the health to the new max
            p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                    .addPersistentModifier(
                            new EntityAttributeModifier("ServerPatcher", healthOperation, EntityAttributeModifier.Operation.ADDITION
                            )
                    );
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound pairsNbt = new NbtCompound();
        for (int i = 0; i < pairs.size(); i++) {
            pairsNbt.putString(String.valueOf(i), pairs.get(i).serialize());
        }
        nbt.put(PAIRS_KEY, pairsNbt);
        return nbt;
    }

    private static PlayerPairManager deserialize(NbtCompound tag) {
        PlayerPairManager playerPairManager = new PlayerPairManager();

        NbtCompound pairsTag = tag.getCompound(PAIRS_KEY);
        pairsTag.getKeys().forEach(k -> {
            pairs.add(new PlayerPair(pairsTag.getString(k)));
        });

        return playerPairManager;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("[");
        for (PlayerPair pair : pairs) {
            output.append(pair.serialize());
            output.append(", ");
        }
        output.append("]");
        return output.toString();
    }

    public void clearPairs() {
        pairs.clear();
    }
}
