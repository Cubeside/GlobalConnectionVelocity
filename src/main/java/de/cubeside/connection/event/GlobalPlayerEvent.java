package de.cubeside.connection.event;

import de.cubeside.connection.GlobalPlayer;
import de.cubeside.connection.GlobalServer;
import net.md_5.bungee.api.plugin.Event;

public abstract class GlobalPlayerEvent extends Event {
    private final GlobalServer server;
    private final GlobalPlayer player;

    public GlobalPlayerEvent(GlobalServer server, GlobalPlayer player) {
        this.server = server;
        this.player = player;
    }

    public GlobalServer getServer() {
        return server;
    }

    public GlobalPlayer getPlayer() {
        return player;
    }
}
