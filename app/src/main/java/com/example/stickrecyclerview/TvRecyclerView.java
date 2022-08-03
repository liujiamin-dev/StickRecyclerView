package com.example.stickrecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class TvRecyclerView extends RecyclerView {
    private static final String TAG = "TvRecyclerView";

    private int position;

    //焦点是否居中
    private boolean mSelectedItemCentered;

    private int mSelectedItemOffsetStart;

    private int mSelectedItemOffsetEnd;

    //分页的时候使用
    private int mLoadMoreBeforehandCount = 0;

    //按键拦截时间
    private long mInterceptTime = 0L;

    //当长按下键或右键加载更多时，如果不将该按键拦截，焦点可能跑到页面其他地方
    private boolean isInterceptDownkey = false;
    private boolean isInterceptRightKey = false;

    private boolean isScrolling = false;

    public TvRecyclerView(Context context) {
        this(context, null);
    }

    public TvRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public TvRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        initView();
        initAttr(context, attrs);
    }

    private void initView() {
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setHasFixedSize(true);
//        setWillNotDraw(true);
//        setOverScrollMode(View.OVER_SCROLL_NEVER);
        setChildrenDrawingOrderEnabled(true);

        setClipChildren(false);
        setClipToPadding(false);

        setClickable(false);
        setFocusable(true);
        setFocusableInTouchMode(true);
        /**
         防止RecyclerView刷新时焦点不错乱bug的步骤如下:
         (1)adapter执行setHasStableIds(true)方法
         (2)重写getItemId()方法,让每个view都有各自的id
         (3)RecyclerView的动画必须去掉
         */
        setItemAnimator(null);
    }

    private void initAttr(Context context, AttributeSet attrs) {
        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.TvRecyclerView);


            mSelectedItemCentered = a.getBoolean(R.styleable.TvRecyclerView_tv_selectedItemCentered, false);

            mLoadMoreBeforehandCount = a.getInteger(R.styleable.TvRecyclerView_tv_loadMoreBeforehandCount, 0);

            mSelectedItemOffsetStart = a.getDimensionPixelSize(R.styleable.TvRecyclerView_tv_selectedItemOffsetStart, 0);

            mSelectedItemOffsetEnd = a.getDimensionPixelSize(R.styleable.TvRecyclerView_tv_selectedItemOffsetEnd, 0);

            mInterceptTime = a.getInteger(R.styleable.TvRecyclerView_tv_setInterceptTime, 100);

            a.recycle();
        }
    }


    private int getFreeWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getFreeHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }


    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public boolean hasFocus() {
        return super.hasFocus();
    }

    @Override
    public boolean isInTouchMode() {
        // 解决4.4版本抢焦点的问题
        if (Build.VERSION.SDK_INT == 19) {
            return !(hasFocus() && !super.isInTouchMode());
        } else {
            return super.isInTouchMode();
        }

        //return super.isInTouchMode();
    }


    @Override
    public int getBaseline() {
        return -1;
    }


    public int getSelectedItemOffsetStart() {
        return mSelectedItemOffsetStart;
    }

    public int getSelectedItemOffsetEnd() {
        return mSelectedItemOffsetEnd;
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
    }

    /**
     * 判断是垂直，还是横向.
     */
    private boolean isVertical() {
        LayoutManager manager = getLayoutManager();
        if (manager != null) {
            LinearLayoutManager layout = (LinearLayoutManager) getLayoutManager();
            return layout.getOrientation() == LinearLayoutManager.VERTICAL;

        }
        return false;
    }

    /**
     * 设置选中的Item距离开始或结束的偏移量；
     * 与滚动方向有关；
     * 与setSelectedItemAtCentered()方法二选一
     *
     * @param offsetStart
     * @param offsetEnd   从结尾到你移动的位置.
     */
    public void setSelectedItemOffset(int offsetStart, int offsetEnd) {
        setSelectedItemAtCentered(false);
        mSelectedItemOffsetStart = offsetStart;
        mSelectedItemOffsetEnd = offsetEnd;
    }

    /**
     * 设置选中的Item居中；
     * 与setSelectedItemOffset()方法二选一
     *
     * @param isCentered
     */
    public void setSelectedItemAtCentered(boolean isCentered) {
        this.mSelectedItemCentered = isCentered;
    }


    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        View view = getFocusedChild();
        if (null != view) {

            position = getChildAdapterPosition(view) - getFirstVisiblePosition();
            if (position < 0) {
                return i;
            } else {
                if (i == childCount - 1) {//这是最后一个需要刷新的item
                    if (position > i) {
                        position = i;
                    }
                    return position;
                }
                if (i == position) {//这是原本要在最后一个刷新的item
                    return childCount - 1;
                }
            }
        }
        return i;
    }

    public int getFirstVisiblePosition() {
        if (getChildCount() == 0)
            return 0;
        else
            return getChildAdapterPosition(getChildAt(0));
    }

    public int getLastVisiblePosition() {
        final int childCount = getChildCount();
        if (childCount == 0)
            return 0;
        else
            return getChildAdapterPosition(getChildAt(childCount - 1));
    }


    /***********
     * 按键加载更多 start
     **********/

    private OnLoadMoreListener mOnLoadMoreListener;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }


    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.mOnLoadMoreListener = onLoadMoreListener;
    }

    private OnInterceptListener mInterceptLister;


    public void setOnInterceptListener(OnInterceptListener listener) {
        this.mInterceptLister = listener;
    }

    public void setInterceptTime(long interceptTime) {
        mInterceptTime = interceptTime;
    }

    //加载更多时是否增加抖动效果
    private OnShakeListener mOnShakeListener;

    public interface OnShakeListener {
        void shakeRight(View rightView);
        void shakeBottom(View bottomView);
    }

    public void setOnShakeListener(OnShakeListener onShakeListener) {
        this.mOnShakeListener = onShakeListener;
    }

    //上键监听
    private OnkeyUpListener mOnKeyUplistener;

    public interface OnkeyUpListener {
        void onKeyUp();
    }

    public void setOnKeyUpListener(OnkeyUpListener onKeyUpListener) {
        mOnKeyUplistener = onKeyUpListener;
    }

    /**
     * 设置为0，这样可以防止View获取焦点的时候，ScrollView自动滚动到焦点View的位置
     */

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return 0;
    }

    private OnScrollingListener mOnScrollingListener;

    public interface OnScrollingListener {
        void onScrolling(boolean isScrolling);
    }

    public void setOnScrollingListener(OnScrollingListener onScrollingListener) {
        this.mOnScrollingListener = onScrollingListener;
    }

    public boolean isScrolling() {
        return isScrolling;
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == SCROLL_STATE_IDLE) {
            isScrolling = false;
            if(mOnScrollingListener != null)
                mOnScrollingListener.onScrolling(false);
            // 加载更多回调
            if (null != mOnLoadMoreListener) {
                if (getLastVisiblePosition() >= getAdapter().getItemCount() - (1 + mLoadMoreBeforehandCount)) {
                    mOnLoadMoreListener.onLoadMore();
                }
            }
        } else if(state == SCROLL_STATE_DRAGGING || state == SCROLL_STATE_SETTLING) {
            isScrolling = true;
            if(mOnScrollingListener != null)
                mOnScrollingListener.onScrolling(true);
        }
        super.onScrollStateChanged(state);
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
    }

    private long currTime;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_DPAD_RIGHT && event.getKeyCode() != KeyEvent.KEYCODE_DPAD_LEFT && event.getKeyCode() != KeyEvent.KEYCODE_DPAD_DOWN
                && event.getKeyCode() != KeyEvent.KEYCODE_DPAD_UP) {
            return super.dispatchKeyEvent(event);
        }

        if (mInterceptLister != null && mInterceptLister.onIntercept(event)) {
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && System.currentTimeMillis() - currTime < mInterceptTime) {
            return true;
        }
        if(event.getAction() == KeyEvent.ACTION_DOWN)
            currTime = System.currentTimeMillis();

        boolean result = super.dispatchKeyEvent(event);
        View focusView = this.getFocusedChild();
        if (focusView == null) {
            return result;
        } else {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int[] amount;
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        View rightView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_RIGHT);
                        Log.i(TAG, "rightView is null:" + (rightView == null));
                        if (rightView != null) {
                            rightView.requestFocus();
                            if (mSelectedItemCentered) {
                                amount = getScrollAmount(this, rightView);
                                smoothScrollBy(amount[0], 0);
                            } else {
                                //偏移
                                int right = getWidth() - rightView.getRight() - getPaddingRight();
                                if (right < mSelectedItemOffsetEnd) {
                                    smoothScrollBy(mSelectedItemOffsetEnd - right, 0);
                                }
                            }
                            return true;
                        } else {
                            if(mOnShakeListener != null) {
                                mOnShakeListener.shakeRight(focusView);
                            }
                        }
                        if(isInterceptRightKey)
                            return true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        View leftView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_LEFT);
                        Log.i(TAG, "leftView is null:" + (leftView == null));
                        if (leftView != null) {
                            leftView.requestFocus();
                            if (mSelectedItemCentered) {
                                amount = getScrollAmount(this, leftView);
                                smoothScrollBy(amount[0], 0);
                            } else {
                                //偏移
                                int left = leftView.getLeft();
                                if (left < mSelectedItemOffsetStart) {
                                    smoothScrollBy(left - mSelectedItemOffsetStart, 0);
                                }
                            }
                            return true;
                        }
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        View downView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_DOWN);
                        Log.i(TAG, " downView is null:" + (downView == null));
                        if(focusView.getBottom() > getHeight()) {
                            int bottom = getHeight() - focusView.getBottom() - getPaddingBottom();
                            if (bottom < mSelectedItemOffsetEnd) {
                                smoothScrollBy(0, mSelectedItemOffsetEnd - bottom);
                            }
                        } else if (downView != null) {
                            downView.requestFocus();
                            if (mSelectedItemCentered) {
                                amount = getScrollAmount(this, downView);
                                smoothScrollBy(0, amount[1]);
                            } else {
                                //偏移
                                int bottom = getHeight() - downView.getBottom() - getPaddingBottom();
                                if (bottom < mSelectedItemOffsetEnd) {
                                    smoothScrollBy(0, mSelectedItemOffsetEnd - bottom);
                                }
                            }
                            return true;
                        } else {
                            if(mOnShakeListener != null) {
                                mOnShakeListener.shakeBottom(focusView);
                            }
                        }
                        if(isInterceptDownkey)
                            return true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        View upView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_UP);
                        Log.i(TAG, "upView is null:" + (upView == null));

                        if (upView != null) {
                            upView.requestFocus();
                            if (mSelectedItemCentered) {
                                amount = getScrollAmount(this, upView);
                                smoothScrollBy(0, amount[1]);
                            } else {
                                //偏移
                                int top = upView.getTop();
                                if (top < mSelectedItemOffsetStart) {
                                    smoothScrollBy(0, top - mSelectedItemOffsetStart);
                                }
                            }
                            return true;
                        } else if(mOnKeyUplistener != null) {
                            mOnKeyUplistener.onKeyUp();
                            return true;
                        }
                        break;
                }
            }
        }
        return result;
    }

    /*
        分页加载时，当加载数据后，需要将内容往上滑动一段距离，以显示下面的内容
     */
    public void continueScrollY(int dy) {
        View focusView = this.getFocusedChild();
        View downView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_DOWN);
        if(downView == null) {
            smoothScrollBy(0, dy);
        }
    }

    public void continueScrollX(int dx) {
        View focusView = this.getFocusedChild();
        View rightView = FocusFinder.getInstance().findNextFocus(this, focusView, View.FOCUS_RIGHT);
        if(rightView == null) {
            smoothScrollBy(dx, 0);
        }
    }

    public void isIntereptDownKey(boolean flag) {
        isInterceptDownkey = flag;
    }

    public void isIntereptRightKey(boolean flag) {
        isInterceptRightKey = flag;
    }

    /**
     * 计算需要滑动的距离,使焦点在滑动中始终居中
     *
     * @param recyclerView
     * @param view
     */
    private int[] getScrollAmount(RecyclerView recyclerView, View view) {
        int[] out = new int[2];
        final int parentLeft = recyclerView.getPaddingLeft();
        final int parentTop = recyclerView.getPaddingTop();
        final int parentRight = recyclerView.getWidth() - recyclerView.getPaddingRight();
        final int parentBottom = recyclerView.getHeight() - recyclerView.getPaddingBottom();
        final int childLeft = view.getLeft() - view.getScrollX();
        final int childTop = view.getTop() - view.getScrollY();

        final int dx = childLeft - parentLeft - ((parentRight - view.getWidth()) / 2);

        final int dy = childTop - parentTop - ((parentBottom - view.getHeight()) / 2);
        out[0] = dx;
        out[1] = dy;
        return out;

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return super.onInterceptTouchEvent(e);
    }


    /**
     * 设置默认选中.
     */

    public void setSelectedPosition(int pos) {
        this.smoothScrollToPosition(pos);
    }

    //防止Activity时,RecyclerView崩溃
    @Override
    protected void onDetachedFromWindow() {
        if (getLayoutManager() != null) {
            super.onDetachedFromWindow();
        }
    }


    /**
     * 是否是最右边的item，如果是竖向，表示右边，如果是横向表示下边
     *
     * @param childPosition
     * @return
     */
    public boolean isRightEdge(int childPosition) {
        LayoutManager layoutManager = getLayoutManager();

        if (layoutManager instanceof GridLayoutManager) {

            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            GridLayoutManager.SpanSizeLookup spanSizeLookUp = gridLayoutManager.getSpanSizeLookup();

            int totalSpanCount = gridLayoutManager.getSpanCount();
            int totalItemCount = gridLayoutManager.getItemCount();
            int childSpanCount = 0;

            for (int i = 0; i <= childPosition; i++) {
                childSpanCount += spanSizeLookUp.getSpanSize(i);
            }
            if (isVertical()) {
                if (childSpanCount % gridLayoutManager.getSpanCount() == 0) {
                    return true;
                }
            } else {
                int lastColumnSize = totalItemCount % totalSpanCount;
                if (lastColumnSize == 0) {
                    lastColumnSize = totalSpanCount;
                }
                if (childSpanCount > totalItemCount - lastColumnSize) {
                    return true;
                }
            }

        } else if (layoutManager instanceof LinearLayoutManager) {
            if (isVertical()) {
                return true;
            } else {
                return childPosition == getLayoutManager().getItemCount() - 1;
            }
        }

        return false;
    }

    /**
     * 是否是最左边的item，如果是竖向，表示左方，如果是横向，表示上边
     *
     * @param childPosition
     * @return
     */
    public boolean isLeftEdge(int childPosition) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            GridLayoutManager.SpanSizeLookup spanSizeLookUp = gridLayoutManager.getSpanSizeLookup();

            int totalSpanCount = gridLayoutManager.getSpanCount();
            int childSpanCount = 0;
            for (int i = 0; i <= childPosition; i++) {
                childSpanCount += spanSizeLookUp.getSpanSize(i);
            }
            if (isVertical()) {
                if (childSpanCount % gridLayoutManager.getSpanCount() == 1) {
                    return true;
                }
            } else {
                if (childSpanCount <= totalSpanCount) {
                    return true;
                }
            }

        } else if (layoutManager instanceof LinearLayoutManager) {
            if (isVertical()) {
                return true;
            } else {
                return childPosition == 0;
            }

        }

        return false;
    }

    /**
     * 是否是最上边的item，以recyclerview的方向做参考
     *
     * @param childPosition
     * @return
     */
    public boolean isTopEdge(int childPosition) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            GridLayoutManager.SpanSizeLookup spanSizeLookUp = gridLayoutManager.getSpanSizeLookup();

            int totalSpanCount = gridLayoutManager.getSpanCount();

            int childSpanCount = 0;
            for (int i = 0; i <= childPosition; i++) {
                childSpanCount += spanSizeLookUp.getSpanSize(i);
            }

            if (isVertical()) {
                if (childSpanCount <= totalSpanCount) {
                    return true;
                }
            } else {
                if (childSpanCount % totalSpanCount == 1) {
                    return true;
                }
            }


        } else if (layoutManager instanceof LinearLayoutManager) {
            if (isVertical()) {
                return childPosition == 0;
            } else {
                return true;
            }

        }

        return false;
    }

    /**
     * 是否是最下边的item，以recyclerview的方向做参考
     *
     * @param childPosition
     * @return
     */
    public boolean isBottomEdge(int childPosition) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            GridLayoutManager.SpanSizeLookup spanSizeLookUp = gridLayoutManager.getSpanSizeLookup();
            int itemCount = gridLayoutManager.getItemCount();
            int childSpanCount = 0;
            int totalSpanCount = gridLayoutManager.getSpanCount();
            for (int i = 0; i <= childPosition; i++) {
                childSpanCount += spanSizeLookUp.getSpanSize(i);
            }
            if (isVertical()) {
                //最后一行item的个数
                int lastRowCount = itemCount % totalSpanCount;
                if (lastRowCount == 0) {
                    lastRowCount = gridLayoutManager.getSpanCount();
                }
                if (childSpanCount > itemCount - lastRowCount) {
                    return true;
                }
            } else {
                if (childSpanCount % totalSpanCount == 0) {
                    return true;
                }
            }

        } else if (layoutManager instanceof LinearLayoutManager) {
            if (isVertical()) {
                return childPosition == getLayoutManager().getItemCount() - 1;
            } else {
                return true;
            }

        }
        return false;
    }


}

