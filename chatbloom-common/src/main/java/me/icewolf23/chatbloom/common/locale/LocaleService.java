package me.icewolf23.chatbloom.common.locale;

import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.UUID;

public interface LocaleService {
    Component message(UUID playerId, String key);

    default String template(UUID playerId, String key) {
        return null;
    }

    default Set<String> availableLocales() {
        return Set.of();
    }

    default boolean isSupported(String localeTag) {
        return localeTag != null && availableLocales().contains(localeTag.toLowerCase(java.util.Locale.ROOT));
    }
}
