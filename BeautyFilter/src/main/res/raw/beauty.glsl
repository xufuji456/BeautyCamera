precision mediump float;

uniform sampler2D inputImageTexture;
uniform vec2 singleStepOffset;
uniform mediump float params;

vec2 blurCoordinates[20];

uniform sampler2D inputTexture;
varying lowp vec2 textureCoordinate;

uniform float opacity;

void main() {
    // 高斯模糊
    blurCoordinates[0]  = textureCoordinate.xy + singleStepOffset * vec2(0.0, -10.0);
    blurCoordinates[1]  = textureCoordinate.xy + singleStepOffset * vec2(0.0, 10.0);
    blurCoordinates[2]  = textureCoordinate.xy + singleStepOffset * vec2(-10.0, 0.0);
    blurCoordinates[3]  = textureCoordinate.xy + singleStepOffset * vec2(10.0, 0.0);
    blurCoordinates[4]  = textureCoordinate.xy + singleStepOffset * vec2(5.0, -8.0);
    blurCoordinates[5]  = textureCoordinate.xy + singleStepOffset * vec2(5.0, 8.0);
    blurCoordinates[6]  = textureCoordinate.xy + singleStepOffset * vec2(-5.0, 8.0);
    blurCoordinates[7]  = textureCoordinate.xy + singleStepOffset * vec2(-5.0, -8.0);
    blurCoordinates[8]  = textureCoordinate.xy + singleStepOffset * vec2(8.0, -5.0);
    blurCoordinates[9]  = textureCoordinate.xy + singleStepOffset * vec2(8.0, 5.0);
    blurCoordinates[10] = textureCoordinate.xy + singleStepOffset * vec2(-8.0, 5.0);
    blurCoordinates[11] = textureCoordinate.xy + singleStepOffset * vec2(-8.0, -5.0);
    blurCoordinates[12] = textureCoordinate.xy + singleStepOffset * vec2(0.0, -6.0);
    blurCoordinates[13] = textureCoordinate.xy + singleStepOffset * vec2(0.0, 6.0);
    blurCoordinates[14] = textureCoordinate.xy + singleStepOffset * vec2(6.0, 0.0);
    blurCoordinates[15] = textureCoordinate.xy + singleStepOffset * vec2(-6.0, 0.0);
    blurCoordinates[16] = textureCoordinate.xy + singleStepOffset * vec2(-4.0, -4.0);
    blurCoordinates[17] = textureCoordinate.xy + singleStepOffset * vec2(-4.0, 4.0);
    blurCoordinates[18] = textureCoordinate.xy + singleStepOffset * vec2(4.0, -4.0);
    blurCoordinates[19] = textureCoordinate.xy + singleStepOffset * vec2(4.0, 4.0);

    float blurColor = inputColor.g * 20.0;
    blurColor += texture2D(inputImageTexture, blurCoordinates[0]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[1]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[2]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[3]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[4]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[5]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[6]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[7]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[8]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[9]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[10]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[11]).g;
    blurColor += texture2D(inputImageTexture, blurCoordinates[12]).g * 2.0;
    blurColor += texture2D(inputImageTexture, blurCoordinates[13]).g * 2.0;
    blurColor += texture2D(inputImageTexture, blurCoordinates[14]).g * 2.0;
    blurColor += texture2D(inputImageTexture, blurCoordinates[15]).g * 2.0;
    blurColor += texture2D(inputImageTexture, blurCoordinates[16]).g * 2.0;
    blurColor += texture2D(inputImageTexture, blurCoordinates[17]).g * 2.0;
    blurColor += texture2D(inputImageTexture, blurCoordinates[18]).g * 2.0;
    blurColor += texture2D(inputImageTexture, blurCoordinates[19]).g * 2.0;

    blurColor = blurColor / 48.0;

    // 高反差
    float highPass = inputColor.g - blurColor + 0.5;

    // 高反差保留
    for(int i = 0; i < 5; i++) {
        if(highPass <= 0.5) {
            highPass = pow(highPass, 2.0) * 2.0;
        } else {
            highPass = 1.0 - pow(1.0 - highPass, 2.0) * 2.0;
        }
    }
    // 强光处理
    float aa = 1.0 + pow(sum, 0.3) * 0.09;
    vec3 smoothColor = inputColor * aa - vec3(highPass) * (aa - 1.0);
    smoothColor = clamp(smoothColor, vec3(0.0), vec3(1.0));

    smoothColor = mix(inputColor, smoothColor, pow(inputColor.g, 0.33));
    smoothColor = mix(inputColor, smoothColor, pow(inputColor.g, 0.39));
    smoothColor = mix(inputColor, smoothColor, opacity);

    gl_FragColor = vec4(pow(smoothColor, vec3(0.96)), 1.0);
}