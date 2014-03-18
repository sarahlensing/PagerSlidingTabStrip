package com.astuetz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.astuetz.pagerslidingtabstrip.R;

/**
 * Created by sarahlensing on 3/17/14.
 */
public class SmartQuickReturnView extends FrameLayout {

    private Context mContext;
    private ListView mListView;
    private ViewPager mPager;

    private ViewPager.OnPageChangeListener mExternalPageChangeListener;

    public int mScrollY;
    public int mMinRawY = 0;

    private int mItemCount;
    private int mItemOffsetY[];
    private boolean scrollIsComputed = false;
    private int mHeight;

    public int mCachedVerticalScrollRange;
    public int mQuickReturnHeight;
    public View mPlaceHolder;
    public View mQuickReturnView;

    // Quick return view states
    public static final int STATE_ONSCREEN = 0;
    public static final int STATE_OFFSCREEN = 1;
    public static final int STATE_RETURNING = 2;
    public static final int STATE_EXPANDED = 3;
    public int mState = STATE_RETURNING;

    public SmartQuickReturnView(Context context) {
        super(context);
        commonInit(context);
    }

    public SmartQuickReturnView(Context context, AttributeSet attrs) {
        super(context, attrs);
        commonInit(context);
    }

    public SmartQuickReturnView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        commonInit(context);
    }

    private void commonInit(Context context) {
        mContext = context;
    }


    public int getListHeight() {
        return mHeight;
    }

    public ListView getListView() {
        return mListView;
    }

    public void computeScrollY() {
        mHeight = 0;
        mItemCount = mListView.getAdapter().getCount();

        if (mItemOffsetY == null || mItemCount != mItemOffsetY.length) {
            mItemOffsetY = new int[mItemCount];
        }
        for (int i = 0; i < mItemCount; ++i) {
            // We need to think of a better way measure the height,
            // since this is not a very good one (see the method's comment)
            View view = mListView.getAdapter().getView(i, null, this);
            view.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            mItemOffsetY[i] = mHeight;
            mHeight += view.getMeasuredHeight();
        }
        if (mHeight < mListView.getHeight()) {
            mHeight = mListView.getHeight();
        }
        scrollIsComputed = true;
    }

    public boolean scrollYIsComputed() {
        return scrollIsComputed;
    }

    public int getComputedScrollY() {
        int pos, nScrollY, nItemY;
        View view = null;
        pos = mListView.getFirstVisiblePosition();
        view = mListView.getChildAt(0);
        nItemY = view.getTop();
        nScrollY = mItemOffsetY[pos] - nItemY;
        return nScrollY;
    }

    public int calcRawY() {
        mScrollY = 0;

        if (scrollYIsComputed()) {
            mScrollY = getComputedScrollY();
        }

        return mPlaceHolder.getTop()
                - Math.min(
                mCachedVerticalScrollRange
                        - mListView.getHeight(), mScrollY
        );
    }

    public void resetToReturnState() {
        mMinRawY = calcRawY() - mQuickReturnHeight;
    }

    public void resetToOffscreenState() {
        mMinRawY = calcRawY();
    }

    private void configureListView() {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Putting a placeholder as a list header, since the quick return item will be on top of it
        // when the list is scrolled all the way up
        View header = inflater.inflate(R.layout.quick_return_view, null, false);
        header.setMinimumHeight(mQuickReturnHeight);
        mPlaceHolder = header;

        //TODO: improve to check for existance of THIS header view instead of only allowing one header view
        if (mListView.getHeaderViewsCount() == 0) {
            mListView.addHeaderView(header);
        }

        getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mQuickReturnHeight = mQuickReturnView.getHeight();
                        if (!scrollIsComputed)
                            computeScrollY();
                        mCachedVerticalScrollRange = getListHeight();
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    }
                });

        mListView.setOnScrollListener(onScrollListener);
    }

    private void resetVars() {
        scrollIsComputed = false;
    }

    public void setListView(ListView listView, View quickReturnView) {
        resetVars();
        mListView = listView;
        mQuickReturnView = quickReturnView;
        configureListView();
    }

    AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {

        int translationY = 0;
        int rawY = 0;

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            rawY = calcRawY();

            switch (mState) {

                // The return view is currently off screen
                case STATE_OFFSCREEN:
                    if (rawY <= mMinRawY) {
                        mMinRawY = rawY;
                    } else {
                        mState = STATE_RETURNING;
                    }
                    translationY = rawY;
                    break;

                // The return view is on screen and scrolling with the list (not applicable for the PX design)
                case STATE_ONSCREEN:
                    if (rawY < -mQuickReturnHeight) {
                        mState = STATE_OFFSCREEN;
                        mMinRawY = rawY;
                    }
                    translationY = rawY;
                    break;

                // The return view is sticking to the top of the list
                case STATE_RETURNING:
                    translationY = (rawY - mMinRawY) - mQuickReturnHeight;
                    if (translationY > 0) {
                        translationY = 0;
                        mMinRawY = rawY - mQuickReturnHeight;
                    }

                    if (rawY > 0) {
                        mState = STATE_ONSCREEN;
                        translationY = rawY;
                    }

                    if (translationY < -mQuickReturnHeight) {
                        mState = STATE_OFFSCREEN;
                        mMinRawY = rawY;
                    }
                    break;
            }

            // Update the position of the quick return view
            mQuickReturnView.setAlpha(mQuickReturnHeight > 0 ? (mQuickReturnHeight+translationY)/(float)mQuickReturnHeight : 1f);
            mQuickReturnView.setTranslationY(translationY);
        }

        @Override
        public void onScrollStateChanged(final AbsListView view, int scrollState) {

            // If we lifted our finger while the quick return view is "on the move" -
            // we need to animate it to the closest state, either OFFSCREEN or RETURING
            if (scrollState == SCROLL_STATE_IDLE && -mQuickReturnView.getTranslationY()> 0 && -mQuickReturnView.getTranslationY() < mQuickReturnHeight) {
                final boolean needToDisappear = (-mQuickReturnView.getTranslationY() / (float)mQuickReturnHeight) > 0.5f;
                mQuickReturnView.animate().
                        setInterpolator(new DecelerateInterpolator(2f)).
                        translationY(needToDisappear ? -mQuickReturnHeight : 0).
                        alpha(needToDisappear ? 0 : 1f).
                        setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // Adapting all the parameters to the new state
                                if (needToDisappear) {
                                    mState = STATE_OFFSCREEN;
                                    resetToOffscreenState();
                                } else {
                                    mState = STATE_RETURNING;
                                    resetToReturnState();
                                }
                            }
                        }).start();
            }
        }
    };

    public void setViewPager(ViewPager viewPager) {
        mPager = viewPager;
        mPager.setOnPageChangeListener(mPageChangeListener);
    }

    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mExternalPageChangeListener = listener;
    }

    // This page listener is in charge of moving the curr tab indicator
    ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {

        int inTransitionPos = -1;
        boolean isQuickReturnViewInTransition = false;
        boolean directionRight = true;
        int currState = ViewPager.SCROLL_STATE_IDLE;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            // If we're not in a transition and we need to start one
            // (if we're dragging the ViewPager and the quick-return view is currently not visible)
            if (!isQuickReturnViewInTransition &&
                    currState == ViewPager.SCROLL_STATE_DRAGGING &&
                    mQuickReturnView.getAlpha() < 0.1f) {

                isQuickReturnViewInTransition = true;
                inTransitionPos = position;
                directionRight = positionOffset < 0.5f;
            }

            // If the quick return view is in transition - let's update its position
            if (isQuickReturnViewInTransition && position == inTransitionPos) {
                float translationY = -mQuickReturnHeight + mQuickReturnHeight*(directionRight?positionOffset:(1-positionOffset));
                mQuickReturnView.setTranslationY(translationY);
                mQuickReturnView.setAlpha(mQuickReturnHeight > 0 ? (mQuickReturnHeight + translationY) / (float) mQuickReturnHeight : 1f);
            }

            if (mExternalPageChangeListener != null) {
                mExternalPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

            currState = state;

            // If we finished the ViewPager's dragging, we need to end the transition
            if (isQuickReturnViewInTransition && currState == ViewPager.SCROLL_STATE_IDLE) {
                isQuickReturnViewInTransition = false;
                inTransitionPos = -1;

                // If the transition was actually completed
                if (mQuickReturnView.getAlpha() > 0.7f) {
                    mState = STATE_RETURNING;

                    resetToReturnState();
                }
            }

            if (mExternalPageChangeListener != null) {
                mExternalPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (mExternalPageChangeListener != null) {
                mExternalPageChangeListener.onPageSelected(position);
            }
        }
    };
}
