package com.aidaima.roundviewpager;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * A {@link ViewPager} that allows pseudo-infinite paging with a wrap-around
 * effect. Should be used with an {@link RoundPagerAdapter}.
 */
public class RoundViewPager extends ViewPager {
    private static final String TAG = "RoundViewPager";

    private final Rect mTempRect = new Rect();

    public RoundViewPager(Context context) {
        super(context);
    }

    public RoundViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        super.setAdapter(adapter);
        // offset first element so that we can scroll to the left
        setCurrentItem(0);
    }

    @Override
    public void setCurrentItem(int item) {
        // offset the current item to ensure there is space to scroll
        setCurrentItem(item, false);
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (getAdapter().getCount() == 0) {
            super.setCurrentItem(item, smoothScroll);
            return;
        }
        item = getOffsetAmount() + (item % getAdapter().getCount());

        super.setCurrentItem(item, smoothScroll);
    }

    @Override
    public int getCurrentItem() {
        if (getAdapter().getCount() == 0) {
            return super.getCurrentItem();
        }
        int position = super.getCurrentItem();
        if (getAdapter() instanceof RoundPagerAdapter) {
            RoundPagerAdapter infAdapter = (RoundPagerAdapter) getAdapter();
            // Return the actual item position in the data backing
            // InfinitePagerAdapter
            return (position % infAdapter.getRealCount());
        } else {
            return super.getCurrentItem();
        }
    }

    private Rect getChildRectInPagerCoordinates(Rect outRect, View child) {
        if (outRect == null) {
            outRect = new Rect();
        }
        if (child == null) {
            outRect.set(0, 0, 0, 0);
            return outRect;
        }
        outRect.left = child.getLeft();
        outRect.right = child.getRight();
        outRect.top = child.getTop();
        outRect.bottom = child.getBottom();

        ViewParent parent = child.getParent();
        while (parent instanceof ViewGroup && parent != this) {
            final ViewGroup group = (ViewGroup) parent;
            outRect.left += group.getLeft();
            outRect.right += group.getRight();
            outRect.top += group.getTop();
            outRect.bottom += group.getBottom();

            parent = group.getParent();
        }
        return outRect;
    }

    @Override
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        } else if (currentFocused != null) {
            boolean isChild = false;
            for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup; parent = parent
                    .getParent()) {
                if (parent == this) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                // This would cause the focus search down below to fail in fun
                // ways.
                final StringBuilder sb = new StringBuilder();
                sb.append(currentFocused.getClass().getSimpleName());
                for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup; parent = parent
                        .getParent()) {
                    sb.append(" => ").append(parent.getClass().getSimpleName());
                }
                Log.e(TAG,
                        "arrowScroll tried to find focus based on non-child "
                                + "current focused view " + sb.toString());
                currentFocused = null;
            }
        }

        boolean handled = false;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this,
                currentFocused, direction);
        if (nextFocused != null && nextFocused != currentFocused) {
            if (direction == View.FOCUS_LEFT) {
                // If there is nothing to the left, or this is causing us to
                // jump to the right, then what we really want to do is page
                // left.
                final int nextLeft = getChildRectInPagerCoordinates(mTempRect,
                        nextFocused).left;
                final int currLeft = getChildRectInPagerCoordinates(mTempRect,
                        currentFocused).left;
                if (currentFocused != null && nextLeft >= currLeft) {
                    handled = doPageLeft();
                } else {
                    handled = nextFocused.requestFocus();
                }
            } else if (direction == View.FOCUS_RIGHT) {
                // If there is nothing to the right, or this is causing us to
                // jump to the left, then what we really want to do is page
                // right.
                final int nextLeft = getChildRectInPagerCoordinates(mTempRect,
                        nextFocused).left;
                final int currLeft = getChildRectInPagerCoordinates(mTempRect,
                        currentFocused).left;
                if (currentFocused != null && nextLeft <= currLeft) {
                    handled = doPageRight();
                } else {
                    handled = nextFocused.requestFocus();
                }
            }
        } else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
            // Trying to move left and nothing there; try to page.
            handled = doPageLeft();
        } else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
            // Trying to move right and nothing there; try to page.
            handled = doPageRight();
        }
        if (handled) {
            playSoundEffect(SoundEffectConstants
                    .getContantForFocusDirection(direction));
        }
        return handled;
    }

    private boolean doPageLeft() {
        if (super.getCurrentItem() > 0) {
            super.setCurrentItem(super.getCurrentItem() - 1, true);
            return true;
        }
        return false;
    }

    private boolean doPageRight() {
        PagerAdapter adapter = super.getAdapter();
        if (adapter != null
                && super.getCurrentItem() < (adapter.getCount() - 1)) {
            super.setCurrentItem(super.getCurrentItem() + 1, true);
            return true;
        }
        return false;
    }

    private int getOffsetAmount() {
        if (getAdapter().getCount() == 0) {
            return 0;
        }
        if (getAdapter() instanceof RoundPagerAdapter) {
            RoundPagerAdapter infAdapter = (RoundPagerAdapter) getAdapter();
            // allow for 100 back cycles from the beginning
            // should be enough to create an illusion of infinity
            // warning: scrolling to very high values (1,000,000+) results in
            // strange drawing behaviour
            int realCount = infAdapter.getRealCount();
            int count = infAdapter.getCount();
            return realCount * (count - realCount) / 2;
        } else {
            return 0;
        }
    }
}
