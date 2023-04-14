
package com.frank.videoedit.effect.util;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GlProgram {

  // https://www.khronos.org/registry/OpenGL/extensions/EXT/EXT_YUV_target.txt
  private static final int GL_SAMPLER_EXTERNAL_2D_Y2Y_EXT = 0x8BE7;

  private final int programId;

  private final Uniform[] uniforms;
  private final Attribute[] attributes;
  private final Map<String, Uniform> uniformByName;
  private final Map<String, Attribute> attributeByName;

  public GlProgram(Context context, String vertexShaderFilePath, String fragmentShaderFilePath)
      throws IOException, GlUtil.GlException {
    this(loadAsset(context, vertexShaderFilePath), loadAsset(context, fragmentShaderFilePath));
  }

  private static byte[] toByteArray(InputStream inputStream) throws IOException {
    byte[] buffer = new byte[1024 * 4];
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    return outputStream.toByteArray();
  }

  public static String loadAsset(Context context, String assetPath) throws IOException {
    @Nullable InputStream inputStream = null;
    try {
      inputStream = context.getAssets().open(assetPath);
      return new String(toByteArray(inputStream), Charset.forName("UTF-8"));
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  public GlProgram(String vertexShaderGlsl, String fragmentShaderGlsl) throws GlUtil.GlException {
    programId = GLES20.glCreateProgram();

    addShader(programId, GLES20.GL_VERTEX_SHADER, vertexShaderGlsl);
    addShader(programId, GLES20.GL_FRAGMENT_SHADER, fragmentShaderGlsl);

    GLES20.glLinkProgram(programId);
    int[] linkStatus = new int[] {GLES20.GL_FALSE};
    GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
    GlUtil.checkGlException(
        linkStatus[0] == GLES20.GL_TRUE,
        "Unable to link shader program: \n" + GLES20.glGetProgramInfoLog(programId));
    GLES20.glUseProgram(programId);
    attributeByName = new HashMap<>();
    int[] attributeCount = new int[1];
    GLES20.glGetProgramiv(programId, GLES20.GL_ACTIVE_ATTRIBUTES, attributeCount, 0);
    attributes = new Attribute[attributeCount[0]];
    for (int i = 0; i < attributeCount[0]; i++) {
      Attribute attribute = Attribute.create(programId, i);
      attributes[i] = attribute;
      attributeByName.put(attribute.name, attribute);
    }
    uniformByName = new HashMap<>();
    int[] uniformCount = new int[1];
    GLES20.glGetProgramiv(programId, GLES20.GL_ACTIVE_UNIFORMS, uniformCount, 0);
    uniforms = new Uniform[uniformCount[0]];
    for (int i = 0; i < uniformCount[0]; i++) {
      Uniform uniform = Uniform.create(programId, i);
      uniforms[i] = uniform;
      uniformByName.put(uniform.name, uniform);
    }
    GlUtil.checkGlError();
  }

  private static void addShader(int programId, int type, String glsl) throws GlUtil.GlException {
    int shader = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shader, glsl);
    GLES20.glCompileShader(shader);

    int[] result = new int[] {GLES20.GL_FALSE};
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, result, 0);
    GlUtil.checkGlException(
        result[0] == GLES20.GL_TRUE, GLES20.glGetShaderInfoLog(shader) + ", source: " + glsl);

    GLES20.glAttachShader(programId, shader);
    GLES20.glDeleteShader(shader);
    GlUtil.checkGlError();
  }

  private static int getAttributeLocation(int programId, String attributeName) {
    return GLES20.glGetAttribLocation(programId, attributeName);
  }

  private int getAttributeLocation(String attributeName) {
    return getAttributeLocation(programId, attributeName);
  }

  private static int getUniformLocation(int programId, String uniformName) {
    return GLES20.glGetUniformLocation(programId, uniformName);
  }

  public int getUniformLocation(String uniformName) {
    return getUniformLocation(programId, uniformName);
  }

  public void use() throws GlUtil.GlException {
    GLES20.glUseProgram(programId);
    GlUtil.checkGlError();
  }

  public void delete() throws GlUtil.GlException {
    GLES20.glDeleteProgram(programId);
    GlUtil.checkGlError();
  }

  public void setBufferAttribute(String name, float[] values, int size) {
    Objects.requireNonNull(attributeByName.get(name)).setBuffer(values, size);
  }

  public void setSamplerTexIdUniform(String name, int texId, int texUnitIndex) {
    Objects.requireNonNull(uniformByName.get(name)).setSamplerTexId(texId, texUnitIndex);
  }

  public void setIntUniform(String name, int value) {
    Objects.requireNonNull(uniformByName.get(name)).setInt(value);
  }

  public void setFloatUniform(String name, float value) {
    Objects.requireNonNull(uniformByName.get(name)).setFloat(value);
  }

  public void setFloatsUniform(String name, float[] value) {
    Objects.requireNonNull(uniformByName.get(name)).setFloats(value);
  }

  public void bindAttributesAndUniforms() throws GlUtil.GlException {
    for (Attribute attribute : attributes) {
      attribute.bind();
    }
    for (Uniform uniform : uniforms) {
      uniform.bind();
    }
  }

  private static int getCStringLength(byte[] cString) {
    for (int i = 0; i < cString.length; ++i) {
      if (cString[i] == '\0') {
        return i;
      }
    }
    return cString.length;
  }

  private static final class Attribute {

    public static Attribute create(int programId, int index) {
      int[] length = new int[1];
      GLES20.glGetProgramiv(
          programId, GLES20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, length, /* offset= */ 0);
      byte[] nameBytes = new byte[length[0]];

      GLES20.glGetActiveAttrib(
          programId, index, length[0], new int[1], 0, new int[1], 0,
              new int[1], 0, nameBytes, 0);
      String name = new String(nameBytes, 0, getCStringLength(nameBytes));
      int location = getAttributeLocation(programId, name);

      return new Attribute(name, index, location);
    }

    private int size;
    private final int index;
    private final int location;

    public final String name;

    @Nullable private Buffer buffer;

    private Attribute(String name, int index, int location) {
      this.name     = name;
      this.index    = index;
      this.location = location;
    }

    public void setBuffer(float[] buffer, int size) {
      this.buffer = GlUtil.createBuffer(buffer);
      this.size = size;
    }

    public void bind() throws GlUtil.GlException {
      Buffer buffer = this.buffer;
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
      GLES20.glVertexAttribPointer(
          location, size, GLES20.GL_FLOAT, false, 0, buffer);
      GLES20.glEnableVertexAttribArray(index);
      GlUtil.checkGlError();
    }
  }

  private static final class Uniform {

    public static Uniform create(int programId, int index) {
      int[] length = new int[1];
      GLES20.glGetProgramiv(
          programId, GLES20.GL_ACTIVE_UNIFORM_MAX_LENGTH, length,  0);

      int[] type = new int[1];
      byte[] nameBytes = new byte[length[0]];

      GLES20.glGetActiveUniform(
          programId, index, length[0], new int[1], 0,
          new int[1], 0, type, 0, nameBytes, 0);

      String name  = new String(nameBytes, /* offset= */ 0, getCStringLength(nameBytes));
      int location = getUniformLocation(programId, name);

      return new Uniform(name, location, type[0]);
    }

    private final int type;
    private final int location;
    public final String name;
    private final float[] floatValue;

    private int intValue;
    private int texIdValue;
    private int texUnitIndex;

    private Uniform(String name, int location, int type) {
      this.name       = name;
      this.type       = type;
      this.location   = location;
      this.floatValue = new float[16];
    }

    public void setSamplerTexId(int texId, int texUnitIndex) {
      this.texIdValue  = texId;
      this.texUnitIndex = texUnitIndex;
    }

    public void setInt(int value) {
      this.intValue = value;
    }

    public void setFloat(float value) {
      this.floatValue[0] = value;
    }

    public void setFloats(float[] value) {
      System.arraycopy(value,  0, this.floatValue,  0, value.length);
    }

    public void bind() throws GlUtil.GlException {
      switch (type) {
        case GLES20.GL_INT:
          GLES20.glUniform1i(location, intValue);
          break;
        case GLES20.GL_FLOAT:
          GLES20.glUniform1fv(location, 1, floatValue, 0);
          GlUtil.checkGlError();
          break;
        case GLES20.GL_FLOAT_VEC2:
          GLES20.glUniform2fv(location, 1, floatValue, 0);
          GlUtil.checkGlError();
          break;
        case GLES20.GL_FLOAT_VEC3:
          GLES20.glUniform3fv(location, 1, floatValue, 0);
          GlUtil.checkGlError();
          break;
        case GLES20.GL_FLOAT_MAT3:
          GLES20.glUniformMatrix3fv(
              location, 1, false, floatValue, 0);
          GlUtil.checkGlError();
          break;
        case GLES20.GL_FLOAT_MAT4:
          GLES20.glUniformMatrix4fv(
              location, 1, false, floatValue, 0);
          GlUtil.checkGlError();
          break;
        case GLES20.GL_SAMPLER_2D:
        case GLES11Ext.GL_SAMPLER_EXTERNAL_OES:
        case GL_SAMPLER_EXTERNAL_2D_Y2Y_EXT:
          if (texIdValue == 0) {
            throw new IllegalStateException("No call to setSamplerTexId() before bind.");
          }
          GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + texUnitIndex);
          GlUtil.checkGlError();
          GlUtil.bindTexture(
              type == GLES20.GL_SAMPLER_2D
                  ? GLES20.GL_TEXTURE_2D
                  : GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
              texIdValue);
          GLES20.glUniform1i(location, texUnitIndex);
          GlUtil.checkGlError();
          break;
        default:
          throw new IllegalStateException("Unexpected uniform type: " + type);
      }
    }
  }
}
