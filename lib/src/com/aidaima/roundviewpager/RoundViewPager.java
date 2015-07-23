package com.aidaima.roundviewpager;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.PagerAdapter;
//import android.support.v4.view.ViewPager;
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

    /**
     * Get the virtual current item position. Maybe we can get virtual current
     * item as 3 while real current item as 1790032
     * 
     * @return
     */
    public int getVirtualCurrentItem() {
        if (getAdapter().getCount() == 0) {
            return 0;
        }

        if (getAdapter() instanceof RoundPagerAdapter) {
            RoundPagerAdapter infAdapter = (RoundPagerAdapter) getAdapter();
            int realCount = infAdapter.getRealCount();
            return super.getCurrentItem() % realCount;
        } else {
            return super.getCurrentItem();
        }
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        super.setAdapter(adapter);

        setCurrentItemEx(0, true);
    }

    @Override
    public void setCurrentItem(int position) {
        setCurrentItemEx(position, true);
    }

    @Override
    public void setCurrentItem(int position, boolean smooth) {
        setCurrentItemEx(position, smooth);
    }

    public void setCurrentItemEx(int position, boolean smooth) {
        if (getAdapter().getCount() == 0) {
            super.setCurrentItem(position, true);
            return;
        }

        int curPos = super.getCurrentItem();
        if (curPos > 0) {
            RoundPagerAdapter infAdapter = (RoundPagerAdapter) getAdapter();
            int realCount = infAdapter.getRealCount();

            int dif = ((position - curPos) % realCount);
            position += dif;
        } else {
            position = getOffsetAmount() + (position % getAdapter().getCount());
        }

        super.setCurrentItem(position, smooth);
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
                    handled = movePrev(true);
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
                    handled = moveNext(true);
                } else {
                    handled = nextFocused.requestFocus();
                }
            }
        } else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
            // Trying to move left and nothing there; try to page.
            handled = movePrev(true);
        } else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
            // Trying to move right and nothing there; try to page.
            handled = moveNext(true);
        }
        if (handled) {
            playSoundEffect(SoundEffectConstants
                    .getContantForFocusDirection(direction));
        }
        return handled;
    }

    /**
     * Move to the prev pager
     * 
     * @param smooth
     * @return
     */
    public boolean movePrev(boolean smooth) {
        if (super.getCurrentItem() > 0) {
            super.setCurrentItem(super.getCurrentItem() - 1, smooth);
            return true;
        }
        return false;
    }

    /**
     * Move to the next pager
     * 
     * @param smooth
     * @return
     */
    public boolean moveNext(boolean smooth) {
        PagerAdapter adapter = super.getAdapter();
        if (adapter != null
                && super.getCurrentItem() < (adapter.getCount() - 1)) {
            super.setCurrentItem(super.getCurrentItem() + 1, smooth);
            return true;
        }
        return false;
    }

    /**
     * Get the beginning scroll position
     * 
     * @return
     */
    private int getOffsetAmount() {
        if (getAdapter().getCount() == 0) {
            return 0;
        }
        if (getAdapter() instanceof RoundPagerAdapter) {
            RoundPagerAdapter infAdapter = (RoundPagerAdapter) getAdapter();
            int realCount = infAdapter.getRealCount();
            int count = infAdapter.getCount();
            int offset = count / 2;
            return offset - (offset % realCount);
        } else {
            return 0;
        }
    }
}
