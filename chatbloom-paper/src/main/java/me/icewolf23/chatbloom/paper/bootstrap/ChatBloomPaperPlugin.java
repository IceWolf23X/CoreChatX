package me.icewolf23.chatbloom.paper.bootstrap;

import icewolf23x.chatBloom.ChatBloom;
import icewolf23x.chatBloom.hook.HookService;
import me.icewolf23.chatbloom.common.config.DeploymentMode;

public final class ChatBloomPaperPlugin extends ChatBloom {

    @Override
    public void onEnable() {
        saveDefaultConfigFiles();
        try {
            this.configRegistry = new ConfigRegistry(this);
            this.configRegistry.loadAll();
            this.bootDeploymentConfig = configRegistry.deployment();

            getLogger().info("ChatBloom deployment mode: " + bootDeploymentConfig.mode());

            this.repositoryRegistry = new RepositoryRegistry(this, configRegistry);
            this.repositoryRegistry.initialize();
            this.serviceRegistry = new ServiceRegistry(this, configRegistry, repositoryRegistry);
            this.serviceRegistry.initialize();
            this.bridgeRegistry = new BridgeRegistry(this, configRegistry, serviceRegistry);
            this.bridgeRegistry.initialize();
            this.commandRegistry = new CommandRegistry(this);
            this.commandRegistry.registerAll();
            this.listenerRegistry = new ListenerRegistry(this);
            this.listenerRegistry.registerAll();
            this.hookService = new HookService(this);
            hookService.refresh();

            if (bootDeploymentConfig.mode() == DeploymentMode.PROXY) {
                getLogger().warning("Proxy mode is configured. A full restart is required after deployment mode changes, and remote chat/PM routing depends on a matching ChatBloom Velocity proxy install.");
            }
        } catch (Exception exception) {
            getLogger().severe("ChatBloom could not start cleanly: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        boolean autoSave = configRegistry == null || configuration().main().getBoolean("player-data.auto-save-on-disable", true);
        if (repositoryRegistry != null && autoSave) {
            repositoryRegistry.shutdown();
        }
        if (serviceRegistry != null && serviceRegistry.snapshotRegistry() != null) {
            serviceRegistry.snapshotRegistry().clear();
        }
    }

    @Override
    public void reloadPlugin() {
        repositoryRegistry.shutdown();
        configRegistry.reloadAll();
        if (configRegistry.deployment().mode() != bootDeploymentConfig.mode() && configRegistry.deployment().requireFullRestartOnModeChange()) {
            getLogger().warning("Deployment mode changed from " + bootDeploymentConfig.mode() + " to " + configRegistry.deployment().mode() + ". A full restart is required before the new mode becomes active.");
        }
        repositoryRegistry.reload();
        hookService.refresh();
        serviceRegistry.reload();
        bridgeRegistry.reload();
    }

    private void saveDefaultConfigFiles() {
        saveResourceIfAbsent("config.yml");
        saveResourceIfAbsent("messages.yml");
        saveResourceIfAbsent("chat.yml");
        saveResourceIfAbsent("pings.yml");
        saveResourceIfAbsent("filter.yml");
        saveResourceIfAbsent("chatitems.yml");
        saveResourceIfAbsent("playerdata.yml");
        saveResourceIfAbsent("state.yml");
        saveResourceIfAbsent("channels.yml");
        saveResourceIfAbsent("privacy.yml");
        saveResourceIfAbsent("moderation.yml");
        saveResourceIfAbsent("storage.yml");
        saveResourceIfAbsent("discord.yml");
        saveResourceIfAbsent("telegram.yml");
    }

    private void saveResourceIfAbsent(String name) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        java.io.File file = new java.io.File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }
}
