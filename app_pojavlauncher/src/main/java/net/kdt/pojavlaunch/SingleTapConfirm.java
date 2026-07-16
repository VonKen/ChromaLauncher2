package net.kdt.pojavlaunch;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }

    public static boolean handle(View v, MotionEvent event) {
        GestureDetector gd = new GestureDetector(v.getContext(), new SingleTapConfirm());
        return gd.onTouchEvent(event);
    }
}
