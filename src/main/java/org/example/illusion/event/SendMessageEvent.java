package org.example.illusion.event;

public final class SendMessageEvent extends CancellableEvent {
	public String message;

	public SendMessageEvent(String message) {
		this.message = message;
	}
}
