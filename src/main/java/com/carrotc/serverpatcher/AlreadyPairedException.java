package com.carrotc.serverpatcher;

import java.util.UUID;

public class AlreadyPairedException extends Throwable {

    private final UUID attemptedPair;

    public AlreadyPairedException(UUID attemptedPair) {
        this.attemptedPair = attemptedPair;
    }

    public UUID getAttemptedPair() {
        return attemptedPair;
    }
}
