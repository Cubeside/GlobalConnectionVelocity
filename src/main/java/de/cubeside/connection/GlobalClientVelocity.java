package de.cubeside.connection;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.cubeside.connection.event.GlobalDataEvent;
import de.cubeside.connection.event.GlobalPlayerDisconnectedEvent;
import de.cubeside.connection.event.GlobalPlayerJoinedEvent;
import de.cubeside.connection.event.GlobalServerConnectedEvent;
import de.cubeside.connection.event.GlobalServerDisconnectedEvent;
import java.util.ArrayDeque;

public class GlobalClientVelocity extends GlobalClient {
    private final GlobalClientPlugin plugin;
    private boolean stoppingServer;

    protected final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    protected final Object sync = new Object();
    protected boolean running = true;

    public GlobalClientVelocity(GlobalClientPlugin connectionPlugin) {
        super(null);
        plugin = connectionPlugin;
        plugin.getServer().getScheduler().buildTask(plugin, new MainThread()).schedule();
        plugin.getServer().getEventManager().register(plugin, this);
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
                        plugin.getLogger().error("Exception in Client thread", t);
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
                GlobalClientVelocity.super.setServer(host, port, account, password);
                for (Player p : plugin.getServer().getAllPlayers()) {
                    onPlayerOnline(p.getUniqueId(), p.getUsername(), System.currentTimeMillis());
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
        plugin.getServer().getEventManager().fire(new GlobalDataEvent(source, targetPlayer, channel, data));
    }

    @Subscribe(priority = Byte.MIN_VALUE + 2)
    public void onPlayerJoin(PostLoginEvent e) {
        Player p = e.getPlayer();
        schedule(new Runnable() {
            @Override
            public void run() {
                GlobalPlayer existing = getPlayer(p.getUniqueId());
                if (existing == null || !existing.isOnServer(getThisServer())) {
                    onPlayerOnline(p.getUniqueId(), p.getUsername(), System.currentTimeMillis());
                }
            }
        });
    }

    @Subscribe(priority = Byte.MAX_VALUE - 1)
    public void onPlayerQuit(DisconnectEvent e) {
        Player p = e.getPlayer();
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
        plugin.getServer().getEventManager().fire(new GlobalPlayerJoinedEvent(server, player, joinedTheNetwork));
    }

    @Override
    protected void onPlayerDisconnected(GlobalServer server, GlobalPlayer player, boolean leftTheNetwork) {
        plugin.getServer().getEventManager().fire(new GlobalPlayerDisconnectedEvent(server, player, leftTheNetwork));
    }

    @Override
    protected void onServerConnected(GlobalServer server) {
        plugin.getServer().getEventManager().fire(new GlobalServerConnectedEvent(server));
    }

    @Override
    protected void onServerDisconnected(GlobalServer server) {
        plugin.getServer().getEventManager().fire(new GlobalServerDisconnectedEvent(server));
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