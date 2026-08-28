package com.android.launcher3.applibrary.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.launcher3.util.Themes;

public class AppLibraryAlphabetIndexer extends View {

    public interface OnSectionSelectedListener {
        void onSectionSelected(String section);
    }

    private static final String[] ALPHABET = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private OnSectionSelectedListener mListener;
    private int mLastSelectedIndex = -1;

    public AppLibraryAlphabetIndexer(Context context) {
        this(context, null);
    }

    public AppLibraryAlphabetIndexer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLibraryAlphabetIndexer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        float textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10f, getResources().getDisplayMetrics());
        mPaint.setTextSize(textSize);
        int textColor = Themes.getAttrColor(getContext(), android.R.attr.textColorSecondary);
        mPaint.setColor(textColor);
    }

    public void setOnSectionSelectedListener(OnSectionSelectedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getHeight();
        int width = getWidth();
        if (height <= 0 || width <= 0) {
            return;
        }

        int count = ALPHABET.length;
        float itemHeight = (float) height / count;
        float xPos = width / 2f;

        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float fontHeight = fontMetrics.descent - fontMetrics.ascent;
        float baseLineOffset = (itemHeight - fontHeight) / 2f - fontMetrics.ascent;

        for (int i = 0; i < count; i++) {
            float yPos = i * itemHeight + baseLineOffset;
            canvas.drawText(ALPHABET[i], xPos, yPos, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                int height = getHeight();
                if (height > 0) {
                    float y = Math.max(0, Math.min(event.getY(), height - 1));
                    int index = (int) (y / ((float) height / ALPHABET.length));
                    index = Math.max(0, Math.min(index, ALPHABET.length - 1));
                    if (index != mLastSelectedIndex) {
                        mLastSelectedIndex = index;
                        if (mListener != null) {
                            mListener.onSectionSelected(ALPHABET[index]);
                        }
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastSelectedIndex = -1;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }
}
