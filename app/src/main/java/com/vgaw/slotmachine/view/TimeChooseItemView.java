package com.vgaw.slotmachine.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;

import com.vgaw.slotmachine.R;

public class TimeChooseItemView extends AppCompatImageView {
    private int mHeight;
    private int mNumber;

    public TimeChooseItemView(Context context) {
        super(context);
        init();
    }

    public TimeChooseItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimeChooseItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public int getData() {
        return mNumber;
    }

    public void setData(int number) {
        mNumber = number;

        setImageResource(getResId(number));
    }

    protected void setItemHeight(int height) {
        mHeight = height;
    }

    protected void setItemSelected(boolean selected) {}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
    }

    private void init() {}

    private @DrawableRes int getResId(int number) {
        switch (number) {
            case 0:
                return R.drawable.yaoshaizi_0;
            case 1:
                return R.drawable.yaoshaizi_1;
            case 2:
                return R.drawable.yaoshaizi_2;
            case 3:
                return R.drawable.yaoshaizi_3;
            case 4:
                return R.drawable.yaoshaizi_4;
            case 5:
                return R.drawable.yaoshaizi_5;
            case 6:
                return R.drawable.yaoshaizi_6;
            case 7:
                return R.drawable.yaoshaizi_7;
            case 8:
                return R.drawable.yaoshaizi_8;
            case 9:
                return R.drawable.yaoshaizi_9;
        }
        return -1;
    }
}
