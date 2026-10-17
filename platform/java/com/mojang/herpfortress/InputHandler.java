package com.mojang.herpfortress;

/**
 * Web version of the input handler.
 *
 * Instead of listening to AWT events, the browser host keeps the current input
 * state (see {@code site/host.js}) and this class simply polls it once per frame.
 */
public class InputHandler {
	private Input input = new Input();
	private boolean[] keysDown = new boolean[65536];
	private int[] previouslyDown = new int[0];

	public InputHandler() {
	}

	public Input updateMouseStatus(int scale) {
		for (int i = 0; i < previouslyDown.length; i++) {
			keysDown[previouslyDown[i]] = false;
		}

		int[] down = Web.downKeys();
		for (int i = 0; i < down.length; i++) {
			int key = down[i];
			if (key > 0 && key < keysDown.length) keysDown[key] = true;
		}
		previouslyDown = down;

		input.update(Web.mouseX() / scale, Web.mouseY() / scale, Web.mouseButton(0), Web.mouseButton(1), Web.mouseButton(2), Web.mouseOnScreen(), keysDown, "");
		return input;
	}
}
