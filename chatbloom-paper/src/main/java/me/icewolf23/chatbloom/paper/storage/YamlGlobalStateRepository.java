package me.icewolf23.chatbloom.paper.storage;

import me.icewolf23.chatbloom.paper.data.GlobalStateStore;
import me.icewolf23.chatbloom.common.model.GlobalStateRecord;
import me.icewolf23.chatbloom.common.storage.repository.GlobalStateRepository;

public final class YamlGlobalStateRepository implements GlobalStateRepository {

    private final GlobalStateStore globalStateStore;

    public YamlGlobalStateRepository(GlobalStateStore globalStateStore) {
        this.globalStateStore = globalStateStore;
    }

    @Override
    public GlobalStateRecord load() {
        return new GlobalStateRecord(globalStateStore.getFirstJoinCount(), globalStateStore.isChatMuted());
    }

    @Override
    public void save(GlobalStateRecord record) {
        globalStateStore.setFirstJoinCount(record.firstJoinCount());
        globalStateStore.setChatMuted(record.chatMuted());
    }
}
