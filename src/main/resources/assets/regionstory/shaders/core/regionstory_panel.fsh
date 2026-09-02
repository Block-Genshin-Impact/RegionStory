#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 localCoord;
flat in ivec2 panelParams;
in vec4 vertexColor;

out vec4 fragColor;

float aaWidth(float distanceValue) {
    return max(fwidth(distanceValue) * 1.25, 0.0015);
}

float insetBorderMask(float sdf, float fillMask) {
    // This is a nested contour, not an outer-edge stroke: leave a dark gutter
    // around the silhouette, then place a one-pixel line farther inside.
    const float inset = 0.085;
    float distanceFromInsetContour = abs(sdf - inset);
    float contour = 1.0 - smoothstep(0.018, 0.045, distanceFromInsetContour);
    return fillMask * contour;
}

void main() {
    vec2 p = localCoord;
    float aspect = max(1.0, float(panelParams.x) / 1000.0);
    float fadeLength = clamp(float(panelParams.y) / 1000.0, 0.01, aspect);
    float yBody = min(p.y, 1.0 - p.y);
    float leftCap = 0.5 - length(vec2(p.x - 0.5, p.y - 0.5));
    float shape = 0.0;
    float fade = 1.0;

#ifdef REGIONSTORY_OPEN_FADE
    // Left semicircle plus a flat body. There is deliberately no right cap.
    // p.x < 0.5 must stay outside the rectangular body. Multiplying by step()
    // would turn the negative distance there into zero and paint a square halo
    // behind the semicircle.
    float openBody = min(yBody, min(p.x - 0.5, aspect - p.x));
    shape = max(leftCap, openBody);
    fade = 1.0 - smoothstep(aspect - fadeLength, aspect, p.x);
#endif

#ifdef REGIONSTORY_CAPSULE
    // The standalone F key is a softly rounded square, not a pill.
    float cornerRadius = 0.22;
    vec2 halfSize = vec2(aspect * 0.5, 0.5);
    vec2 q = abs(p - halfSize) - (halfSize - vec2(cornerRadius));
    shape = cornerRadius - length(max(q, vec2(0.0))) - min(max(q.x, q.y), 0.0);
#endif

    float fillMask = smoothstep(-aaWidth(shape), aaWidth(shape), shape);
    float borderMask = insetBorderMask(shape, fillMask);
    float outerGlowMask = 0.0;

#ifdef REGIONSTORY_BAND
    // A broad lower-screen veil has no visible edge: it fades in gradually,
    // then keeps one consistent transparency through the dialogue text area.
    fillMask = smoothstep(0.0, 0.58, p.y);
    borderMask = 0.0;
    fade = 1.0;
#endif

#ifdef REGIONSTORY_OPEN_FADE
    // A sub-pixel white halo is kept strictly outside the true SDF silhouette.
    // The padded quad only supplies room for anti-aliasing; it never becomes a box.
    float outerDistance = max(0.0, -shape);
    outerGlowMask = (1.0 - fillMask)
            * (1.0 - smoothstep(0.0, 0.055, outerDistance));
#endif

    vec3 fillColor = vertexColor.rgb;
    vec3 borderColor = mix(fillColor, vec3(0.98, 0.99, 1.0), 0.86);
    vec3 color = mix(fillColor, borderColor, borderMask);
    color = mix(color, borderColor, outerGlowMask * 0.88);

    float alpha = vertexColor.a * max(fillMask, outerGlowMask * 0.13) * fade;
    if (alpha <= 0.001) discard;
    fragColor = vec4(color, alpha) * ColorModulator;
}
