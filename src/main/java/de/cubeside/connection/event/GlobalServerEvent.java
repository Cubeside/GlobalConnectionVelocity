package de.cubeside.connection.event;

import de.cubeside.connection.GlobalServer;

public abstract class GlobalServerEvent {
    private final GlobalServer server;

    public GlobalServerEvent(GlobalServer server) {
        this.server = server;
    }

    public GlobalServer getServer() {
        return server;
    }
}
