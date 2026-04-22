package me.icewolf23.chatbloom.paper.locale;

import me.icewolf23.chatbloom.common.locale.LocaleService;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;
import me.icewolf23.chatbloom.paper.ChatBloom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PaperLocaleService implements LocaleService {

    private static final String DEFAULT_LOCALE = "en_us";

    private final ChatBloom plugin;
    private final PlayerStateRepository playerStateRepository;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, FileConfiguration> localeCache = new HashMap<>();

    public PaperLocaleService(ChatBloom plugin, PlayerStateRepository playerStateRepository) {
        this.plugin = plugin;
        this.playerStateRepository = playerStateRepository;
    }

    @Override
    public Component message(UUID playerId, String key) {
        String template = template(playerId, key);
        return template == null ? Component.empty() : miniMessage.deserialize(template);
    }

    @Override
    public String template(UUID playerId, String key) {
        String localeTag = normalizeLocale(playerId == null ? DEFAULT_LOCALE : playerStateRepository.load(playerId).localeTag());
        String localized = locale(localeTag).getString(key);
        if (localized != null) {
            return localized;
        }
        return DEFAULT_LOCALE.equals(localeTag) ? null : locale(DEFAULT_LOCALE).getString(key);
    }

    @Override
    public Set<String> availableLocales() {
        File folder = localeFolder();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            return Set.of(DEFAULT_LOCALE);
        }
        LinkedHashSet<String> locales = new LinkedHashSet<>();
        for (File file : files) {
            String name = file.getName();
            locales.add(name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT));
        }
        locales.add(DEFAULT_LOCALE);
        return java.util.Collections.unmodifiableSet(locales);
    }

    @Override
    public boolean isSupported(String localeTag) {
        return availableLocales().contains(normalizeLocale(localeTag));
    }

    public String normalizeLocaleTag(String localeTag) {
        return normalizeLocale(localeTag);
    }

    private FileConfiguration locale(String localeTag) {
        return localeCache.computeIfAbsent(localeTag, ignored -> {
            File file = new File(localeFolder(), localeTag + ".yml");
            return file.exists() ? YamlConfiguration.loadConfiguration(file) : YamlConfiguration.loadConfiguration(new File(localeFolder(), DEFAULT_LOCALE + ".yml"));
        });
    }

    private File localeFolder() {
        File folder = new File(plugin.getDataFolder(), "locales");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private String normalizeLocale(String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return DEFAULT_LOCALE;
        }
        return localeTag.toLowerCase(Locale.ROOT);
    }
}
