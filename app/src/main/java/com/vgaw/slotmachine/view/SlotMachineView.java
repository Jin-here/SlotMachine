package com.vgaw.slotmachine.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.vgaw.slotmachine.R;
import com.vgaw.slotmachine.util.DensityUtil;

public class SlotMachineView extends LinearLayout {
    private TimeChooseView mItemLeft;
    private TimeChooseView mItemMiddle;
    private TimeChooseView mItemRight;
    private int mEndCount;

    private boolean mStarted;

    public SlotMachineView(Context context) {
        super(context);
        init();
    }

    public SlotMachineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SlotMachineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void start() {
        if (mStarted) {
            return;
        }
        mStarted = true;

        mEndCount = 0;

        mItemLeft.start( 72);
        mItemMiddle.start(88);
        mItemRight.start(96);
    }

    public void stop(String result) {
        if (!mStarted) {
            return;
        }
        char left = result.charAt(0);
        char middle = result.charAt(1);
        char right = result.charAt(2);
        mItemLeft.stop(left + 2);
        mItemMiddle.stop(middle + 2);
        mItemRight.stop(right + 2);
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int paddingLeft = DensityUtil.dp2px(getContext(), 60);
        int paddingRight = DensityUtil.dp2px(getContext(), 64);
        setPadding(paddingLeft, 0, paddingRight, 0);
        setBackgroundResource(R.drawable.yaoshaizi_bg);

        mItemLeft = buildItem();
        mItemMiddle = buildItem();
        mItemRight = buildItem();

        addView(mItemLeft, buildLayoutParam(true));
        addView(mItemMiddle, buildLayoutParam(false));
        addView(mItemRight, buildLayoutParam(false));
    }

    private LayoutParams buildLayoutParam(boolean first) {
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (!first) {
            params.leftMargin = DensityUtil.dp2px(getContext(), 2);
        }
        params.topMargin = DensityUtil.dp2px(getContext(), 4);
        return params;
    }

    private TimeChooseView buildItem() {
        TimeChooseView item = new TimeChooseView(getContext());
        item.setAutoScrollEndListener(() -> {
            mEndCount++;
            if (mEndCount == getChildCount()) {
                onShaiEnd();
            }
        });
        return item;
    }

    private void onShaiEnd() {
        mStarted = false;
    }
}
