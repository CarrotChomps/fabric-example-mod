package com.carrotc.serverpatcher;

import java.util.UUID;

public class NullPairingException extends Throwable {
    private final String source;

    public NullPairingException(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
