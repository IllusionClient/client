package org.example.illusion.event;

public record KeyPressEvent(int key, boolean down) implements Event {
}
