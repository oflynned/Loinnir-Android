package com.syzible.loinnir.utils;

import java.nio.charset.Charset;

/**
 * Created by ed on 19/05/2017.
 */

public class Constants {
    public static final boolean DEV_MODE = false;

    public static String getCountyFileName(String county) {
        return county.toLowerCase().replace(" ", "_")
                .replace("á", "a").replace("é", "e")
                .replace("í", "i").replace("ó", "o").replace("ú", "u");
    }

    public static final int ONE_SECOND = 1000;
    public static final int ONE_MINUTE = ONE_SECOND * 60;
    public static final int FIVE_MINUTES = ONE_MINUTE * 5;
    public static final int FIFTEEN_MINUTES = FIVE_MINUTES * 3;
}
