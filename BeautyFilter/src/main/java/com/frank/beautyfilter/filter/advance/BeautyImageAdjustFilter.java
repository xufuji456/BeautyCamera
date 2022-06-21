package com.frank.beautyfilter.filter.advance;

import com.frank.beautyfilter.filter.base.BeautyAdjustFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageBrightnessFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageContrastFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageExposureFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageHueFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageSaturationFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageSharpenFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xufulong
 * @date 2022/6/21 4:36 下午
 * @desc
 */
public class BeautyImageAdjustFilter extends BeautyAdjustFilter {

    public BeautyImageAdjustFilter() {
        super(initFilters());
    }

    private static List<GPUImageFilter> initFilters(){
        List<GPUImageFilter> filters = new ArrayList<>();
        filters.add(new GPUImageBrightnessFilter());
        filters.add(new GPUImageContrastFilter());
        filters.add(new GPUImageExposureFilter());
        filters.add(new GPUImageHueFilter());
        filters.add(new GPUImageSaturationFilter());
        filters.add(new GPUImageSharpenFilter());
        return filters;
    }

    public void setBrightness(final float range){
        ((GPUImageBrightnessFilter) filters.get(0)).setBrightness(range);
    }

    public void setContrast(final float range){
        ((GPUImageContrastFilter) filters.get(1)).setContrast(range);
    }

    public void setExposure(final float range){
        ((GPUImageExposureFilter) filters.get(2)).setExposure(range);
    }

    public void setHue(final float range){
        ((GPUImageHueFilter) filters.get(3)).setHue(range);
    }

    public void setSaturation(final float range){
        ((GPUImageSaturationFilter) filters.get(4)).setSaturation(range);
    }

    public void setSharpness(final float range){
        ((GPUImageSharpenFilter) filters.get(5)).setSharpness(range);
    }

}
