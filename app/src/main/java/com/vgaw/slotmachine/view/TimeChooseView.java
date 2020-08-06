package com.vgaw.slotmachine.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.vgaw.slotmachine.util.DensityUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeChooseView extends ListView implements ListView.OnScrollListener, ViewTreeObserver.OnGlobalLayoutListener {
    private static final int WIDTH = 40;
    private static final int HEIGHT = 74;

    private static final int WHAT_SCROLL = 0x01;
    private static final int WHAT_DECREASE = 0x02;
    private static final int WHAT_END = 0x03;

    private static final int MIN_SCROLL_SPEED = 72;
    private static final int MAX_STOP_SPEED = 0;
    private static final int DECREASE_SPEED_DURATION = 400;
    // 滚动配置
    public static final int SMOOTH_SCROLL_DURATION = 1200;
    public static final int SECTION_DELAY = 300;

    private int mMaxScrollSpeed;

    private WheelAdapter mAdapter = new WheelAdapter(getContext());

    // 每一项高度
    private int itemHeightPixels = 0;
    // 记录滚轮当前刻度
    private int mCurrentPosition = -1;

    private OnWheelChangeListener mOnWheelChangeListener;
    private OnScrollFinishedListener mListener;
    private OnScrollUiFinishedListener mUiFinishedListener;

    private Handler mHandler;
    private AutoScrollEndListener mAutoScrollListener;

    public TimeChooseView(Context context) {
        super(context);
        init();
    }

    public TimeChooseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimeChooseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private ValueAnimator mScrollSpeedAnimator;
    private int mCrtScrollSpeed;

    public void start(int maxScrollSpeed) {
        mActualDistance = 0;

        mMaxScrollSpeed = maxScrollSpeed;
        mCrtScrollSpeed = mMaxScrollSpeed;
        mHandler.removeMessages(WHAT_SCROLL);
        mHandler.sendEmptyMessage(WHAT_SCROLL);
    }

    public void stop(final int index) {
        mDistance = -1;
        mFinalIndex = index;
        if (mScrollSpeedAnimator != null) {
            mScrollSpeedAnimator.cancel();
            mScrollSpeedAnimator.removeUpdateListener(mUpdateListener);
            mScrollSpeedAnimator.removeAllListeners();
        }
        mScrollSpeedAnimator = ValueAnimator.ofInt(mMaxScrollSpeed, MIN_SCROLL_SPEED);
        mScrollSpeedAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mHandler.removeMessages(WHAT_SCROLL);

                mHandler.sendEmptyMessage(WHAT_DECREASE);
            }
        });
        mScrollSpeedAnimator.addUpdateListener(mUpdateListener);
        mScrollSpeedAnimator.setDuration(DECREASE_SPEED_DURATION);
        mScrollSpeedAnimator.start();

        mHandler.removeMessages(WHAT_SCROLL);
        mHandler.sendEmptyMessage(WHAT_SCROLL);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(null);
        if (mScrollSpeedAnimator != null) {
            mScrollSpeedAnimator.cancel();
        }
    }

    private ValueAnimator.AnimatorUpdateListener mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mCrtScrollSpeed = (int) animation.getAnimatedValue();
        }
    };

    public void setItems(List<Integer> list) {
        _setItems(list);
    }

    /**
     * 设置滚轮个数偏移量
     */
    public void setOffset(int offset) {
        if (offset < 1 || offset > 3) {
            throw new IllegalArgumentException("Offset must between 1 and 3");
        }
        int wheelSize = offset * 2 + 1;
        mAdapter.setWheelSize(wheelSize);
    }

    private int getListViewScrollY(ListView listView, int toIndex) {
        TimeChooseItemView child = (TimeChooseItemView) listView.getChildAt(0);
        int top = child.getTop();
        int index = child.getData();

        int itemHeight = DensityUtil.dp2px(getContext(), HEIGHT);
        int distance = -1;
        if (top >= 0) {
            if (toIndex >= index) {
                distance = top + (toIndex - index) * itemHeight;
            } else {
                distance = top + (10 - index + toIndex) * itemHeight;
            }
        } else {
            if (toIndex > index) {
                distance = (itemHeight + top) + (toIndex - index - 1) * itemHeight;
            } else {
                distance = (itemHeight + top) + (9 - index + toIndex - 1) * itemHeight;
            }
        }
        return distance;
    }

    // 初始速度：a
    // 终止速度：e
    // 总距离：D
    // 时间间隔：d
    // 求速度差：delta
    // (a + e) * (((a - e) / delta) + 1) / 2 = D
    // delta = (a - e) / ((D * 2 / (a + e) - 1)

    private int mDistance = -1;
    private float mDelta;
    private float mFloatSpeed;
    private int mFinalIndex;

    public interface AutoScrollEndListener {
        void onScrollEnd();
    }

    public void setAutoScrollEndListener(AutoScrollEndListener listener) {
        mAutoScrollListener = listener;
    }

    private int mActualDistance;

    private void init() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == WHAT_SCROLL) {
                    scrollListBy(crtSpeed());

                    sendEmptyMessageDelayed(WHAT_SCROLL, 16);
                } else if (msg.what == WHAT_DECREASE) {
                    /*备选方案，停止速度不匀速*/
                    /*smoothScrollBy(listViewScrollY, (int) ((float) listViewScrollY / MIN_SCROLL_SPEED * 16));*/
                    if (mDistance == -1) {
                        mDistance = getListViewScrollY(TimeChooseView.this, mFinalIndex);
                        mDelta = ((float) MIN_SCROLL_SPEED - MAX_STOP_SPEED) / (((float) mDistance * 2 / (MIN_SCROLL_SPEED + MAX_STOP_SPEED)) - 1);
                        mFloatSpeed = mCrtScrollSpeed;
                    }

                    mActualDistance += mCrtScrollSpeed;
                    scrollListBy(mCrtScrollSpeed);
                    if (mFloatSpeed > MAX_STOP_SPEED) {
                        mFloatSpeed -= mDelta;
                        mCrtScrollSpeed = Math.round(mFloatSpeed);

                        sendEmptyMessageDelayed(WHAT_DECREASE, 16);
                    } else {
                        //mCrtScrollSpeed = Math.round(MAX_STOP_SPEED);

                        sendEmptyMessageDelayed(WHAT_END, 16);
                    }
                } else if (msg.what == WHAT_END) {
                    // 校正距离
                    scrollListBy(mDistance - mActualDistance);

                    if (mAutoScrollListener != null) {
                        mAutoScrollListener.onScrollEnd();
                    }
                }
            }
        };
        mHandler.sendEmptyMessage(WHAT_SCROLL);

        setVerticalScrollBarEnabled(false);
        setScrollingCacheEnabled(false);
        setCacheColorHint(Color.TRANSPARENT);
        setFadingEdgeLength(0);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setDividerHeight(0);
        setOnScrollListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setNestedScrollingEnabled(true);
        }
        if (!isInEditMode()) {
            getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
        super.setAdapter(mAdapter);

        setOffset(1);
        setCanLoop(true);
        setItems(buildData());
    }

    private int crtSpeed() {
        return mCrtScrollSpeed;
    }

    private void _setItems(List<Integer> list) {
        if (null == list || list.size() == 0) {
            throw new IllegalArgumentException("data are empty");
        }
//        isUserScroll = false;
        mCurrentPosition = -1;
        mAdapter.setData(list);
    }

    private void setCanLoop(boolean canLoop) {
        mAdapter.setLoop(canLoop);
    }

    public void setSelectedIndexBySecond(long second) {
        int index = getIndexByMinute((int) (second / 60));
        setSelectedIndex(index);
    }

    public void setSelectedIndex(final int index) {
        final int realPosition = getRealPosition(index);
//        WheelListView.super.setSelection(realPosition);
//        refreshCurrentPosition();
        //延时一下以保证数据初始化完成，才定位到选中项
        postDelayed(new Runnable() {
            @Override
            public void run() {
                TimeChooseView.super.setSelection(realPosition);
                refreshCurrentPosition();
            }
        }, SECTION_DELAY);
    }

    @Override
    public Integer getSelectedItem() {
        return mAdapter.getItem(getCurrentPosition());
    }

    /**
     * 获得滚轮当前真实位置
     */
    private int getRealPosition(int position) {
        int realCount = mAdapter.getRealCount();
        if (realCount == 0) {
            return 0;
        }
        if (mAdapter.isLoop()) {
            int d = Integer.MAX_VALUE / 2 / realCount;
            return position + d * realCount - mAdapter.getWheelSize() / 2;
        }
        return position;
    }

    /**
     * 获取当前滚轮位置
     */
    public int getCurrentPosition() {
        if (mCurrentPosition == -1) {
            return 0;
        }
        return mCurrentPosition;
    }

    private void onSelectedCallback() {
        int index = getCurrentPosition();
        Integer item = getSelectedItem();
        if (null != mOnWheelChangeListener) {
            mOnWheelChangeListener.onItemSelected(index, item);
        }
    }

    private int obtainSmoothDistance(float scrollDistance) {
        if (Math.abs(scrollDistance) <= 2) {
            return (int) scrollDistance;
        } else if (Math.abs(scrollDistance) < 12) {
            return scrollDistance > 0 ? 2 : -2;
        } else {
            return (int) (scrollDistance / 6);  // 减缓平滑滑动速率
        }
    }

    private void refreshCurrentPosition() {
        if (getChildAt(0) == null || itemHeightPixels == 0) {
            return;
        }
        int firstPosition = getFirstVisiblePosition();
        if (mAdapter.isLoop() && firstPosition == 0) {
            return;
        }
        int position;
        if (Math.abs(getChildAt(0).getY()) <= itemHeightPixels / 2) {
            position = firstPosition;
        } else {
            position = firstPosition + 1;
        }
        //由这个逆推：int wheelSize = offset * 2 + 1;
        int offset = (mAdapter.getWheelSize() - 1) / 2;
        int curPosition = position + offset;
        refreshVisibleItems(firstPosition, curPosition, offset);
        if (mAdapter.isLoop()) {
            position = curPosition % mAdapter.getRealCount();
        }
        if (position == mCurrentPosition) {
            return;
        }
        mCurrentPosition = position;
        onSelectedCallback();
    }

    private void refreshVisibleItems(int firstPosition, int curPosition, int offset) {
        for (int i = curPosition - offset; i <= curPosition + offset; i++) {
            View itemView = getChildAt(i - firstPosition);
            if (itemView != null && itemView instanceof TimeChooseItemView) {
                refreshTextView(i, curPosition, (TimeChooseItemView) itemView);
            }
        }
    }

    private void refreshTextView(int position, int curPosition, TimeChooseItemView itemView) {
        itemView.setItemSelected(curPosition == position);
    }

    @Override
    public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        int childCount = getChildCount();
        if (childCount > 0 && itemHeightPixels == 0) {
            itemHeightPixels = getChildAt(0).getHeight();
            if (itemHeightPixels == 0) {
                return;
            }
            int wheelSize = mAdapter.getWheelSize();
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = itemHeightPixels * wheelSize;
            refreshVisibleItems(getFirstVisiblePosition(),
                    getCurrentPosition() + wheelSize / 2, wheelSize / 2);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = DensityUtil.dp2px(getContext(), WIDTH);
        int height = DensityUtil.dp2px(getContext(), HEIGHT);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    /*@Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(mScrollFinishRunnable);
                callScrollFinished(getMinuteByIndex(getCurrentPosition()));
                postDelayed(mScrollFinishRunnable, SMOOTH_SCROLL_DURATION);
                break;
        }
        return super.onTouchEvent(ev);
    }*/

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState != SCROLL_STATE_IDLE) {
            removeCallbacks(mScrollFinishRunnable);
            return;
        }
        View itemView = getChildAt(0);
        if (itemView == null) {
            return;
        }
        float deltaY = itemView.getY();
        // fixed: 17-1-7  Equality tests should not be made with floating point values.
        if ((int) deltaY == 0 || itemHeightPixels == 0) {
            return;
        }
        if (Math.abs(deltaY) < itemHeightPixels / 2) {
            int d = obtainSmoothDistance(deltaY);
            smoothScrollBy(d, SMOOTH_SCROLL_DURATION);
        } else {
            int d = obtainSmoothDistance(itemHeightPixels + deltaY);
            smoothScrollBy(d, SMOOTH_SCROLL_DURATION);
        }
        removeCallbacks(mScrollFinishRunnable);
        callScrollFinished(getMinuteByIndex(getCurrentPosition()));
        postDelayed(mScrollFinishRunnable, SMOOTH_SCROLL_DURATION);
    }

    private Runnable mScrollFinishRunnable = new Runnable() {
        @Override
        public void run() {
            callScrollUiFinished();
        }
    };

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int
            visibleItemCount, int totalItemCount) {
        if (visibleItemCount != 0) {
            refreshCurrentPosition();
        }
    }

    private int getIndexByMinute(int minute) {
        return (minute / 5) - 1;
    }

    private int getMinuteByIndex(int index) {
        return (index + 1) * 5;
    }

    private List<Integer> buildData() {
        return Arrays.asList(0, 1, 2, 3, 4, 5,6, 7, 8, 9);
    }

    public void setOnWheelChangeListener(OnWheelChangeListener onWheelChangeListener) {
        this.mOnWheelChangeListener = onWheelChangeListener;
    }

    public void setOnScrollFinishListener(OnScrollFinishedListener listener) {
        mListener = listener;
    }

    public void setOnScrollUiFinishListener(OnScrollUiFinishedListener listener) {
        mUiFinishedListener = listener;
    }

    private void callScrollFinished(int crtMinute) {
        if (mListener != null) {
            mListener.onScrollFinished(crtMinute);
        }
    }

    private void callScrollUiFinished() {
        if (mUiFinishedListener != null) {
            mUiFinishedListener.onScrollUiFinished();
        }
    }

    public interface OnScrollFinishedListener {
        /**
         * 时间选中即回调
         * @param crtMinute
         */
        void onScrollFinished(int crtMinute);
    }

    public interface OnScrollUiFinishedListener {
        /**
         * 时间选中且选中效果消失才回调
         */
        void onScrollUiFinished();
    }

    public interface OnWheelChangeListener {
        /**
         * 滑动选择回调
         *
         * @param index        当前选择项的索引
         * @param item         当前选择项的值
         */
        void onItemSelected(int index, Integer item);
    }

    private static class WheelAdapter extends BaseAdapter {
        private List<Integer> data = new ArrayList<>();
        private boolean isLoop = false;
        private int wheelSize = 10;
        private Context mContext;

        public WheelAdapter(Context context) {
            mContext = context;
        }

        public final int getRealCount() {
            return data.size();
        }

        @Override
        public final int getCount() {
            if (isLoop) {
                return Integer.MAX_VALUE;
            }
            return data.size() > 0 ? (data.size() + wheelSize - 1) : 0;
        }

        @Override
        public final long getItemId(int position) {
            if (isLoop) {
                return data.size() > 0 ? position % data.size() : position;
            }
            return position;
        }

        @Override
        public final Integer getItem(int position) {
            if (isLoop) {
                return data.size() > 0 ? data.get(position % data.size()) : null;
            }
            if (data.size() <= position) {
                position = data.size() - 1;
            }
            return data.get(position);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public final View getView(int position, View convertView, ViewGroup parent) {
            if (isLoop) {
                position = position % data.size();
            } else {
                if (position < wheelSize / 2) {
                    position = -1;
                } else if (position >= wheelSize / 2 + data.size()) {
                    position = -1;
                } else {
                    position = position - wheelSize / 2;
                }
            }
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                TimeChooseItemView timeChooseItemView = new TimeChooseItemView(parent.getContext());
                timeChooseItemView.setItemHeight(DensityUtil.dp2px(mContext, HEIGHT));
                holder.itemView = timeChooseItemView;
                convertView = holder.itemView;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (!isLoop) {
                holder.itemView.setVisibility(position == -1 ? View.INVISIBLE : View.VISIBLE);
            }
            if (position == -1) {
                position = 0;
            }
            holder.itemView.setData(data.get(position));
            return convertView;
        }

        public final WheelAdapter setLoop(boolean loop) {
            if (loop != isLoop) {
                isLoop = loop;
                super.notifyDataSetChanged();
            }
            return this;
        }

        public final WheelAdapter setWheelSize(int wheelSize) {

            if ((wheelSize & 1) == 0) {
                throw new IllegalArgumentException("wheel size must be an odd number.");
            }
            this.wheelSize = wheelSize;
            super.notifyDataSetChanged();
            return this;
        }

        public final WheelAdapter setData(List<Integer> list) {
            data.clear();
            if (null != list) {
                data.addAll(list);
            }
            super.notifyDataSetChanged();
            return this;
        }

        public List<Integer> getData() {
            return data;
        }

        public int getWheelSize() {
            return wheelSize;
        }

        public boolean isLoop() {
            return isLoop;
        }

        private static class ViewHolder {
            TimeChooseItemView itemView;
        }
    }
}