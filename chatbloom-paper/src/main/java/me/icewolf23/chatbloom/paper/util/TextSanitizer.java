package me.icewolf23.chatbloom.paper.util;

import java.util.regex.Pattern;

public final class TextSanitizer {

    private static final Pattern MINI_MESSAGE = Pattern.compile("<[^>\\r\\n]+>");
    private static final Pattern AMPERSAND_HEX = Pattern.compile("(?i)&#[0-9a-f]{6}");
    private static final Pattern AMPERSAND_VERBOSE_HEX = Pattern.compile("(?i)&x(?:&[0-9a-f]){6}");
    private static final Pattern SECTION_CODES = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private TextSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        String result = input.replace('\n', ' ').replace('\r', ' ');
        result = MINI_MESSAGE.matcher(result).replaceAll("");
        result = AMPERSAND_VERBOSE_HEX.matcher(result).replaceAll("");
        result = AMPERSAND_HEX.matcher(result).replaceAll("");
        result = SECTION_CODES.matcher(result).replaceAll("");
        result = CONTROL.matcher(result).replaceAll("");
        return result;
    }
}
