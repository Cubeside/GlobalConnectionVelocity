package de.cubeside.connection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class GlobalClientPlugin extends Plugin {
    private GlobalClientBungee globalClient;

    private PlayerPropertiesImplementation propertiesAPI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        globalClient = new GlobalClientBungee(this);

        reconnectClient();

        propertiesAPI = new PlayerPropertiesImplementation(this);

        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
    }

    private void saveDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        if (globalClient != null) {
            globalClient.shutdown();
        }
        globalClient = null;
    }

    public class ReloadCommand extends Command {
        public ReloadCommand() {
            super("bungeeglobalclientreload", "globalclient.reload");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!sender.hasPermission("globalclient.reload")) {
                return;
            }
            reconnectClient();
        }
    }

    public void reconnectClient() {
        try {
            Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

            String account = configuration.getString("client.account");
            String password = configuration.getString("client.password");
            String host = configuration.getString("server.host");
            int port = configuration.getInt("server.port");
            globalClient.setServer(host, port, account, password);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Exception while loading the config", e);
        }
    }

    public ConnectionAPI getConnectionAPI() {
        return globalClient;
    }

    public PlayerPropertiesAPI getPropertiesAPI() {
        return propertiesAPI;
    }
}
