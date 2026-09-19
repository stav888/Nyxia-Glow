#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES uTexture;
uniform sampler2D uMakeupMask;
uniform sampler2D uColorLut;
uniform float uLutIntensity;
uniform vec2 uTexelSize;

uniform float uSkinSmooth;
uniform float uSkinGlow;
uniform float uSkinWhitening;
uniform float uTexturePreservation;

uniform float uLipIntensity;
uniform vec3 uLipColor;

uniform float uBlushIntensity;
uniform vec3 uBlushColor;

uniform float uEyeEnhancement;
uniform float uTeethWhitening;

uniform float uSharpness;
uniform float uContrast;
uniform float uSaturation;
uniform float uExposure;
uniform float uTemperature;

varying vec2 vTextureCoord;

vec3 sampleLut(vec3 color) {
    float blue = color.b * 63.0;
    float blue0 = floor(blue);
    float blue1 = min(63.0, ceil(blue));
    float mixBlue = blue - blue0;
    vec2 tile0;
    tile0.y = floor(blue0 / 8.0);
    tile0.x = blue0 - tile0.y * 8.0;
    vec2 tile1;
    tile1.y = floor(blue1 / 8.0);
    tile1.x = blue1 - tile1.y * 8.0;
    vec2 uv0;
    uv0.x = (tile0.x * 64.0 + 0.5 + color.r * 63.0) / 512.0;
    uv0.y = (tile0.y * 64.0 + 0.5 + color.g * 63.0) / 512.0;
    vec2 uv1;
    uv1.x = (tile1.x * 64.0 + 0.5 + color.r * 63.0) / 512.0;
    uv1.y = (tile1.y * 64.0 + 0.5 + color.g * 63.0) / 512.0;
    return mix(texture2D(uColorLut, uv0).rgb, texture2D(uColorLut, uv1).rgb, mixBlue);
}

void accumulateSmooth(inout vec3 acc, inout float weightSum, float centerLuma, vec2 uv, vec2 offset) {
    vec3 sampleColor = texture2D(uTexture, uv + offset).rgb;
    float sampleLuma = dot(sampleColor, vec3(0.299, 0.587, 0.114));
    float weight = exp(-abs(sampleLuma - centerLuma) * 12.0);
    acc += sampleColor * weight;
    weightSum += weight;
}

vec3 applyLipMakeup(vec3 color, float lipMask, vec3 lipColor, float intensity) {
    float amount = clamp(lipMask * intensity, 0.0, 1.0);
    return mix(color, lipColor, amount);
}

vec3 applyBlushMakeup(vec3 color, float blushMask, vec3 blushColor, float intensity) {
    float amount = clamp(blushMask * intensity, 0.0, 1.0);
    return mix(color, blushColor, amount);
}

vec3 applyColorGrade(vec3 color) {
    color *= (1.0 + uExposure * 0.35);
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 saturated = mix(vec3(luma), color, 1.0 + uSaturation);
    color = mix(color, saturated, uSaturation);
    vec3 contrasted = (color - vec3(0.5)) * (1.0 + uContrast) + vec3(0.5);
    color = mix(color, contrasted, uContrast);
    color.r += uTemperature * 0.04;
    color.b -= uTemperature * 0.03;
    return color;
}

void main() {
    vec4 cameraColor = texture2D(uTexture, vTextureCoord);
    vec4 mask = texture2D(uMakeupMask, vTextureCoord);
    vec3 color = cameraColor.rgb;
    float lipMask = mask.r;
    float blushMask = mask.g;
    float makeupCoverage = smoothstep(0.02, 0.25, max(lipMask, blushMask));
    float skinMask = mix(0.22, 1.0, makeupCoverage);

    if (uSkinSmooth > 0.001) {
        vec2 offset = uTexelSize * 1.4;
        float centerLuma = dot(cameraColor.rgb, vec3(0.299, 0.587, 0.114));
        vec3 acc = cameraColor.rgb;
        float weightSum = 1.0;
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(-offset.x, -offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(0.0, -offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(offset.x, -offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(-offset.x, 0.0));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(offset.x, 0.0));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(-offset.x, offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(0.0, offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(offset.x, offset.y));
        float preserve = mix(1.0, 0.55, uTexturePreservation);
        float smoothAmount = uSkinSmooth * mix(0.20, 0.42, makeupCoverage) * preserve;
        color = mix(cameraColor.rgb, acc / max(weightSum, 0.001), smoothAmount);

        if (uSharpness > 0.001) {
            vec3 highPass = color - (acc / max(weightSum, 0.001));
            color += highPass * uSharpness * 0.65;
        }
    }

    vec3 warmTone = vec3(1.0, 0.985, 0.96);
    color *= mix(vec3(1.0), warmTone, 0.12 * uSkinGlow);
    color += vec3(0.035, 0.012, 0.0) * uSkinGlow * skinMask;
    color += vec3(0.06, 0.05, 0.045) * uSkinWhitening * skinMask;

    color = applyLipMakeup(color, lipMask, uLipColor, uLipIntensity);
    color = applyBlushMakeup(color, blushMask, uBlushColor, uBlushIntensity);

    color += vec3(0.04, 0.03, 0.02) * uEyeEnhancement * 0.0;
    color += vec3(0.05, 0.05, 0.04) * uTeethWhitening * 0.0;

    if (uLutIntensity > 0.001) {
        color = mix(color, sampleLut(clamp(color, 0.0, 1.0)), uLutIntensity);
    }

    color = applyColorGrade(color);
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    color += vec3(max(luminance - 0.65, 0.0) * uSkinGlow * 0.25);
    gl_FragColor = vec4(clamp(color, 0.0, 1.0), cameraColor.a);
}
