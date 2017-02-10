package cn.sunjian.coolmenu;

import android.animation.TypeEvaluator;
import android.graphics.PointF;

/**
 * Created by sunjian on 2017/1/20.
 */

final class PointEvaluator implements TypeEvaluator<PointF> {

    @Override
    public final PointF evaluate(float fraction, PointF startValue, PointF endValue) {
        float x = startValue.x + fraction * (endValue.x - startValue.x);
        float y = startValue.y + fraction * (endValue.y - startValue.y);
        return new PointF(x, y);
    }
}
