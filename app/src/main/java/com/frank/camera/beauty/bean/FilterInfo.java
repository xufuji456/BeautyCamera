package com.frank.camera.beauty.bean;

import com.frank.beautyfilter.filter.helper.BeautyFilterType;

/**
 * @author xufulong
 * @date 2022/6/24 9:05 上午
 * @desc
 */
public class FilterInfo {

    private boolean selected = false;
    private boolean favourite = false;
    private BeautyFilterType filterType;

    public FilterInfo() {}

    public FilterInfo(BeautyFilterType type) {
        this.filterType = type;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public BeautyFilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(BeautyFilterType filterType) {
        this.filterType = filterType;
    }
}
