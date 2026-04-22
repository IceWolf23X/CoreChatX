package me.icewolf23.chatbloom.common.event;

import java.util.function.Consumer;

public interface EventBus {
    <T> void subscribe(Class<T> type, Consumer<T> consumer);

    void publish(Object event);
}
