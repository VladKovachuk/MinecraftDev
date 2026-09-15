#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Strength;
uniform float CuredStrength;
uniform float EffectTime;
in vec2 texCoord;
out vec4 fragColor;

// Adapted from Psychedelicraft shaderUtils.frag (Lukas Tenbrink, 2014).
// See META-INF/licenses/psychedelicraft.txt.
vec3 getIntensifiedColor(vec3 color) {
    float gray = dot(color, vec3(0.3086, 0.6084, 0.0820));
    vec3 saturated = color * 2.0 - vec3(gray);
    return clamp(saturated * saturated * 10.0, 0.0, 1.0);
}

mat2 rotation(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, s, -s, c);
}

vec2 distortedUv(vec2 uv, float strength) {
    if (strength <= 0.0) return uv;
    float aspect = InSize.x / max(InSize.y, 1.0);
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);
    float ticks = EffectTime * 20.0;
    // Slow, overlapping waves reverse direction continuously, without a fast spinning stretch.
    float wobble = strength * 0.12;
    float base = 5.0 / (wobble * wobble + 5.0) - wobble * 0.04;
    base *= base;
    vec2 wave = sin(vec2(ticks / 150.0, ticks / 170.0) * 3.14159265);
    float swayAngle = 0.35 * sin(EffectTime * 0.27) + 0.18 * sin(EffectTime * 0.41);
    mat2 turn = rotation(swayAngle);
    vec2 warped = turn * ((rotation(-swayAngle) * p) * (base + wobble * wave * 0.5));

    // Gentle central twist that slowly reverses direction.
    float radius = length(p);
    float twist = strength * (0.66 * sin(EffectTime * 0.38) + 0.16 * sin(EffectTime * 0.23))
            * (1.0 - smoothstep(0.0, 0.85, radius));
    warped = rotation(twist) * warped;
    warped += vec2(sin(p.y * 8.0 + ticks * 3.14159265 / 190.0),
                   sin(p.x * 7.0 - ticks * 3.14159265 / 170.0))
            * (0.008 * strength * smoothstep(0.0, 0.15, radius));

    // Cured-only breathing: a slow, small expansion/contraction around the fixed crosshair.
    warped *= 1.0 + 0.018 * strength * sin(EffectTime * 0.72);

    // Anchor the border so twisting never exposes black corners or mirrored terrain.
    vec2 edge = min(uv, 1.0 - uv);
    float edgeFade = smoothstep(0.0, 0.16, min(edge.x, edge.y));
    vec2 result = uv + (warped - p) / vec2(aspect, 1.0) * edgeFade;
    vec2 halfPixel = 0.5 / InSize;
    return clamp(result, halfPixel, 1.0 - halfPixel);
}

void main() {
    float strength = clamp(Strength, 0.0, 1.0);
    float cured = clamp(CuredStrength, 0.0, 1.0);
    vec3 color = texture(DiffuseSampler, distortedUv(texCoord, cured)).rgb;
    // DrugCannabis.superSaturationHallucinationStrength: clamp(value / 0.5) * 0.3.
    float intensification = clamp(strength / 0.5, 0.0, 1.0) * 0.3;
    color = mix(color, getIntensifiedColor(color), intensification);
    color *= 1.0 + 0.12 * strength;
    float breath = 0.5 + 0.5 * sin(EffectTime * 0.72);
    // Gentle amber light breathing, strongest in the periphery, exclusive to cured buds.
    float peripheral = smoothstep(0.10, 0.65, length(texCoord - 0.5));
    color += vec3(0.040, 0.023, 0.006) * cured * breath * peripheral;
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
