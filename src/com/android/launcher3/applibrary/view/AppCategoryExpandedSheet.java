package com.android.launcher3.applibrary.view;

import static com.android.app.animation.Interpolators.EMPHASIZED;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.applibrary.model.AppCategoryGroup;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;
import java.util.List;

public class AppCategoryExpandedSheet extends FrameLayout implements Insettable {

    public interface OnExpandedSheetDismissListener {
        void onDismissStarted(long duration);
        void onDismissFinished();
    }

    private View mCardView;
    private ImageButton mBackButton;
    private TextView mTitleView;
    private RecyclerView mGridView;
    private ExpandedGridAdapter mAdapter;
    private final ActivityContext mActivityContext;
    private OnExpandedSheetDismissListener mDismissListener;
    private final Rect mInsets = new Rect();

    private Rect mSourceBounds;
    private float mStartTranslationX = 0f;
    private float mStartTranslationY = 0f;
    private float mStartScaleX = 0.85f;
    private float mStartScaleY = 0.85f;
    private boolean mIsAnimating = false;

    public AppCategoryExpandedSheet(Context context) {
        this(context, null);
    }

    public AppCategoryExpandedSheet(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppCategoryExpandedSheet(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivityContext = ActivityContext.lookupContext(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCardView = findViewById(R.id.expanded_sheet_card);
        mBackButton = findViewById(R.id.expanded_sheet_back_button);
        mTitleView = findViewById(R.id.expanded_sheet_title);
        mGridView = findViewById(R.id.expanded_sheet_grid);

        if (mBackButton != null) {
            mBackButton.setOnClickListener(v -> hide(true));
        }

        setOnClickListener(v -> hide(true));
        if (mCardView != null) {
            mCardView.setOnClickListener(v -> {});
        }

        if (mGridView != null) {
            int spanCount = calculateAdaptiveSpanCount();
            mGridView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
            mAdapter = new ExpandedGridAdapter(mActivityContext);
            mGridView.setAdapter(mAdapter);
            mGridView.setNestedScrollingEnabled(true);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int safeTop = mInsets.top + (int) (24 * getResources().getDisplayMetrics().density);
        int safeBottom = mInsets.bottom + (int) (24 * getResources().getDisplayMetrics().density);
        int availableHeight = Math.max(0, heightSize - safeTop - safeBottom);
        int maxCardHeight = (int) (availableHeight * 0.84f);

        if (mCardView != null) {
            int cardWidthSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(0, widthSize - (int) (32 * getResources().getDisplayMetrics().density)),
                    MeasureSpec.AT_MOST);
            int cardHeightSpec = MeasureSpec.makeMeasureSpec(maxCardHeight, MeasureSpec.AT_MOST);
            mCardView.measure(cardWidthSpec, cardHeightSpec);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    public void setOnExpandedSheetDismissListener(OnExpandedSheetDismissListener listener) {
        mDismissListener = listener;
    }

    private int calculateAdaptiveSpanCount() {
        if (mActivityContext instanceof Launcher) {
            DeviceProfile dp = ((Launcher) mActivityContext).getDeviceProfile();
            if (dp.getDeviceProperties().isLargeScreen()) {
                return dp.getDeviceProperties().isLandscape() ? 7 : 5;
            }
            if (dp.getDeviceProperties().isLandscape()) {
                return 6;
            }
        }
        return 4;
    }

    public void show(@NonNull AppCategoryGroup group, @Nullable Rect sourceBounds) {
        mSourceBounds = sourceBounds;
        if (mTitleView != null) {
            mTitleView.setText(group.getTitleRes());
        }
        if (mAdapter != null) {
            mAdapter.setApps(group.getApps());
        }

        if (mGridView != null) {
            int spanCount = calculateAdaptiveSpanCount();
            GridLayoutManager layoutManager = (GridLayoutManager) mGridView.getLayoutManager();
            if (layoutManager != null && layoutManager.getSpanCount() != spanCount) {
                layoutManager.setSpanCount(spanCount);
            }
            mGridView.scrollToPosition(0);
        }

        setVisibility(View.VISIBLE);
        setAlpha(0f);
        mIsAnimating = true;

        post(() -> {
            if (mCardView != null) {
                int cardWidth = mCardView.getWidth();
                int cardHeight = mCardView.getHeight();

                if (cardWidth > 0 && cardHeight > 0 && mSourceBounds != null && mSourceBounds.width() > 0 && mSourceBounds.height() > 0) {
                    float targetCenterX = mCardView.getX() + cardWidth / 2f;
                    float targetCenterY = mCardView.getY() + cardHeight / 2f;

                    float sourceCenterX = mSourceBounds.exactCenterX();
                    float sourceCenterY = mSourceBounds.exactCenterY();

                    mStartTranslationX = sourceCenterX - targetCenterX;
                    mStartTranslationY = sourceCenterY - targetCenterY;
                    mStartScaleX = (float) mSourceBounds.width() / cardWidth;
                    mStartScaleY = (float) mSourceBounds.height() / cardHeight;
                } else {
                    mStartTranslationX = 0f;
                    mStartTranslationY = 0f;
                    mStartScaleX = 0.85f;
                    mStartScaleY = 0.85f;
                }

                mCardView.setPivotX(cardWidth / 2f);
                mCardView.setPivotY(cardHeight / 2f);
                mCardView.setTranslationX(mStartTranslationX);
                mCardView.setTranslationY(mStartTranslationY);
                mCardView.setScaleX(mStartScaleX);
                mCardView.setScaleY(mStartScaleY);
                mCardView.setAlpha(0.6f);

                mCardView.animate()
                        .translationX(0f)
                        .translationY(0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(280)
                        .setInterpolator(EMPHASIZED)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mIsAnimating = false;
                            }
                        })
                        .start();
            }

            animate()
                    .alpha(1.0f)
                    .setDuration(280)
                    .setInterpolator(EMPHASIZED)
                    .setListener(null)
                    .start();
        });
    }

    public void hide(boolean animate) {
        if (getVisibility() != View.VISIBLE) {
            return;
        }

        if (!animate) {
            setVisibility(View.GONE);
            if (mDismissListener != null) {
                mDismissListener.onDismissFinished();
            }
            return;
        }

        long duration = 240;
        if (mDismissListener != null) {
            mDismissListener.onDismissStarted(duration);
        }

        mIsAnimating = true;
        if (mCardView != null) {
            mCardView.animate()
                    .translationX(mStartTranslationX)
                    .translationY(mStartTranslationY)
                    .scaleX(mStartScaleX)
                    .scaleY(mStartScaleY)
                    .alpha(0f)
                    .setDuration(duration)
                    .setInterpolator(EMPHASIZED)
                    .start();
        }

        animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(EMPHASIZED)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setVisibility(View.GONE);
                        mIsAnimating = false;
                        if (mDismissListener != null) {
                            mDismissListener.onDismissFinished();
                        }
                    }
                })
                .start();
    }

    public boolean isOpen() {
        return getVisibility() == View.VISIBLE;
    }

    public boolean isAnimating() {
        return mIsAnimating;
    }

    private static class ExpandedGridAdapter extends RecyclerView.Adapter<ExpandedGridAdapter.ViewHolder> {

        private final ActivityContext mActivityContext;
        private final List<AppInfo> mApps = new ArrayList<>();

        ExpandedGridAdapter(ActivityContext activityContext) {
            mActivityContext = activityContext;
        }

        void setApps(List<AppInfo> apps) {
            mApps.clear();
            if (apps != null) {
                mApps.addAll(apps);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            BubbleTextView icon = (BubbleTextView) inflater.inflate(
                    R.layout.all_apps_icon, parent, false);
            return new ViewHolder(icon);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppInfo app = mApps.get(position);
            holder.mIcon.reset();
            holder.mIcon.applyFromApplicationInfo(app);
            holder.mIcon.setOnClickListener(mActivityContext.getItemOnClickListener());
            if (mActivityContext instanceof Launcher) {
                Launcher launcher = (Launcher) mActivityContext;
                holder.mIcon.setOnLongClickListener(launcher.getAllAppsItemLongClickListener());
            }
        }

        @Override
        public int getItemCount() {
            return mApps.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final BubbleTextView mIcon;

            ViewHolder(BubbleTextView icon) {
                super(icon);
                mIcon = icon;
            }
        }
    }
}
