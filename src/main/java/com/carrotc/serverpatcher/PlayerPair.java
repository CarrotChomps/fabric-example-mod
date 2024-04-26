package com.carrotc.serverpatcher;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class PlayerPair {

    private final UUID player1;
    private final UUID player2;

    public PlayerPair(UUID player1, UUID player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public boolean has(UUID player) {
        return player == player1 || player == player2;
    }
}
