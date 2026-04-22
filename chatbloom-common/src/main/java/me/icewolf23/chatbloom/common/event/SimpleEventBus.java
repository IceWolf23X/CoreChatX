package me.icewolf23.chatbloom.common.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SimpleEventBus implements EventBus {

    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    @Override
    public synchronized <T> void subscribe(Class<T> type, Consumer<T> consumer) {
        listeners.computeIfAbsent(type, ignored -> new ArrayList<>()).add(consumer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void publish(Object event) {
        List<Consumer<?>> consumers = listeners.getOrDefault(event.getClass(), List.of());
        for (Consumer<?> consumer : consumers) {
            ((Consumer<Object>) consumer).accept(event);
        }
    }
}
