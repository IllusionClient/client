package org.example.illusion.event;

import org.example.illusion.feature.module.api.Module;

public record ModuleToggleEvent(Module module, boolean enabled) implements Event {
}
