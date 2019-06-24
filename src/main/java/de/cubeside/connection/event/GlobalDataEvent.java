package de.cubeside.connection.event;

import de.cubeside.connection.GlobalPlayer;
import de.cubeside.connection.GlobalServer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import net.md_5.bungee.api.plugin.Event;

public class GlobalDataEvent extends Event {
    private final GlobalServer source;
    private final GlobalPlayer targetPlayer;

    private final String channel;
    private final byte[] data;

    public GlobalDataEvent(GlobalServer source, GlobalPlayer targetPlayer, String channel, byte[] data) {
        this.source = source;
        this.targetPlayer = targetPlayer;
        this.channel = channel;
        this.data = data;
    }

    public GlobalServer getSource() {
        return source;
    }

    public GlobalPlayer getTargetPlayer() {
        return targetPlayer;
    }

    public String getChannel() {
        return channel;
    }

    public InputStream getData() {
        return new ByteArrayInputStream(data);
    }
}
