package com.frank.camera.beauty.edit;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;

import com.frank.beautyfilter.display.BeautyImageDisplay;

public abstract class ImageEditFragment extends Fragment {

    protected Context mContext;
    protected OnHideListener mOnHideListener;
    protected BeautyImageDisplay mMagicDisplay;

    public ImageEditFragment(Context context, BeautyImageDisplay magicDisplay) {
        this.mMagicDisplay = magicDisplay;
        this.mContext = context;
    }

    public void onHide() {
        if (isChanged()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Tip").setMessage("Are you sure to save").setNegativeButton("No", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onDialogButtonClick(dialog);
                    mMagicDisplay.commit();
                }
            }).setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onDialogButtonClick(dialog);
                    mMagicDisplay.restore();
                }
            }).create().show();
        } else {
            mOnHideListener.onHide();
        }
    }

    public void setOnHideListener(OnHideListener listener) {
        this.mOnHideListener = listener;
    }

    protected abstract boolean isChanged();

    protected void onDialogButtonClick(DialogInterface dialog) {
        if (mOnHideListener != null)
            mOnHideListener.onHide();
        dialog.dismiss();
    }

    public interface OnHideListener {
        void onHide();
    }
}
