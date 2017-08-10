package com.syzible.loinnir.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ed on 10/06/2017.
 */

public class CachingUtil {

    public static boolean doesImageExist(Context context, String id) {
        checkDirectoryExists(context);
        File[] files = new File(getDirectoryPath(context)).listFiles();

        if(files == null)
            return false;

        if (files.length == 0)
            return false;

        for (File file : files)
            if (file.getName().equals(getFileWithExtension(id)))
                return true;

        return false;
    }

    public static void cacheImage(Context context, String id, Bitmap image) {
        saveToFile(context, id, image);
    }

    public static void clearCache(Context context) {
        File[] files = new File(getDirectoryPath(context)).listFiles();
        for (File file : files)
            file.delete();
    }

    public static Bitmap getCachedImage(Context context, String name) {
        String pathToFile = getDirectoryPath(context) + "/" + getFileWithExtension(name);
        Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);

        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        return bitmap;
    }

    private static void saveToFile(Context context, String name, Bitmap file) {
        File pictureFile = getOutputMediaFile(context, name);
        assert pictureFile != null;

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            file.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File getOutputMediaFile(Context context, String name) {
        File mediaStorageDir = new File(getDirectoryPath(context));

        if (!mediaStorageDir.exists())
            if (!mediaStorageDir.mkdirs())
                return null;

        return new File(mediaStorageDir.getPath() + File.separator + getFileWithExtension(name));
    }

    private static void checkDirectoryExists(Context context) {
        String path = getDirectoryPath(context);
        File directory = new File(path);

        if (!directory.exists()) {
            File dir = new File(path);
            dir.mkdirs();
        }
    }

    private static String getDirectoryPath(Context context) {
        return Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + context.getPackageName()
                + "/Images";
    }

    private static String getFileWithExtension(String name) {
        return name + ".png";
    }
}
