#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 localCoord;
flat in int symbolType;
in vec4 vertexColor;

out vec4 fragColor;

float aa(float value) {
    return max(fwidth(value) * 1.25, 0.002);
}

float filled(float sdf) {
    return smoothstep(-aa(sdf), aa(sdf), sdf);
}

void main() {
    vec2 p = localCoord;
    vec2 centered = p - vec2(0.5);
    float mask = 0.0;

    if (symbolType == 1) {
        float d = 0.5 - abs(centered.x) - abs(centered.y);
        mask = filled(d);
    } else if (symbolType == 2) {
        float halfHeight = 0.5 - abs(centered.y);
        float d = halfHeight - p.x * 0.82;
        mask = filled(d) * step(0.08, p.x);
    } else if (symbolType == 3) {
        vec2 q = abs(centered) - vec2(0.40, 0.28);
        float rounded = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - 0.10;
        float bubble = filled(-rounded);
        float tail = step(0.18, p.x) * step(p.x, 0.48)
                * step(0.64, p.y) * step(p.y, 0.86);
        mask = max(bubble, tail);
    } else if (symbolType == 4) {
        float starField = max(abs(centered.x) * 0.55 + abs(centered.y),
                abs(centered.y) * 0.55 + abs(centered.x));
        mask = filled(0.42 - starField);
    } else if (symbolType == 5) {
        mask = filled(0.46 - length(centered));
    } else if (symbolType == 6) {
        float circle = 0.30 - length(centered - vec2(0.0, -0.08));
        float tip = 0.34 - abs(centered.x) - max(0.0, centered.y + 0.02) * 0.85;
        mask = max(filled(circle), filled(tip) * step(0.48, p.y));
    } else if (symbolType == 7) {
        float shaft = 0.08 - abs(centered.y);
        float head = 0.36 - abs(centered.x + 0.18) - abs(centered.y) * 0.65;
        mask = max(filled(shaft), filled(head));
    } else if (symbolType == 8) {
        float ring = 0.035 - abs(length(centered) - 0.34);
        float needle = 0.06 - abs(centered.x) - abs(centered.y) * 0.42;
        float cross = 0.045 - max(abs(centered.x), abs(centered.y));
        mask = max(filled(ring), filled(needle));
        mask = max(mask, filled(cross));
    } else if (symbolType == 9) {
        float line = 0.10 - abs(centered.y);
        float taper = smoothstep(0.0, 0.12, p.x)
                * smoothstep(1.0, 0.88, p.x);
        mask = filled(line) * taper;
    } else if (symbolType == 10) {
        mask = filled(0.42 - length(centered));
    } else {
        mask = filled(0.46 - length(centered));
    }

    if (mask <= 0.001 || vertexColor.a <= 0.001) discard;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * mask) * ColorModulator;
}
