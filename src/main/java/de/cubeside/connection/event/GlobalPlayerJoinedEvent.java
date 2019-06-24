package de.cubeside.connection.event;

import de.cubeside.connection.GlobalPlayer;
import de.cubeside.connection.GlobalServer;

public class GlobalPlayerJoinedEvent extends GlobalPlayerEvent {
    private final boolean joinedTheNetwork;

    public GlobalPlayerJoinedEvent(GlobalServer server, GlobalPlayer player, boolean joinedTheNetwork) {
        super(server, player);
        this.joinedTheNetwork = joinedTheNetwork;
    }

    public boolean hasJustJoinedTheNetwork() {
        return joinedTheNetwork;
    }
}
