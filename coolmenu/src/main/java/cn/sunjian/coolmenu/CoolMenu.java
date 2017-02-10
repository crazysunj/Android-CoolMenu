package cn.sunjian.coolmenu;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Observable;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 炫酷大风车
 * Created by sunjian on 2017/1/17.
 */

public class CoolMenu extends ViewGroup {

    // 惯性滑动，拖拽, 旋转事件,这几个变量有点尴尬，也不想删除了
    public static final int IDLE = 0;
    public static final int ROTATE = 1;
    public static final int FLING = 2;
    public static final int DAGGLE = 3;

    public static final int WHAT_HANDLE_FLING = 0;

//    private static final int WHAT_HANDLE_ADD_DRAG_VIEW = 1;

    private static final int ANIM_SHOW_DURATION = 1600;

    private static final int ANIM_DISMISS_DURATION = 2400;


    private final float DAGGLE_RANGE = dp2px(10);

    // 修改这个值可以改变滑行速度
    private static final int VELOCITYFLING = 5;
    static final float CLICK_ANGLE = 0.2F;

    int state = IDLE;

    @IntDef({IDLE, ROTATE, FLING, DAGGLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {

    }

    //角度
    float mAngle = 0;

    //边长
    int mEdge;

    //屏幕距离
    private int screenDistance;

    // 记录上一次的x，y坐标
    private float mLastX;
    private float mLastY;

    // 记录第一次的角度
    private float mFirstAngle;

    private float mPerAngle;

    //开始时间
    private long mStartTime;

    //拖拽的view
    private ImageView mDragView;
    //拖拽的原view
    private View mStartDragView;
    //拖拽的图片
    private Bitmap mDragBitmap;

    private WindowManager.LayoutParams mWindowLayoutParams;

    private WindowManager mWindowManager;
    //震动
    Vibrator mVibrator;

    private int mStatusBarHeight = -1;

    private boolean mIsEnterDaggle = false;

    //前一个位置
    private int mPreDragMovePosition = -1;

    //缓存位置的x,y
    private List<CoolMenuBean> mCoolMenuBeanList = new ArrayList<>();

    //当前位置
    private int mCurrentDragMovePosition = -1;

    //拖拽移动时记住移动的position
    private int[] mMovePosArr;

    //线程统一管理
    ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mFuture;

    Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_HANDLE_FLING:
                    layoutChild();
                    break;
//                case WHAT_HANDLE_ADD_DRAG_VIEW:
//                    mWindowManager.addView(mDragView, mWindowLayoutParams);
//                    break;
            }
        }
    };

    private Adapter mAdapter;

    private final CoolMenuDataObserver mObserver = new CoolMenuDataObserver();

    private int mCurrentChildCount = -1;

    OnItemClickListener mItemClickListener;

    OnItemDragListener mItemDragListener;

    OnItemFlingListener mItemFlingListener;

    private boolean mIsDrag = true;

    private boolean mIsFling = true;

    public CoolMenu(Context context) {
        this(context, null);
    }

    public CoolMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CoolMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initData(context);

        if (attrs != null) {

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.coolmenu, 0, 0);

            @LayoutRes int centerResId = a.getResourceId(R.styleable.coolmenu_centerLayout, R.layout.center);

            mIsDrag = a.getBoolean(R.styleable.coolmenu_isDrag, mIsDrag);

            mIsFling = a.getBoolean(R.styleable.coolmenu_isFling, mIsFling);

            View.inflate(context, centerResId, this);
        }

        setLayoutTransition(new LayoutTransition());

    }

    /**
     * 关于孩子的个数没有限制，但是多了肯定会卡，而且不美观
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int childCount = getChildCount();

        if (childCount <= 0) {
            throw new RuntimeException("child count must be more than 1");
        }

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int edge;

        //存储孩子宽高数组
        int[] childsWidth = new int[childCount];
        int[] childsHeight = new int[childCount];

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {

            edge = width >= height ? height : width;

        } else {

            for (int i = 0; i < childCount; i++) {

                View child = getChildAt(i);

                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                MarginLayoutParams lp = (MarginLayoutParams) child
                        .getLayoutParams();

                int childWidth = child.getMeasuredWidth() + lp.leftMargin
                        + lp.rightMargin;
                int childHeight = child.getMeasuredHeight() + lp.topMargin
                        + lp.bottomMargin;

                childsWidth[i] = childWidth;
                childsHeight[i] = childHeight;

            }

            Arrays.sort(childsWidth);
            Arrays.sort(childsHeight);

            if (childCount == 1) {
                edge = childsWidth[0] > childsHeight[0] ? childsWidth[0] : childsHeight[0];
                edge = edge > screenDistance ? screenDistance : edge;
            } else if (childCount == 2) {
                int childW = childsWidth[0] + childsWidth[1];
                int childH = childsHeight[0] + childsHeight[1];
                edge = childW > childH ? childW : childH;
                edge = edge > screenDistance ? screenDistance : edge;
            } else {
                int childW = childsWidth[childCount - 1] + childsWidth[childCount - 2] + childsWidth[childCount - 3];
                int childH = childsHeight[childCount - 1] + childsHeight[childCount - 2] + childsHeight[childCount - 3];
                edge = childW > childH ? childW : childH;
                edge = edge > screenDistance ? screenDistance : edge;
            }
        }

        this.mEdge = edge;

        int measured = MeasureSpec.makeMeasureSpec(edge, MeasureSpec.EXACTLY);

        setMeasuredDimension(measured, measured);

        for (int i = 0; i < childCount; i++) {

            View child = getChildAt(i);
            int m = MeasureSpec.makeMeasureSpec((int) (edge * (i == 0 ? 0.8f : 0.6f) * 1.0f / 3), MeasureSpec.EXACTLY);
            child.measure(m, m);
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        layoutChild();
    }

    float mDragAngle;

    //没有用GestureDetector，纯手工，更方便你的学习
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                mStartTime = System.currentTimeMillis();

                cancelFuture();

                mPerAngle = 0;

                mLastX = x;
                mLastY = y;

                mFirstAngle = getAngle(x, y);

                break;
            case MotionEvent.ACTION_MOVE:

                float start = getAngle(mLastX, mLastY);

                float end = getAngle(x, y);

                mAngle = (mAngle + end - start) % 360;

                mPerAngle = end - start;

                mLastX = x;
                mLastY = y;

                if (!mIsEnterDaggle) {

                    long differMoveTime = System.currentTimeMillis() - mStartTime;

                    if (differMoveTime > ViewConfiguration.getLongPressTimeout()) {

                        if (Math.abs(end - mFirstAngle) < CLICK_ANGLE) {
                            //处理拖拽事件
                            float rawX = event.getRawX() - mEdge * 0.1F;
                            float rawY = event.getRawY() - mEdge * 0.1F - mStatusBarHeight;
                            //判断是否能拖拽
                            if (isCanDrag(rawX, rawY) && mIsDrag) {
                                mDragAngle = mAngle;
                                startDragEvent(rawX, rawY);
                            }
                        } else {

                            state = ROTATE;
                        }
                        mIsEnterDaggle = true;
                    } else {
                        state = ROTATE;
                    }
                }

                if (state == ROTATE) {
                    // 重新布局
                    layoutChild();
                }

                break;
            case MotionEvent.ACTION_UP:

                mIsEnterDaggle = false;

                long differUpTime = System.currentTimeMillis() - mStartTime;

                if (Math.abs(mPerAngle) < CLICK_ANGLE && differUpTime < ViewConfiguration.getTapTimeout()) {
                    //处理点击事件

                    int childCount = getChildCount();

                    for (int i = 0; i < childCount; i++) {

                        if (i != 0) {

                            View child = getChildAt(i);
                            int[] location = new int[2];
                            child.getLocationOnScreen(location);

                            float rawX = event.getRawX() - mEdge * 0.1F;
                            float rawY = event.getRawY() - mEdge * 0.1F - mStatusBarHeight;

                            if (isPointInView(rawX, rawY, location[0], location[1])) {

                                if (mItemClickListener != null) {
                                    mItemClickListener.onItemClick(child, i - 1);
                                }

                                state = IDLE;
                                break;
                            }
                        }
                    }

                } else if (Math.abs(mPerAngle) >= CLICK_ANGLE && differUpTime < ViewConfiguration.getTapTimeout() && mIsFling) {
                    //处理惯性滑动事件
                    // 计算，每秒移动的角度
                    float anglePerSecond = mPerAngle * 1000
                            / differUpTime;

                    startScroll(anglePerSecond);

                }

                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();

        int childCount = getChildCount();
        if (childCount > 1) {

            mMovePosArr = new int[childCount - 1];
        }
    }

    //不多BB,通通拦截
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (state == DAGGLE && mDragView != null) {
            //处理拖拽
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:

                    float moveX = event.getRawX();
                    float moveY = event.getRawY();
                    //拖动item
                    onDragView(moveX - mEdge * 0.1F, moveY - mEdge * 0.1F - mStatusBarHeight);
                    break;
                case MotionEvent.ACTION_UP:

                    float upX = event.getRawX();
                    float upY = event.getRawY();
                    onStopDrag(upX, upY);
                    break;
            }
        }
        return true;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {

        int wrap_content = getResources().getDimensionPixelOffset(R.dimen.cool_menu_wrap_content);
        return new MarginLayoutParams(wrap_content, wrap_content);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    //初始化数据
    private void initData(Context context) {

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        screenDistance = displayMetrics.widthPixels > displayMetrics.heightPixels ? displayMetrics.heightPixels : displayMetrics.widthPixels;
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowLayoutParams = new WindowManager.LayoutParams();
        mWindowLayoutParams.format = PixelFormat.TRANSLUCENT; //图片之外的其他地方透明
        mWindowLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    }

    public void setDrag(boolean isDrag) {
        this.mIsDrag = isDrag;
    }

    public void setFling(boolean isFling) {
        this.mIsFling = isFling;
    }

    //能否拖拽
    private boolean isCanDrag(float rawX, float rawY) {

        boolean isCanDrag = false;
        int childCount = getChildCount();
        mCoolMenuBeanList.clear();

        for (int i = 0; i < childCount; i++) {

            if (i != 0) {

                View child = getChildAt(i);
                int[] location = new int[2];
                child.getLocationOnScreen(location);

                if (!isCanDrag) {

                    if (isPointInView(rawX, rawY, location[0], location[1])) {

                        isCanDrag = true;
                        mStartDragView = child;
                        mCurrentDragMovePosition = i - 1;
                        mPreDragMovePosition = mCurrentDragMovePosition;
                        mStartDragView.setDrawingCacheEnabled(true);
                        mDragBitmap = Bitmap.createBitmap(mStartDragView.getDrawingCache());
                        mStartDragView.destroyDrawingCache();
                    }
                }

                mMovePosArr[i - 1] = i - 1;
                mCoolMenuBeanList.add(new CoolMenuBean(location[0], location[1] - mStatusBarHeight, i - 1));
            }
        }

        return isCanDrag;
    }

    //判断触摸点是否在view中
    private boolean isPointInView(float rawX, float rawY, int x, int y) {

        return rawX >= x - DAGGLE_RANGE * 3 && rawX <= (x + DAGGLE_RANGE + mEdge * 0.1F)
                && rawY >= y - mStatusBarHeight - DAGGLE_RANGE * 3
                && rawY <= (y + DAGGLE_RANGE - mStatusBarHeight + mEdge * 0.1F);
    }

    //根据x,y拖拽view
    private void onDragView(float moveX, float moveY) {

        mWindowLayoutParams.x = (int) moveX;
        mWindowLayoutParams.y = (int) moveY;
        mWindowManager.updateViewLayout(mDragView, mWindowLayoutParams); //更新镜像的位置

        if (mItemDragListener != null) {

            mItemDragListener.onDragMove(mStartDragView, moveX, moveY, mCurrentDragMovePosition);
        }

        onSwapItem(moveX, moveY);
    }

    //根据x,y交换view
    private void onSwapItem(float moveX, float moveY) {

        if (!mCoolMenuBeanList.isEmpty()) {

            for (CoolMenuBean coolMenuBean : mCoolMenuBeanList) {

                int originalPos = coolMenuBean.getPosition();

                if (isPointInView(moveX, moveY, coolMenuBean.getX(), coolMenuBean.getY()) && mMovePosArr[originalPos] != mCurrentDragMovePosition) {

                    View child = getChildAt(mCurrentDragMovePosition + 1);
                    View moveChild = getChildAt(mMovePosArr[originalPos] + 1);
                    mMovePosArr[mPreDragMovePosition] = mMovePosArr[originalPos];
                    mPreDragMovePosition = originalPos;
                    mMovePosArr[originalPos] = mCurrentDragMovePosition;
                    int moveLeft = moveChild.getLeft();
                    int moveTop = moveChild.getTop();
                    int moveRight = moveChild.getRight();
                    int moveBottom = moveChild.getBottom();
                    moveChild.layout(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                    child.layout(moveLeft, moveTop, moveRight, moveBottom);
                    break;
                }
            }
        }
    }

    //停止拖拽
    private void onStopDrag(float upX, float upY) {

        int[] inflate = dragFinishInflate((int) upX, (int) upY);

//        CoolMenuBean bean = mCoolMenuBeanList.get(mMovePosArr[mCurrentDragMovePosition]);//用这个有时候不准

        new DragValueAnimator(mDragView, new PointF(upX - mEdge * 0.1F, upY - mEdge * 0.1F - mStatusBarHeight), new PointF(inflate[0], inflate[1]))
                .addDragListener(new DragValueAnimator.DragAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mStartDragView.setVisibility(VISIBLE);
                        removeDragView();
                        state = IDLE;
                        setLayoutTransition(new LayoutTransition());
                        if (mItemDragListener != null) {
                            mItemDragListener.onDragEnd(mStartDragView, mCurrentDragMovePosition);
                        }
                    }
                })
                .addDragUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        PointF animatedValue = (PointF) animation.getAnimatedValue();
                        mWindowLayoutParams.x = (int) animatedValue.x;
                        mWindowLayoutParams.y = (int) animatedValue.y;
                        mWindowManager.updateViewLayout(mDragView, mWindowLayoutParams);
                    }
                })
                .start();
    }

    //拖拽结束，恢复布局
    private int[] dragFinishInflate(int upX, int upY) {

        int[] dragPosition = new int[]{upX, upY};
        //返回拖拽时的角度
        mAngle = mDragAngle;

        int childCount = getChildCount();
        if (childCount > 1) {

            List<View> list = new ArrayList<View>();

            for (int i = 1; i < childCount; i++) {

                View child = getChildAt(mMovePosArr[i - 1] + 1);

                if (child.getVisibility() == INVISIBLE) {

                    mStartDragView = child;
                    child.getLocationOnScreen(dragPosition);
                    dragPosition[1] = dragPosition[1] - mStatusBarHeight;

                }
                list.add(child);
            }

            removeViewsInLayout(1, childCount - 1);

            for (int i = 0; i < list.size(); i++) {

                View child = list.get(i);
                addViewInLayout(child, i + 1, child.getLayoutParams());
            }

            requestLayout();
        }

        return dragPosition;
    }

    //是否是逆时针
    private boolean isAntiClockWise(float downY, float anglePerSecond) {

        float centerY = getMeasuredHeight() * 0.5f;
        if (downY >= 0 && downY <= centerY) {
            return anglePerSecond < 0;
        }
        return anglePerSecond > 0;
    }

    // 根据触摸的位置，计算角度
    private float getAngle(float currentX, float currentY) {

        double x = currentX - mEdge * 0.5f;
        double y = mEdge * 0.5f - currentY;
        return (float) Math.toDegrees(Math.atan2(y, x));
    }

    public void cancelFuture() {

        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    private void startDragEvent(float downX, float downY) {

        cancelFuture();
        state = DAGGLE;

        setLayoutTransition(null);

        if (mItemDragListener != null) {
            mItemDragListener.onDragStart(mStartDragView, mCurrentDragMovePosition);
        }
        mStartDragView.setVisibility(View.INVISIBLE);//隐藏该item
        mVibrator.vibrate(50); //震动一下
        //根据我们按下的点显示item镜像
        createDragView((int) downX, (int) downY);
//        mFuture = mExecutor.schedule(new CoolMenuDragRunnable(this, downX, downY), 0, TimeUnit.MILLISECONDS);
    }

    protected final void startScroll(float velocity) {

        cancelFuture();
        state = FLING;
        if (mItemFlingListener != null) {
            mItemFlingListener.onFlingStart();
        }
        mFuture = mExecutor.scheduleWithFixedDelay(new CoolMenuVelocityTimerTask(this, velocity), 0, VELOCITYFLING, TimeUnit.MILLISECONDS);
    }

    //创建拖动的镜像
    protected final void createDragView(int downX, int downY) {

        mWindowLayoutParams.x = downX;
        mWindowLayoutParams.y = downY;
        mDragView = new ImageView(getContext());
        mDragView.setImageBitmap(mDragBitmap);
        mWindowManager.addView(mDragView, mWindowLayoutParams);
//        mHandle.sendEmptyMessage(WHAT_HANDLE_ADD_DRAG_VIEW);
    }

    //从界面上面移动拖动镜像
    private void removeDragView() {

        if (mDragView != null) {
            mWindowManager.removeView(mDragView);
            mDragView = null;
        }
    }

    //摆放
    private void layoutChild() {

        int childCount = getChildCount();
        int childPerAngle = 0;
        if (childCount > 1) {
            childPerAngle = (int) (360 * 1.0f / (childCount - 1));
        }

        for (int i = 0; i < childCount; i++) {

            View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            int left;
            int top;
            int right;
            int bottom;

            if (i == 0) {

                left = (int) (mEdge * 0.5f - width * 0.5f);
                top = (int) (mEdge * 0.5f - height * 0.5f);
                right = (int) (mEdge * 0.5f + width * 0.5f);
                bottom = (int) (mEdge * 0.5f + height * 0.5f);
            } else {

                left = (int) (1.0f * mEdge / 3 * Math.cos(Math.toRadians(mAngle + (i - 1) * childPerAngle)) - width * 0.5f + 0.5f * mEdge);
                top = (int) (0.5f * mEdge - 1.0f * mEdge / 3 * Math.sin(Math.toRadians(mAngle + (i - 1) * childPerAngle)) - height * 0.5f);
                right = (int) (1.0f * mEdge / 3 * Math.cos(Math.toRadians(mAngle + (i - 1) * childPerAngle)) + width * 0.5f + 0.5f * mEdge);
                bottom = (int) (0.5f * mEdge - 1.0f * mEdge / 3 * Math.sin(Math.toRadians(mAngle + (i - 1) * childPerAngle)) + height * 0.5f);
            }

            child.layout(left, top, right, bottom);
        }
    }

    private float dp2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return dpValue * scale + 0.5f;
    }

    /**
     * 关于适配器的写法是模仿recyclerview，具体的可以看源码，有想法的同学可以联系我，因为关于圆盘我确实想不到太多，重点是个架构的学习
     *
     * @param adapter
     */
    public void setAdapter(@NonNull Adapter adapter) {

        int childCount = getChildCount();

        if (childCount > 1 && mAdapter == null) {
            throw new RuntimeException("static and dynamic can only have one");
        }

        int count = adapter.getItemCount();

        if (count == 0) return;

        this.mCurrentChildCount = count;

        mMovePosArr = new int[count];

        if (this.mAdapter != null) {
            this.mAdapter.unregisterAdapterDataObserver(mObserver);
        }

        this.mAdapter = adapter;

        this.mAdapter.registerAdapterDataObserver(mObserver);

        updateAdapterData(adapter, count);
    }

    public Adapter getAdapter() {

        return mAdapter;
    }

    //关于这里的优化，等有时间我会想想，暂时先暴力点
    private void updateAdapterData(@NonNull Adapter adapter, int count) {

        if (getChildCount() > 1) {
            removeViewsInLayout(1, mCurrentChildCount);
        }

        if (count != mCurrentChildCount) {

            this.mCurrentChildCount = count;
            mMovePosArr = new int[count];

        }

        //因为我们控件没有复用一说，所以直接搞
        for (int i = 0; i < count; i++) {

            int viewType = adapter.getItemViewType(i);

            ViewHolder holder = adapter.onCreateViewHolder(this, viewType);

            adapter.onBindViewHolder(holder, i);

            addViewInLayout(holder.itemView, i + 1, holder.itemView.getLayoutParams(), true);

        }

        requestLayout();

    }

    //设置监听
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public void setOnItemDragListener(OnItemDragListener listener) {
        this.mItemDragListener = listener;
    }

    public void setOnItemFlingListener(OnItemFlingListener listener) {
        this.mItemFlingListener = listener;
    }

    //显示动画，可以自己调
    public void show() {

        int childCount = getChildCount();
        if (childCount > 1) {

            mAngle = 0;//复位

            float x = 0.5f * mEdge;
            float y = 0.5f * mEdge;
            Animator[] anim = new Animator[childCount];

            for (int i = 0; i < childCount; i++) {

                View child = getChildAt(i);
                float pivotX = x - child.getLeft();
                float pivotY = y - child.getTop();
                child.setPivotX(pivotX);
                child.setPivotY(pivotY);
                float childX = 0.5f * child.getLeft() + child.getRight() * 0.5f;
                float childY = 0.5f * child.getTop() + 0.5f * child.getBottom();

                PropertyValuesHolder ca = PropertyValuesHolder.ofFloat("alpha", 0.5f, 1.0f);
                PropertyValuesHolder csx = PropertyValuesHolder.ofFloat("scaleX", 0, 1.0f);
                PropertyValuesHolder csy = PropertyValuesHolder.ofFloat("scaleY", 0, 1.0f);

                if (i == 0) {

                    anim[i] = ObjectAnimator.ofPropertyValuesHolder(child, ca, csx, csy);
                } else {
                    PropertyValuesHolder ctx = PropertyValuesHolder.ofFloat("translationX", -x + childX, 0);
                    PropertyValuesHolder cty = PropertyValuesHolder.ofFloat("translationY", -y + childY, 0);
                    PropertyValuesHolder cr = PropertyValuesHolder.ofFloat("rotation", 0, 360f);
                    anim[i] = ObjectAnimator.ofPropertyValuesHolder(child, ctx, cty, ca, csx, csy, cr);
                }
            }

            AnimatorSet set = new AnimatorSet();
            set.setDuration(ANIM_SHOW_DURATION);
            set.setInterpolator(new LinearInterpolator());
            set.playTogether(anim);
            set.start();
        }
    }

    //关闭时动画
    public void dismiss() {

        int childCount = getChildCount();
        if (childCount > 1) {

            float x = 0.5f * mEdge;
            float y = 0.5f * mEdge;

            Animator[] before = new Animator[childCount];

            Animator[] after = new Animator[childCount - 1];

            for (int i = 0; i < childCount; i++) {

                View child = getChildAt(i);
                float pivotX = x - child.getLeft();
                float pivotY = y - child.getTop();
                float childX = 0.5f * child.getLeft() + child.getRight() * 0.5f;
                float childY = 0.5f * child.getTop() + 0.5f * child.getBottom();

                child.setPivotX(pivotX);
                child.setPivotY(pivotY);

                PropertyValuesHolder ca = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0);
                PropertyValuesHolder csx = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0);
                PropertyValuesHolder csy = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0);

                if (i == 0) {

                    before[i] = ObjectAnimator.ofPropertyValuesHolder(child, ca, csx, csy);

                } else {

                    PropertyValuesHolder ctx = PropertyValuesHolder.ofFloat("translationX", 0, x - childX);
                    PropertyValuesHolder cty = PropertyValuesHolder.ofFloat("translationY", 0, y - childY);
                    PropertyValuesHolder cr = PropertyValuesHolder.ofFloat("rotation", 0, 360f);
                    before[i] = ObjectAnimator.ofPropertyValuesHolder(child, cr, ctx, cty);

                    PropertyValuesHolder cx = PropertyValuesHolder.ofFloat("translationX", x - childX, (childX - x) * 10);
                    PropertyValuesHolder cy = PropertyValuesHolder.ofFloat("translationY", y - childY, (childY - y) * 10);

                    after[i - 1] = ObjectAnimator.ofPropertyValuesHolder(child, cx, cy);
                }
            }

            AnimatorSet set = new AnimatorSet();
            AnimatorSet first = new AnimatorSet();
            AnimatorSet last = new AnimatorSet();
            first.playTogether(before);
            last.playTogether(after);
            set.setDuration(ANIM_DISMISS_DURATION);
            set.setInterpolator(new DecelerateInterpolator());
            set.play(first).before(last);
            set.start();

        }
    }


    @Override
    public void setLayoutTransition(LayoutTransition transition) {
        super.setLayoutTransition(transition);
    }

    private class CoolMenuDataObserver extends AdapterDataObserver {

        CoolMenuDataObserver() {
        }

        @Override
        public void onChanged() {

            if (mAdapter == null) return;

            int itemCount = mAdapter.getItemCount();
            if (itemCount == 0) {
                if (getChildCount() > 1) {

                    removeViews(1, mCurrentChildCount);
                }
            } else {

                updateAdapterData(mAdapter, itemCount);
            }
        }

        @Override
        public void onItemInserted(int position) {

            if (mAdapter == null) return;

            int itemCount = mAdapter.getItemCount();

            if (position > mCurrentChildCount) return;

            mCurrentChildCount = mCurrentChildCount > itemCount ? mCurrentChildCount - 1 : mCurrentChildCount + 1;

            mMovePosArr = new int[mCurrentChildCount];

            int viewType = mAdapter.getItemViewType(position);

            ViewHolder holder = mAdapter.onCreateViewHolder(CoolMenu.this, viewType);

            mAdapter.onBindViewHolder(holder, position);

            addViewInLayout(holder.itemView, position + 1, holder.itemView.getLayoutParams(), true);

            requestLayout();

        }

        @Override
        public void onItemRemoved(int position) {
            if (mAdapter == null) return;

            int itemCount = mAdapter.getItemCount();

            if (position + 1 > mCurrentChildCount) return;

            mCurrentChildCount = mCurrentChildCount > itemCount ? mCurrentChildCount - 1 : mCurrentChildCount + 1;

            mMovePosArr = new int[mCurrentChildCount];

            removeViewsInLayout(position + 1, 1);

            requestLayout();
        }
    }

    /**
     * 其实我想说holder毫无用处，因为现在的设计没有回收复用一说，如果以后需要，可增加相应api
     * 现在纯粹是为了分开findviewbyid，不影响具体UI逻辑
     */
    public static abstract class Adapter<VH extends ViewHolder> {

        private final AdapterDataObservable mObservable = new AdapterDataObservable();

        public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

        public abstract void onBindViewHolder(VH holder, int position);

        //TraceCompat

        public int getItemViewType(int position) {
            return 0;
        }

        public abstract int getItemCount();

        public final void notifyDataSetChanged() {

            mObservable.notifyChanged();
        }

        public final boolean hasObservers() {

            return mObservable.hasObservers();
        }

        public void registerAdapterDataObserver(AdapterDataObserver observer) {

            mObservable.registerObserver(observer);
        }

        public void unregisterAdapterDataObserver(AdapterDataObserver observer) {

            mObservable.unregisterObserver(observer);
        }

        public final void notifyItemInserted(int position) {
            mObservable.notifyItemInserted(position);
        }

        public final void notifyItemRemoved(int position) {
            mObservable.notifyItemRemoved(position);
        }

    }

    static class AdapterDataObservable extends Observable<AdapterDataObserver> {

        public boolean hasObservers() {

            return !mObservers.isEmpty();
        }

        public void notifyChanged() {

            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }

        public void notifyItemInserted(int position) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemInserted(position);
            }
        }

        public void notifyItemRemoved(int position) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRemoved(position);
            }
        }

    }

    public static abstract class AdapterDataObserver {

        public void onChanged() {
        }

        public void onItemInserted(int position) {
        }

        public void onItemRemoved(int position) {
        }

    }


    public static abstract class ViewHolder {
        public final View itemView;

        public ViewHolder(View itemView) {
            if (itemView == null) {
                throw new IllegalArgumentException("itemView may not be null");
            }
            this.itemView = itemView;
        }
    }

    //关于监听暂时定下这几个
    public interface OnItemClickListener {

        void onItemClick(View view, int position);
    }

    public interface OnItemDragListener {

        void onDragStart(View view, int position);

        void onDragMove(View view, float rawX, float rawY, int position);

        void onDragEnd(View view, int position);
    }

    public interface OnItemFlingListener {

        void onFlingStart();

        void onFlingEnd();
    }

}
