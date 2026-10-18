#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out float uiOutline;

const int UI_EDGE_SLACK = 1;

float uiEdge(vec4 position, vec4 color) {
    if (color.r < 0.99 || color.g < 0.99 || color.b < 0.99 || color.a < 0.99) return 0.0;

    int screenWidth = int(round(2.0 / ProjMat[0][0]));
    int x = int(round(position.x));
    int widths[2] = int[2](264, 408);

    for (int i = 0; i < 2; i++) {
        int left = (screenWidth - widths[i]) / 2;
        int right = left + widths[i];
        if (abs(x - left) <= UI_EDGE_SLACK || abs(x - right) <= UI_EDGE_SLACK) return 1.0;
    }
    return 0.0;
}

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPosition;

    vertexColor = Color;
    uiOutline = uiEdge(viewPosition, Color);
}
