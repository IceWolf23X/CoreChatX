package me.icewolf23.chatbloom.common.locale;

import net.kyori.adventure.text.Component;

import java.util.UUID;

public interface LocaleService {
    Component message(UUID playerId, String key);
}
