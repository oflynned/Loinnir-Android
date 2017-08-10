package com.syzible.loinnir.utils;

/**
 * Created by ed on 17/05/2017.
 */

public class EmojiUtils {
    public static final int HAPPY = 0x1F603;
    public static final int COOL = 0x1F60E;
    public static final int HEART_EYES = 0x1F60D;
    public static final int ANNOYED = 0x1F62B;
    public static final int TONGUE = 0x1F61C;
    public static final int SAD = 0x1F62D;

    public static String getEmoji(int unicode){
        return new String(Character.toChars(unicode));
    }
}
