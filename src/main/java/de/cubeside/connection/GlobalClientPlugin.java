package de.cubeside.connection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

@Plugin(id = "globalconnectionvelocity", name = "Global Connection Velocity", version = "0.0.1-SNAPSHOT", url = "https://cubeside.de", description = "GlobalClient", authors = {"Cubeside"})
public class GlobalClientPlugin {
    private GlobalClientVelocity globalClient;

    private PlayerPropertiesImplementation propertiesAPI;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private CommentedConfigurationNode configuration;

    @Inject
    public GlobalClientPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        saveDefaultConfig();
        globalClient = new GlobalClientVelocity(this);

        reconnectClient();

        propertiesAPI = new PlayerPropertiesImplementation(this);

        CommandManager commandManager = server.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("velocityglobalclientreload").plugin(this).build();
        commandManager.register(commandMeta, new ReloadCommand());
    }

    private void saveDefaultConfig() {
        try {
            Path configFile = dataDirectory.resolve("config.yml");
            if (Files.notExists(dataDirectory)) {
                Files.createDirectory(dataDirectory);
                if (Files.notExists(configFile)) {
                    try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                        Files.copy(stream, configFile);
                    }
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(configFile).build();
            configuration = loader.load();
        } catch (IOException e) {
            logger.error("Error while loading config", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (globalClient != null) {
            globalClient.shutdown();
        }
        globalClient = null;
    }

    public class ReloadCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            if (!invocation.source().hasPermission("globalclient.reload")) {
                return;
            }
            reconnectClient();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("globalclient.reload");
        }
    }

    public void reconnectClient() {
        String account = configuration.node("client", "account").getString();
        String password = configuration.node("client", "password").getString();
        String host = configuration.node("server", "host").getString();
        int port = configuration.node("server", "port").getInt();
        logger.info(account + " " + password + " " + host + " " + port);
        globalClient.setServer(host, port, account, password);
    }

    public ConnectionAPI getConnectionAPI() {
        return globalClient;
    }

    public PlayerPropertiesAPI getPropertiesAPI() {
        return propertiesAPI;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
