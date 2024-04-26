package com.carrotc.serverpatcher;


import java.util.UUID;

public class PlayerData {
    public UUID pair;

    public void setPair(UUID pair) {
        this.pair = pair;
    }

    public void removePair() {
        this.pair = null;
    }

    public UUID getPair() {
        return pair;
    }
}
