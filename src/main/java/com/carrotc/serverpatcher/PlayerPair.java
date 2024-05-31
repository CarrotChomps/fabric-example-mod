package com.carrotc.serverpatcher;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerPair {

    private final UUID uuid1;
    @Getter
    private final String name1;

    private final UUID uuid2;
    @Getter
    private final String name2;

    @Setter
    @Getter
    private boolean beenDamaged = false;
    @Setter
    @Getter
    private boolean beenKilled = false;
    @Getter
    @Setter
    private boolean beenHealed = false;
    @Getter
    @Setter
    private boolean usedTotem = false;

    @Getter
    @Setter
    private float recentDamage = 0;

    @Getter
    private float maxHealth = 20;

    public PlayerPair(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        this.uuid1 = player1.getUuid();
        this.name1 = player1.getName().getString();

        this.uuid2 = player2.getUuid();
        this.name2 = player2.getName().getString();
    }

    public PlayerPair(String serializedString) {
        Pattern firstNamePattern = Pattern.compile("(?<=1:)(.*?)(?=\\()"); // looks for the first set of characters after "1:" and before "("
        Matcher firstNameMatcher = firstNamePattern.matcher(serializedString);

        Pattern secondNamePattern = Pattern.compile("(?<=2:)(.*?)(?=\\()"); // looks for the second set of characters after "1:" and before "("
        Matcher secondNameMatcher = secondNamePattern.matcher(serializedString);

        Pattern uuidPattern = Pattern.compile("(?<=\\()(.*?)(?=\\))"); // looks for any set of characters within "(" and ")"
        Matcher uuidMatcher = uuidPattern.matcher(serializedString);

        Pattern maxHealthPattern = Pattern.compile("(?<=\\[)(.*?)(?=])"); // looks for any set of characters within "[" and "]"
        Matcher maxHealthMatcher = maxHealthPattern.matcher(serializedString);

        this.name1 = firstNameMatcher.find() ? firstNameMatcher.group() : null;
        this.name2 = secondNameMatcher.find() ? secondNameMatcher.group() : null;

        this.uuid1 = uuidMatcher.find() ? UUID.fromString(uuidMatcher.group()) : null;
        this.uuid2 = uuidMatcher.find() ? UUID.fromString(uuidMatcher.group()) : null;

        this.maxHealth = maxHealthMatcher.find() ? Float.parseFloat(maxHealthMatcher.group()) : 20.0f;
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
        return String.format("1:%s(%s) ; 2:%s(%s)[%s]", name1, uuid1, name2, uuid2, maxHealth);
    }

    public String pairMessage() {
        return String.format("%s <-> %s [%s].", name1, name2, maxHealth);
    }

    public UUID getOtherPairUUID(UUID uuid) {
        if (uuid.equals(uuid1)) {
            return uuid2;
        } else {
            return uuid1;
        }
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }
}
