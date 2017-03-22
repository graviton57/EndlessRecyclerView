package com.havrylyuk.endlessrecyclerview.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.havrylyuk.endlessrecyclerview.BuildConfig;
import com.havrylyuk.endlessrecyclerview.R;
import com.havrylyuk.endlessrecyclerview.adapter.EndlessRecyclerViewAdapter;
import com.havrylyuk.endlessrecyclerview.adapter.EndlessAdapterCallback;
import com.havrylyuk.endlessrecyclerview.adapter.EndlessScrollListener;
import com.havrylyuk.endlessrecyclerview.model.Movie;
import com.havrylyuk.endlessrecyclerview.model.TopRatedMovies;
import com.havrylyuk.endlessrecyclerview.service.ApiClient;
import com.havrylyuk.endlessrecyclerview.service.ApiService;
import com.havrylyuk.endlessrecyclerview.util.Utility;

import java.util.List;
import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int PAGE_START = 1;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    // limiting to 5 for this tutorial, since total pages in actual API is very large. Feel free to modify.
    private int TOTAL_PAGES = 5;
    private int currentPage = PAGE_START;
    private ApiService apiService;
    private EndlessRecyclerViewAdapter adapter;
    private ProgressBar progressBar;
    private TextView errorView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.main_progress);
        errorView = (TextView) findViewById(R.id.error_txt_cause);
        setupRecyclerView();
        apiService = ApiClient.getClient().create(ApiService.class);
        loadFirstPage();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new EndlessRecyclerViewAdapter(this, new EndlessAdapterCallback() {
            @Override
            public void retryPageLoad() {
                loadNextPage();
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new EndlessScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;
                loadNextPage();
            }

            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });
    }


    private void loadFirstPage() {
        Log.d(LOG_TAG, "loadFirstPage: ");
        hideErrorView();
        apiService.getTopRatedMovies(BuildConfig.API_KEY, "en_US", currentPage)
                .enqueue(new Callback<TopRatedMovies>() {
            @Override
            public void onResponse(Call<TopRatedMovies> call, Response<TopRatedMovies> response) {
                hideErrorView();
                List<Movie> results = response.body().getResults();
                progressBar.setVisibility(View.GONE);
                adapter.addAll(results);
                if (currentPage <= TOTAL_PAGES) adapter.addLoadingFooter();
                else isLastPage = true;
            }

            @Override
            public void onFailure(Call<TopRatedMovies> call, Throwable t) {
                t.printStackTrace();
                showErrorView(t);
            }
        });
    }

    private void loadNextPage() {
        Log.d(LOG_TAG, "loadNextPage: " + currentPage);
        apiService.getTopRatedMovies(BuildConfig.API_KEY, "en_US", currentPage)
                .enqueue(new Callback<TopRatedMovies>() {
            @Override
            public void onResponse(Call<TopRatedMovies> call, Response<TopRatedMovies> response) {
                adapter.removeLoadingFooter();
                isLoading = false;
                List<Movie> results =  response.body().getResults();
                adapter.addAll(results);
                if (currentPage != TOTAL_PAGES) adapter.addLoadingFooter();
                else isLastPage = true;
            }

            @Override
            public void onFailure(Call<TopRatedMovies> call, Throwable t) {
                t.printStackTrace();
                adapter.showRetry(true, fetchErrorMessage(t));
            }
        });
    }

    private void showErrorView(Throwable throwable) {
        if (errorView != null && errorView.getVisibility() == View.GONE) {
            errorView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            errorView.setText(fetchErrorMessage(throwable));
        }
    }

    private String fetchErrorMessage(Throwable throwable) {
        String errorMsg = getResources().getString(R.string.error_msg_unknown);
        if (!Utility.isNetworkConnected(this)) {
            errorMsg = getResources().getString(R.string.error_msg_no_internet);
        } else if (throwable instanceof TimeoutException) {
            errorMsg = getResources().getString(R.string.error_msg_timeout);
        }
        return errorMsg;
    }

    private void hideErrorView() {
        if (errorView.getVisibility() == View.VISIBLE) {
            errorView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }
}
