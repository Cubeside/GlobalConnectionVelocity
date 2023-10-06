package de.cubeside.connection;

import com.google.common.base.Preconditions;
import de.cubeside.connection.event.GlobalDataEvent;
import de.cubeside.connection.event.GlobalPlayerDisconnectedEvent;
import de.cubeside.connection.event.GlobalPlayerPropertyChangedEvent;
import de.cubeside.connection.event.GlobalServerConnectedEvent;
import de.cubeside.connection.util.AutoCloseableLockWrapper;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerPropertiesImplementation implements PlayerPropertiesAPI, Listener {

    private final static int MESSAGE_SET_PROPERTY = 1;
    private final static int MESSAGE_DELETE_PROPERTY = 2;
    private final static int MESSAGE_MULTISET_PROPERTIES = 3;

    private final GlobalClientPlugin plugin;

    private final static String CHANNEL = "GlobalClient.playerProperties";

    private final HashMap<UUID, HashMap<String, String>> playerProperties;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AutoCloseableLockWrapper readLock = new AutoCloseableLockWrapper(lock.readLock());
    private final AutoCloseableLockWrapper writeLock = new AutoCloseableLockWrapper(lock.writeLock());

    public PlayerPropertiesImplementation(GlobalClientPlugin plugin) {
        this.plugin = plugin;
        this.playerProperties = new HashMap<>();
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler
    public void onGlobalPlayerDisconnected(GlobalPlayerDisconnectedEvent e) {
        if (e.hasJustLeftTheNetwork()) {
            try (AutoCloseableLockWrapper lock = writeLock.open()) {
                playerProperties.remove(e.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void onGlobalServerConnected(GlobalServerConnectedEvent e) {
        // send all properties
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_MULTISET_PROPERTIES);
            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                UUID uuid = p.getUniqueId();
                try (AutoCloseableLockWrapper lock = readLock.open()) {
                    HashMap<String, String> properties = playerProperties.get(uuid);
                    if (properties != null) {
                        dos.writeBoolean(true);
                        dos.writeLong(uuid.getMostSignificantBits());
                        dos.writeLong(uuid.getLeastSignificantBits());
                        dos.writeInt(properties.size());
                        for (Entry<String, String> entry : properties.entrySet()) {
                            dos.writeUTF(entry.getKey());
                            dos.writeUTF(entry.getValue());
                        }
                    }
                }
            }
            dos.writeBoolean(false);
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        e.getServer().sendData(CHANNEL, baos.toByteArray());
    }

    @EventHandler
    public void onGlobalData(GlobalDataEvent e) {
        if (e.getChannel().equals(CHANNEL)) {
            DataInputStream dis = new DataInputStream(e.getData());
            try {
                int type = dis.readByte();
                if (type == MESSAGE_SET_PROPERTY) {
                    UUID uuid = readUUID(dis);
                    GlobalPlayer target = plugin.getConnectionAPI().getPlayer(uuid);
                    String property = dis.readUTF();
                    String value = dis.readUTF();
                    try (AutoCloseableLockWrapper lock = writeLock.open()) {
                        HashMap<String, String> properties = playerProperties.computeIfAbsent(uuid, theUuid -> new HashMap<>());
                        properties.put(property, value);
                    }
                    plugin.getProxy().getPluginManager().callEvent(new GlobalPlayerPropertyChangedEvent(e.getSource(), target, property, value));
                } else if (type == MESSAGE_DELETE_PROPERTY) {
                    UUID uuid = readUUID(dis);
                    GlobalPlayer target = plugin.getConnectionAPI().getPlayer(uuid);
                    String property = dis.readUTF();
                    boolean event = false;
                    try (AutoCloseableLockWrapper lock = writeLock.open()) {
                        HashMap<String, String> properties = playerProperties.get(uuid);
                        if (properties != null) {
                            properties.remove(property);
                            if (properties.isEmpty()) {
                                playerProperties.remove(uuid);
                            }
                            event = true;
                        }
                    }
                    if (event) {
                        plugin.getProxy().getPluginManager().callEvent(new GlobalPlayerPropertyChangedEvent(e.getSource(), target, property, null));
                    }
                } else if (type == MESSAGE_MULTISET_PROPERTIES) {
                    while (dis.readBoolean()) {
                        UUID uuid = readUUID(dis);
                        GlobalPlayer target = plugin.getConnectionAPI().getPlayer(uuid);
                        int propertiesCount = dis.readInt();
                        if (propertiesCount > 0) {
                            ArrayList<GlobalPlayerPropertyChangedEvent> events = new ArrayList<>();
                            try (AutoCloseableLockWrapper lock = writeLock.open()) {
                                HashMap<String, String> properties = playerProperties.computeIfAbsent(uuid, theUuid -> new HashMap<>());
                                for (int i = 0; i < propertiesCount; i++) {
                                    String property = dis.readUTF();
                                    String value = dis.readUTF();
                                    properties.put(property, value);
                                    events.add(new GlobalPlayerPropertyChangedEvent(e.getSource(), target, property, value));
                                }
                            }
                            for (GlobalPlayerPropertyChangedEvent event : events) {
                                plugin.getProxy().getPluginManager().callEvent(event);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not parse PlayerProperties message", ex);
            }
        }
    }

    private UUID readUUID(DataInputStream dis) throws IOException {
        long msb = dis.readLong();
        long lsb = dis.readLong();
        return new UUID(msb, lsb);
    }

    @Override
    public boolean hasProperty(GlobalPlayer player, String property) {
        try (AutoCloseableLockWrapper lock = readLock.open()) {
            Preconditions.checkNotNull(player, "player");
            Preconditions.checkNotNull(property, "property");
            HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
            return properties != null && properties.containsKey(property);
        }
    }

    @Override
    public String getPropertyValue(GlobalPlayer player, String property) {
        try (AutoCloseableLockWrapper lock = readLock.open()) {
            Preconditions.checkNotNull(player, "player");
            Preconditions.checkNotNull(property, "property");
            HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
            return properties == null ? null : properties.get(property);
        }
    }

    @Override
    public Map<String, String> getAllProperties(GlobalPlayer player) {
        try (AutoCloseableLockWrapper lock = readLock.open()) {
            Preconditions.checkNotNull(player, "player");
            HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
            return properties == null ? Collections.emptyMap() : new HashMap<>(properties);
        }
    }

    @Override
    public void setPropertyValue(GlobalPlayer player, String property, String value) {
        // Preconditions.checkState(Bukkit.isPrimaryThread(), "not on main thread!");
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(property, "property");
        Preconditions.checkArgument(player.isOnAnyServer(), "player is not online");
        if (value == null) {
            try (AutoCloseableLockWrapper lock = writeLock.open()) {
                HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
                if (properties != null) {
                    if (properties.remove(property) != null) {
                        if (properties.isEmpty()) {
                            playerProperties.remove(player.getUniqueId());
                        }
                    }
                }
            }
            // send remove
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
                dos.writeByte(MESSAGE_DELETE_PROPERTY);
                dos.writeLong(player.getUniqueId().getMostSignificantBits());
                dos.writeLong(player.getUniqueId().getLeastSignificantBits());
                dos.writeUTF(property);
                dos.close();
                plugin.getConnectionAPI().sendData(CHANNEL, baos.toByteArray(), true);
            } catch (IOException ex) {
                throw new Error("impossible");
            }
        } else { // value != null
            try (AutoCloseableLockWrapper lock = writeLock.open()) {
                HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
                if (properties == null) {
                    properties = new HashMap<>();
                    playerProperties.put(player.getUniqueId(), properties);
                }
                properties.put(property, value);
            }
            // send set
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
                dos.writeByte(MESSAGE_SET_PROPERTY);
                dos.writeLong(player.getUniqueId().getMostSignificantBits());
                dos.writeLong(player.getUniqueId().getLeastSignificantBits());
                dos.writeUTF(property);
                dos.writeUTF(value);
                dos.close();
                plugin.getConnectionAPI().sendData(CHANNEL, baos.toByteArray(), true);
            } catch (IOException ex) {
                throw new Error("impossible");
            }
        }
        plugin.getProxy().getPluginManager().callEvent(new GlobalPlayerPropertyChangedEvent(plugin.getConnectionAPI().getThisServer(), player, property, value));
    }
}
