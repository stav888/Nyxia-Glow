#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES uTexture;
uniform sampler2D uMakeupMask;
uniform sampler2D uColorLut;
uniform float uGlowStrength;
uniform float uSmoothStrength;
uniform float uLutIntensity;
uniform vec3 uLipColor;
uniform vec3 uBlushColor;
uniform float uLipStrength;
uniform float uBlushStrength;
uniform vec2 uTexelSize;
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

void main() {
    vec4 color = texture2D(uTexture, vTextureCoord);
    vec4 mask = texture2D(uMakeupMask, vTextureCoord);
    vec3 finalColor = color.rgb;
    float makeupCoverage = smoothstep(0.02, 0.25, max(mask.r, mask.g));

    if (uSmoothStrength > 0.001) {
        vec2 offset = uTexelSize * 1.4;
        float centerLuma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        vec3 acc = color.rgb;
        float weightSum = 1.0;
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(-offset.x, -offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(0.0, -offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(offset.x, -offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(-offset.x, 0.0));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(offset.x, 0.0));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(-offset.x, offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(0.0, offset.y));
        accumulateSmooth(acc, weightSum, centerLuma, vTextureCoord, vec2(offset.x, offset.y));
        float smoothAmount = uSmoothStrength * mix(0.16, 0.36, makeupCoverage);
        finalColor = mix(color.rgb, acc / weightSum, smoothAmount);
    }

    vec3 warmTone = vec3(1.0, 0.985, 0.96);
    finalColor *= mix(vec3(1.0), warmTone, 0.12 * uGlowStrength);
    finalColor += vec3(0.035, 0.012, 0.0) * uGlowStrength;
    finalColor = mix(finalColor, uLipColor, clamp(mask.r * uLipStrength, 0.0, 1.0));
    finalColor = mix(finalColor, uBlushColor, clamp(mask.g * uBlushStrength, 0.0, 1.0));

    if (uLutIntensity > 0.001) {
        finalColor = mix(finalColor, sampleLut(clamp(finalColor, 0.0, 1.0)), uLutIntensity);
    }

    float luminance = dot(finalColor, vec3(0.299, 0.587, 0.114));
    finalColor += vec3(max(luminance - 0.65, 0.0) * uGlowStrength * 0.25);
    gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), color.a);
}
