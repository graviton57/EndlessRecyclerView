package com.havrylyuk.endlessrecyclerview.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.havrylyuk.endlessrecyclerview.BuildConfig;
import com.havrylyuk.endlessrecyclerview.R;
import com.havrylyuk.endlessrecyclerview.model.Movie;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Igor Havrylyuk on 08.03.2017.
 */

public class EndlessRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM = 0;
    private static final int LOADING = 1;
    private List<Movie> entityList;
    private Context context;

    private boolean isLoadingAdded = false;
    private boolean retryPageLoad = false;

    private EndlessAdapterCallback callback;

    private String errorMsg ;

    public EndlessRecyclerViewAdapter(Context context, EndlessAdapterCallback callback) {
        this.context = context;
        this.entityList = new ArrayList<>();
        this.callback = callback;
    }

    public List<Movie> getData() {
        return entityList;
    }

    public void setData(List<Movie> movieResults) {
        this.entityList = movieResults;
    }

    public void add(Movie r) {
        entityList.add(r);
        notifyItemInserted(entityList.size() - 1);
    }

    public void addAll(List<Movie> moveResults) {
        for (Movie result : moveResults) {
            add(result);
        }
    }

    public void remove(Movie r) {
        int position = entityList.indexOf(r);
        if (position > RecyclerView.NO_POSITION) {
            entityList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        isLoadingAdded = false;
        while (getItemCount() > 0) {
            remove(getItem(0));
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public void addLoadingFooter() {
        isLoadingAdded = true;
        add(new Movie());
    }

    public void removeLoadingFooter() {
        isLoadingAdded = false;
        int position = entityList.size() - 1;
        Movie result = getItem(position);
        if (result != null) {
            entityList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Movie getItem(int position) {
        return entityList.get(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ITEM:
                viewHolder = getViewHolder(parent, inflater);
                break;
            case LOADING:
                View progressView = inflater.inflate(R.layout.item_progress, parent, false);
                viewHolder = new LoadingHolder(progressView);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM:
                final ItemHolder itemHolder = (ItemHolder) holder;
                String title = entityList.get(position).getTitle();
                itemHolder.item.setText(title);
                String releaseDate = entityList.get(position).getReleaseDate();
                itemHolder.subItem.setText(releaseDate);
                String flagUrl = BuildConfig.BASE_URL_IMG + entityList.get(position).getPosterPath();
                itemHolder.itemImage.setImageURI(Uri.parse(flagUrl));
                break;
            case LOADING:
                LoadingHolder loadingHolder = (LoadingHolder) holder;
                if (retryPageLoad) {
                    loadingHolder.errorLayout.setVisibility(View.VISIBLE);
                    loadingHolder.progressBar.setVisibility(View.GONE);
                    loadingHolder.errorTextView.setText(
                            errorMsg != null ?
                                    errorMsg :
                                    context.getString(R.string.error_msg_unknown));
                } else {
                    loadingHolder.errorLayout.setVisibility(View.GONE);
                    loadingHolder.progressBar.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @NonNull
    private RecyclerView.ViewHolder getViewHolder(ViewGroup parent, LayoutInflater inflater) {
        RecyclerView.ViewHolder viewHolder;
        View itemHolder = inflater.inflate(R.layout.recycler_item, parent, false);
        viewHolder = new ItemHolder(itemHolder);
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return entityList != null ? entityList.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == entityList.size() - 1 && isLoadingAdded) ? LOADING : ITEM;
    }

    public void showRetry(boolean show, @Nullable String errorMsg) {
        retryPageLoad = show;
        notifyItemChanged(entityList.size() - 1);
        if (errorMsg != null) this.errorMsg = errorMsg;
    }

    public class ItemHolder extends RecyclerView.ViewHolder {

        private  View view;
        private  SimpleDraweeView itemImage;
        private  TextView item;
        private  TextView subItem;

        public ItemHolder(View view) {
            super(view);
            this.view = view;
            this.itemImage = (SimpleDraweeView) view.findViewById(R.id.list_item_icon);
            this.item = (TextView) view.findViewById(R.id.list_item_name);
            this.subItem = (TextView) view.findViewById(R.id.list_item_sub_name);
        }
    }

    protected class LoadingHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ProgressBar progressBar;
        private ImageButton retryButton;
        private TextView errorTextView;
        private LinearLayout errorLayout;

        public LoadingHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.load_more_progress);
            retryButton = (ImageButton) itemView.findViewById(R.id.load_more_retry);
            errorTextView = (TextView) itemView.findViewById(R.id.load_more_error);
            errorLayout = (LinearLayout) itemView.findViewById(R.id.load_more_error_layout);
            retryButton.setOnClickListener(this);
            errorLayout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.load_more_retry:
                case R.id.load_more_error_layout:
                    showRetry(false, null);
                    callback.retryPageLoad();
                    break;
            }
        }
    }
}
