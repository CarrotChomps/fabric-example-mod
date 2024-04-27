package com.carrotc.serverpatcher;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerPair {

    private final UUID uuid1;
    private final String name1;

    private final UUID uuid2;
    private final String name2;

    @Setter
    @Getter
    private boolean beenDamaged = false;
    @Getter
    @Setter
    private boolean beenHealed = false;

    @Getter
    @Setter
    private float recentDamage = 0;

    public PlayerPair(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        this.uuid1 = player1.getUuid();
        this.name1 = player1.getName().getString();

        this.uuid2 = player2.getUuid();
        this.name2 = player2.getName().getString();
    }

    public PlayerPair(String serializedString) {
        Pattern firstNamePattern = Pattern.compile("(?<=1:)(.*?)(?=\\()");
        Matcher firstNameMatcher = firstNamePattern.matcher(serializedString);

        Pattern secondNamePattern = Pattern.compile("(?<=2:)(.*?)(?=\\()");
        Matcher secondNameMatcher = secondNamePattern.matcher(serializedString);

        Pattern uuidPattern = Pattern.compile("(?<=\\()(.*?)(?=\\))");
        Matcher uuidMatcher = uuidPattern.matcher(serializedString);

        this.name1 = firstNameMatcher.find() ? firstNameMatcher.group() : null;
        this.name2 = secondNameMatcher.find() ? secondNameMatcher.group() : null;

        this.uuid1 = uuidMatcher.find() ? UUID.fromString(uuidMatcher.group()) : null;
        this.uuid2 = uuidMatcher.find() ? UUID.fromString(uuidMatcher.group()) : null;
    }

    public UUID getUUID1() {
        return uuid1;
    }

    public UUID getUUID2() {
        return uuid2;
    }

    public boolean has(UUID uuid) {
        return uuid.equals(uuid1) || uuid.equals(uuid2);
    }

    public boolean has(String name) {
        return name.equals(name1) || name.equals(name2);
    }

    public String serialize() {
        return String.format("1:%s(%s) ; 2:%s(%s)", name1, uuid1, name2, uuid2);
    }

    public String pairMessage() {
        return String.format("%s is paired with %s.", name1, name2);
    }

    public UUID getOtherPairUUID(UUID uuid) {
        if (uuid.equals(uuid1)) {
            return uuid2;
        } else {
            return uuid1;
        }
    }
}
