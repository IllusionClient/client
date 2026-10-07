package org.example.illusion.event;

public final class SneakEvent implements Event {
    public boolean sneaking;

    public SneakEvent(boolean sneaking) {
        this.sneaking = sneaking;
    }
}
