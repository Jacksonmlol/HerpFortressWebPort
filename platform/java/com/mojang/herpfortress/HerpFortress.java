package com.mojang.herpfortress;

import org.teavm.jso.JSExport;

import com.mojang.herpfortress.level.EntityListCache;

/**
 * WebAssembly entry point for Herp Fortress.
 *
 * This is the same fixed timestep game loop as the desktop version, except that
 * the browser's {@code requestAnimationFrame} replaces the render thread and the
 * finished frame is handed to the canvas instead of an AWT {@code BufferStrategy}.
 */
public class HerpFortress {
	public static final int WIDTH = 320;
	public static final int HEIGHT = 240;
	public static final int SCALE = 3;

	private static final double MS_PER_FRAME = 1000.0 / 60.0;
	private static final double MAX_SKIP_FRAMES = 10;

	private static HerpFortress instance;

	private Bitmap screenBitmap;

	private InputHandler inputHandler;
	private Input mouse;
	private Game game;
	private PlayerView playerView;

	private double unprocessedTime = 0;
	private double lastTime = Web.nowMillis();

	public static void main(String[] args) {
		Art.init();
		instance = new HerpFortress();
		instance.init();
	}

	public void init() {
		inputHandler = new InputHandler();
		screenBitmap = new Bitmap(WIDTH, HEIGHT);
		mouse = inputHandler.updateMouseStatus(SCALE);

		game = new Game();
		playerView = new PlayerView(game, game.level.redPlayer, mouse);
	}

	/** Called by the browser once for every animation frame. */
	@JSExport
	public static void frame() {
		instance.doFrame();
	}

	private void doFrame() {
		double now = Web.nowMillis();
		double passedTime = (now - lastTime) / MS_PER_FRAME;
		lastTime = now;

		if (passedTime < -MAX_SKIP_FRAMES) passedTime = -MAX_SKIP_FRAMES;
		if (passedTime > MAX_SKIP_FRAMES) passedTime = MAX_SKIP_FRAMES;

		unprocessedTime += passedTime;

		while (unprocessedTime > 1) {
			unprocessedTime -= 1;
			mouse = inputHandler.updateMouseStatus(SCALE);
			EntityListCache.reset();
			tick();
		}

		scrollWithWheel();

		EntityListCache.reset();
		render(screenBitmap);
		Web.present(screenBitmap.pixels, WIDTH, HEIGHT);
	}

	/**
	 * The original game only pans the camera with the arrow keys and the middle
	 * mouse button. The browser also offers a scroll wheel (and trackpads send
	 * wheel events for two-finger scrolling), so feed it into the same camera
	 * offset, in the same direction as the arrow keys.
	 */
	private void scrollWithWheel() {
		double wheelX = Web.takeWheelX();
		double wheelY = Web.takeWheelY();
		if (wheelX == 0 && wheelY == 0) return;

		playerView.xScroll += wheelX;
		playerView.yScroll += wheelY;
	}

	private void render(Bitmap screen) {
		playerView.render(screen);
		if (mouse.onScreen) screen.draw(Art.i.cursors[0][0], mouse.x - 1, mouse.y - 1);
	}

	private void tick() {
		game.tick();
		playerView.tick();
	}
}
