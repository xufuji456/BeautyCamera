package com.frank.camera.beauty.adapter;

import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.camera.R;


public class FilterTypeHelper {

    public static int FilterType2Color(BeautyFilterType filterType) {
        switch (filterType) {
            case NONE:
                return R.color.filter_color_grey_light;
            case WHITE_CAT:
            case SUNRISE:
                return R.color.filter_color_brown_light;
            case COOL:
                return R.color.filter_color_blue_dark;
            case FAIRYTALE:
                return R.color.filter_color_blue;
            case ROMANCE:
            case WARM:
                return R.color.filter_color_pink;
            case HUDSON:
            case INKWELL:
            case PIXAR:
            case SIERRA:
                return R.color.filter_color_brown_dark;
            case ANTIQUE:
                return R.color.filter_color_green_dark;
            case BEAUTY:
            case SKIN_WHITEN:
            case HEALTHY:
                return R.color.filter_color_red;
            case TENDER:
                return R.color.filter_color_brown;
            case BREATH_CIRCLE:
                return R.color.filter_color_pink;
            default:
                return R.color.filter_color_grey_light;
        }
    }

    public static int FilterType2Thumb(BeautyFilterType filterType) {
        switch (filterType) {
            case NONE:
                return R.drawable.filter_thumb_original;
            case WHITE_CAT:
                return R.drawable.filter_thumb_whitecat;
            case ROMANCE:
                return R.drawable.filter_thumb_romance;
            case HUDSON:
                return R.drawable.filter_thumb_hudson;
            case INKWELL:
                return R.drawable.filter_thumb_inkwell;
            case PIXAR:
                return R.drawable.filter_thumb_piaxr;
            case SIERRA:
                return R.drawable.filter_thumb_sierra;
            case ANTIQUE:
                return R.drawable.filter_thumb_antique;
            case BEAUTY:
            case SKIN_WHITEN:
                return R.drawable.filter_thumb_beauty;
            case COOL:
                return R.drawable.filter_thumb_cool;
            case FAIRYTALE:
                return R.drawable.filter_thumb_fairytale;
            case HEALTHY:
                return R.drawable.filter_thumb_healthy;
            case WARM:
                return R.drawable.filter_thumb_warm;
            case SUNRISE:
                return R.drawable.filter_thumb_sunrise;
            case CRAYON:
                return R.drawable.filter_thumb_crayon;
            case SKETCH:
                return R.drawable.filter_thumb_sketch;
            default:
                return R.drawable.filter_thumb_original;
        }
    }

    public static int FilterType2Name(BeautyFilterType filterType) {
        switch (filterType) {
            case NONE:
                return R.string.filter_none;
            case WHITE_CAT:
                return R.string.filter_whitecat;
            case ROMANCE:
                return R.string.filter_romance;
            case HUDSON:
                return R.string.filter_hudson;
            case INKWELL:
                return R.string.filter_inkwell;
            case PIXAR:
                return R.string.filter_pixar;
            case SIERRA:
                return R.string.filter_sierra;
            case ANTIQUE:
                return R.string.filter_antique;
            case BEAUTY:
                return R.string.filter_beauty;
            case COOL:
                return R.string.filter_cool;
            case FAIRYTALE:
                return R.string.filter_fairytale;
            case HEALTHY:
                return R.string.filter_healthy;
            case TENDER:
                return R.string.filter_tender;
            case WARM:
                return R.string.filter_warm;
            case SUNRISE:
                return R.string.filter_sunrise;
            case SKIN_WHITEN:
                return R.string.filter_skinwhiten;
            case CRAYON:
                return R.string.filter_crayon;
            case SKETCH:
                return R.string.filter_sketch;
            case BREATH_CIRCLE:
                    return R.string.filter_breath_circle;
            default:
                return R.string.filter_none;
        }
    }
}
