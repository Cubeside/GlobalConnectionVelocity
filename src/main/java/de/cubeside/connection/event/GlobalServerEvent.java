package de.cubeside.connection.event;

import de.cubeside.connection.GlobalServer;
import net.md_5.bungee.api.plugin.Event;

public abstract class GlobalServerEvent extends Event {
    private final GlobalServer server;

    public GlobalServerEvent(GlobalServer server) {
        this.server = server;
    }

    public GlobalServer getServer() {
        return server;
    }
}
