package me.icewolf23.chatbloom.paper.service;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.icewolf23.chatbloom.paper.model.FilteredToken;
import me.icewolf23.chatbloom.paper.util.TemplatePlaceholderNormalizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class WordFilterService {

    private final ChatBloom plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private boolean enabled;
    private boolean hoverOriginal;
    private String hoverTemplate;
    private char replacementCharacter;
    private Set<String> blockedWords = new HashSet<>();

    public WordFilterService(ChatBloom plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        var config = plugin.configuration().filter();
        this.enabled = config.getBoolean("enabled", true);
        this.hoverOriginal = config.getBoolean("hover-original.enabled", false);
        this.hoverTemplate = config.getString("hover-original.text", "<gray>{word}</gray>");
        String rawReplacement = config.getString("replacement-character", "*");
        this.replacementCharacter = rawReplacement == null || rawReplacement.isEmpty() ? '*' : rawReplacement.charAt(0);
        this.blockedWords = new HashSet<>();
        for (String word : config.getStringList("blocked-words")) {
            if (word != null && !word.isBlank()) {
                blockedWords.add(word.toLowerCase(Locale.ROOT));
            }
        }
    }

    public FilteredToken filterToken(String token) {
        if (token == null) {
            return new FilteredToken("", Component.empty());
        }
        if (!enabled || token.isBlank()) {
            return new FilteredToken(token, Component.text(token));
        }
        if (!blockedWords.contains(token.toLowerCase(Locale.ROOT))) {
            return new FilteredToken(token, Component.text(token));
        }
        String replacement = String.valueOf(replacementCharacter).repeat(token.length());
        Component rendered = Component.text(replacement);
        if (hoverOriginal) {
            Component hover = miniMessage.deserialize(TemplatePlaceholderNormalizer.normalize(hoverTemplate), Placeholder.unparsed("word", token));
            rendered = rendered.hoverEvent(HoverEvent.showText(hover));
        }
        return new FilteredToken(replacement, rendered);
    }

    public String stripFormatting(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
