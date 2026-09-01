package com.android.launcher3.applibrary.view;

import static com.android.app.animation.Interpolators.EMPHASIZED;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.applibrary.model.AppCategoryGroup;
import com.android.launcher3.applibrary.model.AppLibraryModel;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.views.ActivityContext;

import java.util.List;

public class AppLibraryContainerView extends FrameLayout
        implements AppLibraryModel.AppLibraryModelListener,
        AppLibrarySearchBar.OnSearchListener,
        AppCategoryCardView.OnCategoryExpandListener,
        Insettable {

    private View mAppLibraryContent;
    private AppLibrarySearchBar mSearchBar;
    private RecyclerView mGridView;
    private RecyclerView mSearchResultsView;
    private AppCategoryExpandedSheet mExpandedSheet;

    private AppLibraryAdapter mCategoryAdapter;
    private AppLibrarySearchAdapter mSearchAdapter;
    private AppLibraryModel mModel;
    private final ActivityContext mActivityContext;
    private final Rect mInsets = new Rect();
    private ValueAnimator mBlurAnimator;

    public AppLibraryContainerView(Context context) {
        this(context, null);
    }

    public AppLibraryContainerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLibraryContainerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivityContext = ActivityContext.lookupContext(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAppLibraryContent = findViewById(R.id.app_library_content);
        mSearchBar = findViewById(R.id.app_library_search_bar);
        mGridView = findViewById(R.id.app_library_grid_view);
        mSearchResultsView = findViewById(R.id.app_library_search_results_view);
        mExpandedSheet = findViewById(R.id.app_library_expanded_sheet);

        if (mSearchBar != null) {
            mSearchBar.setOnSearchListener(this);
        }

        mCategoryAdapter = new AppLibraryAdapter(mActivityContext);
        mCategoryAdapter.setOnCategoryExpandListener(this);

        int spanCount = 2;
        if (mActivityContext instanceof Launcher) {
            DeviceProfile dp = ((Launcher) mActivityContext).getDeviceProfile();
            if (dp.isTablet || dp.getDeviceProperties().isLandscape()) {
                spanCount = 3;
            }
        }

        if (mGridView != null) {
            mGridView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
            mGridView.setAdapter(mCategoryAdapter);
        }

        mSearchAdapter = new AppLibrarySearchAdapter(mActivityContext);
        if (mSearchResultsView != null) {
            mSearchResultsView.setLayoutManager(new LinearLayoutManager(getContext()));
            mSearchResultsView.setAdapter(mSearchAdapter);
        }

        if (mExpandedSheet != null) {
            mExpandedSheet.setOnExpandedSheetDismissListener(new AppCategoryExpandedSheet.OnExpandedSheetDismissListener() {
                @Override
                public void onDismissStarted(long duration) {
                    applyBackgroundBlur(false, duration);
                }

                @Override
                public void onDismissFinished() {
                    if (mAppLibraryContent != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        mAppLibraryContent.setRenderEffect(null);
                    }
                }
            });
        }
    }

    public void setModel(AppLibraryModel model) {
        if (mModel != null) {
            mModel.removeListener(this);
        }
        mModel = model;
        if (mModel != null) {
            mModel.addListener(this);
        }
    }

    @Override
    public void onCategoriesUpdated(List<AppCategoryGroup> groups) {
        if (mCategoryAdapter != null) {
            mCategoryAdapter.setGroups(groups);
        }
    }

    @Override
    public void onAllAppsUpdated(List<AppInfo> allApps) {
        if (mSearchAdapter != null && mSearchBar != null && !mSearchBar.isSearching()) {
            mSearchAdapter.setApps(allApps);
        }
    }

    @Override
    public void onSearchQueryChanged(String query) {
        if (mModel != null && mSearchAdapter != null) {
            List<AppInfo> results = mModel.searchApps(query);
            mSearchAdapter.setApps(results);
        }
    }

    @Override
    public void onSearchStateChanged(boolean isSearching) {
        if (mExpandedSheet != null && mExpandedSheet.isOpen()) {
            mExpandedSheet.hide(false);
            if (mAppLibraryContent != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mAppLibraryContent.setRenderEffect(null);
            }
        }

        if (isSearching) {
            if (mSearchResultsView != null) {
                mSearchResultsView.setVisibility(View.VISIBLE);
                mSearchResultsView.setAlpha(0f);
                mSearchResultsView.animate().alpha(1f).setDuration(200).start();
            }
            if (mGridView != null) {
                mGridView.animate().alpha(0f).setDuration(150).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mGridView.setVisibility(View.GONE);
                    }
                }).start();
            }
        } else {
            if (mGridView != null) {
                mGridView.setVisibility(View.VISIBLE);
                mGridView.setAlpha(0f);
                mGridView.animate().alpha(1f).setDuration(200).start();
            }
            if (mSearchResultsView != null) {
                mSearchResultsView.animate().alpha(0f).setDuration(150).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mSearchResultsView.setVisibility(View.GONE);
                    }
                }).start();
            }
        }
    }

    @Override
    public void onExpandCategory(AppCategoryGroup group, View sourceCardView) {
        if (mExpandedSheet != null && group != null) {
            Rect sourceBounds = null;
            if (sourceCardView != null) {
                int[] pos = new int[2];
                sourceCardView.getLocationInWindow(pos);
                sourceBounds = new Rect(
                        pos[0],
                        pos[1],
                        pos[0] + sourceCardView.getWidth(),
                        pos[1] + sourceCardView.getHeight()
                );
            }
            applyBackgroundBlur(true, 280);
            mExpandedSheet.show(group, sourceBounds);
        }
    }

    private void applyBackgroundBlur(boolean enable, long duration) {
        if (mBlurAnimator != null) {
            mBlurAnimator.cancel();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || mAppLibraryContent == null) {
            return;
        }

        float startRadius = enable ? 0.1f : 25f;
        float endRadius = enable ? 25f : 0.1f;

        mBlurAnimator = ValueAnimator.ofFloat(startRadius, endRadius);
        mBlurAnimator.setDuration(duration);
        mBlurAnimator.setInterpolator(EMPHASIZED);
        mBlurAnimator.addUpdateListener(animation -> {
            if (mAppLibraryContent != null) {
                float r = (Float) animation.getAnimatedValue();
                mAppLibraryContent.setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP));
            }
        });
        mBlurAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!enable && mAppLibraryContent != null) {
                    mAppLibraryContent.setRenderEffect(null);
                }
            }
        });
        mBlurAnimator.start();
    }

    public boolean handleBack() {
        if (mExpandedSheet != null && mExpandedSheet.isOpen()) {
            mExpandedSheet.hide(true);
            return true;
        }
        if (mSearchBar != null && mSearchBar.isSearching()) {
            mSearchBar.resetSearch();
            return true;
        }
        return false;
    }

    public void reset() {
        if (mExpandedSheet != null) {
            mExpandedSheet.hide(false);
        }
        if (mAppLibraryContent != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mAppLibraryContent.setRenderEffect(null);
        }
        if (mSearchBar != null) {
            mSearchBar.resetSearch();
        }
        if (mGridView != null) {
            mGridView.scrollToPosition(0);
        }
    }

    public boolean shouldContainerScroll(MotionEvent ev) {
        if (mExpandedSheet != null && mExpandedSheet.isOpen()) {
            return false;
        }
        if (mSearchResultsView != null && mSearchResultsView.getVisibility() == View.VISIBLE) {
            return mSearchResultsView.canScrollVertically(-1);
        }
        if (mGridView != null) {
            return mGridView.canScrollVertically(-1);
        }
        return false;
    }

    public AppLibrarySearchBar getSearchBar() {
        return mSearchBar;
    }

    public RecyclerView getGridView() {
        return mGridView;
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);
        setPadding(insets.left, insets.top, insets.right, insets.bottom);
    }
}
