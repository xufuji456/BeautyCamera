package com.frank.beautyfilter.filter.factory;

import com.frank.beautyfilter.filter.advance.BeautyCoolFilter;
import com.frank.beautyfilter.filter.advance.BeautyHealthyFilter;
import com.frank.beautyfilter.filter.advance.BeautyImageAdjustFilter;
import com.frank.beautyfilter.filter.advance.BeautyLatteFilter;
import com.frank.beautyfilter.filter.advance.BeautyPixarFilter;
import com.frank.beautyfilter.filter.advance.BeautyRiseFilter;
import com.frank.beautyfilter.filter.advance.BeautyRomanceFilter;
import com.frank.beautyfilter.filter.advance.BeautySketchFilter;
import com.frank.beautyfilter.filter.advance.BeautySkinWhitenFilter;
import com.frank.beautyfilter.filter.advance.BeautySunriseFilter;
import com.frank.beautyfilter.filter.advance.BeautySweetFilter;
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
            case LATTE:
                return new BeautyLatteFilter();
            case PIXAR:
                return new BeautyPixarFilter();
            case RISE:
                return new BeautyRiseFilter();
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
            case SWEETS:
                return new BeautySweetFilter();
 /*           case AMARO:
                return new BeautyAmaroFilter();
            case ANTIQUE:
                return new BeautyAntiqueFilter();
            case BLACKCAT:
                return new BeautyBlackCatFilter();
            case BRANNAN:
                return new BeautyBrannanFilter();
            case BROOKLYN:
                return new BeautyBrooklynFilter();
            case CALM:
                return new BeautyCalmFilter();
            case CRAYON:
                return new BeautyCrayonFilter();
            case EARLYBIRD:
                return new BeautyEarlyBirdFilter();
            case EMERALD:
                return new BeautyEmeraldFilter();
            case EVERGREEN:
                return new BeautyEverGreenFilter();
            case FAIRYTALE:
                return new BeautyFairyTaleFilter();
            case FREUD:
                return new BeautyFreudFilter();
            case HEFE:
                return new BeautyHefeFilter();
            case HUDSON:
                return new BeautyHudsonFilter();
            case INKWELL:
                return new BeautyInkwellFilter();
            case KEVIN:
                return new BeautyKevinFilter();
            case LOMO:
                return new BeautyLomoFilter();
            case N1977:
                return new BeautyN1977Filter();
            case NASHVILLE:
                return new BeautyNashVilleFilter();
            case NOSTALGIA:
                return new BeautyNostalgiaFilter();
            case SAKURA:
                return new BeautySakuraFilter();
            case SIERRA:
                return new BeautySierraFilter();
            case SUNSET:
                return new BeautySunsetFilter();
            case SUTRO:
                return new BeautySutroFilter();
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
                return new BeautyXproiiFilter();*/
            default:
                return null;
        }
    }

    public static BeautyFilterType getFilterType() {
        return filterType;
    }

}
