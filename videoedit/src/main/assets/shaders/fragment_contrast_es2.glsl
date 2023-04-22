#version 100
// Copyright 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// ES 2 fragment shader that samples from a (non-external) texture with
// uTexSampler, copying from this texture to the current output
// while adjusting contrast based on uContrastFactor.

precision mediump float;
uniform sampler2D uTexSampler;
uniform float uContrastFactor;
varying vec2 vTexSamplingCoord;

const vec3 halfColor = vec3(0.5);

void main() {
    vec4 inputColor = texture2D(uTexSampler, vTexSamplingCoord);
    vec3 outputColor = (inputColor.rgb - halfColor) * uContrastFactor + halfColor;
    gl_FragColor = vec4(outputColor, inputColor.a);
}
