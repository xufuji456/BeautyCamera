package com.frank.camera.beauty.edit;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frank.beautyfilter.display.BeautyBaseDisplay;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.camera.R;
import com.frank.camera.beauty.adapter.FilterAdapter;
import com.frank.camera.beauty.bean.FilterInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FilterLayoutUtils {

    private int position;
    private final Context mContext;
    private FilterAdapter mAdapter;
    private ImageView btn_Favourite;
    private final BeautyBaseDisplay mBeautyDisplay;

    private List<FilterInfo> filterList;
    private List<FilterInfo> favouriteFilterList;

    private BeautyFilterType mFilterType = BeautyFilterType.NONE;


    public FilterLayoutUtils(Context context, BeautyBaseDisplay beautyDisplay) {
        mContext = context;
        mBeautyDisplay = beautyDisplay;
    }

    public void init() {
        btn_Favourite = ((Activity) mContext).findViewById(R.id.btn_camera_favourite);
        btn_Favourite.setOnClickListener(btn_Favourite_listener);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        RecyclerView mFilterListView = ((Activity) mContext).findViewById(R.id.filter_listView);
        mFilterListView.setLayoutManager(linearLayoutManager);

        mAdapter = new FilterAdapter(mContext);
        mFilterListView.setAdapter(mAdapter);
        initFilterInfos();
        mAdapter.setFilterList(filterList);
        mAdapter.setOnFilterChangeListener(onFilterChangeListener);
    }

    public void init(View view) {
        btn_Favourite = view.findViewById(R.id.btn_camera_favourite);
        btn_Favourite.setOnClickListener(btn_Favourite_listener);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        RecyclerView mFilterListView = view.findViewById(R.id.filter_listView);
        mFilterListView.setLayoutManager(linearLayoutManager);

        mAdapter = new FilterAdapter(mContext);
        mFilterListView.setAdapter(mAdapter);
        initFilterInfos();
        mAdapter.setFilterList(filterList);
        mAdapter.setOnFilterChangeListener(onFilterChangeListener);

        view.findViewById(R.id.btn_camera_closefilter).setVisibility(View.GONE);
    }

    private final FilterAdapter.OnFilterChangeListener onFilterChangeListener = new FilterAdapter.OnFilterChangeListener() {

        @Override
        public void onFilterChanged(BeautyFilterType filterType, int position) {
            BeautyFilterType type = filterList.get(position).getFilterType();
            FilterLayoutUtils.this.position = position;
            mBeautyDisplay.setFilter(filterType);
            mFilterType = filterType;
            if (position != 0)
                btn_Favourite.setVisibility(View.VISIBLE);
            else
                btn_Favourite.setVisibility(View.INVISIBLE);
            btn_Favourite.setSelected(filterList.get(position).isFavourite());
            if (position <= favouriteFilterList.size()) {
                for (int i = favouriteFilterList.size() + 2; i < filterList.size(); i++) {
                    if (filterList.get(i).getFilterType() == type) {
                        filterList.get(i).setSelected(true);
                        mAdapter.setLastSelected(i);
                        FilterLayoutUtils.this.position = i;
                        mAdapter.notifyItemChanged(i);
                    } else if (filterList.get(i).isSelected()) {
                        filterList.get(i).setSelected(false);
                        mAdapter.notifyItemChanged(i);
                    }
                }
            }
            for (int i = 1; i < favouriteFilterList.size() + 1; i++) {
                if (filterList.get(i).getFilterType() == type) {
                    filterList.get(i).setSelected(true);
                    mAdapter.notifyItemChanged(i);
                } else if (filterList.get(i).isSelected()) {
                    filterList.get(i).setSelected(false);
                    mAdapter.notifyItemChanged(i);
                }
            }
        }

    };

    private void initFilterInfos() {
        filterList = new ArrayList<>();
        //add original
        FilterInfo filterInfo = new FilterInfo();
        filterInfo.setFilterType(BeautyFilterType.NONE);
        filterInfo.setSelected(true);
        filterList.add(filterInfo);

        //add Favourite
        loadFavourite();
        for (int i = 0; i < favouriteFilterList.size(); i++) {
            filterInfo = new FilterInfo();
            filterInfo.setFilterType(favouriteFilterList.get(i).getFilterType());
            filterInfo.setFavourite(true);
            filterList.add(filterInfo);
        }
        //add Divider
        filterInfo = new FilterInfo();
        filterInfo.setFilterType(BeautyFilterType.NONE);
        filterList.add(filterInfo);

        //addAll
        for (int i = 0; i < BeautyFilterType.values().length; i++) {
            filterInfo = new FilterInfo();
            filterInfo.setFilterType(BeautyFilterType.values()[i]);
            for (int j = 0; j < favouriteFilterList.size(); j++) {
                if (BeautyFilterType.values()[i] == favouriteFilterList.get(j).getFilterType()) {
                    filterInfo.setFavourite(true);
                    break;
                }
            }
            filterList.add(filterInfo);
        }
    }

    private final OnClickListener btn_Favourite_listener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (position != 0 && filterList.get(position).getFilterType() != BeautyFilterType.NONE) {
                BeautyFilterType type = filterList.get(position).getFilterType();
                if (filterList.get(position).isFavourite()) {
                    btn_Favourite.setSelected(false);
                    filterList.get(position).setFavourite(false);
                    mAdapter.notifyItemChanged(position);
                    int i;
                    for (i = 0; i < favouriteFilterList.size(); i++) {
                        if (type == favouriteFilterList.get(i).getFilterType()) {
                            favouriteFilterList.remove(i);
                            filterList.remove(i + 1);
                            mAdapter.notifyItemRemoved(i + 1);
                            mAdapter.setLastSelected(mAdapter.getLastSelected() - 1);
                            break;
                        }
                    }
                    position--;
                    mAdapter.notifyItemRangeChanged(i + 1, filterList.size() - i - 1);
                } else {
                    btn_Favourite.setSelected(true);
                    filterList.get(position).setFavourite(true);
                    mAdapter.notifyItemChanged(position);

                    FilterInfo filterInfo = new FilterInfo();
                    filterInfo.setFilterType(type);
                    filterInfo.setSelected(true);
                    filterInfo.setFavourite(true);
                    filterList.add(favouriteFilterList.size() + 1, filterInfo);
                    position++;
                    mAdapter.notifyItemInserted(favouriteFilterList.size() + 1);
                    mAdapter.notifyItemRangeChanged(favouriteFilterList.size() + 1,
                            filterList.size() - favouriteFilterList.size() - 1);
                    favouriteFilterList.add(filterInfo);
                    mAdapter.setLastSelected(mAdapter.getLastSelected() + 1);
                }
                saveFavourite();
            }
        }
    };

    private void loadFavourite() {
        favouriteFilterList = new ArrayList<>();
        String[] typeList = mContext.getSharedPreferences("favourite_filter", Activity.MODE_PRIVATE)
                .getString("favourite_filter_list", "").split(",");
        for (int i = 0; i < typeList.length && !Objects.equals(typeList[i], ""); i++) {
            FilterInfo filterInfo = new FilterInfo();
            filterInfo.setFilterType(BeautyFilterType.valueOf(typeList[i]));
            filterInfo.setFavourite(true);
            favouriteFilterList.add(filterInfo);
        }
    }

    private void saveFavourite() {
        SharedPreferences shared = mContext.getSharedPreferences("favourite_filter", Activity.MODE_PRIVATE);
        Editor editor = shared.edit();
        editor.remove("favourite_filter_list");
        editor.apply();
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < favouriteFilterList.size(); i++) {
            str.append(favouriteFilterList.get(i).getFilterType()).append(",");
        }
        editor.putString("favourite_filter_list", str.toString());
        editor.apply();
    }

    public BeautyFilterType getFilterType() {
        return mFilterType;
    }
}
