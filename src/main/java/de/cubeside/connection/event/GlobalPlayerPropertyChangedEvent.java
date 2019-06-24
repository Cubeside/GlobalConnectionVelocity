package de.cubeside.connection.event;

import de.cubeside.connection.GlobalPlayer;
import de.cubeside.connection.GlobalServer;

public class GlobalPlayerPropertyChangedEvent extends GlobalPlayerEvent {
    private final String property;
    private final String value;

    public GlobalPlayerPropertyChangedEvent(GlobalServer server, GlobalPlayer player, String property, String value) {
        super(server, player);
        this.property = property;
        this.value = value;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }
}
