package com.carrotc.serverpatcher;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class AlreadyPairedException extends Throwable {

    private final ServerPlayerEntity attemptedPair;

    public AlreadyPairedException(ServerPlayerEntity attemptedPair) {
        this.attemptedPair = attemptedPair;
    }

    public ServerPlayerEntity getAttemptedPair() {
        return attemptedPair;
    }
}
