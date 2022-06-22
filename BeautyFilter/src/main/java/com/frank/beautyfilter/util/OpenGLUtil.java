package com.frank.beautyfilter.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author xufulong
 * @date 2022/6/18 3:20 下午
 * @desc
 */
public class OpenGLUtil {

    public final static int ON_DRAWN   = 1;
    public static final int NOT_INIT   = -1;
    public static final int NO_SHADER  = 0;
    public static final int NO_TEXTURE = -1;

    private static void bindTexture(int[] textures) {
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR); // nearest
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,     GLES20.GL_CLAMP_TO_EDGE); // repeat
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,     GLES20.GL_CLAMP_TO_EDGE);
    }

    public static int loadTexture(final Bitmap bitmap, final int textureId) {
        return loadTexture(bitmap, textureId, false);
    }

    public static int loadTexture(final Bitmap bitmap, final int textureId, boolean recycle) {
        if (bitmap == null)
            return NO_TEXTURE;
        final int[] textures = new int[1];
        if (textureId == NO_TEXTURE) {
            bindTexture(textures);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap);
            textures[0] = textureId;
        }
        if (recycle)
            bitmap.recycle();
        return textures[0];
    }

    public static int loadTexture(final Buffer data, final int width, final int height, final int textureId) {
        return loadTexture(data, width, height, textureId, GLES20.GL_UNSIGNED_BYTE);
    }

    public static int loadTexture(final Buffer data, final int width, final int height, final int textureId, final int type) {
        if (data == null)
            return NO_TEXTURE;
        int[] textures = new int[1];
        if (textureId == NO_TEXTURE) {
            bindTexture(textures);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                    0, GLES20.GL_RGBA, type, data);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                    0, GLES20.GL_RGBA, type, data);
            textures[0] = textureId;
        }
        return textures[0];
    }

    public static int loadTexture(final Context context, final String name) {
        if (context == null || name == null)
            return NO_TEXTURE;
        final int[] textures = new int[1];
        Bitmap bitmap;
       AssetManager assetManager = context.getResources().getAssets();
        try {
           InputStream stream = assetManager.open(name);
           bitmap = BitmapFactory.decodeStream(stream);
           stream.close();
        } catch (IOException e) {
            return NO_TEXTURE;
        }
        bindTexture(textures);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        return textures[0];
    }

    private static int loadShader(final String source, final int type) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compile = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compile, 0);
        if (compile[0] <= 0) {
            Log.e("OpenGlUtil", "Shader compile error=" + GLES20.glGetShaderInfoLog(shader));
            return NO_SHADER;
        }
        return shader;
    }

    public static int loadProgram(final String vertexSource, final String fragmentSource) {
        int vertexShader   = loadShader(vertexSource, GLES20.GL_VERTEX_SHADER);
        int fragmentShader = loadShader(fragmentSource, GLES20.GL_FRAGMENT_SHADER);
        if (vertexShader == NO_SHADER || fragmentShader == NO_SHADER) {
            return 0;
        }
        int programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        GLES20.glLinkProgram(programId);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] <= 0) {
            programId = 0;
            Log.e("OpenGlUtil", "program link error=" + GLES20.glGetProgramInfoLog(programId));
        }
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        return programId;
    }

    // GLES11Ext GL10
    public static int getExternalOESTexturedId() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return textures[0];
    }

    public static String readShaderFromSource(final int resourceId) {
        String line;
        StringBuilder builder = new StringBuilder();
        InputStream inputStream = BeautyParams.context.getResources().openRawResource(resourceId);
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(reader);
        try {
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                inputStream.close();
                reader.close();
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return builder.toString();
    }

    public static void checkGLError(String operator) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = operator + ":glError=" + error;
            Log.e("OpenGLUtil", msg);
            throw new RuntimeException(msg);
        }
    }

}