package com.android.launcher3.applibrary.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.applibrary.model.AppCategoryGroup;
import com.android.launcher3.icons.FastBitmapDrawable;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.views.ActivityContext;

import java.util.List;

public class AppCategoryCardView extends LinearLayout {

    public interface OnCategoryExpandListener {
        void onExpandCategory(AppCategoryGroup group, View sourceCardView);
    }

    private TextView mTitleView;
    private View mCardContent;
    private BubbleTextView mSlot0;
    private BubbleTextView mSlot1;
    private BubbleTextView mSlot2;
    private FrameLayout mSlot3Container;
    private BubbleTextView mSlot3Single;
    private LinearLayout mSlot3MiniCluster;
    private ImageView mMiniIcon0;
    private ImageView mMiniIcon1;
    private ImageView mMiniIcon2;
    private ImageView mMiniIcon3;

    private AppCategoryGroup mGroup;
    private OnCategoryExpandListener mExpandListener;

    public AppCategoryCardView(Context context) {
        this(context, null);
    }

    public AppCategoryCardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppCategoryCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitleView = findViewById(R.id.category_title);
        mCardContent = findViewById(R.id.category_card_content);
        mSlot0 = findViewById(R.id.card_slot_0);
        mSlot1 = findViewById(R.id.card_slot_1);
        mSlot2 = findViewById(R.id.card_slot_2);
        mSlot3Container = findViewById(R.id.card_slot_3_container);
        mSlot3Single = findViewById(R.id.card_slot_3_single);
        mSlot3MiniCluster = findViewById(R.id.card_slot_3_mini_cluster);
        mMiniIcon0 = findViewById(R.id.mini_icon_0);
        mMiniIcon1 = findViewById(R.id.mini_icon_1);
        mMiniIcon2 = findViewById(R.id.mini_icon_2);
        mMiniIcon3 = findViewById(R.id.mini_icon_3);

        OnClickListener expandClick = v -> {
            if (mExpandListener != null && mGroup != null) {
                mExpandListener.onExpandCategory(mGroup, this);
            }
        };

        if (mTitleView != null) {
            mTitleView.setOnClickListener(expandClick);
        }
        if (mSlot3MiniCluster != null) {
            mSlot3MiniCluster.setOnClickListener(expandClick);
        }
        if (mCardContent != null) {
            mCardContent.setOnClickListener(expandClick);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mCardContent != null) {
            int cardWidth = mCardContent.getMeasuredWidth();
            int cardHeightSpec = MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY);
            int cardWidthSpec = MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY);
            mCardContent.measure(cardWidthSpec, cardHeightSpec);
            int totalHeight = mCardContent.getMeasuredHeight() + getPaddingTop() + getPaddingBottom();
            if (mTitleView != null && mTitleView.getVisibility() != GONE) {
                totalHeight += mTitleView.getMeasuredHeight();
                MarginLayoutParams lp = (MarginLayoutParams) mTitleView.getLayoutParams();
                if (lp != null) {
                    totalHeight += lp.topMargin + lp.bottomMargin;
                }
            }
            setMeasuredDimension(getMeasuredWidth(), totalHeight);
        }
    }

    public void setOnCategoryExpandListener(OnCategoryExpandListener listener) {
        mExpandListener = listener;
    }

    public void bindGroup(AppCategoryGroup group, ActivityContext activityContext) {
        mGroup = group;
        if (group == null) {
            return;
        }

        if (mTitleView != null) {
            mTitleView.setText(group.getTitleRes());
        }

        List<AppInfo> apps = group.getApps();
        int count = apps.size();

        bindSlot(mSlot0, count > 0 ? apps.get(0) : null, activityContext);
        bindSlot(mSlot1, count > 1 ? apps.get(1) : null, activityContext);
        bindSlot(mSlot2, count > 2 ? apps.get(2) : null, activityContext);

        if (count <= 4) {
            if (mSlot3MiniCluster != null) {
                mSlot3MiniCluster.setVisibility(View.GONE);
            }
            bindSlot(mSlot3Single, count > 3 ? apps.get(3) : null, activityContext);
        } else {
            if (mSlot3Single != null) {
                mSlot3Single.setVisibility(View.GONE);
            }
            if (mSlot3MiniCluster != null) {
                mSlot3MiniCluster.setVisibility(View.VISIBLE);
                bindMiniIcon(mMiniIcon0, apps.get(3));
                bindMiniIcon(mMiniIcon1, count > 4 ? apps.get(4) : null);
                bindMiniIcon(mMiniIcon2, count > 5 ? apps.get(5) : null);
                bindMiniIcon(mMiniIcon3, count > 6 ? apps.get(6) : null);
            }
        }
    }

    private void bindSlot(BubbleTextView view, AppInfo app, ActivityContext activityContext) {
        if (view == null) {
            return;
        }
        if (app != null) {
            view.setVisibility(View.VISIBLE);
            view.reset();
            view.applyFromApplicationInfo(app);
            view.setOnClickListener(activityContext.getItemOnClickListener());
            if (activityContext instanceof Launcher) {
                Launcher launcher = (Launcher) activityContext;
                view.setOnLongClickListener(launcher.getAllAppsItemLongClickListener());
            }
        } else {
            view.setVisibility(View.INVISIBLE);
        }
    }

    private void bindMiniIcon(ImageView view, AppInfo app) {
        if (view == null) {
            return;
        }
        if (app != null && app.bitmap != null) {
            view.setVisibility(View.VISIBLE);
            FastBitmapDrawable drawable = app.newIcon(getContext());
            view.setImageDrawable(drawable);
        } else {
            view.setVisibility(View.INVISIBLE);
        }
    }
}
