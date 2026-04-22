package me.icewolf23.chatbloom.paper.service;

import me.icewolf23.chatbloom.paper.ChatBloom;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LegacyFormattingService {

    private static final Map<Character, String> PERMISSIONS = new HashMap<>();

    static {
        register("color.black", '0');
        register("color.dark_blue", '1');
        register("color.dark_green", '2');
        register("color.dark_aqua", '3');
        register("color.dark_red", '4');
        register("color.dark_purple", '5');
        register("color.gold", '6');
        register("color.gray", '7');
        register("color.dark_gray", '8');
        register("color.blue", '9');
        register("color.green", 'a');
        register("color.aqua", 'b');
        register("color.red", 'c');
        register("color.light_purple", 'd');
        register("color.yellow", 'e');
        register("color.white", 'f');

        register("style.magic", 'k');
        register("style.bold", 'l');
        register("style.strikethrough", 'm');
        register("style.underline", 'n');
        register("style.italic", 'o');
        register("style.reset", 'r');
    }

    private final ChatBloom plugin;

    public LegacyFormattingService(ChatBloom plugin) {
        this.plugin = plugin;
    }

    public void reload() {
    }

    public String sanitizeForSender(CommandSender sender, String input) {
        String sanitized = me.icewolf23.chatbloom.paper.util.TextSanitizer.sanitize(input);
        return filterAllowedCodes(sender, sanitized);
    }

    public String sanitizeForPlayer(Player player, String input) {
        return sanitizeForSender(player, input);
    }

    public String stripCodes(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if (current == '&' && index + 1 < input.length() && isLegacyCode(input.charAt(index + 1))) {
                index++;
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }

    private String filterAllowedCodes(CommandSender sender, String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if (current == '&' && index + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(index + 1));
                if (isLegacyCode(code)) {
                    if (isAllowed(sender, code)) {
                        result.append('&').append(code);
                    }
                    index++;
                    continue;
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private boolean isAllowed(CommandSender sender, char code) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        String permission = PERMISSIONS.get(Character.toLowerCase(code));
        return permission == null || player.hasPermission(permission);
    }

    private static boolean isLegacyCode(char code) {
        char normalized = Character.toLowerCase(code);
        return PERMISSIONS.containsKey(normalized);
    }

    private static void register(String suffix, char code) {
        PERMISSIONS.put(Character.toLowerCase(code), "chatbloom.format." + suffix.toLowerCase(Locale.ROOT));
    }
}
