package com.frank.beautyfilter.filter.helper;

import com.frank.beautyfilter.filter.advance.BeautyImageAdjustFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageBrightnessFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageContrastFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageExposureFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageHueFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageSaturationFilter;
import com.frank.beautyfilter.filter.base.gpuimage.GPUImageSharpenFilter;

/**
 * @author xufulong
 * @date 2022/6/21 4:00 下午
 * @desc
 */
public class BeautyFilterAdjuster {

    private final Adjuster<? extends GPUImageFilter> adjuster;

    public BeautyFilterAdjuster(GPUImageFilter filter) {
        if (filter instanceof GPUImageBrightnessFilter) {
            adjuster = new BrightnessAdjuster().filter(filter);
        } else if (filter instanceof GPUImageContrastFilter) {
            adjuster = new ContrastAdjuster().filter(filter);
        } else if (filter instanceof GPUImageExposureFilter) {
            adjuster = new ExposureAdjuster().filter(filter);
        } else if (filter instanceof GPUImageHueFilter) {
            adjuster = new HueAdjuster().filter(filter);
        } else if (filter instanceof GPUImageSaturationFilter) {
            adjuster = new SaturationAdjuster().filter(filter);
        } else if (filter instanceof GPUImageSharpenFilter) {
            adjuster = new SharpenAdjuster().filter(filter);
        }  else if (filter instanceof BeautyImageAdjustFilter) {
            adjuster = new ImageAdjustAdjuster().filter(filter);
        } else {
            adjuster = null;
        }
    }

    public boolean canAdjust() {
        return adjuster !=null;
    }

    public void adjust(int percent) {
        if (adjuster != null) {
            adjuster.adjust(percent);
        }
    }

    public void adjust(int percent, BeautyFilterType type) {
        if (adjuster instanceof ImageAdjustAdjuster) {
            ((ImageAdjustAdjuster) adjuster).adjust(percent, type);
        }
    }


    private abstract static class Adjuster<T extends GPUImageFilter> {
        private T filter;

        public Adjuster<T> filter(GPUImageFilter filter) {
            this.filter = (T) filter;
            return this;
        }

        public T getFilter() {
            return filter;
        }

        public abstract void adjust(int percent);

        protected float range(int percent, float start, float end) {
            return (end - start) * percent / 100.0f + start;
        }
    }

    private static class BrightnessAdjuster extends Adjuster<GPUImageBrightnessFilter> {
        @Override
        public void adjust(int percent) {
            getFilter().setBrightness(range(percent, -0.5f, 0.5f));
        }
    }

    private static class ContrastAdjuster extends Adjuster<GPUImageContrastFilter> {
        @Override
        public void adjust(int percent) {
            getFilter().setContrast(range(percent, 0.0f, 4.0f));
        }
    }

    private static class ExposureAdjuster extends Adjuster<GPUImageExposureFilter> {
        @Override
        public void adjust(int percent) {
            getFilter().setExposure(range(percent, -2.0f, 2.0f));
        }
    }

    private static class HueAdjuster extends Adjuster<GPUImageHueFilter> {
        @Override
        public void adjust(int percent) {
            getFilter().setHue(range(percent, 0.0f, 360.0f));
        }
    }

    private static class SaturationAdjuster extends Adjuster<GPUImageSaturationFilter> {
        @Override
        public void adjust(int percent) {
            getFilter().setSaturation(range(percent, 0.0f, 2.0f));
        }
    }

    private static class SharpenAdjuster extends Adjuster<GPUImageSharpenFilter> {
        @Override
        public void adjust(int percent) {
            getFilter().setSharpness(range(percent, -4.0f, 4.0f));
        }
    }

    private static class ImageAdjustAdjuster extends Adjuster<BeautyImageAdjustFilter> {
        @Override
        public void adjust(int percent) {

        }

        public void adjust(int percent, BeautyFilterType type) {
            switch (type) {
                case BRIGHTNESS:
                    getFilter().setBrightness(range(percent, -0.5f, 0.5f));
                    break;
                case CONTRAST:
                    getFilter().setContrast(range(percent, 0.0f, 4.0f));
                    break;
                case EXPOSURE:
                    getFilter().setExposure(range(percent, -2.0f, 2.0f));
                    break;
                case HUE:
                    getFilter().setHue(range(percent, 0.0f, 360.0f));
                    break;
                case SATURATION:
                    getFilter().setSaturation(range(percent, 0.0f, 2.0f));
                    break;
                case SHARPEN:
                    getFilter().setSharpness(range(percent, -4.0f, 4.0f));
                    break;
                default:
                    break;
            }
        }
    }

}
