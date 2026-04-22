package me.icewolf23.chatbloom.paper.bridge;

import me.icewolf23.chatbloom.common.bridge.BridgeMessage;
import me.icewolf23.chatbloom.common.bridge.OutboundBridge;
import me.icewolf23.chatbloom.paper.ChatBloom;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class TelegramOutboundBridge implements OutboundBridge {

    private final ChatBloom plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TelegramOutboundBridge(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "telegram";
    }

    @Override
    public boolean isEnabled() {
        return plugin.configs().telegram().getBoolean("telegram.enabled", false)
            && !plugin.configs().telegram().getString("telegram.bot-token", "").isBlank()
            && !plugin.configs().telegram().getString("telegram.default-chat-id", "").isBlank();
    }

    @Override
    public void forward(BridgeMessage message) {
        if (!isEnabled()) {
            return;
        }
        String token = plugin.configs().telegram().getString("telegram.bot-token", "");
        String chatId = plugin.configs().telegram().getString("telegram.chat-overrides." + message.channelId(),
            plugin.configs().telegram().getString("telegram.default-chat-id", ""));
        if (chatId.isBlank()) {
            return;
        }
        String text = format(
            plugin.configs().telegram().getString("telegram.format", "[{source_server}] [{channel_id}] {sender_name}: {plain_text}"),
            message
        );
        String form = "chat_id=" + encode(chatId) + "&text=" + encode(text);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
            .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    plugin.getLogger().warning("Telegram bridge returned HTTP " + response.statusCode() + " for channel '" + message.channelId() + "'.");
                }
            })
            .exceptionally(throwable -> {
                plugin.getLogger().warning("Telegram bridge failed for channel '" + message.channelId() + "': " + throwable.getMessage());
                return null;
            });
    }

    private String format(String template, BridgeMessage message) {
        return template
            .replace("{source_type}", message.sourceType())
            .replace("{source_server}", message.sourceServer())
            .replace("{channel_id}", message.channelId())
            .replace("{sender_name}", message.senderName())
            .replace("{plain_text}", message.plainText());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
