package me.icewolf23.chatbloom.paper.pipeline;

import me.icewolf23.chatbloom.common.channel.ChannelService;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineStep;

public final class ActiveChannelResolutionStep implements ChatPipelineStep {

    private final ChannelService channelService;

    public ActiveChannelResolutionStep(ChannelService channelService) {
        this.channelService = channelService;
    }

    @Override
    public void apply(ChatPipelineContext context) {
        context.channelId(channelService.getActiveChannel(context.senderId()));
    }
}
