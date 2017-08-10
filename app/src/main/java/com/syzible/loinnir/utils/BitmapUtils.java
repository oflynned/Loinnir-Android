package com.syzible.loinnir.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;

/**
 * Created by ed on 17/05/2017.
 */

public class BitmapUtils {
    public static final int BITMAP_SIZE = 640;
    public static final int BITMAP_SIZE_SMALL = 128;

    public static Bitmap getCroppedCircle(Bitmap bitmap) {
        Bitmap scaledBitmap = scaleBitmap(bitmap, BITMAP_SIZE);
        final Bitmap outputBitmap = Bitmap.createBitmap(scaledBitmap.getWidth(),
                scaledBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        final Path path = new Path();
        path.addCircle((float) (scaledBitmap.getWidth() / 2), (float) (scaledBitmap.getHeight() / 2),
                (float) Math.min(scaledBitmap.getWidth(), (scaledBitmap.getHeight() / 2)), Path.Direction.CCW);

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(scaledBitmap, 0, 0, null);
        return outputBitmap;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int size) {
        float ratio = Math.min(
                (float) size / bitmap.getWidth(),
                (float) size / bitmap.getHeight());
        int width = Math.round(ratio * bitmap.getWidth());
        int height = Math.round(ratio * bitmap.getHeight());

        return Bitmap.createScaledBitmap(bitmap, width, height, true);

    }

    public static Bitmap generateMetUserAvatar(Bitmap originalImage) {
        Bitmap croppedImage = BitmapUtils.getCroppedCircle(originalImage);
        return BitmapUtils.scaleBitmap(croppedImage, BitmapUtils.BITMAP_SIZE_SMALL);
    }
}
