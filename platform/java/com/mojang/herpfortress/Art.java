package com.mojang.herpfortress;

/**
 * Web version of the asset loader.
 *
 * TeaVM has no {@code javax.imageio}, so PNG decoding is left to the browser
 * (see {@code site/host.js}); the decoded ARGB pixels are handed to
 * {@link Web#addImage} before the game starts and sliced up here, exactly like
 * the desktop version sliced up its {@code BufferedImage}s.
 */
public class Art {
	public static Art i;

	public static void init() {
		i = new Art();
	}

	public Bitmap[][] red = loadAndCut("/chars/red.png", 16, 16);
	public Bitmap[][] blu = loadAndCut("/chars/blu.png", 16, 16);
	public Bitmap[][] cursors = loadAndCut("/cursors.png", 16, 16);
	public Bitmap[][] particles = loadAndCut("/particles.png", 8, 8);
	public Bitmap[][] pickups = loadAndCut("/pickups.png", 8, 8);
	public Bitmap[][] missiles = loadAndCut("/missiles.png", 8, 8);
	public Bitmap[][] mapTiles = loadAndCut("/tiles.png", 24, 24);
	public Bitmap panel = load("/panel.png");

	public static Bitmap[][] loadAndCut(String name, int sw, int sh) {
		Bitmap img = load(name);

		int xSlices = img.w / sw;
		int ySlices = img.h / sh;

		Bitmap[][] result = new Bitmap[xSlices][ySlices];
		for (int x = 0; x < xSlices; x++) {
			for (int y = 0; y < ySlices; y++) {
				Bitmap cut = new Bitmap(sw, sh);
				for (int yy = 0; yy < sh; yy++) {
					int src = (x * sw) + ((y * sh) + yy) * img.w;
					int dst = yy * sw;
					for (int xx = 0; xx < sw; xx++) {
						cut.pixels[dst + xx] = img.pixels[src + xx];
					}
				}
				result[x][y] = cut;
			}
		}
		return result;
	}

	public static Bitmap load(String name) {
		int w = Web.imageWidth(name);
		int h = Web.imageHeight(name);
		if (w <= 0 || h <= 0) {
			throw new RuntimeException("Failed to load " + name);
		}
		return new Bitmap(w, h, Web.imagePixels(name));
	}

	public static Bitmap[][] recolor(Bitmap[][] bitmaps, int a0, int b0, int a1, int b1) {
		for (int x = 0; x < bitmaps.length; x++) {
			for (int y = 0; y < bitmaps[x].length; y++) {
				Bitmap bm = bitmaps[x][y];
				for (int i = 0; i < bm.pixels.length; i++) {
					if (bm.pixels[i] == a0) bm.pixels[i] = b0;
					if (bm.pixels[i] == a1) bm.pixels[i] = b1;
				}
			}
		}
		return bitmaps;
	}
}
