package me.icewolf23.chatbloom.paper.bridge;

import me.icewolf23.chatbloom.common.bridge.BridgeMessage;
import me.icewolf23.chatbloom.common.bridge.OutboundBridge;
import me.icewolf23.chatbloom.paper.ChatBloom;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class DiscordOutboundBridge implements OutboundBridge {

    private final ChatBloom plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DiscordOutboundBridge(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "discord";
    }

    @Override
    public boolean isEnabled() {
        return plugin.configs().discord().getBoolean("discord.enabled", false)
            && !plugin.configs().discord().getString("discord.bot-token", "").isBlank()
            && !plugin.configs().discord().getString("discord.default-channel-id", "").isBlank();
    }

    @Override
    public void forward(BridgeMessage message) {
        if (!isEnabled()) {
            return;
        }
        String token = plugin.configs().discord().getString("discord.bot-token", "");
        String channelId = plugin.configs().discord().getString("discord.channel-overrides." + message.channelId(),
            plugin.configs().discord().getString("discord.default-channel-id", ""));
        if (channelId.isBlank()) {
            return;
        }
        String formatted = format(
            plugin.configs().discord().getString("discord.format", "[{source_server}] [{channel_id}] {sender_name}: {plain_text}"),
            message
        );
        String body = "{\"content\":\"" + escapeJson(formatted) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://discord.com/api/v10/channels/" + channelId + "/messages"))
            .header("Authorization", "Bot " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    plugin.getLogger().warning("Discord bridge returned HTTP " + response.statusCode() + " for channel '" + message.channelId() + "'.");
                }
            })
            .exceptionally(throwable -> {
                plugin.getLogger().warning("Discord bridge failed for channel '" + message.channelId() + "': " + throwable.getMessage());
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

    private String escapeJson(String input) {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}
