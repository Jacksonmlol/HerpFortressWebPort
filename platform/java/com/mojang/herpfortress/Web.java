package com.mojang.herpfortress;

import org.teavm.jso.JSBody;

/**
 * The bridge between the (platform independent) game code and the browser.
 *
 * Everything the game needs from the outside world goes through this class:
 * decoded artwork, mouse/keyboard state and access to the canvas. The companion
 * JavaScript host lives in {@code site/host.js} and exposes a global object
 * called {@code HerpHost}.
 */
public class Web {
	@JSBody(params = { "name" }, script = "return HerpHost.imageWidth(name);")
	static native int imageWidth(String name);

	@JSBody(params = { "name" }, script = "return HerpHost.imageHeight(name);")
	static native int imageHeight(String name);

	/** Straight ARGB values, the format {@link Bitmap} works with. */
	@JSBody(params = { "name" }, script = "return HerpHost.imagePixels(name);")
	static native int[] imagePixels(String name);

	/** Milliseconds since the page started loading, with sub-millisecond resolution. */
	@JSBody(script = "return performance.now();")
	static native double nowMillis();

	@JSBody(script = "return HerpHost.mouseX();")
	static native int mouseX();

	@JSBody(script = "return HerpHost.mouseY();")
	static native int mouseY();

	/** {@code button} is 0 = left, 1 = right, 2 = middle (AWT style). */
	@JSBody(params = { "button" }, script = "return HerpHost.mouseButton(button);")
	static native boolean mouseButton(int button);

	@JSBody(script = "return HerpHost.mouseOnScreen();")
	static native boolean mouseOnScreen();

	/** Key codes (see {@link Keys}) of every key that is currently held down. */
	@JSBody(script = "return HerpHost.downKeys();")
	static native int[] downKeys();

	/**
	 * Consumes the horizontal scroll wheel movement accumulated since the last
	 * call, in buffer pixels. Returns 0 when nothing was scrolled.
	 */
	@JSBody(script = "return HerpHost.takeWheelX();")
	static native double takeWheelX();

	/** Like {@link #takeWheelX()} but for the vertical axis. */
	@JSBody(script = "return HerpHost.takeWheelY();")
	static native double takeWheelY();

	/** Copies the freshly rendered ARGB screen buffer onto the canvas. */
	@JSBody(params = { "pixels", "w", "h" }, script = "HerpHost.present(pixels, w, h);")
	static native void present(int[] pixels, int w, int h);
}
