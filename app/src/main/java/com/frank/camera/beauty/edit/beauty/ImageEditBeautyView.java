package com.frank.camera.beauty.edit.beauty;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.frank.beautyfilter.BeautyManager;
import com.frank.beautyfilter.display.BeautyImageDisplay;
import com.frank.camera.R;
import com.frank.camera.beauty.edit.ImageEditFragment;
import com.frank.camera.beauty.widget.BubbleSeekBar;

@SuppressLint("ValidFragment")
public class ImageEditBeautyView extends ImageEditFragment {

    private boolean mIsWhiten = false;
    private boolean mIsSmoothed = false;
    private RadioGroup mRadioGroup;
    private BeautyManager mBeautyManager;
    private RelativeLayout mSkinColorView;
    private RelativeLayout mSkinSmoothView;
    private BubbleSeekBar mWhiteBubbleSeekBar;
    private BubbleSeekBar mSmoothBubbleSeekBar;

    public ImageEditBeautyView(Context context, BeautyImageDisplay beautyDisplay) {
        super(context, beautyDisplay);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_edit_beauty, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mSkinSmoothView = getView().findViewById(R.id.fragment_beauty_skin);
        mSkinColorView = getView().findViewById(R.id.fragment_beauty_color);
        mRadioGroup = getView().findViewById(R.id.fragment_beauty_radiogroup);
        mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.fragment_beauty_btn_skinsmooth:
                        mSkinSmoothView.setVisibility(View.VISIBLE);
                        mSkinColorView.setVisibility(View.GONE);
                        break;
                    case R.id.fragment_beauty_btn_skincolor:
                        mSkinColorView.setVisibility(View.VISIBLE);
                        mSkinSmoothView.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }
        });

        mBeautyManager = BeautyManager.getInstance();
        mBeautyManager.setBeautyListener(mBeautyListener);
        mSmoothBubbleSeekBar = view.findViewById(R.id.fragment_beauty_skin_seekbar);
        mSmoothBubbleSeekBar.setOnBubbleSeekBarChangeListener(smoothSeekBarChangeListener);
        mWhiteBubbleSeekBar = view.findViewById(R.id.fragment_beauty_white_seekbar);
        mWhiteBubbleSeekBar.setOnBubbleSeekBarChangeListener(colorSeekBarChangeListener);

        init();

        super.onViewCreated(view, savedInstanceState);
    }

    private void init() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                mBeautyManager.initBeauty();
            }
        }).start();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            mSmoothBubbleSeekBar.setProgress(0);
            mWhiteBubbleSeekBar.setProgress(0);
            init();
        } else {
            mBeautyManager.unInitBeauty();
        }
    }

    private final BubbleSeekBar.OnBubbleSeekBarChangeListener smoothSeekBarChangeListener
            = new BubbleSeekBar.OnBubbleSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    float level = seekBar.getProgress() / 10.0f;
                    if (level < 0)
                        level = 0;
                    mBeautyManager.onStartSkinSmooth(level);
                    if (seekBar.getProgress() != 0)
                        mIsSmoothed = true;
                    else
                        mIsSmoothed = false;
                }
            }).start();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }
    };

    private final BubbleSeekBar.OnBubbleSeekBarChangeListener colorSeekBarChangeListener
            = new BubbleSeekBar.OnBubbleSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            float level = seekBar.getProgress() / 20.0f;
            if (level < 1)
                level = 1;
            mBeautyManager.onStartSkinWhite(level);
            if (seekBar.getProgress() != 0)
                mIsWhiten = true;
            else
                mIsWhiten = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }
    };

    private final BeautyManager.BeautyListener mBeautyListener = new BeautyManager.BeautyListener() {

        @Override
        public void onEnd() {

        }
    };

    @Override
    protected boolean isChanged() {
        return mIsWhiten || mIsSmoothed;
    }

    @Override
    protected void onDialogButtonClick(DialogInterface dialog) {
        mBeautyManager.unInitBeauty();
        super.onDialogButtonClick(dialog);
    }

}
