package com.frank.camera.beauty.edit.adjust;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.frank.beautyfilter.display.BeautyImageDisplay;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.camera.R;
import com.frank.camera.beauty.edit.ImageEditFragment;
import com.frank.camera.beauty.widget.TwoLineSeekBar;

@SuppressLint("ValidFragment")
public class ImageEditAdjustView extends ImageEditFragment {

    private TextView mVal;
    private ImageView mLabel;
    private float hue = 0.0f;
    private RadioGroup mRadioGroup;
    private TwoLineSeekBar mSeekBar;
    private LinearLayout mLinearLayout;

    private float exposure   = 0.0f;
    private float contrast   = -50.0f;
    private float sharpness  = 0.0f;
    private float saturation = 0.0f;
    private float brightness = 0.0f;

    private BeautyFilterType type = BeautyFilterType.NONE;

    public ImageEditAdjustView(Context context, BeautyImageDisplay beautyDisplay) {
        super(context, beautyDisplay);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_edit_adjust, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRadioGroup = getView().findViewById(R.id.fragment_adjust_radiogroup);
        mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId != -1)
                    mLinearLayout.setVisibility(View.VISIBLE);
                switch (checkedId) {
                    case R.id.fragment_radio_contrast:
                        type = BeautyFilterType.CONTRAST;
                        mSeekBar.reset();
                        mSeekBar.setSeekLength(-100, 100, -50, 1);
                        mSeekBar.setValue(contrast);
                        mLabel.setBackgroundResource(R.drawable.selector_image_edit_adjust_contrast);
                        break;
                    case R.id.fragment_radio_exposure:
                        type = BeautyFilterType.EXPOSURE;
                        mSeekBar.reset();
                        mSeekBar.setSeekLength(-100, 100, 0, 1);
                        mSeekBar.setValue(exposure);
                        mLabel.setBackgroundResource(R.drawable.selector_image_edit_adjust_exposure);
                        break;
                    case R.id.fragment_radio_saturation:
                        type = BeautyFilterType.SATURATION;
                        mSeekBar.reset();
                        mSeekBar.setSeekLength(-100, 100, 0, 1);
                        mSeekBar.setValue(saturation);
                        mLabel.setBackgroundResource(R.drawable.selector_image_edit_adjust_saturation);
                        break;
                    case R.id.fragment_radio_sharpness:
                        type = BeautyFilterType.SHARPEN;
                        mSeekBar.reset();
                        mSeekBar.setSeekLength(-100, 100, 0, 1);
                        mSeekBar.setValue(sharpness);
                        mLabel.setBackgroundResource(R.drawable.selector_image_edit_adjust_saturation);
                        break;
                    case R.id.fragment_radio_bright:
                        type = BeautyFilterType.BRIGHTNESS;
                        mSeekBar.reset();
                        mSeekBar.setSeekLength(-100, 100, 0, 1);
                        mSeekBar.setValue(brightness);
                        break;
                    case R.id.fragment_radio_hue:
                        type = BeautyFilterType.HUE;
                        mSeekBar.reset();
                        mSeekBar.setSeekLength(0, 360, 0, 1);
                        mSeekBar.setValue(hue);
                        break;
                    default:
                        break;
                }
            }
        });

        mVal = view.findViewById(R.id.item_val);
        mLabel = view.findViewById(R.id.item_label);
        mSeekBar = view.findViewById(R.id.item_seek_bar);
        mSeekBar.setOnSeekChangeListener(mOnSeekChangeListener);
        mLinearLayout = view.findViewById(R.id.seek_bar_item_menu);
        mBeautyDisplay.setFilter(BeautyFilterType.IMAGE_ADJUST);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            contrast = -50.0f;
            exposure = 0.0f;
            saturation = 0.0f;
            sharpness = 0.0f;
            brightness = 0.0f;
            hue = 0.0f;
            mRadioGroup.clearCheck();
            mBeautyDisplay.setFilter(BeautyFilterType.NONE);
            mLinearLayout.setVisibility(View.INVISIBLE);
            type = BeautyFilterType.NONE;
        } else {
            mBeautyDisplay.setFilter(BeautyFilterType.IMAGE_ADJUST);
        }
    }

    protected boolean isChanged() {
        return contrast != -50.0f || exposure != 0.0f || saturation != 0.0f
                || sharpness != 0.0f || brightness != 0.0f || hue != 0.0f;
    }

    private int convertToProgress(float value) {
        switch (mRadioGroup.getCheckedRadioButtonId()) {
            case R.id.fragment_radio_contrast:
                contrast = value;
                return (int) Math.round((value + 100) / 2);
            case R.id.fragment_radio_exposure:
                exposure = value;
                return (int) Math.round((value + 100) / 2);
            case R.id.fragment_radio_saturation:
                saturation = value;
                return (int) Math.round((value + 100) / 2);
            case R.id.fragment_radio_sharpness:
                sharpness = value;
                return (int) Math.round((value + 100) / 2);
            case R.id.fragment_radio_bright:
                brightness = value;
                return (int) Math.round((value + 100) / 2);
            case R.id.fragment_radio_hue:
                hue = value;
                return (int) Math.round(100 * value / 360.0f);
            default:
                return 0;
        }
    }

    private final TwoLineSeekBar.OnSeekChangeListener mOnSeekChangeListener = new TwoLineSeekBar.OnSeekChangeListener() {

        @Override
        public void onSeekStopped(float value, float step) {

        }

        @Override
        public void onSeekChanged(float value, float step) {
            mVal.setText("" + value);
            mLabel.setPressed(value != 0.0f);
            mBeautyDisplay.adjustFilter(convertToProgress(value), type.ordinal());
        }
    };
}
