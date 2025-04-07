package minicraft.level;

import minicraft.core.Game;
import minicraft.core.io.Settings;
import minicraft.level.tile.Tiles;
import minicraft.screen.LoadingDisplay;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Random;

public class LevelGen {

    private static long worldSeed = 0;

    private static final Random random = new Random(worldSeed); // Initializes the random class
    private static final int stairsRadius = 15;

    private double[] values; // An array of doubles, used to help making noise for the map

    private final int w, h;

    /**
     * This creates noise to create random values for level generation
     */
    private LevelGen(int w, int h, int featureSize) {
        this.w = w;
        this.h = h;

        values = new double[w * h]; // Creates the size of the value array (width * height)

        /// to be 16 or 32, in the code below.
        for (int y = 0; y < w; y += featureSize) {
            for (int x = 0; x < w; x += featureSize) {

                // This method sets the random value from -1 to 1 at the given coordinate.
                setSample(x, y, random.nextFloat() * 2 - 1);
            }
        }

        int stepSize = featureSize;
        double scale = (1.3 / ((double) w));
        double scaleMod = 1.0;

        do {
            int halfStep = stepSize / 2;
            for (int y = 0; y < h; y += stepSize) {
                for (int x = 0; x < w; x += stepSize) { // this loops through the values again, by a given increment...

                    double a = sample(x, y); // fetches the value at the coordinate set previously (it fetches the exact same ones that were just set above)
                    double b = sample(x + stepSize, y); // fetches the value at the next coordinate over. This could possibly loop over at the end, and fetch the first value in the row instead.
                    double c = sample(x, y + stepSize); // fetches the next value down, possibly looping back to the top of the column.
                    double d = sample(x + stepSize, y + stepSize); // fetches the value one down, one right.

                    /*
                     * This could probably use some explaining... Note: the number values are
                     * probably only good the first time around...
                     *
                     * This starts with taking the average of the four numbers from before (they
                     * form a little square in adjacent tiles), each of which holds a value from -1
                     * to 1. Then, it basically adds a 5th number, generated the same way as before.
                     * However, this 5th number is multiplied by a few things first... ...by
                     * stepSize, aka featureSize, and scale, which is 2/size the first time.
                     * featureSize is 16 or 32, which is a multiple of the common level size, 128.
                     * Precisely, it is 128 / 8, or 128 / 4, respectively with 16 and 32. So, the
                     * equation becomes size / const * 2 / size, or, simplified, 2 / const. For a
                     * feature size of 32, stepSize * scale = 2 / 4 = 1/2. featureSize of 16, it's 2
                     * / 8 = 1/4. Later on, this gets closer to 4 / 4 = 1, so... the 5th value may
                     * not change much at all in later iterations for a feature size of 32, which
                     * means it has an effect of 1, which is actually quite significant to the value
                     * that is set. So, it tends to decrease the 5th -1 or 1 number, sometimes
                     * making it of equal value to the other 4 numbers, sort of. It will usually
                     * change the end result by 0.5 to 0.25, perhaps; at max.
                     */
                    double e = (a + b + c + d) / 4.0 + (random.nextFloat() * 2 - 1) * stepSize * scale;

                    /*
                     * This sets the value that is right in the middle of the other 4 to an average
                     * of the four, plus a 5th number, which makes it slightly off, differing by
                     * about 0.25 or so on average, the first time around.
                     */
                    setSample(x + halfStep, y + halfStep, e);
                }
            }

            // This loop does the same as before, but it takes into account some of the half Steps we set in the last loop.
            for (int y = 0; y < h; y += stepSize) {
                for (int x = 0; x < w; x += stepSize) {

                    double a = sample(x, y); // middle (current) tile
                    double b = sample(x + stepSize, y); // right tile
                    double c = sample(x, y + stepSize); // bottom tile
                    double d = sample(x + halfStep, y + halfStep); // mid-right, mid-bottom tile
                    double e = sample(x + halfStep, y - halfStep); // mid-right, mid-top tile
                    double f = sample(x - halfStep, y + halfStep); // mid-left, mid-bottom tile

                    // The 0.5 at the end is because we are going by half-steps..?
                    // The H is for the right and surrounding mids, and g is the bottom and
                    // surrounding mids.
                    double H = (a + b + d + e) / 4.0 + (random.nextFloat() * 2 - 1) * stepSize * scale * 0.5; // adds middle, right, mr-mb, mr-mt, and random.
                    double g = (a + c + d + f) / 4.0 + (random.nextFloat() * 2 - 1) * stepSize * scale * 0.5; // adds middle, bottom, mr-mb, ml-mb, and random.

                    setSample(x + halfStep, y, H); // Sets the H to the mid-right
                    setSample(x, y + halfStep, g); // Sets the g to the mid-bottom
                }
            }

			/*
             THEN... this stuff is set to repeat the system all over again! The featureSize is halved, allowing access to further unset
			 mids, and the scale changes... The scale increases the first time, x1.8, but the second time it's x1.1, and after that
			 probably a little less than 1. So, it generally increases a bit, maybe to 4 / w at tops. This results in the 5th random value
			 being more significant than the first 4 ones in later iterations.
			*/
            stepSize /= 2;
            scale *= (scaleMod + 0.7);
            scaleMod *= 0.3;

        } while (stepSize > 1); // This stops when the stepsize is < 1, aka 0 b/c it's an int. At this point there are no more mid values.
    }

    // This merely returns the value, like Level.getTile(x, y).
    private double sample(int x, int y) {
        return values[(x & (w - 1)) + (y & (h - 1)) * w];
    }

    private void setSample(int x, int y, double value) {
		/*
		  This method is short, but difficult to understand. This is what I think it does: The values array is like a 2D array, but
		  formatted into a 1D array; so the basic "x + y * w" is used to access a given value. The value parameter is a random number,
		  above set to be a random decimal from -1 to 1. From above, we can see that the x and y values passed in range from 0 to the
		  width/height, and increment by a certain constant known as the "featureSize". This implies that the locations chosen from this
		  array, to put the random value in, somehow determine the size of biomes, perhaps. The x/y value is taken and AND'ed with the
		  size-1, which could be 127. This just caps the value at 127; however, it shouldn't be higher in the first place, so it is merely
		  a safety measure.

		  In other words, this is just "values[x + y * w] = value;"
		 */
        values[(x & (w - 1)) + (y & (h - 1)) * w] = value;
    }

    @Nullable
    static short[][] createAndValidateMap(int w, int h, int level, long seed) {
        worldSeed = seed;

        if (level == 1) return createAndValidateSkyMap(w, h);
        if (level == 0) return createAndValidateTopMap(w, h);
        if (level == -4) return createAndValidateDungeon(w, h);
        if ((level > -4) && (level < 0)) return createAndValidateUndergroundMap(w, h, -level);
        if (level == 2) return createAndValidateVoidMap(w, h);  // World.java is 2 as here

        Logger.error("Could not generate level {}, index is not valid.", level);
        return null;
    }

    private static short[][] createAndValidateTopMap(int w, int h) {
        random.setSeed(worldSeed);

        LoadingDisplay.setMessage("Generating surface!");

        do {
            short[][] result = createTopMap(w, h);

            int[] count = new int[256];

            for (int i = 0; i < w * h; i++) {
                count[result[0][i] & 0xffff]++;
            }

            if (count[Tiles.get("Rock").id & 0xffff] < 100) continue;
            if (count[Tiles.get("Grass").id & 0xffff] < 100) continue;
            if (count[Tiles.get("Oak Tree").id & 0xffff] < 100) continue;
            if (count[Tiles.get("Stairs Down").id & 0xffff] == 0) continue;

            return result;
        } while (true);
    }

    private static short[][] createAndValidateUndergroundMap(int w, int h, int depth) {
        random.setSeed(worldSeed);

        LoadingDisplay.setMessage("Generating caves!");

        do {
            short[][] result = createUndergroundMap(w, h, depth);

            int[] count = new int[256];

            for (int i = 0; i < w * h; i++) {
                count[result[0][i] & 0xffff]++;
            }

            if (count[Tiles.get("Rock").id & 0xffff] < 100) continue;
            if (count[Tiles.get("Dirt").id & 0xffff] < 100) continue;
            if (count[(Tiles.get("Iron Ore").id & 0xffff) + depth - 1] < 20) continue;
            if ((depth < 3) && (count[Tiles.get("Stairs Down").id & 0xffff] < (w / 32))) continue;
            return result;

        } while (true);
    }

    private static short[][] createAndValidateDungeon(int w, int h) {
        random.setSeed(worldSeed);

        LoadingDisplay.setMessage("Generating the dungeon!");

        do {
            short[][] result = createDungeon(w, h);

            int[] count = new int[256];

            for (int i = 0; i < w * h; i++) {
                count[result[0][i] & 0xffff]++;
            }

            if (count[Tiles.get("Obsidian").id & 0xffff] < 100) continue;
            if (count[Tiles.get("Obsidian Wall").id & 0xffff] < 100) continue;
            if (count[Tiles.get("Raw Obsidian").id & 0xffff] < 100) continue;

            return result;

        } while (true);
    }

    @Nullable
    private static short[][] createAndValidateSkyMap(int w, int h) {
        random.setSeed(worldSeed);

        LoadingDisplay.setMessage("Generating the heaven!");

        do {
            short[][] result = createSkyMap(w, h);

            int[] count = new int[256];

            for (int i = 0; i < w * h; i++) {
                count[result[0][i] & 0xffff]++;
            }

            if (count[Tiles.get("Cloud").id & 0xffff] < 2000) continue;
            if (count[Tiles.get("Stairs Down").id & 0xffff] < w / 64) continue;

            return result;

        } while (true);
    }

    @Nullable
    public static short[][] createAndValidateVoidMap(int w, int h) {
        random.setSeed(worldSeed);

        do {
            short[][] result = createVoidMap(w, h);

            int[] count = new int[256];

            for (int i = 0; i < w * h; i++) {
                count[result[0][i] & 0xffff]++;
            }

            if (count[Tiles.get("Rock").id & 0xffff] < 100) continue;
            if (count[Tiles.get("Grass").id & 0xffff] < 100) continue;
            if (count[Tiles.get("Oak Tree").id & 0xffff] < 100) continue;

            return result;

        } while (true);
    }

    // Surface generation code
    private static short[][] createTopMap(int w, int h) { // create surface map

        // creates a bunch of value maps, some with small size...
        LevelGen mnoise1 = new LevelGen(w, h, 16);
        LevelGen mnoise2 = new LevelGen(w, h, 16);
        LevelGen mnoise3 = new LevelGen(w, h, 16);

        // ...and some with larger size..
        LevelGen noise1 = new LevelGen(w, h, 32);
        LevelGen noise2 = new LevelGen(w, h, 32);

        // humidity noise
        LevelGen hnoise1 = new LevelGen(w, h, 64);
        LevelGen hnoise2 = new LevelGen(w, h, 64);

        int fullSize = w * h;

        short[] map = new short[fullSize];
        short[] data = new short[fullSize];

        String terrainType = (String) Settings.get("Type");
        String terrainTheme = (String) Settings.get("Theme");

        // Decoration
        int treeThreshold = fullSize / 200;
        int flowerThreshold = fullSize / 400;

        // Tiles that the mountains generation can overwrite
        int[] rockableTiles = {
            Tiles.get("Grass").id, Tiles.get("Snow").id, Tiles.get("Sand").id,
            Tiles.get("Fir Tree").id, Tiles.get("Oak Tree").id, Tiles.get("Birch Tree").id,
            Tiles.get("Pine Tree").id, Tiles.get("Ice Spike").id, Tiles.get("Rose").id,
            Tiles.get("Lawn").id, Tiles.get("Dandelion").id, Tiles.get("Poppy").id,
            Tiles.get("Daisy").id
        };

        // Tiles that the beachs generation can overwrite
        int[] beachableTiles = {
            Tiles.get("Grass").id, Tiles.get("Oak Tree").id, Tiles.get("Daisy").id,
            Tiles.get("Poppy").id, Tiles.get("Rose").id, Tiles.get("Birch Tree").id,
            Tiles.get("Lawn").id, Tiles.get("Dandelion").id
        };

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = x + y * w;

                double val = Math.abs(noise1.values[i] - noise2.values[i]) * 3 - 2;
                double mval = Math.abs(mnoise1.values[i] - mnoise2.values[i]);
                mval = Math.abs(mval - mnoise3.values[i]) * 3 - 2;

                // temperature and humidity noise values
                double tval = Math.abs(noise2.values[i] - noise1.values[i]) * 3 - 2;
                double hval = Math.abs(hnoise1.values[i] - hnoise2.values[i]) * 3 - 2;

                // this calculates a sort of distance based on the current coordinate.
                double xd = x / (w - 1.0) * 2 - 1;
                double yd = y / (h - 1.0) * 2 - 1;

                if (xd < 0) xd = -xd;
                if (yd < 0) yd = -yd;

                double dist = Math.max(xd, yd);
                dist = dist * dist * dist * dist;
                dist = dist * dist * dist * dist;
                val += 1 - dist * 16;

                tval += 1 - dist * 16;
                hval += 1 - dist * 16;

                // NOTES:
                // val -> height value
                // tval -> temperature value
                // hval -> humidity value
                // mval -> mountain value

                // World themes logic
                if (Objects.equals(terrainType, "Island")) {
                    // Oceans generation
                    if (val < -0.8) {
                        if (Objects.equals(terrainTheme, "Hell")) {
                            map[i] = Tiles.get("Lava").id;
                        } else {
                            if (tval > -0.20 && hval > 0.60) {
                                // Glaciers
                                map[i] = Tiles.get("Ice").id;
                            } else {
                                // Warm Ocean
                                map[i] = Tiles.get("Water").id;
                            }
                        }

                    // Mountains generation
                    } else if (val > 1.2 && mval > -1.5) {
                        // Terrain themes
                        if (Objects.equals(terrainTheme, "Desert")) {
                            if (tval > 0.30 && hval < 0.16) {
                                //map[i] = Tiles.get("Sand Rock").id;
                                // Desert mountains
                                map[i] = Tiles.get("Up Rock").id;
                            } else {
                                // Normal mountains
                                map[i] = Tiles.get("Up Rock").id;
                            }
                        } else if (Objects.equals(terrainTheme, "Tundra")) {
                            map[i] = Tiles.get("Up Rock").id;
                        }

                        // Normal generation
                        else if (tval > 0.30 && hval < 0.16) {
                            //map[i] = Tiles.get("Sand Rock").id;
                            // Desert mountains
                            map[i] = Tiles.get("Up Rock").id;
                        } else {
                            // Normal mountains
                            map[i] = Tiles.get("Up Rock").id;
                        }

                    // Terrain generation
                    } else {
                        // Terrain themes
                        if (Objects.equals(terrainTheme, "Desert")) {
                            if (tval > -0.45 && hval > -0.95 && val > -0.16) {
                                map[i] = Tiles.get("Sand").id;
                            } else {
                                map[i] = Tiles.get("Grass").id;
                            }
                        } else if (Objects.equals(terrainTheme, "Tundra")) {
                            if (tval > -0.45 && hval > -0.95) {
                                map[i] = Tiles.get("Snow").id;
                            } else {
                                map[i] = Tiles.get("Grass").id;
                            }
                        }

                        // Normal generation
                        else if (tval > 0.30 && hval < 0.16 && val > 0.41) {
                            map[i] = Tiles.get("Sand").id;
                        } else {
                            if (tval > -0.20 && hval > 0.60) {
                                map[i] = Tiles.get("Snow").id;
                            } else {
                                map[i] = Tiles.get("Grass").id;
                            }
                        }
                    }
                }

                if (Objects.equals(terrainType, "Box")) {
                    if (val < -1.5) {
                        if (Objects.equals(terrainTheme, "Hell")) {
                            map[i] = Tiles.get("Lava").id;
                        } else {
                            map[i] = Tiles.get("Water").id;
                        }

                    } else if (val > 0.5 && mval < -1.5) {
                        map[i] = Tiles.get("Up Rock").id;

                    } else {
                        map[i] = Tiles.get("Grass").id;
                    }
                }

                if (Objects.equals(terrainType, "Mountain")) {
                    if (val < -0.4) {
                        map[i] = Tiles.get("Grass").id;
                    } else if (val > 0.5 && mval < -1.5) {
                        if (Objects.equals(terrainTheme, "Hell")) {
                            map[i] = Tiles.get("Lava").id;
                        } else {
                            map[i] = Tiles.get("Water").id;
                        }
                    } else {
                        map[i] = Tiles.get("Up rock").id;
                    }
                }

                if (Objects.equals(terrainType, "Irregular")) {
                    if (val < -0.5 && mval < -0.5) {
                        if (Objects.equals(terrainTheme, "Hell")) {
                            map[i] = Tiles.get("Lava").id;
                        } else {
                            map[i] = Tiles.get("Water").id;
                        }

                    } else if (val > 0.5 && mval < -1.5) {
                        map[i] = Tiles.get("Rock").id;
                    } else if (val < -0.5 && mval > -1.5) {
                        // Irregular beaches beaches
                        map[i] = Tiles.get("sand").id;
                    } else {
                        map[i] = Tiles.get("Grass").id;
                    }
                }

            }
        }


        /// FOREST GENERATION STEP
        LoadingDisplay.setMessage("Generating forests");
        /*for (int i = 0; i < treeThreshold; i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);

            for (int j = 0; j < 100; j++) {
                int xx = (x + random.nextInt(15)) - random.nextInt(10);
                int yy = (y + random.nextInt(15)) - random.nextInt(10);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    int index = xx + yy * w;

                    if (map[index] == Tiles.get("Snow").id) {
                        if (j % 2 == 0) {
                            map[index] = Tiles.get("Fir Tree").id;
                        } else {
                            map[index] = Tiles.get("Pine Tree").id;
                        }

                    } else if (map[index] == Tiles.get("Grass").id) {
                        if (j % 2 == 0) {
                            map[index] = Tiles.get("Oak Tree").id;
                        } else {
                            map[index] = Tiles.get("Birch Tree").id;
                        }
                    }
                }
            }
        }*/

        for (int i = 0; i < treeThreshold; i++) {
            int centerX = random.nextInt(w);
            int centerY = random.nextInt(h);

            // The value to set the tree type for this cluster
            float baseNoise = random.nextFloat();

            for (int j = 0; j < 100; j++) { // 100 is the cluster size
                // 10 is the spread offset, and 15 the spread radius
                int xx = (centerX + random.nextInt(15)) - random.nextInt(10);
                int yy = (centerY + random.nextInt(15)) - random.nextInt(10);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    int index = xx + yy * w;

                    // Get noise in the current position
                    float noise = (
                        baseNoise +
                        (float) Math.sin(xx * 0.1) * 0.3f +
                        (float) Math.cos(yy * 0.1) * 0.3f
                    ) % 1.0f;

                    // We modify the noise value using the distancre from the cluster center
                    float distanceFromCenter = (float) Math.sqrt(Math.pow(xx - centerX, 2) + Math.pow(yy - centerY, 2)) / 15;

                    noise = (noise + (1 - distanceFromCenter)) * 0.5f;

                    if (map[index] == Tiles.get("Snow").id) {
                        if (noise < 0.5f) {
                            map[index] = Tiles.get("Fir Tree").id;
                        } else {
                            map[index] = Tiles.get("Pine Tree").id;
                        }
                    } else if (map[index] == Tiles.get("Grass").id) {
                        if (noise < 0.5f) {
                            map[index] = Tiles.get("Oak Tree").id;
                        } else {
                            map[index] = Tiles.get("Birch Tree").id;
                        }
                    }
                }
            }
        }

        // VEGETATION GENERATION STEP
        LoadingDisplay.setMessage("Generating meadow");
        for (int i = 0; i < flowerThreshold; i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            int pos = random.nextInt(4);

            for (int j = 0; j < 30; j++) {
                int xx = x + random.nextInt(5) - random.nextInt(5);
                int yy = y + random.nextInt(5) - random.nextInt(5);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    int index = xx + yy * w;

                    if (map[index] == Tiles.get("Grass").id) {
                        if (random.nextBoolean()) {
                            if (random.nextBoolean()) {
                                map[index] = Tiles.get("Rose").id;
                            } else {
                                map[index] = Tiles.get("Poppy").id;
                            }
                        } else {
                            if (random.nextBoolean()) {
                                map[index] = Tiles.get("Dandelion").id;
                            } else {
                                map[index] = Tiles.get("Daisy").id;
                            }
                        }

                        data[index] = (short) (pos + random.nextInt(4) * 16);  // data determines which way the flower faces
                    }
                }
            }
        }

        for (int i = 0; i < flowerThreshold; i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            int pos = random.nextInt(4);

            for (int j = 0; j < 100; j++) {
                int xx = x + random.nextInt(5) - random.nextInt(5);
                int yy = y + random.nextInt(5) - random.nextInt(5);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    int index = xx + yy * w;

                    if (map[index] == Tiles.get("Grass").id) {
                        map[index] = Tiles.get("Lawn").id;
                        data[index] = (short) (pos + random.nextInt(4) * 16);
                    }
                }
            }
        }

        for (int i = 0; i < (fullSize / 100); i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);

            for (int j = 0; j < 8; j++) {
                int xx = x + random.nextInt(15) + random.nextInt(10) + random.nextInt(5);
                int yy = y + random.nextInt(15) + random.nextInt(10) + random.nextInt(5);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    int index = xx + yy * w;
                    if (map[index] == Tiles.get("Sand").id && random.nextInt(6) == 3) {
                        map[index] = Tiles.get("Cactus").id;
                    }
                }
            }
        }


        // add ice spikes to snow
        for (int i = 0; i < (fullSize / 100); i++) {
            int xx = random.nextInt(w);
            int yy = random.nextInt(h);

            if ((xx >= 0 && yy >= 0) && (xx < w && yy < h)) {
                int index = xx + yy * w;
                if (map[index] == Tiles.get("Snow").id) {
                    map[index] = Tiles.get("Ice Spike").id;
                }
            }
        }

        // same...
        for (int i = 0; i < (fullSize / 100); i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);

            for (int j = 0; j < 20; j++) {
                int xx = x + random.nextInt(2) - random.nextInt(2);
                int yy = y + random.nextInt(2) - random.nextInt(2);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    int index = xx + yy * w;
                    if (map[index] == Tiles.get("Snow").id) {
                        map[index] = Tiles.get("Ice Spike").id;
                    }
                }
            }
        }

        for (int i = 0; i < (fullSize / 100); i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);

            for (int j = 0; j < 20; j++) {
                int xx = x + random.nextInt(2) - random.nextInt(2);
                int yy = y + random.nextInt(2) - random.nextInt(2);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    int index = xx + yy * w;
                    if (map[index] == Tiles.get("Snow").id) {
                        map[index] = Tiles.get("Ice").id;
                    }
                }
            }
        }


        LoadingDisplay.setMessage("Generating beaches");
        for (int j = 0; j < h; j++) {
            int offset = j * w;

            for (int x = 0; x < w; x++) {
                int currentTile = x + offset;

                boolean isReplaceable = false;
                for (int tile : beachableTiles) {
                    if (map[currentTile] == tile) {
                        isReplaceable = true;
                        break;
                    }
                }

                if (!isReplaceable) continue;

                // Calcular los límites de búsqueda una sola vez
                int searchRadius = 1 + random.nextInt(1);
                int minTx = Math.max(0, x - searchRadius);
                int maxTx = Math.min(w - 1, x + searchRadius);
                int minTy = Math.max(0, j - searchRadius);
                int maxTy = Math.min(h - 1, j + searchRadius);

                // Buscar agua o lava en los alrededores
                checkSurroundings: {
                    for (int ty = minTy; ty <= maxTy; ty++) {
                        int tyOffset = ty * w;
                        for (int tx = minTx; tx <= maxTx; tx++) {
                            if (tx == x && ty == j) continue;

                            int nearbyTile = map[tx + tyOffset];
                            if (nearbyTile == Tiles.get("Water").id || nearbyTile == Tiles.get("Lava").id) {
                                map[currentTile] = Tiles.get("Sand").id;
                                break checkSurroundings;
                            }
                        }
                    }
                }
            }
        }

		/*if (random.nextBoolean()) {
			for (int j = 0; j < h; j++) {
				for (int x = 0; x < w; x++) {
					int index = x + j * w;
					if (map[index] != Tiles.get("Up Rock").id && (
							map[index] == Tiles.get("Sand Rock").id)) {
						boolean replace = false;

						// Check the tiles around the current position
						for (int tx = x - (2 + random.nextInt(1)); tx <= x + (2 + random.nextInt(1)); tx++) {
							for (int ty = j - (2 + random.nextInt(1)); ty <= j + (2 + random.nextInt(1)); ty++) {
								if ((tx >= 0 && ty >= 0 && tx < w && ty < h) && (tx != x || ty != j)) {
									if (map[tx + ty * w] == Tiles.get("Up Rock").id) {
										replace = true;
										break;
									}
								}
							}
						}

						if (replace) {
							map[index] = Tiles.get("Sand").id;
						}
					}
				}
			}
		}*/

        LoadingDisplay.setMessage("Generating mountains");
        for (int j = 0; j < h; j++) { // Iterate on the world height (y axis)
            int offset = j * w;

            for (int x = 0; x < w; x++) { // Iterate on the world width (x axis)
                int currentTile = x + offset; // We save the current tile position

                if (map[currentTile] != Tiles.get("Up Rock").id) continue;

                int minTx = Math.max(0, x - 1);
                int maxTx = Math.min(w - 1, x + 1);
                int minTy = Math.max(0, j - 1);
                int maxTy = Math.min(h - 1, j + 1);

                checkSurroundings: {
                    for (int ty = minTy; ty <= maxTy; ty++) {
                        int tyOffset = ty * w;
                        for (int tx = minTx; tx <= maxTx; tx++) {
                            if (tx == x && ty == j) continue;

                            int tileToCheck = map[tx + tyOffset];
                            for (int replaceableTile : rockableTiles) {
                                if (tileToCheck == replaceableTile) {
                                    map[currentTile] = Tiles.get("Rock").id;
                                    break checkSurroundings;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (int j = 0; j < h; j++) {
            for (int x = 0; x < w; x++) { // if there are Grass tiles or Trees in front of the Sand tiles, if so, replace them with Grass
                if (map[x + j * w] != Tiles.get("Sand").id && map[x + j * w] == Tiles.get("Grass").id ||
                        map[x + j * w] != Tiles.get("Sand").id && map[x + j * w] == Tiles.get("Birch Tree").id ||
                        map[x + j * w] != Tiles.get("Sand").id && map[x + j * w] == Tiles.get("Oak Tree").id) {
                    boolean replace = false;

                    check_ocean:
                    for (int tx = x - 1; tx <= x + 1; tx++) {
                        for (int ty = j - 1; ty <= j + 1; ty++) {
                            if (tx >= 0 && ty >= 0 && tx <= w && ty <= h && (tx != x || ty != j)) {
                                if (map[tx + ty * w] == Tiles.get("Sand").id) {
                                    replace = true;
                                    break check_ocean;
                                }
                            }
                        }
                    }

                    if (replace) {
                        map[x + j * w] = Tiles.get("Grass").id;
                    }
                }
            }
        }

        for (int j = 0; j < h; j++) {
            for (int x = 0; x < w; x++) { //  if there are Snow tiles or Trees in front of the Ice tiles, if so, replace them with snow
                if (map[x + j * w] != Tiles.get("Ice").id && map[x + j * w] == Tiles.get("Snow").id ||
                        map[x + j * w] != Tiles.get("Ice").id && map[x + j * w] == Tiles.get("Fir Tree").id ||
                        map[x + j * w] != Tiles.get("Ice").id && map[x + j * w] == Tiles.get("Pine Tree").id) {
                    boolean replace = false;

                    check_ocean:
                    for (int tx = x - 1; tx <= x + 1; tx++) {
                        for (int ty = j - 1; ty <= j + 1; ty++) {
                            if (tx >= 0 && ty >= 0 && tx <= w && ty <= h && (tx != x || ty != j)) {
                                if (map[tx + ty * w] == Tiles.get("Ice").id) {
                                    replace = true;
                                    break check_ocean;
                                }
                            }
                        }
                    }

                    if (replace) {
                        map[x + j * w] = Tiles.get("Snow").id;
                    }
                }
            }
        }

        for (int j = 0; j < h; j++) {
            for (int x = 0; x < w; x++) { // if there are Snow tiles or Trees in front of the Grass tiles, if so, replace them with Snow
                if (map[x + j * w] != Tiles.get("Grass").id && map[x + j * w] == Tiles.get("Snow").id ||
                        map[x + j * w] != Tiles.get("Grass").id && map[x + j * w] == Tiles.get("Fir Tree").id ||
                        map[x + j * w] != Tiles.get("Grass").id && map[x + j * w] == Tiles.get("Pine Tree").id) {
                    boolean replace = false;

                    check_ocean:
                    for (int tx = x - 1 - random.nextInt(2); tx <= x + 1 + random.nextInt(2); tx++) {
                        for (int ty = j - 1 - random.nextInt(2); ty <= j + 1 + random.nextInt(2); ty++) {
                            if (tx >= 0 && ty >= 0 && tx <= w && ty <= h && (tx != x || ty != j) && map[tx + ty * w] == Tiles.get("Grass").id) {
                                replace = true;
                                break check_ocean;
                            }
                        }
                    }

                    if (replace) {
                        map[x + j * w] = Tiles.get("Snow").id;
                    }
                }
            }
        }

        // Generate the stairs inside the rock
        int stairsCount = 0;

        stairsLoop:
        for (int i = 0; i < (fullSize / 100); i++) { // loops a certain number of times, more for bigger world

            // Sizes
            int x = random.nextInt(w - 2) + 1;
            int y = random.nextInt(h - 2) + 1;

            // The first loop, which checks to make sure that a new stairs tile will be completely surrounded by rock.
            for (int yy = y - 1; yy <= y + 1; yy++) {
                for (int xx = x - 1; xx <= x + 1; xx++) {
                    if (map[xx + yy * w] != Tiles.get("Up Rock").id) {
                        continue stairsLoop;
                    }
                }
            }

            // This should prevent any stairsDown tile from being within 30 tiles of any other stairsDown tile.
            for (int yy = Math.max(0, y - stairsRadius); yy <= Math.min(h - 1, y + stairsRadius); yy++) {
                for (int xx = Math.max(0, x - stairsRadius); xx <= Math.min(w - 1, x + stairsRadius); xx++) {
                    if (map[xx + yy * w] == Tiles.get("Stairs Down").id) {
                        continue stairsLoop;
                    }
                }
            }

            map[x + y * w] = Tiles.get("Stairs Down").id;

            stairsCount++;
            if (stairsCount >= w / 21) {
                break;
            }
        }

        return new short[][]{map, data};
    }

    // Dungeons generation code
    private static short[][] createDungeon(int w, int h) {

        LevelGen noise1 = new LevelGen(w, h, 8);
        LevelGen noise2 = new LevelGen(w, h, 8);

        int size = w * h;

        short[] map = new short[w * h];
        short[] data = new short[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = x + y * w;

                double val = Math.abs(noise1.values[i] - noise2.values[i]) * 3 - 2;

                double xd = x / (w - 1.1) * 2 - 1;
                double yd = y / (h - 1.1) * 2 - 1;

                if (xd < 0) xd = -xd;
                if (yd < 0) yd = -yd;

                double dist = Math.max(xd, yd);
                dist = dist * dist * dist * dist;
                dist = dist * dist * dist * dist;

                val = -val * 1 - 2.2;
                val += 1 - dist * 2;

                if (val < -0.05) {
                    map[i] = Tiles.get("Obsidian Wall").id;
                } else if (val>=-0.05 && val<-0.03) {
                    map[i] = Tiles.get("Lava").id;
                } else {
                    map[i] = Tiles.get("Obsidian").id;
                }
            }
        }

        lavaLoop:
        for (int i = 0; i < (size / 450); i++) {
            int x = random.nextInt(w - 2) + 1;
            int y = random.nextInt(h - 2) + 1;

            for (int yy = y - 1; yy <= y + 1; yy++) {
                for (int xx = x - 1; xx <= x + 1; xx++) {
                    if (map[xx + yy * w] != Tiles.get("Obsidian Wall").id) {
                        continue lavaLoop;
                    }
                }
            }

            // Generate structure (lava pool)
            Structure.lavaPool.draw(map, x, y, w);
        }

        for (int i = 0; i < (size / 100); i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            for (int j = 0; j < 20; j++) {
                int xx = x + random.nextInt(3) - random.nextInt(3);
                int yy = y + random.nextInt(3) - random.nextInt(3);
                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    if (map[xx + yy * w] == Tiles.get("Obsidian").id) {
                        map[xx + yy * w] = Tiles.get("Raw Obsidian").id;
                    }

                }
            }
        }
        return new short[][]{map, data};
    }

    // Generate cave system
    private static short[][] createUndergroundMap(int w, int h, int depth) {

        LevelGen mnoise1 = new LevelGen(w, h, 16);
        LevelGen mnoise2 = new LevelGen(w, h, 16);
        LevelGen mnoise3 = new LevelGen(w, h, 16);

        LevelGen nnoise1 = new LevelGen(w, h, 16);
        LevelGen nnoise2 = new LevelGen(w, h, 16);
        LevelGen nnoise3 = new LevelGen(w, h, 16);

        LevelGen wnoise1 = new LevelGen(w, h, 16);
        LevelGen wnoise2 = new LevelGen(w, h, 16);
        LevelGen wnoise3 = new LevelGen(w, h, 16);

        LevelGen noise1 = new LevelGen(w, h, 32);
        LevelGen noise2 = new LevelGen(w, h, 32);

        LevelGen hnoise1 = new LevelGen(w, h, 64);
        LevelGen hnoise2 = new LevelGen(w, h, 64);

        /*
         * This generates the 3 levels of cave, iron, gold and gem
         */

        int size = w * h;

        short[] map = new short[w * h];
        short[] data = new short[w * h];

        int oresThreshold = 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = x + y * w;

                /// for the x=0 or y=0 i's, values[i] is always between -1 and 1.
                /// so, val is between -2 and 4.
                /// the rest are between -2 and 7.
                double val = Math.abs(noise1.values[i] - noise2.values[i]) * 3 - 2;

                double mval = Math.abs(mnoise1.values[i] - mnoise2.values[i]);
                mval = Math.abs(mval - mnoise3.values[i]) * 3 - 2;

                double nval = Math.abs(nnoise1.values[i] - nnoise2.values[i]);
                nval = Math.abs(nval - nnoise3.values[i]) * 3 - 2;

                double wval = Math.abs(wnoise1.values[i] - wnoise2.values[i]);
                wval = Math.abs(wval - wnoise3.values[i]) * 3 - 2;

                // Temperature and humidity noise values
                double tval = Math.abs(noise2.values[i] - noise1.values[i]) * 3 - 2;
                double hval = Math.abs(hnoise1.values[i] - hnoise2.values[i]) * 3 - 2;

                double xd = x / (w - 1.0) * 2 - 1;
                double yd = y / (h - 1.0) * 2 - 1;

                if (xd < 0) xd = -xd;
                if (yd < 0) yd = -yd;

                double dist = Math.max(xd, yd);
                dist = Math.pow(dist, 8);
                val += 1 - dist * 16;

                tval += 1 - dist * 16;
                hval += 1 - dist * 16;

                if (val > -1 && wval < -1.4 + (depth) / 2 * 3 && depth == 1) {
                    map[i] = Tiles.get("Dirt").id;

                    // Make level 2 and 3 caves
                } else if (val > -0.7 && wval < -4 + (depth) / 2.0 * 3 && depth != 1) {

                    if (depth == 3) {
                        if (tval < -0.10 && hval < 0.60) {
                            map[i] = Tiles.get("Magma").id;
                        } else {
                            map[i] = Tiles.get("Lava").id;
                        }
                    } else {
                        map[i] = Tiles.get("Water").id;
                    }
                } else if (val > -1.5 && (mval < -1.7 || nval < -1.4)) {
                    if (tval < -0.10 && hval > 0.6 && depth == 1) {
                        map[i] = Tiles.get("Mycelium").id;
                    } else {
                        map[i] = Tiles.get("Dirt").id;
                    }
                } else {
                    map[i] = Tiles.get("Rock").id;
                }
            }
        }

        if (depth == 1) {
            LoadingDisplay.setMessage("Generating mushrooms");
            for (int i = 0; i < (size / 100); i++) {
                int x = random.nextInt(w);
                int y = random.nextInt(h);
                int pos = random.nextInt(4);

                for (int j = 0; j < 20; j++) {
                    int xx = x + random.nextInt(4) - random.nextInt(4);
                    int yy = y + random.nextInt(4) - random.nextInt(4);

                    if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                        if (map[xx + yy * w] == Tiles.get("Mycelium").id) {

                            if (random.nextBoolean()) {
                                map[xx + yy * w] = Tiles.get("Red Mushroom").id;
                            } else {
                                map[xx + yy * w] = Tiles.get("Brown Mushroom").id;
                            }

                            data[xx + yy * w] = (short) (pos + random.nextInt(4) * 16);
                        }
                    }
                }
            }
        }

        /// Generate ores
        LoadingDisplay.setMessage("Generating ores");

        // Iron ore
        for (int i = 0; i < (size / 400); i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            for (int j = 0; j < 25; j++) {
                int xx = x + random.nextInt(5) - random.nextInt(3);
                int yy = y + random.nextInt(5) - random.nextInt(3);
                if (xx >= oresThreshold && yy >= oresThreshold && xx < w - oresThreshold && yy < h - oresThreshold) {
                    if (map[xx + yy * w] == Tiles.get("Rock").id) {
                        map[xx + yy * w] = (short) ((Tiles.get("Iron Ore").id & 0xffff) + depth - 1);
                    }
                }
            }


            // Lapizlazuli ore
            for (int j = 0; j < 10; j++) {
                int xx = x + random.nextInt(3) - random.nextInt(2);
                int yy = y + random.nextInt(3) - random.nextInt(2);
                if (xx >= oresThreshold && yy >= oresThreshold && xx < w - oresThreshold && yy < h - oresThreshold) {
                    if (map[xx + yy * w] == Tiles.get("Rock").id) {
                        map[xx + yy * w] = (short) (Tiles.get("Lapis").id & 0xffff);
                    }
                }
            }
        }


        if (depth > 2) {
            int stairsRadius = 1;
            int xx = w / 2;
            int yy = w / 2;
            for (int i = 0; i < w * h / 380; i++) {
                for (int j = 0; j < 10; j++) {
                    if (xx < w - stairsRadius && yy < h - stairsRadius) {

                        Structure.dungeonLock.draw(map, xx, yy, w);

                        // The "& 0xffff" is a common way to convert a short to an unsigned int,
                        // which basically prevents negative values... except... this doesn't do
                        // anything if you flip it back to a short again...
                        map[xx + yy * w] = (short) (Tiles.get("Stairs Down").id & 0xffff);
                    }
                }
            }
        }

        if (depth < 3) {
            int stairsCount = 0;
            int stairsRadius = 15;

            stairsLoop:
            for (int i = 0; i < w * h / 100; i++) {
                int x = random.nextInt(w - 20) + 10;
                int y = random.nextInt(h - 20) + 10;

                for (int yy = y - 1; yy <= y + 1; yy++) {
                    for (int xx = x - 1; xx <= x + 1; xx++) {
                        if (map[xx + yy * w] != Tiles.get("Rock").id) {
                            continue stairsLoop;
                        }
                    }
                }

                // This should prevent any stairsDown tile from being within 30 tiles of any other stairsDown tile.
                for (int yy = Math.max(0, y - stairsRadius); yy <= Math.min(h - 1, y + stairsRadius); yy++) {
                    for (int xx = Math.max(0, x - stairsRadius); xx <= Math.min(w - 1, x + stairsRadius); xx++) {
                        if (map[xx + yy * w] == Tiles.get("Stairs Down").id) {
                            continue stairsLoop;
                        }
                    }
                }

                map[x + y * w] = Tiles.get("Stairs Down").id;

                stairsCount++;
                if (stairsCount >= w / 32) {
                    break;
                }
            }
        }

        return new short[][] {map, data};
    }

    // Sky dimension generation
    private static short[][] createSkyMap(int w, int h) {
        LevelGen noise1 = new LevelGen(w, h, 8);
        LevelGen noise2 = new LevelGen(w, h, 8);

        int fullsize = w * h;
        int halfWidth = w / 2;
        int halfHeight = h / 2;

        short[] map = new short[fullsize];
        short[] data = new short[fullsize];

        int heavenThreshold = fullsize / 2800;
        int edgesThickness = 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = x + y * w;

                double val = Math.abs(noise1.values[i] - noise2.values[i]) * 3 - 2;
                double xd = x / (w - 1.0) * 2 - 1;
                double yd = y / (h - 1.0) * 2 - 1;

                if (xd < 0) xd = -xd;
                if (yd < 0) yd = -yd;

                double dist = Math.max(xd, yd);
                dist = dist * dist * dist * dist;
                dist = dist * dist * dist * dist;

                val = -val * 1 - 2.2;
                val += 1 - dist * 20;

                if (val < -0.27) {
                    map[i] = Tiles.get("Infinite Fall").id;
                } else {
                    map[i] = Tiles.get("Cloud").id;
                }
                
                data[i] = (short) (0);
            }
        }

        // Generate skygrass in cloud tile
        LoadingDisplay.setMessage("Generating clouds");
        for (int i = 0; i < heavenThreshold; i++) {
            int xs = halfWidth - 22; // divide the 60 (down) by 2 -> 30 to center
            int ys = halfHeight - 22;

            for (int k = 0; k < 90; k++) {
                int x = xs + random.nextInt(28) - random.nextInt(10);
                int y = ys + random.nextInt(28) - random.nextInt(10);

                for (int j = 0; j < 190; j++) {
                    int xo = x + random.nextInt(42) - random.nextInt(21) + random.nextInt(8);
                    int yo = y + random.nextInt(42) - random.nextInt(21) + random.nextInt(8);

                    for (int yy = yo - 1; yy <= yo + 1; yy++) {
                        for (int xx = xo - 1; xx <= xo + 1; xx++) {
                            if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                                if (map[xx + yy * w] == Tiles.get("Cloud").id) {
                                    map[xx + yy * w] = Tiles.get("Sky Grass").id;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Make the central island
        LoadingDisplay.setMessage("Generating mountains");
        for (int i = 0; i < heavenThreshold; i++) {
            int xs = halfWidth - 22;
            int ys = halfHeight - 22;

            for (int k = 0; k < 60; k++) {
                int x = xs + random.nextInt(28) - random.nextInt(10);
                int y = ys + random.nextInt(28) - random.nextInt(10);

                for (int j = 0; j < 90; j++) {
                    int xo = x + random.nextInt(40) - random.nextInt(20) + random.nextInt(10);
                    int yo = y + random.nextInt(40) - random.nextInt(20) + random.nextInt(10);

                    for (int yy = yo - 1; yy <= yo + 1; yy++) {
                        for (int xx = xo - 1; xx <= xo + 1; xx++) {
                            if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                                if (map[xx + yy * w] == Tiles.get("Infinite Fall").id) {
                                    map[xx + yy * w] = Tiles.get("Holy Rock").id;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < heavenThreshold; i++) {
            int xs = halfWidth - 38; // center position
            int ys = halfHeight - 40;

            for (int k = 0; k < 90; k++) {
                int x = xs + random.nextInt(28) - random.nextInt(10);
                int y = ys + random.nextInt(28) - random.nextInt(10);

                for (int j = 0; j < 190; j++) {
                    int xo = x + random.nextInt(80) - random.nextInt(30) + random.nextInt(12);
                    int yo = y + random.nextInt(80) - random.nextInt(30) + random.nextInt(12);

                    for (int yy = yo - 1; yy <= yo + 1; yy++) {
                        for (int xx = xo - 1; xx <= xo + 1; xx++) {
                            if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                                if (map[xx + yy * w] == Tiles.get("Cloud").id) {
                                    map[xx + yy * w] = Tiles.get("Ferrosite").id;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < (fullsize / 800); i++) {
            int x = (halfWidth - random.nextInt(32)) + random.nextInt(32);
            int y = (halfWidth - random.nextInt(32)) + random.nextInt(32);
            int pos = random.nextInt(4);

            for (int j = 0; j < 16; j++) {
                int xx = x + random.nextInt(6) - random.nextInt(6);
                int yy = y + random.nextInt(6) - random.nextInt(6);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    if (map[xx + yy * w] == Tiles.get("Sky grass").id) {
                        map[xx + yy * w] = Tiles.get("Sky lawn").id;
                        data[xx + yy * w] = (short) (pos + random.nextInt(4) * 16);
                    }
                }
            }
        }

        for (int i = 0; i < (fullsize / 400); i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);

            for (int j = 0; j < 42; j++) {
                int xx = x + random.nextInt(14) - random.nextInt(12) + random.nextInt(4);
                int yy = y + random.nextInt(14) - random.nextInt(12) + random.nextInt(4);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    if (map[xx + yy * w] == Tiles.get("Sky grass").id) {
                        map[xx + yy * w] = Tiles.get("Skyroot tree").id;
                    }
                }
            }
        }

        for (int i = 0; i < (fullsize / 200); i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);

            for (int j = 0; j < 36; j++) {
                int xx = x + random.nextInt(14) - random.nextInt(12) + random.nextInt(4);
                int yy = y + random.nextInt(14) - random.nextInt(12) + random.nextInt(4);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    if (map[xx + yy * w] == Tiles.get("Sky grass").id) {
                        map[xx + yy * w] = Tiles.get("Bluroot tree").id;
                    }
                }
            }
        }


        for (int i = 0; i < (fullsize / 150); i++) {
            int xx = random.nextInt(w);
            int yy = random.nextInt(h);

            if ((xx >= 0 && yy >= 0) && (xx < w && yy < h)) {
                if (map[xx + yy * w] == Tiles.get("Ferrosite").id) {
                    map[xx + yy * w] = Tiles.get("Cloud cactus").id;
                }
            }
        }

        for (int i = 0; i < (fullsize / 400); i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);

            for (int j = 0; j < 24; j++) {
                int xx = x + random.nextInt(14) - random.nextInt(12) + random.nextInt(4);
                int yy = y + random.nextInt(14) - random.nextInt(12) + random.nextInt(4);

                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    if (map[xx + yy * w] == Tiles.get("Ferrosite").id) {
                        map[xx + yy * w] = Tiles.get("Goldroot tree").id;
                    }
                }
            }
        }

        // Avoid the connection between the Sky grass and Infinite Fall tiles
        for (int j = 0; j < h; j++) {
            for (int x = 0; x < w; x++) {
                // Check if the current tile is a "sky" tile
                if (map[x + j * w] != Tiles.get("Infinite fall").id && (map[x + j * w] == Tiles.get("Holy Rock").id || map[x + j * w] == Tiles.get("Sky fern").id)) {

                    // Check the surrounding tiles within the specified thickness to see if any of them are "Infinite fall" tiles
                    boolean replace = false;

                    for (int tx = x - edgesThickness; tx <= x + edgesThickness && !replace; tx++) {
                        for (int ty = j - edgesThickness; ty <= j + edgesThickness && !replace; ty++) {
                            if (tx >= 0 && ty >= 0 && tx < w && ty < h && (tx != x || ty != j) && map[tx + ty * w] == Tiles.get("Infinite fall").id) {
                            	replace = true;
                            }
                        }
                    }

                    // If any surrounding tiles are "Infinite fall" tiles, replace the current tile with a "Sky grass" tile
                    if (replace) {
                        map[x + j * w] = Tiles.get("Sky grass").id;
                    }
                }
            }
        }

        int stairsCount = 0;
        int stairsRadius = 15;

        stairsLoop:
        for (int i = 0; i < w * h; i++) {
            int x = random.nextInt(w - 2) + 1;
            int y = random.nextInt(h - 2) + 1;

            for (int yy = y - 1; yy <= y + 1; yy++) {
                for (int xx = x - 1; xx <= x + 1; xx++) {
                    if (map[xx + yy * w] != Tiles.get("Cloud").id) {
                        continue stairsLoop;
                    }
                }
            }

            // This should prevent any stairsDown tile from being within 30 tiles of any other stairsDown tile.
            for (int yy = Math.max(0, y - stairsRadius); yy <= Math.min(h - 1, y + stairsRadius); yy++) {
                for (int xx = Math.max(0, x - stairsRadius); xx <= Math.min(w - 1, x + stairsRadius); xx++) {
                    if (map[xx + yy * w] == Tiles.get("Stairs Down").id) {
                        continue stairsLoop;
                    }
                }
            }

            map[x + y * w] = Tiles.get("Stairs Down").id;

            stairsCount++;
            if (stairsCount >= w / 64) {
                break;
            }
        }

        return new short[][] {map, data};
    }


    public static short[][] createVoidMap(int w, int h) {

        // creates a bunch of value maps, some with small size...
        LevelGen mnoise1 = new LevelGen(w, h, 16);
        LevelGen mnoise2 = new LevelGen(w, h, 16);
        LevelGen mnoise3 = new LevelGen(w, h, 16);

        // ...and some with larger size..
        LevelGen noise1 = new LevelGen(w, h, 32);
        LevelGen noise2 = new LevelGen(w, h, 32);

        //LevelGen jnoise1 = new LevelGen(w, h, 8);
        //LevelGen jnoise2 = new LevelGen(w, h, 4);
        //LevelGen jnoise3 = new LevelGen(w, h, 8);

        short[] map = new short[w * h];
        short[] data = new short[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = x + y * w;

                double val = Math.abs(noise1.values[i] - noise2.values[i]) * 3 - 2;
                double mval = Math.abs(mnoise1.values[i] - mnoise2.values[i]);
                mval = Math.abs(mval - mnoise3.values[i]) * 3 - 2;

                // this calculates a sort of distance based on the current coordinate.
                double xd = x / (w - 1.0) * 2 - 1;
                double yd = y / (h - 1.0) * 2 - 1;

                if (xd < 0) xd = -xd;
                if (yd < 0) yd = -yd;

                double dist = xd >= yd ? xd : yd;
                dist = dist * dist * dist * dist;
                dist = dist * dist * dist * dist;
                val += 1 - dist * 20;

                if (val < -0.5) {
                    map[i] = Tiles.get("Infinite Fall").id;
                } else if (val > 0.5 && mval < -1.0) {
                    map[i] = Tiles.get("Rock").id;
                } else {
                    map[i] = Tiles.get("Grass").id;
                }
            }
        }

        for (int i = 0; i < w * h / 200; i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            for (int j = 0; j < 200; j++) {
                int xx = x + random.nextInt(15) - random.nextInt(14);
                int yy = y + random.nextInt(15) - random.nextInt(14);
                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    if (map[xx + yy * w] == Tiles.get("Grass").id) {
                        map[xx + yy * w] = Tiles.get("Oak Tree").id;
                    }
                }
            }
        }

        return new short[][] {map, data};
    }

    public static void main(String[] args) {
        /*
         * This is used to see seeds without having to run the game I mean, this is a
         * world viewer that uses the same method as above using perlin noise, to
         * generate a world, and be able to see it in a JPane according to the size of
         * the world generated
         */

        LevelGen.worldSeed = 0x100; // 256L

        // Fixes to get this method to work
        // AirWizard needs this in constructor
        Game.gameDir = "";

        // Initialize the tiles
        Tiles.initialize();

        // End of fixes

        int idx = -1;

        int[] maplvls = new int[args.length];
        boolean valid = true;
        if (maplvls.length > 0) {
            for (int i = 0; i < args.length; i++) {
                try {
                    int lvlnum = Integer.parseInt(args[i]);
                    maplvls[i] = lvlnum;
                } catch (NumberFormatException exception) {
                    valid = false;
                    break;
                }
            }
        } else {
            valid = false;
        }

        if (!valid) {
            maplvls = new int[1];
            maplvls[0] = 0;
        }

        boolean hasquit = false;
        while (!hasquit) { // stop the loop and close the program
            int mapsize = 256;
            int mapScale = 2;

            if (mapsize == 512) {
                mapScale = 1;
            }

            int lvl = maplvls[idx++ % maplvls.length];
            if (lvl > 2 || lvl < -4) continue;

            long startNanoTime = System.nanoTime();

            short[][] fullmap = LevelGen.createAndValidateMap(mapsize, mapsize, 0, random.nextLong());

            long endSecondsTime = (System.nanoTime() - startNanoTime) >> 30;

            if (fullmap == null) continue;
            short[] map = fullmap[0];

            // Create the map image
            BufferedImage img = new BufferedImage(mapsize, mapsize, BufferedImage.TYPE_INT_RGB); // Creates an image
            int[] pixels = new int[mapsize * mapsize]; // The pixels in the image. (an integer array, the size is Width * height)

            for (int y = 0; y < mapsize; y++) { // Loops through the height of the map
                for (int x = 0; x < mapsize; x++) { // (inner-loop)Loops through the entire width of the map
                    int i = x + y * mapsize; // Current tile of the map.

                    /*
					  The colors used in the pixels are hexadecimal (0xRRGGBB).
                      0xff0000 would be fully red
                      0x00ff00 would be fully blue
                      0x0000ff would be fully green
                      0x000000 would be black
                      and 0xffffff would be white etc.
					*/

                    // Surface tiles
                    if (map[i] == Tiles.get("Grass").id) pixels[i] = 0x54a854;
                    else if (map[i] == Tiles.get("Lawn").id) pixels[i] = 0x60a560;
                    else if (map[i] == Tiles.get("Dirt").id) pixels[i] = 0x836c6c;
                    else if (map[i] == Tiles.get("Rose").id) pixels[i] = 0x60A560;
                    else if (map[i] == Tiles.get("Dandelion").id) pixels[i] = 0x60a560;
                    else if (map[i] == Tiles.get("Poppy").id) pixels[i] = 0x60a560;
                    else if (map[i] == Tiles.get("Daisy").id) pixels[i] = 0x60a560;
                    else if (map[i] == Tiles.get("Water").id) pixels[i] = 0x1A2C89;
                    else if (map[i] == Tiles.get("Lava").id) pixels[i] = 0xC82020;
                    else if (map[i] == Tiles.get("Magma").id) pixels[i] = 0xC83C20;
                    else if (map[i] == Tiles.get("Rock").id) pixels[i] = 0x7a7a7a;
                    else if (map[i] == Tiles.get("Up Rock").id) pixels[i] = 0x939393;
                    else if (map[i] == Tiles.get("Sand Rock").id) pixels[i] = 0x7a7a7a;

                    else if (map[i] == Tiles.get("Iron Ore").id) pixels[i] = 0x452728;
                    else if (map[i] == Tiles.get("Gold Ore").id) pixels[i] = 0x948028;
                    else if (map[i] == Tiles.get("Gem Ore").id) pixels[i] = 0x821DB6;

                    else if (map[i] == Tiles.get("Sand").id) pixels[i] = 0xe2e26f;
                    else if (map[i] == Tiles.get("Cactus").id) pixels[i] = 0xC82020;
                    else if (map[i] == Tiles.get("Snow").id) pixels[i] = 0xf0f0f0;
                    else if (map[i] == Tiles.get("Ice Spike").id) pixels[i] = 0xe6e6e6;
                    else if (map[i] == Tiles.get("Stone Bricks").id) pixels[i] = 0xa0a040;
                    else if (map[i] == Tiles.get("Oak tree").id) pixels[i] = 0x255325;
                    else if (map[i] == Tiles.get("Birch tree").id) pixels[i] = 0x0c750c;
                    else if (map[i] == Tiles.get("Fir tree").id) pixels[i] = 0x138b62;
                    else if (map[i] == Tiles.get("Pine tree").id) pixels[i] = 0x117f59;

                        // Village
                    else if (map[i] == Tiles.get("Oak Planks").id) pixels[i] = 0x914f0e;
                    else if (map[i] == Tiles.get("Oak Wall").id) pixels[i] = 0x7a430c;
                    else if (map[i] == Tiles.get("Oak Door").id) pixels[i] = 0x7a4817;

                        // Ores tiles
                    else if (map[i] == Tiles.get("Iron Ore").id) pixels[i] = 0xC4B1AA;
                    else if (map[i] == Tiles.get("Lapis").id) pixels[i] = 0x2D2D92;
                    else if (map[i] == Tiles.get("Gold Ore").id) pixels[i] = 0xCE9612;
                    else if (map[i] == Tiles.get("Gem Ore").id) pixels[i] = 0xD25BD2;

                        // Dungeon tiles
                    else if (map[i] == Tiles.get("Obsidian Wall").id) pixels[i] = 0x480887;
                    else if (map[i] == Tiles.get("Raw Obsidian").id) pixels[i] = 0x5f0aa0;
                    else if (map[i] == Tiles.get("Obsidian").id) pixels[i] = 0x660aa0;

                        // Stairs
                    else if (map[i] == Tiles.get("Stairs Down").id) pixels[i] = 0xffffffff;
                    else if (map[i] == Tiles.get("Stairs Up").id) pixels[i] = 0xffffffff;

                        // Sky tiles
                    else if (map[i] == Tiles.get("Infinite Fall").id) pixels[i] = 0x255325;
                    else if (map[i] == Tiles.get("Cloud").id) pixels[i] = 0xf7f7f7;
                    else if (map[i] == Tiles.get("Cloud Cactus").id) pixels[i] = 0xfafafa;
                    else if (map[i] == Tiles.get("Skyroot tree").id) pixels[i] = 0x477044;
                    else if (map[i] == Tiles.get("Goldroot tree").id) pixels[i] = 0xBBA14F;
                    else if (map[i] == Tiles.get("Bluroot tree").id) pixels[i] = 0x00769E;
                    else if (map[i] == Tiles.get("Ferrosite").id) pixels[i] = 0xcbc579;
                    else if (map[i] == Tiles.get("Sky grass").id) pixels[i] = 0x5aab8a;
                    else if (map[i] == Tiles.get("Sky fern").id) pixels[i] = 0x5aab8a;
                    else if (map[i] == Tiles.get("Sky lawn").id) pixels[i] = 0x9EC7C6;
                    else if (map[i] == Tiles.get("Holy rock").id) pixels[i] = 0xB9B9CD;
                    else if (map[i] == Tiles.get("Ice").id) pixels[i] = 0x686EEC;

                    else if (map[i] == Tiles.get("Mycelium").id) pixels[i] = 0x665666;
                    else if (map[i] == Tiles.get("Red Mushroom").id) pixels[i] = 0x685868;
                    else if (map[i] == Tiles.get("Brown Mushroom").id) pixels[i] = 0x645464;
                    else pixels[i] = 0x000000;
                }
            }

            // Print the seed and the elapsed time
            Logger.debug("Generated level {}, with seed {}, took {}s", lvl, worldSeed, endSecondsTime);

            img.setRGB(0, 0, mapsize, mapsize, pixels, 0, mapsize); // Sets the pixels into the image

            // Name of the buttons used for the window.
            String[] options = {
                    "Another",
                    "Quit"
            };

            int Generator = JOptionPane.showOptionDialog(
                    null, null,
                    "Map Generator",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    new ImageIcon(img.getScaledInstance(mapsize * mapScale, mapsize * mapScale, Image.SCALE_SMOOTH)),
                    options, null
            );

            if (LevelGen.worldSeed == 0x100) { // 256L
                LevelGen.worldSeed = 0xAAFF20; // 11206432L
            } else {
                LevelGen.worldSeed = 0x100; // 256L
            }

			/* Now you noticed that we made the dialog an integer. This is because when you click a button it will return a number.
               Since we passed in 'options', the window will return 0 if you press "Another" and it will return 1 when you press "Quit".
               If you press the red "x" close mark, the window will return -1
			 */
            // If the dialog returns -1 (red "x" button) or 1 ("Quit" button) then...
            if (Generator == -1 || Generator == 1) {
                hasquit = true; // Stop the loop and close the program.
            }
        }
    }
}
