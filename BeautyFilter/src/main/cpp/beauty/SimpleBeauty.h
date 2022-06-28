#ifndef SIMPLE_BEAUTY_H_
#define SIMPLE_BEAUTY_H_

#include "../bitmap/JniBitmap.h"

class SimpleBeauty
{
public:
	void initMagicBeauty(JniBitmap* jniBitmap);
	void unInitMagicBeauty();

    void startSkinSmooth(float smoothLevel);
    void startSkinWhite(float whiteLevel);

    static SimpleBeauty* getInstance();
    ~SimpleBeauty();

private:
    static SimpleBeauty * instance;
    SimpleBeauty();

    uint64_t *mIntegralMatrix;
	uint64_t *mIntegralMatrixSqr;

	uint32_t *storedBitmapPixels;
	uint32_t *mImageData_rgb;

	uint8_t *mImageData_yuv;
	uint8_t *mSkinMatrix;

	int mImageWidth;
	int mImageHeight;
	float mSmoothLevel;
	float mWhitenLevel;

	void initIntegral();

	void initSkinMatrix();

	void _startBeauty(float smoothLevel, float whiteLevel);
	void _startSkinSmooth(float smoothLevel);
	void _startSkinWhite(float whiteLevel);
};
#endif
