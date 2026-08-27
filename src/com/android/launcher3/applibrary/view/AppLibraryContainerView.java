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
import android.view.inputmethod.InputMethodManager;
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

    private static final float CATEGORY_BLUR_RADIUS = 20f;

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

    private float mDownX;
    private float mDownY;
    private boolean mPullToSearchEligible = false;

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
            if (dp.getDeviceProperties().isLargeScreen() || dp.getDeviceProperties().isLandscape()) {
                spanCount = 3;
            }
        }

        if (mGridView != null) {
            mGridView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
            mGridView.setAdapter(mCategoryAdapter);
            mGridView.setNestedScrollingEnabled(true);
        }

        mSearchAdapter = new AppLibrarySearchAdapter(mActivityContext);
        if (mSearchResultsView != null) {
            mSearchResultsView.setLayoutManager(new LinearLayoutManager(getContext()));
            mSearchResultsView.setAdapter(mSearchAdapter);
            mSearchResultsView.setNestedScrollingEnabled(true);
            mSearchResultsView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING && mSearchBar != null) {
                        mSearchBar.hideKeyboard();
                    }
                }
            });
        }

        if (mExpandedSheet != null) {
            mExpandedSheet.setOnExpandedSheetDismissListener(new AppCategoryExpandedSheet.OnExpandedSheetDismissListener() {
                @Override
                public void onDismissStarted(long duration) {
                    applyCategoryBackgroundBlur(false, duration);
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mDownX = ev.getX();
            mDownY = ev.getY();
            if (mExpandedSheet != null && mExpandedSheet.isOpen()) {
                mPullToSearchEligible = false;
                getParent().requestDisallowInterceptTouchEvent(true);
            } else if (mSearchBar != null && mSearchBar.isSearching()) {
                mPullToSearchEligible = false;
                getParent().requestDisallowInterceptTouchEvent(true);
            } else if (mGridView != null) {
                mPullToSearchEligible = !mGridView.canScrollVertically(-1);
            } else {
                mPullToSearchEligible = false;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = ev.getX() - mDownX;
            float dy = ev.getY() - mDownY;
            float absDx = Math.abs(dx);
            float absDy = Math.abs(dy);

            if (mExpandedSheet != null && mExpandedSheet.isOpen()) {
                getParent().requestDisallowInterceptTouchEvent(true);
            } else if (mSearchBar != null && mSearchBar.isSearching()) {
                getParent().requestDisallowInterceptTouchEvent(true);
                if (dy > 20 && mSearchBar != null) {
                    mSearchBar.hideKeyboard();
                }
            } else {
                if (absDy > absDx && absDy > 10) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                if (mPullToSearchEligible && dy > 12 && mAppLibraryContent != null) {
                    float pullDistance = dy - 12;
                    float density = getResources().getDisplayMetrics().density;
                    float maxTranslation = 50f * density;
                    float translationY = Math.min(pullDistance * 0.4f, maxTranslation);
                    mAppLibraryContent.setTranslationY(translationY);

                    float threshold = 75f * density;
                    if (pullDistance > threshold) {
                        mPullToSearchEligible = false;
                        mAppLibraryContent.animate().translationY(0f).setDuration(160).start();
                        if (mSearchBar != null) {
                            mSearchBar.setSearching(true);
                            if (mSearchBar.getEditText() != null) {
                                mSearchBar.getEditText().requestFocus();
                                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                                        Context.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.showSoftInput(mSearchBar.getEditText(), InputMethodManager.SHOW_IMPLICIT);
                                }
                            }
                        }
                    }
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mAppLibraryContent != null && mAppLibraryContent.getTranslationY() > 0) {
                mAppLibraryContent.animate().translationY(0f).setDuration(220).setInterpolator(EMPHASIZED).start();
            }
            mPullToSearchEligible = false;
        }
        return super.dispatchTouchEvent(ev);
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
            if (mSearchResultsView != null) {
                mSearchResultsView.scrollToPosition(0);
            }
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

        if (mSearchResultsView != null) {
            mSearchResultsView.animate().cancel();
        }
        if (mGridView != null) {
            mGridView.animate().cancel();
        }

        if (isSearching) {
            if (mSearchResultsView != null) {
                mSearchResultsView.setVisibility(View.VISIBLE);
                mSearchResultsView.setAlpha(0f);
                mSearchResultsView.animate().alpha(1f).setDuration(180).setListener(null).start();
            }
            if (mGridView != null) {
                mGridView.animate().alpha(0f).setDuration(140).setListener(new AnimatorListenerAdapter() {
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
                mGridView.animate().alpha(1f).setDuration(180).setListener(null).start();
            }
            if (mSearchResultsView != null) {
                mSearchResultsView.animate().alpha(0f).setDuration(140).setListener(new AnimatorListenerAdapter() {
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
            Rect sourceBounds = new Rect();
            if (sourceCardView != null) {
                sourceCardView.getGlobalVisibleRect(sourceBounds);
                int[] sheetPos = new int[2];
                mExpandedSheet.getLocationOnScreen(sheetPos);
                sourceBounds.offset(-sheetPos[0], -sheetPos[1]);
            }
            applyCategoryBackgroundBlur(true, 280);
            mExpandedSheet.show(group, sourceBounds);
        }
    }

    private void applyCategoryBackgroundBlur(boolean enable, long duration) {
        if (mBlurAnimator != null) {
            mBlurAnimator.cancel();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || mAppLibraryContent == null) {
            return;
        }

        float startRadius = enable ? 0.1f : CATEGORY_BLUR_RADIUS;
        float endRadius = enable ? CATEGORY_BLUR_RADIUS : 0.1f;

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
        if (mBlurAnimator != null) {
            mBlurAnimator.cancel();
        }
        if (mExpandedSheet != null) {
            mExpandedSheet.hide(false);
        }
        if (mAppLibraryContent != null) {
            mAppLibraryContent.setTranslationY(0f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mAppLibraryContent.setRenderEffect(null);
            }
        }
        if (mSearchBar != null) {
            mSearchBar.resetSearch();
        }
        if (mSearchResultsView != null) {
            mSearchResultsView.animate().cancel();
            mSearchResultsView.setVisibility(View.GONE);
            mSearchResultsView.setAlpha(0f);
        }
        if (mGridView != null) {
            mGridView.animate().cancel();
            mGridView.setVisibility(View.VISIBLE);
            mGridView.setAlpha(1f);
            mGridView.scrollToPosition(0);
        }
        mPullToSearchEligible = false;
    }

    public boolean shouldContainerScroll(MotionEvent ev) {
        return false;
    }

    public AppLibrarySearchBar getSearchBar() {
        return mSearchBar;
    }

    public RecyclerView getGridView() {
        return mGridView;
    }

    public RecyclerView getSearchResultsView() {
        return mSearchResultsView;
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);
        setPadding(insets.left, insets.top, insets.right, insets.bottom);
        if (mExpandedSheet != null) {
            mExpandedSheet.setInsets(insets);
        }
    }
}

