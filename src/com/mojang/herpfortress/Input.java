package com.mojang.herpfortress;

import java.util.*;

public class Input {
	public static class Key {
		public int[] bindings = new int[0];
		public boolean wasDown;
		public boolean down;
		public boolean typed;

		public Key(Input input) {
			input.keys.add(this);
		}

		public Key bind(int key) {
			int[] newBindings = new int[bindings.length + 1];
			System.arraycopy(bindings, 0, newBindings, 0, bindings.length);
			newBindings[bindings.length] = key;
			bindings = newBindings;
			return this;
		}

		public void tick(boolean[] keysDown) {
			wasDown = down;
			down = false;
			for (int i = 0; i < bindings.length; i++) {
				if (keysDown[bindings[i]]) down = true;
			}
			typed = !wasDown && down;
		}
	}

	public int x, y;

	public boolean onScreen;

	public boolean b0, b1, b2;
	public boolean b0Clicked;
	public boolean b1Clicked;
	public boolean b2Clicked;
	public boolean b0Released;
	public boolean b1Released;
	public boolean b2Released;

	public String typed = "";

	private List<Key> keys = new ArrayList<Key>();

	public Key up = new Key(this).bind(Keys.VK_UP).bind(Keys.VK_W);
	public Key down = new Key(this).bind(Keys.VK_DOWN).bind(Keys.VK_S);
	public Key left = new Key(this).bind(Keys.VK_LEFT).bind(Keys.VK_A);
	public Key right = new Key(this).bind(Keys.VK_RIGHT).bind(Keys.VK_D);

	public Key unit1 = new Key(this).bind(Keys.VK_1).bind(Keys.VK_NUMPAD7).bind(Keys.VK_R);
	public Key unit2 = new Key(this).bind(Keys.VK_2).bind(Keys.VK_NUMPAD8).bind(Keys.VK_T);
	public Key unit3 = new Key(this).bind(Keys.VK_3).bind(Keys.VK_NUMPAD9).bind(Keys.VK_Y);
	public Key unit4 = new Key(this).bind(Keys.VK_4).bind(Keys.VK_NUMPAD4).bind(Keys.VK_F);
	public Key unit5 = new Key(this).bind(Keys.VK_5).bind(Keys.VK_NUMPAD5).bind(Keys.VK_G);
	public Key unit6 = new Key(this).bind(Keys.VK_6).bind(Keys.VK_NUMPAD6).bind(Keys.VK_H);
	public Key unit7 = new Key(this).bind(Keys.VK_7).bind(Keys.VK_NUMPAD1).bind(Keys.VK_V);
	public Key unit8 = new Key(this).bind(Keys.VK_8).bind(Keys.VK_NUMPAD2).bind(Keys.VK_B);
	public Key unit9 = new Key(this).bind(Keys.VK_9).bind(Keys.VK_NUMPAD3).bind(Keys.VK_N);
	public Key nextUnit = new Key(this).bind(Keys.VK_E).bind(Keys.VK_TAB);
	public Key prevUnit = new Key(this).bind(Keys.VK_Q);

	public Input() {
	}

	public void update(int x, int y, boolean b0, boolean b1, boolean b2, boolean onScreen, boolean[] keysDown, String typed) {
		b0Clicked = !this.b0 && b0;
		b1Clicked = !this.b1 && b1;
		b2Clicked = !this.b2 && b2;

		b0Released = this.b0 && !b0;
		b1Released = this.b1 && !b1;
		b2Released = this.b2 && !b2;

		this.x = x;
		this.y = y;
		this.b0 = b0;
		this.b1 = b1;
		this.b2 = b2;
		this.onScreen = onScreen;
		this.typed = "";

		for (Key key : keys) {
			key.tick(keysDown);
		}
	}
}
