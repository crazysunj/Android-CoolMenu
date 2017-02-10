package cn.sunjian.coolmenu;

import java.util.TimerTask;

/**
 * 关于这个，你完全可以用值动画，但这个更值得学习
 * Created by sunjian on 2017/1/18.
 */
final class CoolMenuVelocityTimerTask extends TimerTask {

    private final CoolMenu mCoolMenu;
    private float mPerRotationAngle;
    private float mDecValue;
    //万恶的数据
    private final float INIT_DEC_VALUE = 1.21F;
    private final float DEC_SCALE_VALUE = 0.985F;

    CoolMenuVelocityTimerTask(CoolMenu coolMenu, float velocity) {

        this.mCoolMenu = coolMenu;
        this.mPerRotationAngle = (float) (velocity * 180F / Math.PI);
        this.mDecValue = INIT_DEC_VALUE;

    }

    @Override
    public final void run() {
        // 如果小于0.2度,则停止
        if (Math.abs(mPerRotationAngle) >= 0.0F && Math.abs(mPerRotationAngle) < CoolMenu.CLICK_ANGLE) {
            mCoolMenu.cancelFuture();
            if (mCoolMenu.mItemFlingListener != null) {
                mCoolMenu.mItemFlingListener.onFlingEnd();
            }
            mCoolMenu.state = CoolMenu.IDLE;
            return;
        }

        mCoolMenu.mAngle = (mCoolMenu.mAngle + mPerRotationAngle) % 360;
        // 每秒旋转角度逐渐减小
        mPerRotationAngle /= mDecValue;
        float dec = mDecValue;
        mDecValue = (mDecValue - 1) * DEC_SCALE_VALUE + 1;

        if (dec == mDecValue) {
            //数据没那么完美
            mCoolMenu.cancelFuture();
            if (mCoolMenu.mItemFlingListener != null) {
                mCoolMenu.mItemFlingListener.onFlingEnd();
            }
            mCoolMenu.state = CoolMenu.IDLE;
            return;
        }

        mCoolMenu.mHandle.sendEmptyMessageDelayed(CoolMenu.WHAT_HANDLE_FLING, 30);
    }
}
