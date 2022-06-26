package com.frank.beautyfilter.filter.helper;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author xufulong
 * @date 2022/6/18 6:09 下午
 * @desc
 */
public class BeautyFilterParam {

    public static int mGPUPower = 1;

    public static void initFilterParam(GL10 gl){
        mGPUPower = getGPUPower(gl.glGetString(GL10.GL_RENDERER));
    }

    /**
     *
     * @param gpu name of GPU
     * @return GPU type
     */
    private static int getGPUPower(String gpu){
        //for Mali GPU
        if(gpu.contains("Mali-T880"))
            return 1;
        else if(gpu.contains("Mali-T760"))
            return 1;
        else if(gpu.contains("Mali-T628"))
            return 1;
        else if(gpu.contains("Mali-T624"))
            return 1;
        else if(gpu.contains("Mali"))
            return 0;
        // for PowerVR
        if(gpu.contains("PowerVR SGX 544"))
            return 0;
        else if(gpu.contains("PowerVR"))
            return 1;
        // for Exynos
        if(gpu.contains("Exynos 8"))
            return 2;
        else if(gpu.contains("Exynos 7"))
            return 1;
        else if(gpu.contains("Exynos"))
            return 0;
        // for Adreno
        if(gpu.contains("Adreno 330"))
            return 1;
        else if(gpu.contains("Adreno 510"))
            return 1;
        else if(gpu.contains("Adreno 320"))
            return 1;
        else if(gpu.contains("Adreno 306"))
            return 0;
        else if(gpu.contains("Adreno 405"))
            return 0;
        return 1;
    }
}
