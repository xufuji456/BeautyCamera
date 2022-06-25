package com.frank.beautyfilter.filter.factory;

import com.frank.beautyfilter.filter.advance.BeautyAntiqueFilter;
import com.frank.beautyfilter.filter.advance.BeautyCoolFilter;
import com.frank.beautyfilter.filter.advance.BeautyCrayonFilter;
import com.frank.beautyfilter.filter.advance.BeautyFairytaleFilter;
import com.frank.beautyfilter.filter.advance.BeautyHealthyFilter;
import com.frank.beautyfilter.filter.advance.BeautyHudsonFilter;
import com.frank.beautyfilter.filter.advance.BeautyImageAdjustFilter;
import com.frank.beautyfilter.filter.advance.BeautyInkwellFilter;
import com.frank.beautyfilter.filter.advance.BeautyPixarFilter;
import com.frank.beautyfilter.filter.advance.BeautyRomanceFilter;
import com.frank.beautyfilter.filter.advance.BeautySketchFilter;
import com.frank.beautyfilter.filter.advance.BeautySkinWhitenFilter;
import com.frank.beautyfilter.filter.advance.BeautySunriseFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageBrightnessFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageContrastFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageExposureFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageHueFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageSaturationFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageSharpenFilter;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;

/**
 * @author xufulong
 * @date 2022/6/18 6:14 下午
 * @desc
 */
public class BeautyFilterFactory {

    private static BeautyFilterType filterType = BeautyFilterType.NONE;

    public static GPUImageFilter getFilter(BeautyFilterType type) {
        filterType = type;
        switch (type) {
            case BRIGHTNESS:
                return new GPUImageBrightnessFilter();
            case CONTRAST:
                return new GPUImageContrastFilter();
            case COOL:
                return new BeautyCoolFilter();
            case EXPOSURE:
                return new GPUImageExposureFilter();
            case HEALTHY:
                return new BeautyHealthyFilter();
            case HUE:
                return new GPUImageHueFilter();
            case IMAGE_ADJUST:
                return new BeautyImageAdjustFilter();
            case PIXAR:
                return new BeautyPixarFilter();
            case ROMANCE:
                return new BeautyRomanceFilter();
            case SATURATION:
                return new GPUImageSaturationFilter();
            case SHARPEN:
                return new GPUImageSharpenFilter();
            case SKETCH:
                return new BeautySketchFilter();
            case SKINWHITEN:
                return new BeautySkinWhitenFilter();
            case SUNRISE:
                return new BeautySunriseFilter();
            case ANTIQUE:
                return new BeautyAntiqueFilter();
            case CRAYON:
                return new BeautyCrayonFilter();
            case FAIRYTALE:
                return new BeautyFairytaleFilter();
            case HUDSON:
                return new BeautyHudsonFilter();
            case INKWELL:
                return new BeautyInkwellFilter();
            case SIERRA:
                return new BeautySierraFilter();
            case TENDER:
                return new BeautyTenderFilter();
            case WARM:
                return new BeautyWarmFilter();
            case WHITECAT:
                return new BeautyWhiteCatFilter();
            default:
                return null;
        }
    }

    public static BeautyFilterType getFilterType() {
        return filterType;
    }

}
