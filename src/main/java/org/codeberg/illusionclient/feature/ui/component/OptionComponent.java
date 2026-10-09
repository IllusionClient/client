package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.config.api.Option;

public abstract class OptionComponent extends Component {
	protected final Option option;

	protected OptionComponent(Option option) {
		this.option = option;
	}

	@Override
	public boolean visible() {
		return option.visible();
	}
}
