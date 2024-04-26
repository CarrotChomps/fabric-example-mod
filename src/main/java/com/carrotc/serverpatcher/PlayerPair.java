package com.carrotc.serverpatcher;

import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerPair {

    private final ServerPlayerEntity player1;
    private final ServerPlayerEntity player2;

    public PlayerPair(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public ServerPlayerEntity getPlayer1() {
        return player1;
    }

    public ServerPlayerEntity getPlayer2() {
        return player2;
    }

    public boolean has(ServerPlayerEntity player) {
        return player == player1 || player == player2;
    }
}
