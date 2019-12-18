package de.cubeside.connection;

import de.cubeside.connection.event.GlobalDataEvent;
import de.cubeside.connection.event.GlobalPlayerDisconnectedEvent;
import de.cubeside.connection.event.GlobalPlayerJoinedEvent;
import de.cubeside.connection.event.GlobalServerConnectedEvent;
import de.cubeside.connection.event.GlobalServerDisconnectedEvent;
import java.util.ArrayDeque;
import java.util.logging.Level;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class GlobalClientBungee extends GlobalClient implements Listener {
    private final GlobalClientPlugin plugin;
    private boolean stoppingServer;

    protected final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    protected final Object sync = new Object();
    protected boolean running = true;

    public GlobalClientBungee(GlobalClientPlugin connectionPlugin) {
        super(connectionPlugin.getLogger());
        plugin = connectionPlugin;
        plugin.getProxy().getScheduler().runAsync(plugin, new MainThread());
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    private class MainThread implements Runnable {
        @Override
        public void run() {
            Runnable task = null;
            while (true) {
                synchronized (sync) {
                    task = tasks.pollFirst();
                    if (task == null) {
                        if (running) {
                            try {
                                sync.wait();
                            } catch (InterruptedException e) {
                                // ignore
                            }
                        } else {
                            return;
                        }
                    }
                }
                if (task != null) {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        plugin.getLogger().log(Level.SEVERE, "Exception in Client thread", t);
                    }
                }
            }
        }
    }

    protected void schedule(Runnable r) {
        synchronized (sync) {
            boolean wasEmpty = tasks.isEmpty();
            tasks.addLast(r);
            if (wasEmpty) {
                sync.notifyAll();
            }
        }
    }

    @Override
    public void setServer(String host, int port, String account, String password) {
        schedule(new Runnable() {
            @Override
            public void run() {
                GlobalClientBungee.super.setServer(host, port, account, password);
                for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                    onPlayerOnline(p.getUniqueId(), p.getName(), System.currentTimeMillis());
                }
            }
        });
    }

    @Override
    protected void runInMainThread(Runnable r) {
        if (!stoppingServer) {
            schedule(r);
        }
    }

    @Override
    protected void processData(GlobalServer source, String channel, GlobalPlayer targetPlayer, GlobalServer targetServer, byte[] data) {
        plugin.getProxy().getPluginManager().callEvent(new GlobalDataEvent(source, targetPlayer, channel, data));
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent e) {
        ProxiedPlayer p = e.getPlayer();
        schedule(new Runnable() {
            @Override
            public void run() {
                GlobalPlayer existing = getPlayer(p.getUniqueId());
                if (existing == null || !existing.isOnServer(getThisServer())) {
                    onPlayerOnline(p.getUniqueId(), p.getName(), System.currentTimeMillis());
                }
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent e) {
        ProxiedPlayer p = e.getPlayer();
        schedule(new Runnable() {
            @Override
            public void run() {
                GlobalPlayer existing = getPlayer(p.getUniqueId());
                if (existing != null && existing.isOnServer(getThisServer())) {
                    onPlayerOffline(p.getUniqueId());
                }
            }
        });
    }

    @Override
    protected void onPlayerJoined(GlobalServer server, GlobalPlayer player, boolean joinedTheNetwork) {
        plugin.getProxy().getPluginManager().callEvent(new GlobalPlayerJoinedEvent(server, player, joinedTheNetwork));
    }

    @Override
    protected void onPlayerDisconnected(GlobalServer server, GlobalPlayer player, boolean leftTheNetwork) {
        plugin.getProxy().getPluginManager().callEvent(new GlobalPlayerDisconnectedEvent(server, player, leftTheNetwork));
    }

    @Override
    protected void onServerConnected(GlobalServer server) {
        plugin.getProxy().getPluginManager().callEvent(new GlobalServerConnectedEvent(server));
    }

    @Override
    protected void onServerDisconnected(GlobalServer server) {
        plugin.getProxy().getPluginManager().callEvent(new GlobalServerDisconnectedEvent(server));
    }

    @Override
    public void shutdown() {
        this.stoppingServer = true;
        super.shutdown();
        synchronized (sync) {
            running = false;
            sync.notifyAll();
        }
    }
}