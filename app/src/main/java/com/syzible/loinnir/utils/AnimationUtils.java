package com.syzible.loinnir.utils;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import com.syzible.loinnir.R;

/**
 * Created by ed on 01/09/2017.
 */

public class AnimationUtils {
    public static void popView(View view) {
        Animation expandIn = android.view.animation.AnimationUtils.loadAnimation(view.getContext(), R.anim.pop);
        view.startAnimation(expandIn);
    }

    public static void rotateView(View view, boolean isInfinite) {
        rotateView(view, isInfinite ? RotateAnimation.INFINITE : 0);
    }

    public static void rotateView(View view, int count) {
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(350);
        rotateAnimation.setRepeatCount(count);
        rotateAnimation.setInterpolator(new LinearInterpolator());

        view.startAnimation(rotateAnimation);
    }
}
