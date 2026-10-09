package org.codeberg.illusionclient.event;

/**
 * Each event type has one instance, reused for every publish. A listener must not hold on to an
 * event after it returns, or publish that event type from inside its own handler.
 */
public interface Event {
}
