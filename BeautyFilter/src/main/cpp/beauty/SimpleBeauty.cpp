#include "SimpleBeauty.h"
#include <cmath>
#include "../bitmap/BitmapOperation.h"
#include "../bitmap/Conversion.h"

#define  LOG_TAG    "SimpleBeauty"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define div255(x) (x * 0.003921F)
#define abs(x) (x>=0 ? x:(-x))

SimpleBeauty *SimpleBeauty::instance;

SimpleBeauty *SimpleBeauty::getInstance() {
    if (instance == nullptr)
        instance = new SimpleBeauty();
    return instance;
}

SimpleBeauty::SimpleBeauty() {
    mIntegralMatrix = nullptr;
    mIntegralMatrixSqr = nullptr;
    mImageData_yuv = nullptr;
    mSkinMatrix = nullptr;
    mImageData_rgb = nullptr;
    mSmoothLevel = 0.0;
    mWhitenLevel = 0.0;
}

SimpleBeauty::~SimpleBeauty() {
    if (mIntegralMatrix != nullptr)
        delete[] mIntegralMatrix;
    if (mIntegralMatrixSqr != nullptr)
        delete[] mIntegralMatrixSqr;
    if (mImageData_yuv != nullptr)
        delete[] mImageData_yuv;
    if (mSkinMatrix != nullptr)
        delete[] mSkinMatrix;
    if (mImageData_rgb != nullptr)
        delete[] mImageData_rgb;
}

void SimpleBeauty::initMagicBeauty(JniBitmap *jniBitmap) {
    storedBitmapPixels = jniBitmap->_storedBitmapPixels;
    mImageWidth = jniBitmap->_bitmapInfo.width;
    mImageHeight = jniBitmap->_bitmapInfo.height;

    if (mImageData_rgb == nullptr)
        mImageData_rgb = new uint32_t[mImageWidth * mImageHeight];
    memcpy(mImageData_rgb, jniBitmap->_storedBitmapPixels,
           sizeof(uint32_t) * mImageWidth * mImageHeight);
    if (mImageData_yuv == nullptr)
        mImageData_yuv = new uint8_t[mImageWidth * mImageHeight * 3];
    Conversion::RGBToYCbCr((uint8_t *) mImageData_rgb, mImageData_yuv, mImageWidth * mImageHeight);
    initSkinMatrix();
    initIntegral();
}

void SimpleBeauty::unInitMagicBeauty() {
    if (instance != nullptr)
        delete instance;
    instance = nullptr;
}

void SimpleBeauty::startSkinSmooth(float smoothLevel) {
    _startBeauty(smoothLevel, mWhitenLevel);
}

void SimpleBeauty::startSkinWhite(float whiteLevel) {
    _startBeauty(mSmoothLevel, whiteLevel);
}

void SimpleBeauty::_startBeauty(float smoothLevel, float whiteLevel) {
    if (smoothLevel >= 10.0 && smoothLevel <= 510.0) {
        mSmoothLevel = smoothLevel;
        _startSkinSmooth(mSmoothLevel);
    }
    if (whiteLevel >= 1.0 && whiteLevel <= 5.0) {
        mWhitenLevel = whiteLevel;
        _startSkinWhite(mWhitenLevel);
    }
}

void SimpleBeauty::_startSkinWhite(float whiteLevel) {
    float a = log(whiteLevel);
    for (int i = 0; i < mImageHeight; i++) {
        for (int j = 0; j < mImageWidth; j++) {
            int offset = i * mImageWidth + j;
            ARGB RGB;
            BitmapOperation::convertIntToArgb(mImageData_rgb[offset], &RGB);
            if (a != 0) {
                RGB.red = 255 * (log(div255(RGB.red) * (whiteLevel - 1) + 1) / a);
                RGB.green = 255 * (log(div255(RGB.green) * (whiteLevel - 1) + 1) / a);
                RGB.blue = 255 * (log(div255(RGB.blue) * (whiteLevel - 1) + 1) / a);
            }
            storedBitmapPixels[offset] = BitmapOperation::convertArgbToInt(RGB);
        }
    }
}

void SimpleBeauty::_startSkinSmooth(float smoothLevel) {
    if (mIntegralMatrix == nullptr || mIntegralMatrixSqr == nullptr || mSkinMatrix == nullptr) {
        LOGE("not init correctly");
        return;
    }
    Conversion::RGBToYCbCr((uint8_t *) mImageData_rgb, mImageData_yuv, mImageWidth * mImageHeight);

    int radius = mImageWidth > mImageHeight ? mImageWidth * 0.02 : mImageHeight * 0.02;

    for (int i = 1; i < mImageHeight; i++) {
        for (int j = 1; j < mImageWidth; j++) {
            int offset = i * mImageWidth + j;
            if (mSkinMatrix[offset] == 255) {
                int iMax = i + radius >= mImageHeight - 1 ? mImageHeight - 1 : i + radius;
                int jMax = j + radius >= mImageWidth - 1 ? mImageWidth - 1 : j + radius;
                int iMin = i - radius <= 1 ? 1 : i - radius;
                int jMin = j - radius <= 1 ? 1 : j - radius;

                int squar = (iMax - iMin + 1) * (jMax - jMin + 1);
                int i4 = iMax * mImageWidth + jMax;
                int i3 = (iMin - 1) * mImageWidth + (jMin - 1);
                int i2 = iMax * mImageWidth + (jMin - 1);
                int i1 = (iMin - 1) * mImageWidth + jMax;

                float m = (float)(mIntegralMatrix[i4]
                           + mIntegralMatrix[i3]
                           - mIntegralMatrix[i2]
                           - mIntegralMatrix[i1]) / squar;

                float v = (float)(mIntegralMatrixSqr[i4]
                           + mIntegralMatrixSqr[i3]
                           - mIntegralMatrixSqr[i2]
                           - mIntegralMatrixSqr[i1]) / squar - m * m;
                float k = v / (v + smoothLevel);

                mImageData_yuv[offset * 3] = ceil(m - k * m + k * mImageData_yuv[offset * 3]);
            }
        }
    }
    Conversion::YCbCrToRGB(mImageData_yuv, (uint8_t *) storedBitmapPixels,
                           mImageWidth * mImageHeight);
}

void SimpleBeauty::initSkinMatrix() {
    if (mSkinMatrix == nullptr)
        mSkinMatrix = new uint8_t[mImageWidth * mImageHeight];
    for (int i = 0; i < mImageHeight; i++) {
        for (int j = 0; j < mImageWidth; j++) {
            int offset = i * mImageWidth + j;
            ARGB RGB;
            BitmapOperation::convertIntToArgb(mImageData_rgb[offset], &RGB);
            if ((RGB.blue > 95 && RGB.green > 40 && RGB.red > 20 &&
                 RGB.blue - RGB.red > 15 && RGB.blue - RGB.green > 15) ||
                (RGB.blue > 200 && RGB.green > 210 && RGB.red > 170 &&
                 abs(RGB.blue - RGB.red) <= 15 && RGB.blue > RGB.red && RGB.green > RGB.red))
                mSkinMatrix[offset] = 255;
            else
                mSkinMatrix[offset] = 0;
        }
    }
}

void SimpleBeauty::initIntegral() {
    if (mIntegralMatrix == nullptr)
        mIntegralMatrix = new uint64_t[mImageWidth * mImageHeight];
    if (mIntegralMatrixSqr == nullptr)
        mIntegralMatrixSqr = new uint64_t[mImageWidth * mImageHeight];

    auto *columnSum = new uint64_t[mImageWidth];
    auto *columnSumSqr = new uint64_t[mImageWidth];

    columnSum[0] = mImageData_yuv[0];
    columnSumSqr[0] = mImageData_yuv[0] * mImageData_yuv[0];

    mIntegralMatrix[0] = columnSum[0];
    mIntegralMatrixSqr[0] = columnSumSqr[0];

    for (int i = 1; i < mImageWidth; i++) {

        columnSum[i] = mImageData_yuv[3 * i];
        columnSumSqr[i] = mImageData_yuv[3 * i] * mImageData_yuv[3 * i];

        mIntegralMatrix[i] = columnSum[i];
        mIntegralMatrix[i] += mIntegralMatrix[i - 1];
        mIntegralMatrixSqr[i] = columnSumSqr[i];
        mIntegralMatrixSqr[i] += mIntegralMatrixSqr[i - 1];
    }
    for (int i = 1; i < mImageHeight; i++) {
        int offset = i * mImageWidth;

        columnSum[0] += mImageData_yuv[3 * offset];
        columnSumSqr[0] += mImageData_yuv[3 * offset] * mImageData_yuv[3 * offset];

        mIntegralMatrix[offset] = columnSum[0];
        mIntegralMatrixSqr[offset] = columnSumSqr[0];

        for (int j = 1; j < mImageWidth; j++) {
            columnSum[j] += mImageData_yuv[3 * (offset + j)];
            columnSumSqr[j] += mImageData_yuv[3 * (offset + j)] * mImageData_yuv[3 * (offset + j)];

            mIntegralMatrix[offset + j] = mIntegralMatrix[offset + j - 1] + columnSum[j];
            mIntegralMatrixSqr[offset + j] = mIntegralMatrixSqr[offset + j - 1] + columnSumSqr[j];
        }
    }
    delete[] columnSum;
    delete[] columnSumSqr;
}

