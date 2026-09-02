#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in vec4 Color;

out vec2 localCoord;
flat out ivec2 panelParams;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    localCoord = UV0;
    panelParams = UV1;
    vertexColor = Color;
}
