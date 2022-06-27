package com.frank.camera.beauty.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.frank.beautyfilter.display.BeautyImageDisplay;
import com.frank.camera.R;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BeautyImageActivity extends Activity {
    private RadioGroup mRadioGroup;

    private Fragment[] mFragments;
    private int mFragmentTag = -1;

    private BeautyImageDisplay mImageDisplay;

    private final int REQUEST_PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beauty_image);
        initMagicPreview();
        initFragments();
        initRadioButtons();

        findViewById(R.id.image_edit_save).setOnClickListener(
                v -> mImageDisplay.savaImage(getOutputFile(),
                        result -> Log.e("ImageActivity", "onSaved result=" + result)));
    }

    private void initFragments() {
        mFragments = new Fragment[3];
        ImageEditBeautyView mImageEditBeautyView = new ImageEditBeautyView(this, mImageDisplay);
        mImageEditBeautyView.setOnHideListener(mOnHideListener);
        mFragments[0] = mImageEditBeautyView;
        ImageEditAdjustView mImageEditAdjustView = new ImageEditAdjustView(this, mImageDisplay);
        mImageEditAdjustView.setOnHideListener(mOnHideListener);
        mFragments[1] = mImageEditAdjustView;
        ImageEditFilterView mImageEditFilterView = new ImageEditFilterView(this, mImageDisplay);
        mImageEditFilterView.setOnHideListener(mOnHideListener);
        mFragments[2] = mImageEditFilterView;
    }

    private void initRadioButtons() {
        mRadioGroup = (RadioGroup) findViewById(R.id.image_edit_radiogroup);
        mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @SuppressLint("NonConstantResourceId")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int i = -1;
                switch (checkedId) {
                    case R.id.image_edit_adjust:
                        i = 1;
                        if (!mFragments[i].isAdded())
                            getFragmentManager().beginTransaction().add(R.id.image_edit_fragment_container, mFragments[i])
                                    .show(mFragments[i]).commit();
                        else
                            getFragmentManager().beginTransaction().show(mFragments[i]).commit();
                        mFragmentTag = 1;
                        break;
                    case R.id.image_edit_filter:
                        i = 2;
                        if (!mFragments[i].isAdded())
                            getFragmentManager().beginTransaction().add(R.id.image_edit_fragment_container, mFragments[i])
                                    .show(mFragments[i]).commit();
                        else
                            getFragmentManager().beginTransaction().show(mFragments[i]).commit();
                        mFragmentTag = 2;
//					if (mMagicEngine == null) {
//						MagicEngine.Builder builder = new MagicEngine.Builder();
//						mMagicEngine = builder.build((MagicImageView)findViewById(R.id.glsurfaceview_image));
//					}
                        break;
                    case R.id.image_edit_beauty:
                        i = 0;
                        if (!mFragments[i].isAdded())
                            getFragmentManager().beginTransaction().add(R.id.image_edit_fragment_container, mFragments[i])
                                    .show(mFragments[i]).commit();
                        else
                            getFragmentManager().beginTransaction().show(mFragments[i]).commit();
                        mFragmentTag = 0;
                        break;
                    default:
                        if (mFragmentTag != -1)
                            getFragmentManager().beginTransaction()
                                    .hide(mFragments[mFragmentTag])
                                    .commit();
                        mFragmentTag = -1;
                        break;
                }
            }
        });
    }

    private void hideFragment() {
        ((ImageEditFragment) mFragments[mFragmentTag]).onHide();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mFragmentTag != -1) {
                    hideFragment();
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initMagicPreview() {
        GLSurfaceView glSurfaceView = findViewById(R.id.glsurfaceview_image);
        mImageDisplay = new BeautyImageDisplay(this, glSurfaceView);

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mImageDisplay != null) {
            mImageDisplay.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mImageDisplay != null) {
            mImageDisplay.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mImageDisplay != null) {
            mImageDisplay.onDestroy();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri mUri = data.getData();
                        InputStream inputStream;
                        if (mUri.getScheme().startsWith("http") || mUri.getScheme().startsWith("https")) {
                            inputStream = new URL(mUri.toString()).openStream();
                        } else {
                            inputStream = this.getContentResolver().openInputStream(mUri);
                        }
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        mImageDisplay.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private final OnHideListener mOnHideListener = new OnHideListener() {

        @Override
        public void onHide() {
            mRadioGroup.check(View.NO_ID);
        }
    };

    private File getOutputFile() {
        String root;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            root = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            root = getCacheDir().getAbsolutePath();
        }
        File dir = new File(root, "BeautyCamera");
        if (!dir.exists()) {
            if (!dir.mkdir())
                return null;
        }
        String timestamp = new SimpleDateFormat("yyyy:MM:dd_HH:mm:ss", Locale.getDefault()).format(new Date());
        return new File(dir.getPath() + File.separator + timestamp + ".jpg");
    }

}
