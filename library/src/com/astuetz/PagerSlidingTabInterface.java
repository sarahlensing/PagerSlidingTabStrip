package com.astuetz;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

/**
 * Created by sarahlensing on 3/17/14.
 */
public interface PagerSlidingTabInterface {
    public int getCurrentItem();
    public PagerAdapter getAdapter();
    public void setCurrentItem(int item);
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener);
}
