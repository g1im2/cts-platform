/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.graphics.drawable.cts;

import android.content.res.Resources.Theme;
import android.graphics.Outline;
import android.graphics.cts.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.test.InstrumentationTestCase;
import android.util.AttributeSet;
import android.util.Xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class NinePatchDrawableTest extends InstrumentationTestCase {
    // A small value is actually making sure that the values are matching
    // exactly with the golden image.
    // We can increase the threshold if the Skia is drawing with some variance
    // on different devices. So far, the tests show they are matching correctly.
    private static final float PIXEL_ERROR_THRESHOLD = 0.03f;
    private static final float PIXEL_ERROR_COUNT_THRESHOLD = 0.005f;

    private static final int MIN_CHUNK_SIZE = 32;

    // Set true to generate golden images, false for normal tests.
    private static final boolean DBG_DUMP_PNG = false;

    private NinePatchDrawable mNinePatchDrawable;

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = getInstrumentation().getTargetContext().getResources();
        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_0);
    }

    @SuppressWarnings("deprecation")
    public void testConstructors() {
        byte[] chunk = new byte[MIN_CHUNK_SIZE];
        chunk[MIN_CHUNK_SIZE - 1] = 1;

        Rect r = new Rect();

        Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_0);
        String name = mResources.getResourceName(R.drawable.ninepatch_0);

        new NinePatchDrawable(bmp, chunk, r, name);

        new NinePatchDrawable(new NinePatch(bmp, chunk, name));

        chunk = new byte[MIN_CHUNK_SIZE - 1];
        chunk[MIN_CHUNK_SIZE - 2] = 1;
        try {
            new NinePatchDrawable(bmp, chunk, r, name);
            fail("The constructor should check whether the chunk is illegal.");
        } catch (RuntimeException e) {
            // This exception is thrown by native method.
        }
    }

    public void testDraw() {
        Bitmap bmp = Bitmap.createBitmap(9, 9, Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        int ocean = Color.rgb(0, 0xFF, 0x80);

        mNinePatchDrawable.setBounds(0, 0, 9, 9);
        mNinePatchDrawable.draw(c);
        assertColorFillRect(bmp, 0, 0, 4, 4, Color.RED);
        assertColorFillRect(bmp, 5, 0, 4, 4, Color.BLUE);
        assertColorFillRect(bmp, 0, 5, 4, 4, ocean);
        assertColorFillRect(bmp, 5, 5, 4, 4, Color.YELLOW);
        assertColorFillRect(bmp, 4, 0, 1, 9, Color.WHITE);
        assertColorFillRect(bmp, 0, 4, 9, 1, Color.WHITE);

        bmp.eraseColor(0xff000000);

        mNinePatchDrawable.setBounds(0, 0, 3, 3);
        mNinePatchDrawable.draw(c);
        assertColorFillRect(bmp, 0, 0, 1, 1, Color.RED);
        assertColorFillRect(bmp, 2, 0, 1, 1, Color.BLUE);
        assertColorFillRect(bmp, 0, 2, 1, 1, ocean);
        assertColorFillRect(bmp, 2, 2, 1, 1, Color.YELLOW);
        assertColorFillRect(bmp, 1, 0, 1, 3, Color.WHITE);
        assertColorFillRect(bmp, 0, 1, 3, 1, Color.WHITE);

        try {
            mNinePatchDrawable.draw(null);
            fail("The method should check whether the canvas is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetChangingConfigurations() {
        ConstantState constantState = mNinePatchDrawable.getConstantState();

        // default
        assertEquals(0, constantState.getChangingConfigurations());
        assertEquals(0, mNinePatchDrawable.getChangingConfigurations());

        // change the drawable's configuration does not affect the state's configuration
        mNinePatchDrawable.setChangingConfigurations(0xff);
        assertEquals(0xff, mNinePatchDrawable.getChangingConfigurations());
        assertEquals(0, constantState.getChangingConfigurations());

        // the state's configuration get refreshed
        constantState = mNinePatchDrawable.getConstantState();
        assertEquals(0xff,  constantState.getChangingConfigurations());

        // set a new configuration to drawable
        mNinePatchDrawable.setChangingConfigurations(0xff00);
        assertEquals(0xff,  constantState.getChangingConfigurations());
        assertEquals(0xffff,  mNinePatchDrawable.getChangingConfigurations());
    }

    public void testGetPadding() {
        Rect r = new Rect();
        NinePatchDrawable npd = (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatch_0);
        assertTrue(npd.getPadding(r));
        // exact padding unknown due to possible density scaling
        assertEquals(0, r.left);
        assertEquals(0, r.top);
        assertTrue(r.right > 0);
        assertTrue(r.bottom > 0);

        npd = (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatch_1);
        assertTrue(npd.getPadding(r));
        assertTrue(r.left > 0);
        assertTrue(r.top > 0);
        assertTrue(r.right > 0);
        assertTrue(r.bottom > 0);
    }

    public void testSetAlpha() {
        assertEquals(0xff, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(0);
        assertEquals(0, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(-1);
        assertEquals(0xff, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(0xfffe);
        assertEquals(0xfe, mNinePatchDrawable.getPaint().getAlpha());
    }

    public void testSetColorFilter() {
        assertNull(mNinePatchDrawable.getPaint().getColorFilter());

        MockColorFilter cf = new MockColorFilter();
        mNinePatchDrawable.setColorFilter(cf);
        assertSame(cf, mNinePatchDrawable.getPaint().getColorFilter());

        mNinePatchDrawable.setColorFilter(null);
        assertNull(mNinePatchDrawable.getPaint().getColorFilter());
    }

    public void testSetTint() {
        mNinePatchDrawable.setTint(Color.BLACK);
        mNinePatchDrawable.setTintMode(Mode.SRC_OVER);
        assertEquals("Nine-patch is tinted", Color.BLACK,
                DrawableTestingUtils.getPixel(mNinePatchDrawable, 0, 0));

        mNinePatchDrawable.setTintList(null);
        mNinePatchDrawable.setTintMode(null);
    }

    public void testSetDither() {
        mNinePatchDrawable.setDither(false);
        assertFalse(mNinePatchDrawable.getPaint().isDither());

        mNinePatchDrawable.setDither(true);
        assertTrue(mNinePatchDrawable.getPaint().isDither());
    }

    public void testSetFilterBitmap() {
        mNinePatchDrawable.setFilterBitmap(false);
        assertFalse(mNinePatchDrawable.getPaint().isFilterBitmap());

        mNinePatchDrawable.setFilterBitmap(true);
        assertTrue(mNinePatchDrawable.getPaint().isFilterBitmap());
    }

    public void testIsFilterBitmap() {
        mNinePatchDrawable.setFilterBitmap(false);
        assertFalse(mNinePatchDrawable.isFilterBitmap());
        assertEquals(mNinePatchDrawable.isFilterBitmap(),
                mNinePatchDrawable.getPaint().isFilterBitmap());


        mNinePatchDrawable.setFilterBitmap(true);
        assertTrue(mNinePatchDrawable.isFilterBitmap());
        assertEquals(mNinePatchDrawable.isFilterBitmap(),
                mNinePatchDrawable.getPaint().isFilterBitmap());
    }

    public void testGetPaint() {
        Paint paint = mNinePatchDrawable.getPaint();
        assertNotNull(paint);

        assertSame(paint, mNinePatchDrawable.getPaint());
    }

    public void testGetIntrinsicWidth() {
        Bitmap bmp = getBitmapUnscaled(R.drawable.ninepatch_0);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getIntrinsicWidth());
        assertEquals(5, mNinePatchDrawable.getIntrinsicWidth());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = getBitmapUnscaled(R.drawable.ninepatch_1);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getIntrinsicWidth());
        assertEquals(9, mNinePatchDrawable.getIntrinsicWidth());
    }

    public void testGetMinimumWidth() {
        Bitmap bmp = getBitmapUnscaled(R.drawable.ninepatch_0);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getMinimumWidth());
        assertEquals(5, mNinePatchDrawable.getMinimumWidth());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = getBitmapUnscaled(R.drawable.ninepatch_1);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getMinimumWidth());
        assertEquals(9, mNinePatchDrawable.getMinimumWidth());
    }

    public void testGetIntrinsicHeight() {
        Bitmap bmp = getBitmapUnscaled(R.drawable.ninepatch_0);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getIntrinsicHeight());
        assertEquals(5, mNinePatchDrawable.getIntrinsicHeight());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = getBitmapUnscaled(R.drawable.ninepatch_1);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getIntrinsicHeight());
        assertEquals(9, mNinePatchDrawable.getIntrinsicHeight());
    }

    public void testGetMinimumHeight() {
        Bitmap bmp = getBitmapUnscaled(R.drawable.ninepatch_0);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getMinimumHeight());
        assertEquals(5, mNinePatchDrawable.getMinimumHeight());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = getBitmapUnscaled(R.drawable.ninepatch_1);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getMinimumHeight());
        assertEquals(9, mNinePatchDrawable.getMinimumHeight());
    }

    // Known failure: Bug 2834281 - Bitmap#hasAlpha seems to return true for
    // images without alpha
    public void suppress_testGetOpacity() {
        assertEquals(PixelFormat.OPAQUE, mNinePatchDrawable.getOpacity());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        assertEquals(PixelFormat.TRANSLUCENT, mNinePatchDrawable.getOpacity());
    }

    public void testGetTransparentRegion() {
        // opaque image
        Region r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        mNinePatchDrawable.setBounds(0, 0, 7, 7);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        // translucent image
        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        mNinePatchDrawable.setBounds(1, 1, 7, 7);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNotNull(r);
        assertEquals(new Rect(1, 1, 7, 7), r.getBounds());
    }

    public void testGetConstantState() {
        assertNotNull(mNinePatchDrawable.getConstantState());

        ConstantState constantState = mNinePatchDrawable.getConstantState();
        // change the drawable's configuration does not affect the state's configuration
        mNinePatchDrawable.setChangingConfigurations(0xff);
        assertEquals(0, constantState.getChangingConfigurations());
        // the state's configuration refreshed when getConstantState is called.
        constantState = mNinePatchDrawable.getConstantState();
        assertEquals(0xff, constantState.getChangingConfigurations());
    }

    public void testInflate() throws XmlPullParserException, IOException {
        int sourceWidth = 80;
        int sourceHeight = 120;
        int[] colors = new int[sourceWidth * sourceHeight];
        Bitmap bitmap = Bitmap.createBitmap(
                colors, sourceWidth, sourceHeight, Bitmap.Config.RGB_565);
        NinePatchDrawable ninePatchDrawable = new NinePatchDrawable(
                mResources, bitmap, new byte[1000], null, "TESTNAME");

        int sourceDensity = bitmap.getDensity();
        int targetDensity = mResources.getDisplayMetrics().densityDpi;
        int targetWidth = DrawableTestUtils.scaleBitmapFromDensity(
                sourceWidth, sourceDensity, targetDensity);
        int targetHeight = DrawableTestUtils.scaleBitmapFromDensity(
                sourceHeight, sourceDensity, targetDensity);
        assertEquals(targetWidth, ninePatchDrawable.getIntrinsicWidth());
        assertEquals(targetHeight, ninePatchDrawable.getIntrinsicHeight());

        XmlResourceParser parser = mResources.getXml(R.drawable.ninepatchdrawable);
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && type != XmlPullParser.START_TAG) {
        }
        AttributeSet attrs = Xml.asAttributeSet(parser);
        ninePatchDrawable.inflate(mResources, parser, attrs);

        assertTrue(ninePatchDrawable.getPaint().isDither());
        assertTrue(sourceHeight != ninePatchDrawable.getIntrinsicHeight());
        assertTrue(sourceWidth != ninePatchDrawable.getIntrinsicWidth());
    }

    public void testMutate() {
        NinePatchDrawable d1 =
            (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatchdrawable);
        NinePatchDrawable d2 =
            (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatchdrawable);
        NinePatchDrawable d3 =
            (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatchdrawable);

        // the state is not shared before mutate.
        d1.setDither(false);
        assertFalse(d1.getPaint().isDither());
        assertTrue(d2.getPaint().isDither());
        assertTrue(d3.getPaint().isDither());

        // cannot test if mutate worked, since state was not shared before
        d1.mutate();
    }

    private static final int[] DENSITY_VALUES = new int[] {
            160, 80, 320
    };

    private static final int[] DENSITY_IMAGES = new int[] {
            R.drawable.nine_patch_density
    };

    private static final int[][] DENSITY_GOLDEN_IMAGES = new int[][] {
            {
                    R.drawable.nine_patch_density_golden_160,
                    R.drawable.nine_patch_density_golden_80,
                    R.drawable.nine_patch_density_golden_320,
            }
    };

    public void testPreloadDensity() throws XmlPullParserException, IOException {
        final Resources res = mResources;
        final int densityDpi = res.getConfiguration().densityDpi;
        try {
            testPreloadDensityInner(res, DENSITY_IMAGES[0], DENSITY_VALUES, DENSITY_GOLDEN_IMAGES[0]);
        } finally {
            DrawableTestUtils.setResourcesDensity(res, densityDpi);
        }
    }

    private void testPreloadDensityInner(Resources res, int sourceResId, int[] densities,
            int[] goldenResIds) throws XmlPullParserException, IOException {
        final Rect tempPadding = new Rect();
        final Outline tempOutline = new Outline();

        // Capture initial state at preload density.
        final int preloadDensityDpi = densities[0];
        DrawableTestUtils.setResourcesDensity(res, preloadDensityDpi);

        final XmlResourceParser parser = DrawableTestUtils.getResourceParser(res, sourceResId);
        final NinePatchDrawable preloadedDrawable = new NinePatchDrawable(null);
        preloadedDrawable.inflate(res, parser, Xml.asAttributeSet(parser));

        final ConstantState preloadedConstantState = preloadedDrawable.getConstantState();
        final int origWidth = preloadedDrawable.getIntrinsicWidth();
        final int origHeight = preloadedDrawable.getIntrinsicHeight();
        final Rect origPadding = new Rect();
        preloadedDrawable.getPadding(origPadding);
        final Outline origOutline = new Outline();
        preloadedDrawable.getOutline(origOutline);
        final Rect origOutlineRect = origOutline.getRect();
        final float origOutlineRadius = origOutline.getRadius();

        compareOrSave(preloadedDrawable, preloadDensityDpi, sourceResId, goldenResIds[0]);

        for (int i = 1; i < densities.length; i++) {
            final int scaledDensityDpi = densities[i];
            final float scale = scaledDensityDpi / (float) preloadDensityDpi;
            DrawableTestUtils.setResourcesDensity(res, scaledDensityDpi);

            final NinePatchDrawable scaledDrawable =
                    (NinePatchDrawable) preloadedConstantState.newDrawable(res);

            // Sizes are rounded.
            assertEquals(Math.round(origWidth * scale), scaledDrawable.getIntrinsicWidth());
            assertEquals(Math.round(origHeight * scale), scaledDrawable.getIntrinsicHeight());

            // Outlines are truncated, but radius is precise.
            // TODO: Uncomment this once outlines are working.
            /*
            scaledDrawable.getOutline(tempOutline);
            final Rect tempOutlineRect = tempOutline.getRect();
            assertNotNull(tempOutlineRect);
            assertEquals((int) (origOutlineRect.left * scale), tempOutlineRect.left);
            assertEquals((int) (origOutlineRect.top * scale), tempOutlineRect.top);
            assertEquals((int) (origOutlineRect.right * scale), tempOutlineRect.right);
            assertEquals((int) (origOutlineRect.bottom * scale), tempOutlineRect.bottom);
            assertEquals(origOutlineRadius * scale, tempOutline.getRadius());
            */

            // Padding is truncated.
            assertTrue(scaledDrawable.getPadding(tempPadding));
            assertEquals((int) (origPadding.left * scale), tempPadding.left);
            assertEquals((int) (origPadding.top * scale), tempPadding.top);
            assertEquals((int) (origPadding.right * scale), tempPadding.right);
            assertEquals((int) (origPadding.bottom * scale), tempPadding.bottom);

            compareOrSave(scaledDrawable, scaledDensityDpi, sourceResId, goldenResIds[i]);

            // Ensure theme density is applied correctly. Unlike most
            // drawables, we don't have any loss of accuracy because density
            // changes are re-computed from the source every time.
            DrawableTestUtils.setResourcesDensity(res, preloadDensityDpi);

            final Theme t = res.newTheme();
            scaledDrawable.applyTheme(t);
            assertEquals(origWidth, scaledDrawable.getIntrinsicWidth());
            assertEquals(origHeight, scaledDrawable.getIntrinsicHeight());
            assertTrue(scaledDrawable.getPadding(tempPadding));
            assertEquals(origPadding, tempPadding);
        }
    }

    private void assertColorFillRect(Bitmap bmp, int x, int y, int w, int h, int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                assertEquals(color, bmp.getPixel(i, j));
            }
        }
    }

    private NinePatchDrawable getNinePatchDrawable(int resId) {
        // jump through hoops to avoid scaling the tiny ninepatch, which would skew the results
        // depending on device density
        Bitmap bitmap = getBitmapUnscaled(resId);
        NinePatch np = new NinePatch(bitmap, bitmap.getNinePatchChunk(), null);
        return new NinePatchDrawable(mResources, np);
    }

    private Bitmap getBitmapUnscaled(int resId) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inDensity = opts.inTargetDensity = mResources.getDisplayMetrics().densityDpi;
        Bitmap bitmap = BitmapFactory.decodeResource(mResources, resId, opts);
        return bitmap;
    }

    private void compareOrSave(Drawable dr, int densityDpi, int sourceResId, int goldenResId) {
        final int width = dr.getIntrinsicWidth();
        final int height = dr.getIntrinsicHeight();
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setDensity(0);

        final Canvas canvas = new Canvas(bitmap);
        dr.setBounds(0, 0, width, height);
        dr.draw(canvas);

        if (DBG_DUMP_PNG) {
            saveGoldenImage(bitmap, sourceResId, densityDpi);
        } else {
            final Bitmap golden = BitmapFactory.decodeResource(mResources, goldenResId);
            DrawableTestUtils.compareImages(densityDpi + " dpi", golden, bitmap,
                    PIXEL_ERROR_THRESHOLD, PIXEL_ERROR_COUNT_THRESHOLD);
        }
    }

    private void saveGoldenImage(Bitmap bitmap, int sourceResId, int densityDpi) {
        // Save the image to the disk.
        FileOutputStream out = null;

        try {
            final String outputFolder = "/sdcard/temp/";
            final File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }

            final String sourceFilename = new File(mResources.getString(sourceResId)).getName();
            final String sourceTitle = sourceFilename.substring(0, sourceFilename.lastIndexOf("."));
            final String outputTitle = sourceTitle + "_golden_" + densityDpi;
            final String outputFilename = outputFolder + outputTitle + ".png";
            final File outputFile = new File(outputFilename);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            out = new FileOutputStream(outputFile, false);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MockColorFilter extends ColorFilter {
    }
}
