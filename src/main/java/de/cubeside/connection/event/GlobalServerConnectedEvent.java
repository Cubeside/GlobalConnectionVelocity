package de.cubeside.connection.event;

import de.cubeside.connection.GlobalServer;

public class GlobalServerConnectedEvent extends GlobalServerEvent {
    public GlobalServerConnectedEvent(GlobalServer server) {
        super(server);
    }
}
