#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float desaturation;

out vec4 fragColor;

vec3 getDesaturatedColor(vec3 color) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return vec3(gray);
}

void main() {
    vec4 texel = texture(DiffuseSampler, texCoord);
    vec3 outcolor = texel.rgb;
    if (desaturation > 0.0) {
        vec3 desaturated = getDesaturatedColor(outcolor);
        outcolor = mix(outcolor, desaturated, clamp(desaturation, 0.0, 1.0));
    }
    fragColor = vec4(outcolor, 1.0);
}
