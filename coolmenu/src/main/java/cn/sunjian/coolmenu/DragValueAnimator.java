package cn.sunjian.coolmenu;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Created by sunjian on 2017/1/21.
 */

final class DragValueAnimator extends ValueAnimator {

    //有些条件就不判断了
    private long mDragAnimaDuration = 1000L;

    public static final long MAX_DURATION = 1500L;

    public static final long MIN_DURATION = 500L;

    DragValueAnimator(View target, Object... pointFs) {
        this.mDragAnimaDuration = getDragAnimaDuration(pointFs);
        setObjectValues(pointFs);
        setEvaluator(new PointEvaluator());
        setTarget(target);
        setInterpolator(new OvershootInterpolator());
        setDuration(mDragAnimaDuration);
    }

    private long getDragAnimaDuration(Object... pointFs) {

        float x = ((PointF) pointFs[0]).x - ((PointF) pointFs[pointFs.length - 1]).x;
        float y = ((PointF) pointFs[0]).y - ((PointF) pointFs[pointFs.length - 1]).y;
        long duration = (long) (Math.hypot(x, y) * 1.6F);

        //关于这个比例自己调吧
        if (duration == 0) {
            return mDragAnimaDuration;
        } else if (duration > 0 && duration <= MIN_DURATION) {
            return MIN_DURATION;
        } else if (duration > MIN_DURATION && duration <= MAX_DURATION) {
            return duration;
        }
        return MAX_DURATION;
    }

    DragValueAnimator addDragListener(AnimatorListener listener) {
        super.addListener(listener);
        return this;
    }

    DragValueAnimator addDragUpdateListener(AnimatorUpdateListener listener) {
        super.addUpdateListener(listener);
        return this;
    }

    static class DragAnimatorListener implements AnimatorListener {

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }
}
