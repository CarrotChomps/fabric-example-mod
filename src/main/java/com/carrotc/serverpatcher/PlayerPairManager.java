package com.carrotc.serverpatcher;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the memory side of pairs
 */
public enum PlayerPairManager {
    INSTANCE;

    public static final List<PlayerPair> pairs = new ArrayList<>();

    public void addPair(UUID p1, UUID p2) throws NullPairingException, AlreadyPairedException {
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

    public PlayerPair removePair(UUID p1) {
        PlayerPair removingPair = null;
        for (PlayerPair pair : pairs) {
            if (pair.has(p1)) {
                removingPair = pair;
            }
        }
        pairs.remove(removingPair);
        return removingPair;
    }

    public boolean isInAPair(UUID p1) {
        for (PlayerPair pair : pairs) {
            if (pair.has(p1)) {
                return true;
            }
        }
        return false;
    }

    public PlayerPair getPair(UUID p1) {
        for (PlayerPair pair : pairs) {
            if (pair.has(p1)) {
                return pair;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("[");
        for (PlayerPair pair : pairs) {
            output.append("(");
            output.append(pair.getPlayer1());
            output.append(", ");
            output.append(pair.getPlayer2());
            output.append("), ");
        }
        return output.toString();
    }
}
