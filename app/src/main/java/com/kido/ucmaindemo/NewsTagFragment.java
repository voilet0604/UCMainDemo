package com.kido.ucmaindemo;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kido.ucmaindemo.adapter.RecyclerViewAdapter;
import com.kido.ucmaindemo.widget.refresh.KSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 新闻标签对应的fragment
 *
 * @author Kido
 */
public class NewsTagFragment extends Fragment {
    private static final String KEY_TITLE = "title";

    private RecyclerView mRecyclerView;
    private KSwipeRefreshLayout mRefreshLayout;

    private String mTitle = "";
    private List<KSwipeRefreshLayout.OnRefreshListener> mOnRefreshListeners = new ArrayList<>();


    public static NewsTagFragment newInstance() {
        return newInstance("");
    }

    public static NewsTagFragment newInstance(String title) {
        Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        NewsTagFragment fragment = new NewsTagFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_news_tag, container, false);
        initView(rootView);
        initData();
        return rootView;
    }

    private void initView(View rootView) {
        mTitle = getArguments().getString(KEY_TITLE);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        mRefreshLayout = (KSwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRefreshLayout.setOnRefreshListener(new KSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                triggerOnRefresh();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.setRefreshing(false);
                    }
                }, 1500); //模拟下拉耗时
            }

            @Override
            public void onTerminal() {
                triggerOnTerminal();
            }
        });
    }

    public KSwipeRefreshLayout getRereshLayout() {
        return mRefreshLayout;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void setRefreshEnable(boolean refreshEnable) {
        if (mRefreshLayout != null) {
            mRefreshLayout.setEnabled(refreshEnable);
        }
    }

    public void scrollToTop() {
        if (mRecyclerView != null) {
            mRecyclerView.scrollToPosition(0);
        }
    }

    public void addOnRefreshListener(KSwipeRefreshLayout.OnRefreshListener listener) {
        mOnRefreshListeners.add(listener);
    }

    private void triggerOnRefresh() {
        for (KSwipeRefreshLayout.OnRefreshListener listener : mOnRefreshListeners) {
            if (listener != null) {
                listener.onRefresh();
            }
        }
    }

    private void triggerOnTerminal() {
        for (KSwipeRefreshLayout.OnRefreshListener listener : mOnRefreshListeners) {
            if (listener != null) {
                listener.onTerminal();
            }
        }
    }

    private void initData() {
        ArrayList<String> dataList = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            dataList.add("This is the title. (" + mTitle + i + ")");
        }
        mRecyclerView.setAdapter(new RecyclerViewAdapter(dataList));
    }

}