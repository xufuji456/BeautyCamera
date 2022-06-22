package com.frank.beautyfilter.filter.factory;

import com.frank.beautyfilter.filter.advance.BeautyCoolFilter;
import com.frank.beautyfilter.filter.advance.BeautyImageAdjustFilter;
import com.frank.beautyfilter.filter.advance.BeautyLatteFilter;
import com.frank.beautyfilter.filter.advance.BeautyRomanceFilter;
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
            case AMARO:
                return new BeautyAmaroFilter();
            case ANTIQUE:
                return new BeautyAntiqueFilter();
            case BLACKCAT:
                return new BeautyBlackCatFilter();
            case BRANNAN:
                return new BeautyBrannanFilter();
            case BRIGHTNESS:
                return new GPUImageBrightnessFilter();
            case BROOKLYN:
                return new BeautyBrooklynFilter();
            case CALM:
                return new BeautyCalmFilter();
            case CONTRAST:
                return new GPUImageContrastFilter();
            case COOL:
                return new BeautyCoolFilter();
            case CRAYON:
                return new BeautyCrayonFilter();
            case EARLYBIRD:
                return new BeautyEarlyBirdFilter();
            case EMERALD:
                return new BeautyEmeraldFilter();
            case EVERGREEN:
                return new BeautyEverGreenFilter();
            case EXPOSURE:
                return new GPUImageExposureFilter();
            case FAIRYTALE:
                return new BeautyFairyTaleFilter();
            case FREUD:
                return new BeautyFreudFilter();
            case HEALTHY:
                return new BeautyHealthyFilter();
            case HEFE:
                return new BeautyHefeFilter();
            case HUDSON:
                return new BeautyHudsonFilter();
            case HUE:
                return new GPUImageHueFilter();
            case INKWELL:
                return new BeautyInkwellFilter();
            case IMAGE_ADJUST:
                return new BeautyImageAdjustFilter();
            case KEVIN:
                return new BeautyKevinFilter();
            case LATTE:
                return new BeautyLatteFilter();
            case LOMO:
                return new BeautyLomoFilter();
            case N1977:
                return new BeautyN1977Filter();
            case NASHVILLE:
                return new BeautyNashVilleFilter();
            case NOSTALGIA:
                return new BeautyNostalgiaFilter();
            case PIXAR:
                return new BeautyPixarFilter();
            case RISE:
                return new BeautyRiseFilter();
            case ROMANCE:
                return new BeautyRomanceFilter();
            case SAKURA:
                return new BeautySakuraFilter();
            case SATURATION:
                return new GPUImageSaturationFilter();
            case SHARPEN:
                return new GPUImageSharpenFilter();
            case SIERRA:
                return new BeautySierraFilter();
            case SKETCH:
                return new BeautySketchFilter();
            case SKINWHITEN:
                return new BeautySkinWhitenFilter();
            case SUNRISE:
                return new BeautySunriseFilter();
            case SUNSET:
                return new BeautySunsetFilter();
            case SUTRO:
                return new BeautySutroFilter();
            case SWEETS:
                return new BeautySweetFilter();
            case TENDER:
                return new BeautyTenderFilter();
            case TOASTER2:
                return new BeautyToasterFilter();
            case VALENCIA:
                return new BeautyValenciaFilter();
            case WALDEN:
                return new BeautyWaldenFilter();
            case WARM:
                return new BeautyWarmFilter();
            case WHITECAT:
                return new BeautyWhiteCatFilter();
            case XPROII:
                return new BeautyXproiiFilter();
            default:
                return null;
        }
    }

    public static BeautyFilterType getFilterType() {
        return filterType;
    }

}
