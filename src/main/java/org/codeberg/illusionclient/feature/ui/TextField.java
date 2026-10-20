package org.codeberg.illusionclient.feature.ui;

import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

public final class TextField {
	private final int maxLength;
	private String text = "";
	private int cursor, anchor, first;
	private boolean focused, selecting;

	public TextField(int maxLength) {
		this.maxLength = maxLength;
	}

	private static boolean printable(char chr) {
		return chr >= ' ' && chr <= '~';
	}

	public String text() {
		return text;
	}

	public boolean isEmpty() {
		return text.isEmpty();
	}

	public boolean focused() {
		return focused;
	}

	public void focus() {
		focused = true;
	}

	public void blur() {
		focused = false;
		selecting = false;
		anchor = cursor;
	}

	public void clear() {
		text = "";
		cursor = anchor = first = 0;
		blur();
	}

	public void render(int x, float y, int width, String placeholder, int mouseX) {
		if (selecting) cursor = index(mouseX, x);
		if (cursor < first) first = cursor;
		while (Draw.width(text.substring(first, cursor)) > width) first++;

		String visible = Draw.trim(text.substring(first), width);
		int last = first + visible.length();
		float top = y + Draw.textHeight() / 2.0F - 5;

		if (text.isEmpty()) {
			Draw.text(placeholder, x, y, Draw.FAINT, Draw.Weight.REGULAR);
		} else {
			int from = Math.clamp(Math.min(cursor, anchor), first, last);
			int to = Math.clamp(Math.max(cursor, anchor), first, last);
			if (focused && from < to) {
				int left = x + Draw.width(text.substring(first, from));
				int right = x + Draw.width(text.substring(first, to));
				Draw.fill(left, top, right, top + 10, Draw.accent().getRGB() & 0xFFFFFF | 0x60 << 24);
			}
			Draw.text(visible, x, y, Draw.TEXT);
		}

		if (focused) {
			int caret = x + Draw.width(text.substring(first, cursor));
			Draw.fill(caret, top, caret + 1, top + 10, Draw.TEXT);
		}
	}

	public void mouseClicked(int mouseX, int x) {
		focused = true;
		selecting = true;
		cursor = index(mouseX, x);
		if (!Screen.isShiftDown()) anchor = cursor;
	}

	public void mouseReleased() {
		selecting = false;
	}

	public boolean keyPressed(char chr, int key) {
		if (!focused) return false;

		boolean control = Screen.isControlDown();
		if (Screen.isSelectAll(key)) {
			anchor = 0;
			cursor = text.length();
		} else if (Screen.isCopy(key)) {
			if (hasSelection()) Screen.setClipboard(selection());
		} else if (Screen.isCut(key)) {
			if (hasSelection()) {
				Screen.setClipboard(selection());
				insert("");
			}
		} else if (Screen.isPaste(key)) {
			insert(Screen.getClipboard());
		} else {
			switch (key) {
				case Keyboard.KEY_BACK -> erase(control ? wordStart(cursor) : cursor - 1);
				case Keyboard.KEY_DELETE -> erase(control ? wordEnd(cursor) : cursor + 1);
				case Keyboard.KEY_LEFT -> move(control ? wordStart(cursor) : cursor - 1);
				case Keyboard.KEY_RIGHT -> move(control ? wordEnd(cursor) : cursor + 1);
				case Keyboard.KEY_HOME -> move(0);
				case Keyboard.KEY_END -> move(text.length());
				default -> {
					if (!printable(chr)) return false;
					insert(String.valueOf(chr));
				}
			}
		}
		return true;
	}

	private boolean hasSelection() {
		return cursor != anchor;
	}

	private String selection() {
		return text.substring(Math.min(cursor, anchor), Math.max(cursor, anchor));
	}

	private void insert(String value) {
		StringBuilder filtered = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			if (printable(value.charAt(i))) filtered.append(value.charAt(i));
		}

		int from = Math.min(cursor, anchor);
		int to = Math.max(cursor, anchor);
		int room = maxLength - (text.length() - (to - from));
		String added = filtered.substring(0, Math.clamp(filtered.length(), 0, Math.max(0, room)));

		text = text.substring(0, from) + added + text.substring(to);
		cursor = anchor = from + added.length();
	}

	private void erase(int target) {
		if (!hasSelection()) {
			cursor = Math.clamp(target, 0, text.length());
		}
		insert("");
	}

	private void move(int target) {
		boolean extend = Screen.isShiftDown();
		if (!extend && hasSelection() && Math.abs(target - cursor) == 1) {
			target = target < cursor ? Math.min(cursor, anchor) : Math.max(cursor, anchor);
		}
		cursor = Math.clamp(target, 0, text.length());
		if (!extend) anchor = cursor;
	}

	private int wordStart(int from) {
		int i = from;
		while (i > 0 && text.charAt(i - 1) == ' ') i--;
		while (i > 0 && text.charAt(i - 1) != ' ') i--;
		return i;
	}

	private int wordEnd(int from) {
		int i = from;
		while (i < text.length() && text.charAt(i) == ' ') i++;
		while (i < text.length() && text.charAt(i) != ' ') i++;
		return i;
	}

	private int index(int mouseX, int x) {
		int offset = mouseX - x;
		int width = 0;
		for (int i = first; i < text.length(); i++) {
			int next = Draw.width(text.substring(first, i + 1));
			if (offset < (width + next) / 2) return i;
			width = next;
		}
		return text.length();
	}
}
