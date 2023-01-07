package minicraft.gfx;

import java.util.Arrays;

import minicraft.core.Renderer;
import minicraft.core.Updater;

public class Screen {

	public static final int w = Renderer.WIDTH; // Width of the screen
	public static final int h = Renderer.HEIGHT; // Height of the screen
	public static final Point center = new Point(w/2, h/2);

	private static final int MAXDARK = 128;

	/// x and y offset of screen:
	private int xOffset;
	private int yOffset;

	// Used for mirroring an image:
	private static final int BIT_MIRROR_X = 0x01; // Written in hexadecimal; binary: 01
	private static final int BIT_MIRROR_Y = 0x02; // Binary: 10

	public int[] pixels; // Pixels on the screen

	// Since each sheet is 256x256 pixels, each one has 1024 8x8 "tiles"
	// So 0 is the start of the item sheet 1024 the start of the tile sheet, 2048 the start of the entity sheet,
	// And 3072 the start of the gui sheet

	private SpriteSheet[] sheets;

	public Screen(SpriteSheet itemSheet, SpriteSheet tileSheet, SpriteSheet entitySheet, SpriteSheet guiSheet, SpriteSheet iconsSheet, SpriteSheet background) {

		sheets = new SpriteSheet[] { itemSheet, tileSheet, entitySheet, guiSheet, iconsSheet, background };

		/// Screen width and height are determined by the actual game window size, meaning the screen is only as big as the window.
		pixels = new int[Screen.w * Screen.h]; // Makes new integer array for all the pixels on the screen.
	}

	public Screen(Screen model) {
		this(model.sheets[0], model.sheets[1], model.sheets[2], model.sheets[3], model.sheets[4], model.sheets[5]);
	}

	public void setSheet(SpriteSheet itemSheet, SpriteSheet tileSheet, SpriteSheet entitySheet, SpriteSheet guiSheet, SpriteSheet iconsSheet, SpriteSheet background) {
		if (itemSheet != null) sheets[0] = itemSheet;
		if (tileSheet != null) sheets[1] = tileSheet;
		if (entitySheet != null) sheets[2] = entitySheet;
		if (guiSheet != null) sheets[3] = guiSheet;
		if (iconsSheet != null) sheets[4] = iconsSheet;
		if (background != null) sheets[5] = background;
		
	}

	/** Clears all the colors on the screen */
	public void clear(int color) {
		// Turns each pixel into a single color (clearing the screen!)
		Arrays.fill(pixels, color);
	}

	public void render(int[] pixelColors) {
		System.arraycopy(pixelColors, 0, pixels, 0, Math.min(pixelColors.length, pixels.length));
	}

	public void render(int xp, int yp, int tile, int bits) {
		render(xp, yp, tile, bits, 0);
	}

	public void render(int xp, int yp, int tile, int bits, int sheet) {
		render(xp, yp, tile, bits, sheet, -1);
	}

	public void render(int xp, int yp, int tile, int bits, int sheet, int whiteTint) {
		render(xp, yp, tile, bits, sheet, whiteTint, false);
	}

	public void render(int xp, int yp, int tile, int bits, int sheet, int whiteTint, boolean fullbright) {
		render(xp, yp, tile % 32, tile / 32, bits, sheet, whiteTint, fullbright, 0);
	}

	public void render(int xp, int yp, int tile, int bits, int sheet, int whiteTint, boolean fullbright, int color) {
		render(xp, yp, tile % 32, tile / 32, bits, sheet, -1, false, color);
	}

	public void render(int xp, int yp, Pixel pixel) {
		render(xp, yp, pixel, -1);
	}

	public void render(int xp, int yp, Pixel pixel, int whiteTint) {
		render(xp, yp, pixel, whiteTint, false);
	}

	public void render(int xp, int yp, Pixel pixel, int whiteTint, boolean fullbright) {
		render(xp, yp, pixel.getX(), pixel.getY(), pixel.getMirror(), pixel.getIndex(), whiteTint, fullbright, 0);
	}

	public void render(int xp, int yp, Pixel pixel, int whiteTint, boolean fullbright, int color) {
		render(xp, yp, pixel.getX(), pixel.getY(), pixel.getMirror(), pixel.getIndex(), whiteTint, fullbright, color);
	}

	public void render(int xp, int yp, Pixel pixel, int bits, int whiteTint, boolean fullbright) {
		render(xp, yp, pixel.getX(), pixel.getY(), bits, pixel.getIndex(), whiteTint, fullbright, 0);
	}

	public void render(int xp, int yp, Pixel pixel, int bits, int whiteTint, boolean fullbright, int color) {
		render(xp, yp, pixel.getX(), pixel.getY(), bits, pixel.getIndex(), whiteTint, fullbright, color);
	}
	
	/**
	 * Renders a solid color rectangle on a pixel array.
	 * @param xp 		The x coordinate of the top-left corner of the rectangle.
	 * @param yp 		The y coordinate of the top-left corner of the rectangle.
	 * @param width 	The width of the rectangle.
	 * @param height 	The height of the rectangle.
	 * @param color 	The color of the rectangle.
	*/
	public void renderColor(int xp, int yp, int width, int height, int color) {
		// Subtract the xOffset and yOffset values from the coordinates to account for scrolling
		xp -= xOffset;
		yp -= yOffset;

		// Loop through each pixel in the rectangle
		for (int x = 0; x < width; x++) {
			// Skip any pixels that fall outside the bounds of the pixel array
			if (x + width < 0 || x + width > w) {
				continue;
			}
			
			for (int y = 0; y < height; y++) {
				if (y + height < 0 || y + height > h) {
					continue;
				}

				// Calculate the index of the pixel in the pixel array and set its color
				pixels[(x + xp) + (y + yp) * w] = color;
			}
		}
	}
	
	/**
	 * Renders a solid color rectangle on a pixel array with a specified opacity.
	 * @param xp 		The x coordinate of the top-left corner of the rectangle.
	 * @param yp 		The y coordinate of the top-left corner of the rectangle.
	 * @param width 	The width of the rectangle.
	 * @param height 	The height of the rectangle.
	 * @param color 	The color of the rectangle.
	 * @param opacity 	The opacity of the rectangle, specified as a float value between 0 and 1, where 0 is fully transparent and 1 is fully opaque.
	*/
	public void renderColor(int xp, int yp, int width, int height, int color, float opacity) {
		// Subtract the xOffset and yOffset values from the coordinates to account for scrolling
		xp -= xOffset;
		yp -= yOffset;

		for (int x = 0; x < width; x++) { // Loop through each pixel in the rectangle
			
			// Skip any pixels that fall outside the bounds of the pixel array
			if (x + width < 0 || x + width > w) {
				continue;
			}
			
			for (int y = 0; y < height; y++) {
				if (y + height < 0 || y + height > h) {
					continue;
				}

				int index = (x + xp) + (y + yp) * w; // Calculate the index of the pixel in the pixel array
				int pixelColor = pixels[index];
				
				// Set the pixel color in the pixel array
				pixels[index] = blendColors(color, pixelColor, opacity);
			}
		}
	}

	/** Renders an object from the sprite sheet based on screen coordinates, tile (SpriteSheet location), colors, and bits (for mirroring).
	 *  I believe that xp and yp refer to the desired position of the upper-left-most pixel. 
	 */
	private void render(int xp, int yp, int xTile, int yTile, int bits, int sheet, int whiteTint, boolean fullbright, int color) {
	    // xp and yp are originally in level coordinates, but offset turns them to screen coordinates.
	    xp -= xOffset; // account for screen offset
	    yp -= yOffset;

	    // determines if the image should be mirrored...
	    boolean mirrorX = (bits & BIT_MIRROR_X) > 0; // horizontally.
	    boolean mirrorY = (bits & BIT_MIRROR_Y) > 0; // vertically.

	    SpriteSheet currentSheet = sheets[sheet];

	    xTile %= currentSheet.width; // to avoid out of bounds
	    yTile %= currentSheet.height; // ^

	    // Gets the offset of the sprite into the spritesheet
	    // pixel array, the 8's represent the size of the box.
	    // (8 by 8 pixel sprite boxes)
	    int toffs = xTile * 8 + yTile * 8 * currentSheet.width; 

	    // Precompute values outside of loop
	    int yLimit = yp + 8;
	    int xLimit = xp + 8;
	    int[] pixels = this.pixels;
	    int[] sheetPixels = currentSheet.pixels;

	    for (int y = yp; y < yLimit; y++) {
	        // If the pixel is out of bounds, then skip the rest of the loop.
	        if (y < 0 || y >= h) {
	            continue;
	        }
	        for (int x = xp; x < xLimit; x++) {
	            // If the pixel is out of bounds, then skip the rest of the loop.
	            if (x < 0 || x >= w) {
	                continue;
	            }

	            int xs = x - xp;
	            int ys = y - yp;

	            // Reverses the pixel for a mirroring effect if necessary
	            if (mirrorX) {
	                xs = 7 - xs;
	            }
	            if (mirrorY) {
	                ys = 7 - ys;
	            }

	            // Gets the color of the current pixel from the value stored in the sheet.
	            int spriteColor = sheetPixels[toffs + xs + ys * currentSheet.width];

	            // Check if the pixel is transparent
	            if ((spriteColor >> 24) != 0) {
	                int position = x + y * w;

	                if (whiteTint != -1 && spriteColor == 0x1FFFFFF) {
	                    // if this is white, write the whiteTint over it
	                    pixels[position] = Color.upgrade(whiteTint);
	                } else {
	                    // Inserts the colors into the image
	                    if (fullbright) {
	                        pixels[position] = Color.WHITE; // mob color when hit
	                    } else {
	                        if (color != 0) { // full sprite color
	                            pixels[position] = color;
	                        } else {
	                            pixels[position] = Color.upgrade(spriteColor);
	                        }
	                    }
	                }
	            }
	        }
	    }
	}

	/** Sets the offset of the screen */
	public void setOffset(int xOffset, int yOffset) {
		// this is called in few places, one of which is level.renderBackground, right
		// before all the tiles are rendered. The offset is determined by the Game class
		// (this only place renderBackground is called), by using the screen's width and
		// the player's position in the level.
		// in other words, the offset is a conversion factor from level coordinates to
		// screen coordinates. It makes a certain coord in the level the upper left
		// corner of the screen, when subtracted from the tile coord.

		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

	/*
	 * Used for the scattered dots at the edge of the light radius underground.
	 * 
	 * These values represent the minimum light level, on a scale from 0 to 25
	 * (255/10), 0 being no light, 25 being full light (which will be portrayed as
	 * transparent on the overlay lightScreen pixels) that a pixel must have in
	 * order to remain lit (not black). each row and column is repeated every 4
	 * pixels in the proper direction, so the pixel lightness minimum varies. It's
	 * highly worth note that, as the rows progress and loop, there's two sets or
	 * rows (1,4 and 2,3) whose values in the same column add to 15. The exact same
	 * is true for columns (sets are also 1,4 and 2,3), execpt the sums of values in
	 * the same row and set differ for each row: 10, 18, 12, 20. Which...
	 * themselves... are another set... adding to 30... which makes sense, sort of,
	 * since each column totals 15+15=30. In the end, "every other every row", will
	 * need, for example in column 1, 15 light to be lit, then 0 light to be lit,
	 * then 12 light to be lit, then 3 light to be lit. So, the pixels of lower
	 * light levels will generally be lit every other pixel, while the brighter ones
	 * appear more often. The reason for the variance in values is to provide EVERY
	 * number between 0 and 15, so that all possible light levels (below 16) are
	 * represented fittingly with their own pattern of lit and not lit. 16 is the
	 * minimum pixel lighness required to ensure that the pixel will always remain
	 * lit.
	 * 
	 * LOT TEXT
	 */

	private static final int[] dither = new int[] { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5 };

	/** Overlays the screen with pixels */
	public void overlay(Screen screen, int currentLevel, int xa, int ya) {
		double tintFactor = 0;
		if (currentLevel >= 3 && currentLevel < 5) {
			int transTime = Updater.dayLength / 4;
			double relTime = (Updater.tickCount % transTime) * 1.0 / transTime;

			switch (Updater.getTime()) {
				case Morning: tintFactor = Updater.pastDay1 ? (1 - relTime) * MAXDARK : 0; break;
				case Day: tintFactor = 0; break;
				case Evening: tintFactor = relTime * MAXDARK; break;
				case Night: tintFactor = MAXDARK; break;
			}

			if (currentLevel > 3) {
				tintFactor -= (tintFactor < 10 ? tintFactor : 10);
			}

			tintFactor *= -1; // all previous operations were assuming this was a darkening factor.

		} else if (currentLevel >= 5) {
			tintFactor = -MAXDARK;
		}

		int[] oPixels = screen.pixels; // The Integer array of pixels to overlay the screen with.
		int i = 0; // current pixel on the screen
		for (int y = 0; y < h; y++) { // loop through height of screen
			for (int x = 0; x < w; x++) { // loop through width of screen
				if (oPixels[i] / 10 <= dither[((x + xa) & 3) + ((y + ya) & 3) * 4]) {
					/// the above if statement is simply comparing the light level stored in oPixels
					/// with the minimum light level stored in dither. if it is determined that the
					/// oPixels[i] is less than the minimum requirements, the pixel is considered
					/// "dark", and the below is executed...
					if (currentLevel < 3) { // if in caves...
						/// in the caves, not being lit means being pitch black.
						pixels[i] = 0;
					} else {
						/// outside the caves, not being lit simply means being darker.
						pixels[i] = Color.tintColor(pixels[i], (int) tintFactor); // darkens the color one shade.
					}
				}

				// increase the tinting of all colors by 20.
				pixels[i] = Color.tintColor(pixels[i], 20);
				i++; // moves to the next pixel.
			}
		}
	}

	public void darkness(Screen screen, int currentLevel, int xa, int ya) {

		double tintFactor = 0;
		if (currentLevel >= 3  && currentLevel < 5) {
			int transTime = Updater.dayLength / 4;
			double relTime = (Updater.tickCount % transTime) * 1.0 / transTime;

			switch (Updater.getTime()) {
			case Morning: tintFactor = Updater.pastDay1 ? (1 - relTime) * MAXDARK : 0; break;
			case Day: tintFactor = 0; break;
			case Evening: tintFactor = relTime * MAXDARK; break;
			case Night: tintFactor = MAXDARK; break;
			}

			if (currentLevel > 3) {
				tintFactor -= (tintFactor < 10 ? tintFactor : 10);
			}

			tintFactor *= -1; // all previous operations were assuming this was a darkening factor.

		} else if (currentLevel >= 5) {
			//tintFactor = -MAXDARK;
		}

		int[] oPixels = screen.pixels;
		int i = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (oPixels[i] / 256 <= dither[((x + xa) & 3) + ((y + ya) & 3) * 4]) {
					int intense = (128 + oPixels[i]) / 128;

					if (currentLevel < 3) { // if in caves...

						//pixels[i] = Color.createShadowCol(pixels[i], Math.min(intens2, 1), 0, Math.min(intens2, 1));

					} else {
						/// outside the caves, not being lit simply means being darker.
						pixels[i] = Color.tintColor(pixels[i], (int) tintFactor); // darkens the color one shade.
					}

					if (intense != 0) {
						switch (intense) {
                            case 5: pixels[i] = Color.createShadowCol(Color.tintColor(pixels[i], (int) tintFactor), (int)Math.min(intense, 1), 6, Math.min(intense, 1)); break;
						}
					}
				}

				// increase the tinting of all colors by 20.
				pixels[i] = Color.tintColor(pixels[i], 20);
				i++; // moves to the next pixel

			}
		}
	}

	public void renderLight(int x, int y, int r) {
		// applies offsets:
		x -= xOffset;
		y -= yOffset;

		// starting, ending, x, y, positions of the circle (of light)
		int x0 = x - r;
		int x1 = x + r;
		int y0 = y - r;
		int y1 = y + r;

		// prevent light from rendering off the screen:
		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (x1 > w) x1 = w;
		if (y1 > h) y1 = h;

		for (int yy = y0; yy < y1; yy++) { // loop through each y position
			int yd = yy - y; // get distance to the previous y position.
			yd = yd * yd; // square that distance
			for (int xx = x0; xx < x1; xx++) { // loop though each x pos
				int xd = xx - x; // get x delta
				int dist = xd * xd + yd; // square x delta, then add the y delta, to get total distance.

				if (dist <= r * r) {
					// if the distance from the center (x,y) is less or equal to the radius...
					int br = 255 - dist * 255 / (r * r); // area where light will be rendered. // r*r is becuase dist is still x*x+y*y, of pythag theorem.
					// br = brightness... literally. from 0 to 255.

					if (pixels[xx + yy * w] < br) {
						pixels[xx + yy * w] = br; // pixel cannot be smaller than br; in other words, the pixel color (brightness) cannot be less than br.
					}
				}
			}
		}
	}

	public void setPixel(int xp, int yp, int color) {
		// If the pixel is out of bounds, then skip the rest of the loop.
		if (yp < 0 || yp >= h || xp < 0 || xp >= w) {
			return;
		}

		if (color >> 24 != 0) {
			pixels[xp + yp * w] = color;
		}
	}
	
	private int blendColors(int color, int bottomColor, float opacity) {
		// Extract the alpha, red, green, and blue channel values from the color integer
		int alpha = (int)((color >> 24) & 0xff);
		int red = (int)((color >> 16) & 0xff);
		int green = (int)((color >> 8) & 0xff);
		int blue = (int)(color & 0xff);
		
		// Calculate the index of the pixel in the pixel array
		int pixelAlpha = (bottomColor >> 24) & 0xff;
		int pixelRed = (bottomColor >> 16) & 0xff;
		int pixelGreen = (bottomColor >> 8) & 0xff;
		int pixelBlue = bottomColor & 0xff;

		// Blend the two colors using the alpha compositing formula
		int newAlpha = (int)((alpha * opacity) + (pixelAlpha * (1 - opacity)));
		int newRed = (int)((red * opacity) + (pixelRed * (1 - opacity)));
		int newGreen = (int)((green * opacity) + (pixelGreen * (1 - opacity)));
		int newBlue = (int)((blue * opacity) + (pixelBlue * (1 - opacity)));

		// Combine the channels back into a single integer color value
		int newColor = (newAlpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
		
		return newColor; 
	}
	
}
