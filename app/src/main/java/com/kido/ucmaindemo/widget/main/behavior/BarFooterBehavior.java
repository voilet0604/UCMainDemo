package com.kido.ucmaindemo.widget.main.behavior;


import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.OverScroller;

import com.kido.ucmaindemo.utils.Logger;
import com.kido.ucmaindemo.widget.main.UcNewsBarLayout;
import com.kido.ucmaindemo.widget.main.helper.HeaderScrollingViewBehavior;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Behavior for Bar Footer.
 * <p>
 * e.g. TabLayout
 *
 * @author Kido
 */

public class BarFooterBehavior extends HeaderScrollingViewBehavior {
    private static final String TAG = "UNBL_FooterBehavior";
    public static final int STATE_OPENED = 0;
    public static final int STATE_CLOSED = 1;
    public static final int DURATION_SHORT = 300;
    public static final int DURATION_LONG = 600;

    private static final float DRAG_RATE = 1f / 5f; // 用于消耗下拉dy

    private static final float UP_DOWN_DIVIDE = 1f / 3f; // 超过该分割线的话松开是自动合拢

    private int mCurState = STATE_OPENED;
    private OnPagerStateListener mPagerStateListener;

    private OverScroller mOverScroller;


    private WeakReference<CoordinatorLayout> mParent;
    private WeakReference<View> mChild;

    private boolean mWasNestedFlung;


    public void setPagerStateListener(OnPagerStateListener pagerStateListener) {
        mPagerStateListener = pagerStateListener;
    }

    public BarFooterBehavior() {
    }

    public BarFooterBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void layoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        super.layoutChild(parent, child, layoutDirection);
        mParent = new WeakReference<CoordinatorLayout>(parent);
        mChild = new WeakReference<View>(child);
    }


    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        Logger.d(TAG, "onStartNestedScroll: nestedScrollAxes=%s", nestedScrollAxes);
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0 && canScroll(child, 0) && !isClosed(child);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
        //dy>0 scroll up;dy<0,scroll down
        float dealDis = dy * DRAG_RATE; // 处理过的dis，为了不那么敏感
        Logger.d(TAG, "onNestedPreScroll-> dy=%s, dealDis=%s", dy, dealDis);
        if (!canScroll(child, dealDis)) {
            child.setTranslationY(dealDis > 0 ? -getScrollRange(child) : 0);
        } else {
            child.setTranslationY(child.getTranslationY() - dealDis);
        }
        //consumed all scroll behavior after we started Nested Scrolling
        consumed[1] = dy;
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target) {
        super.onStopNestedScroll(coordinatorLayout, child, target);
        if(!mWasNestedFlung) {
            if (!isClosed()) {
                handleActionUp(coordinatorLayout, child);
            }
        }
        mWasNestedFlung = false;
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, View child, View target, float velocityX, float velocityY) {
        // consumed the flinging behavior until Closed
        Logger.d(TAG, "onNestedPreFling: velocityX=%s, velocityY=%s", velocityX, velocityY);
        boolean consumed = !isClosed(child);
        mWasNestedFlung = true;
        closePager();
        return consumed;
    }


    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return isDependOn(dependency);
    }


//    @Override
//    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
////        offsetChildAsNeeded(parent, child, dependency);
//        return false;
//    }
//


    @Override
    protected View findFirstDependency(List<View> views) {
        for (int i = 0, z = views.size(); i < z; i++) {
            View view = views.get(i);
            if (isDependOn(view))
                return view;
        }
        return null;
    }

    @Override
    protected int getScrollRange(View dependency) {
        if (isDependOn(dependency)) {
            return Math.max(0, dependency.getMeasuredHeight() - getFinalTopHeight(dependency));
        } else {
            return super.getScrollRange(dependency);
        }
    }


    private int getFinalTopHeight(View dependency) {
        if (dependency instanceof UcNewsBarLayout) {
            return ((UcNewsBarLayout) dependency).getHeaderHeight();
        }
        return 0;
    }


    private boolean isDependOn(View dependency) {
        return dependency instanceof UcNewsBarLayout;
    }


    private boolean isClosed(View child) {
        boolean isClosed = child.getTranslationY() <= -getScrollRange(child);
        return isClosed;
    }

    public boolean isClosed() {
        return mCurState == STATE_CLOSED;
    }


    private void changeState(int newState) {
        Logger.d(TAG, "changeState-> newState=%s", newState);
        if (mCurState != newState) {
            mCurState = newState;
            if (mCurState == STATE_OPENED) {
                if (mPagerStateListener != null) {
                    mPagerStateListener.onBarOpened();
                }
            } else {
                if (mPagerStateListener != null) {
                    mPagerStateListener.onBarClosed();
                }
            }
        }

    }

    private boolean canScroll(View child, float pendingDy) {
        int pendingTranslationY = (int) (child.getTranslationY() - pendingDy);
        if (pendingTranslationY >= -getScrollRange(child) && pendingTranslationY <= 0) {
            return true;
        }
        return false;
    }


    private void handleActionUp(CoordinatorLayout parent, final View child) {
        boolean isClosed = isClosed(child);
        Logger.d(TAG, "handleActionUp: isClosed=" + isClosed);
        if (mFlingRunnable != null) {
            child.removeCallbacks(mFlingRunnable);
            mFlingRunnable = null;
        }
        mFlingRunnable = new FlingRunnable(parent, child);
        if (child.getTranslationY() < -getScrollRange(child) * UP_DOWN_DIVIDE) {
            mFlingRunnable.scrollToClosed(DURATION_SHORT);
        } else {
            mFlingRunnable.scrollToOpen(DURATION_SHORT);
        }

    }

    private void onFlingFinished(CoordinatorLayout coordinatorLayout, View layout) {
        changeState(isClosed(layout) ? STATE_CLOSED : STATE_OPENED);
    }

    public void openPager() {
        openPager(DURATION_LONG);
    }

    /**
     * @param duration open animation duration
     */
    public void openPager(int duration) {
        View child = mChild.get();
        CoordinatorLayout parent = mParent.get();
        if (isClosed() && child != null) {
            if (mFlingRunnable != null) {
                child.removeCallbacks(mFlingRunnable);
                mFlingRunnable = null;
            }
            mFlingRunnable = new FlingRunnable(parent, child);
            mFlingRunnable.scrollToOpen(duration);
        }
    }

    public void closePager() {
        closePager(DURATION_LONG);
    }

    /**
     * @param duration close animation duration
     */
    public void closePager(int duration) {
        View child = mChild.get();
        CoordinatorLayout parent = mParent.get();
        if (!isClosed()) {
            if (mFlingRunnable != null) {
                child.removeCallbacks(mFlingRunnable);
                mFlingRunnable = null;
            }
            mFlingRunnable = new FlingRunnable(parent, child);
            mFlingRunnable.scrollToClosed(duration);
        }
    }


    private FlingRunnable mFlingRunnable;

    /**
     * For animation , Why not use {@link android.view.ViewPropertyAnimator } to play animation is of the
     * other {@link CoordinatorLayout.Behavior} that depend on this could not receiving the correct result of
     * {@link View#getTranslationY()} after animation finished for whatever reason that i don't know
     */
    private class FlingRunnable implements Runnable {
        private final CoordinatorLayout mParent;
        private final View mLayout;

        FlingRunnable(CoordinatorLayout parent, View layout) {
            mParent = parent;
            mLayout = layout;
            ensureScroller(layout.getContext());
        }

        private void ensureScroller(Context context) {
            if (mOverScroller == null) {
                mOverScroller = new OverScroller(context);
            }
        }


        public void scrollToClosed(int duration) {
            int barOffset = getBarOffsetRange(mLayout);
            float curTranslationY = ViewCompat.getTranslationY(mLayout);
            float dy = barOffset - curTranslationY;

            int startY = (int) curTranslationY;
            int deltaY = barOffset - startY;
            Logger.d(TAG, "scrollToClose-> barOffset=%s, curTranslationY=%s, dy=%s, startY=%s, deltaY=%s",
                    barOffset, curTranslationY, dy, startY, deltaY);

            mOverScroller.startScroll(0, startY, 0, deltaY, duration);
            start();
            if (mPagerStateListener != null) {
                mPagerStateListener.onBarStartClosing();
            }
        }

        public void scrollToOpen(int duration) {
            float curTranslationY = ViewCompat.getTranslationY(mLayout);
            int startY = (int) curTranslationY;
            int deltaY = (int) -curTranslationY;
            Logger.d(TAG, "scrollToOpen-> curTranslationY=%s, startY=%s, deltaY=%s",
                    curTranslationY, startY, deltaY);
            mOverScroller.startScroll(0, startY, 0, deltaY, duration);
            start();
            if (mPagerStateListener != null) {
                mPagerStateListener.onBarStartOpening();
            }
        }

        private void start() {
            if (mOverScroller.computeScrollOffset()) {
                mFlingRunnable = new FlingRunnable(mParent, mLayout);
                ViewCompat.postOnAnimation(mLayout, mFlingRunnable);
            } else {
                onFlingFinished(mParent, mLayout);
            }
        }


        @Override
        public void run() {
            if (mLayout != null && mOverScroller != null) {
                if (mOverScroller.computeScrollOffset()) {
                    Logger.d(TAG, "FlingRunnable run-> mOverScroller.getCurrY()=%s", mOverScroller.getCurrY());
                    ViewCompat.setTranslationY(mLayout, mOverScroller.getCurrY());
                    ViewCompat.postOnAnimation(mLayout, this);
                } else {
                    onFlingFinished(mParent, mLayout);
                }
            }
        }
    }

    /**
     * callback for HeaderPager 's state
     */
    public interface OnPagerStateListener {

        void onBarStartClosing();

        void onBarStartOpening();

        /**
         * do callback when pager closed
         */
        void onBarClosed();

        /**
         * do callback when pager opened
         */
        void onBarOpened();
    }
}