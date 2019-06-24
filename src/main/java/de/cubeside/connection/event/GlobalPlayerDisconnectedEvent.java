package de.cubeside.connection.event;

import de.cubeside.connection.GlobalPlayer;
import de.cubeside.connection.GlobalServer;

public class GlobalPlayerDisconnectedEvent extends GlobalPlayerEvent {
    private final boolean leftTheNetwork;

    public GlobalPlayerDisconnectedEvent(GlobalServer server, GlobalPlayer player, boolean leftTheNetwork) {
        super(server, player);
        this.leftTheNetwork = leftTheNetwork;
    }

    public boolean hasJustLeftTheNetwork() {
        return leftTheNetwork;
    }
}
