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

        if (files == null)
            return false;

        if (files.length == 0)
            return false;

        // does it exist?
        boolean exists = false;
        String fileName = "";
        for (File file : files) {
            if (id.equals(file.getName().split("_")[0])) {
                exists = true;
                fileName = file.getName();
            }
        }

        if (!exists) return false;

        //for(File file : files)
        //    System.out.println(file.getName());

        // it exists, but is it stale?
        if (isStale(fileName)) {
            for (File file : files) {
                System.out.println(file.getName());
                if (id.equals(file.getName().split("_")[0]))
                    file.delete();
            }

            return false;
        }

        return true;
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
        File[] files = new File(getDirectoryPath(context)).listFiles();
        String fileName = name;

        for (File file : files) {
            if (name.equals(file.getName().split("_")[0]))
                fileName = file.getName();
        }

        String pathToFile = getDirectoryPath(context) + "/" + fileName;
        Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
        if (bitmap == null)
            return null;

        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        return bitmap;
    }

    private static boolean isStale(String fileName) {
        // first clear the cache of images that follow the old schema of just <id> for name
        // now we should change to <id>_<time>.png

        String[] fileData = fileName.split("_");
        if (fileData.length > 1) {
            // valid file under new schema
            long oneWeekInMillis = 1000 * 60 * 60 * 24 * 7;
            long cachingTime = Long.valueOf(fileData[1].replace(".png", ""));
            return System.currentTimeMillis() - cachingTime > oneWeekInMillis;
        } else {
            // invalid -- purge and recache
            return true;
        }
    }

    private static void saveToFile(Context context, String name, Bitmap file) {
        File pictureFile = getOutputMediaFile(context, name + "_" + System.currentTimeMillis());
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
                + "/Android/data/com.syzible.loinnir/Images";
    }

    private static String getFileWithExtension(String name) {
        return name + ".png";
    }
}
