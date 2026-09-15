#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D HistorySampler;
uniform vec2 InSize;
uniform vec2 MotionDirection;
uniform float HistoryWeight;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 halfPixel = 0.5 / InSize;
    vec3 color = texture(DiffuseSampler, texCoord).rgb;
    // Nine samples along the actual camera motion, not a permanent full-screen blur.
    if (dot(MotionDirection, MotionDirection) > 0.00000001) {
        vec3 sum = vec3(0.0);
        float weights = 0.0;
        for (int i = -4; i <= 4; i++) {
            float t = float(i) / 4.0;
            float weight = 1.0 - abs(t) * 0.75;
            vec2 uv = clamp(texCoord + MotionDirection * t, halfPixel, 1.0 - halfPixel);
            sum += texture(DiffuseSampler, uv).rgb * weight;
            weights += weight;
        }
        color = sum / weights;
    }
    // Adaptation of EffectMotionBlur: retain a composited frame instead of 30 textures.
    // Never read an uninitialized history buffer, including the first frame after resize.
    if (HistoryWeight > 0.0) {
        color = mix(color, texture(HistorySampler, texCoord).rgb, HistoryWeight);
    }
    fragColor = vec4(color, 1.0);
}
