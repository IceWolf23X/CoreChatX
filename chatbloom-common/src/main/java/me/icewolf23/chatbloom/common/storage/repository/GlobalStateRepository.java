package me.icewolf23.chatbloom.common.storage.repository;

import me.icewolf23.chatbloom.common.model.GlobalStateRecord;

public interface GlobalStateRepository {
    GlobalStateRecord load();

    void save(GlobalStateRecord record);
}
