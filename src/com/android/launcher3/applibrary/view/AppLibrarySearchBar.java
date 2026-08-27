package com.android.launcher3.applibrary.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.R;

public class AppLibrarySearchBar extends LinearLayout {

    public interface OnSearchListener {
        void onSearchQueryChanged(String query);
        void onSearchStateChanged(boolean isSearching);
    }

    private EditText mSearchInput;
    private ImageButton mClearButton;
    private TextView mCancelButton;
    private OnSearchListener mListener;
    private boolean mIsSearching = false;

    public AppLibrarySearchBar(Context context) {
        this(context, null);
    }

    public AppLibrarySearchBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLibrarySearchBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSearchInput = findViewById(R.id.app_library_search_input);
        mClearButton = findViewById(R.id.app_library_search_clear);
        mCancelButton = findViewById(R.id.app_library_search_cancel);

        if (mSearchInput != null) {
            mSearchInput.setGravity(Gravity.CENTER);
            mSearchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString() : "";
                    if (mClearButton != null) {
                        mClearButton.setVisibility(TextUtils.isEmpty(query) ? View.GONE : View.VISIBLE);
                    }
                    if (mListener != null) {
                        mListener.onSearchQueryChanged(query);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            mSearchInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && !mIsSearching) {
                    setSearching(true);
                }
            });
            mSearchInput.setOnClickListener(v -> {
                if (!mIsSearching) {
                    setSearching(true);
                }
            });
        }

        if (mClearButton != null) {
            mClearButton.setOnClickListener(v -> {
                if (mSearchInput != null) {
                    mSearchInput.setText("");
                }
            });
        }

        if (mCancelButton != null) {
            mCancelButton.setOnClickListener(v -> resetSearch());
        }
    }

    public void setOnSearchListener(OnSearchListener listener) {
        mListener = listener;
    }

    public void setSearching(boolean searching) {
        if (mIsSearching == searching) {
            return;
        }
        mIsSearching = searching;
        if (mSearchInput != null) {
            mSearchInput.setGravity(searching ? (Gravity.CENTER_VERTICAL | Gravity.START) : Gravity.CENTER);
        }
        if (mCancelButton != null) {
            mCancelButton.setVisibility(searching ? View.VISIBLE : View.GONE);
        }
        if (mListener != null) {
            mListener.onSearchStateChanged(searching);
        }
    }

    public boolean isSearching() {
        return mIsSearching;
    }

    public void resetSearch() {
        if (mSearchInput != null) {
            mSearchInput.setText("");
            mSearchInput.clearFocus();
            mSearchInput.setGravity(Gravity.CENTER);
        }
        hideKeyboard();
        setSearching(false);
    }

    public void hideKeyboard() {
        if (mSearchInput != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchInput.getWindowToken(), 0);
            }
        }
    }

    public EditText getEditText() {
        return mSearchInput;
    }
}
