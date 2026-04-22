package me.icewolf23.chatbloom.paper.model;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Set;

public record ProcessedMessage(
    Component component,
    String plainText,
    Set<Player> notificationTargets,
    Set<Player> bypassTargets
) {
}
