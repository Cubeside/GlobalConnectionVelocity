package de.cubeside.connection.event;

import de.cubeside.connection.GlobalServer;

public class GlobalServerDisconnectedEvent extends GlobalServerEvent {
    public GlobalServerDisconnectedEvent(GlobalServer server) {
        super(server);
    }
}
