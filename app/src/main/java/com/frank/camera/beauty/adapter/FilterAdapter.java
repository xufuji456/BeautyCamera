package com.frank.camera.beauty.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.camera.R;
import com.frank.camera.beauty.bean.FilterInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xufulong
 * @date 2022/6/24 9:02 上午
 * @desc
 */
public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterHolder> {

    private final Context mContext;
    private int lastSelected = 0;
    private final LayoutInflater layoutInflater;
    private List<FilterInfo> filterInfoList;

    public FilterAdapter(Context context) {
        this.mContext = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemCount() {
        return filterInfoList != null ? filterInfoList.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return filterInfoList.get(position).getFilterType().ordinal();
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull FilterHolder arg0, final int arg1) {
        if (filterInfoList.get(arg1).getFilterType() != BeautyFilterType.NONE) {
            arg0.thumbImage.setImageResource(FilterTypeHelper.FilterType2Thumb(filterInfoList.get(arg1).getFilterType()));
            arg0.filterName.setText(FilterTypeHelper.FilterType2Name(filterInfoList.get(arg1).getFilterType()));
            arg0.filterName.setBackgroundColor(mContext.getResources().getColor(
                    FilterTypeHelper.FilterType2Color(filterInfoList.get(arg1).getFilterType())));
            if (filterInfoList.get(arg1).isSelected()) {
                arg0.thumbSelected.setVisibility(View.VISIBLE);
                arg0.thumbSelected_bg.setBackgroundColor(mContext.getResources().getColor(
                        FilterTypeHelper.FilterType2Color(filterInfoList.get(arg1).getFilterType())));
                arg0.thumbSelected_bg.setAlpha(0.7f);
            } else
                arg0.thumbSelected.setVisibility(View.GONE);

            if (!filterInfoList.get(arg1).isFavourite() || arg1 == 0)
                arg0.filterFavourite.setVisibility(View.GONE);
            else
                arg0.filterFavourite.setVisibility(View.VISIBLE);

            arg0.filterRoot.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    if (onFilterChangeListener != null && filterInfoList.get(arg1).getFilterType() != BeautyFilterType.NONE
                            && arg1 != lastSelected
                            && !filterInfoList.get(arg1).isSelected()) {
                        filterInfoList.get(lastSelected).setSelected(false);
                        filterInfoList.get(arg1).setSelected(true);
                        notifyItemChanged(lastSelected);
                        notifyItemChanged(arg1);
                        lastSelected = arg1;

                        onFilterChangeListener.onFilterChanged(filterInfoList.get(arg1).getFilterType(), arg1);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public FilterHolder onCreateViewHolder(@NonNull ViewGroup arg0, int arg1) {
        if (arg1 != -1) {
            View view = layoutInflater.inflate(R.layout.filter_item_layout,
                    arg0, false);
            FilterHolder viewHolder = new FilterHolder(view);
            viewHolder.thumbImage = view.findViewById(R.id.filter_thumb_image);
            viewHolder.filterName = view.findViewById(R.id.filter_thumb_name);
            viewHolder.filterRoot = view.findViewById(R.id.filter_root);
            viewHolder.thumbSelected = view.findViewById(R.id.filter_thumb_selected);
            viewHolder.filterFavourite = view.findViewById(R.id.filter_thumb_favorite_layout);
            viewHolder.thumbSelected_bg = view.findViewById(R.id.filter_thumb_selected_bg);
            return viewHolder;
        } else {
            View view = layoutInflater.inflate(R.layout.filter_division_layout,
                    arg0, false);
            return new FilterHolder(view);
        }
    }

    public void setLastSelected(int arg) {
        lastSelected = arg;
    }

    public int getLastSelected() {
        return lastSelected;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFilterList(List<FilterInfo> filterList) {
        this.filterInfoList = filterList;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFilterTypes(BeautyFilterType[] filterTypes) {
        if (filterTypes == null || filterTypes.length == 0)
            return;
        List<FilterInfo> list = new ArrayList<>();
        for (BeautyFilterType filterType : filterTypes) {
            list.add(new FilterInfo(filterType));
        }
        this.filterInfoList = list;
        notifyDataSetChanged();
    }

    static class FilterHolder extends RecyclerView.ViewHolder {
        ImageView thumbImage;
        TextView filterName;
        FrameLayout thumbSelected;
        FrameLayout filterRoot;
        FrameLayout filterFavourite;
        View thumbSelected_bg;

        public FilterHolder(View itemView) {
            super(itemView);
        }
    }

    public interface OnFilterChangeListener {
        void onFilterChanged(BeautyFilterType filterType, int position);
    }

    private OnFilterChangeListener onFilterChangeListener;

    public void setOnFilterChangeListener(OnFilterChangeListener onFilterChangeListener) {
        this.onFilterChangeListener = onFilterChangeListener;
    }

}
