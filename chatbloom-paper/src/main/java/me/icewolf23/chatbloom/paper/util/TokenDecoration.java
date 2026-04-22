package me.icewolf23.chatbloom.paper.util;

public record TokenDecoration(String prefix, String core, String suffix) {

    private static final String EDGE_PUNCTUATION =
        "\"'(){}<>.,!?;:"
            + "\u00AB\u00BB"
            + "\u2018\u2019\u201A\u201B"
            + "\u201C\u201D\u201E\u201F"
            + "\u2026"
            + "\uFF0C\u3002\uFF01\uFF1F\uFF1B\uFF1A";

    public static TokenDecoration from(String token) {
        int start = 0;
        int end = token.length();
        while (start < end && isEdgePunctuation(token.charAt(start))) {
            start++;
        }
        while (end > start && isEdgePunctuation(token.charAt(end - 1))) {
            end--;
        }
        return new TokenDecoration(token.substring(0, start), token.substring(start, end), token.substring(end));
    }

    private static boolean isEdgePunctuation(char character) {
        if (EDGE_PUNCTUATION.indexOf(character) >= 0) {
            return true;
        }
        int type = Character.getType(character);
        return type == Character.INITIAL_QUOTE_PUNCTUATION || type == Character.FINAL_QUOTE_PUNCTUATION;
    }
}
