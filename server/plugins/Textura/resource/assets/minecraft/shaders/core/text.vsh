#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 3) in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif

layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;

#ifdef IS_GUI
#define TEXTURA_HUD_QUARTER_SHADOW 0

const int TEXTURA_HUD_GLYPH_RED = 254;
const int TEXTURA_HUD_SHADOW_RED = 253;
const int TEXTURA_HUD_QUARTER_RED = 63;
const int TEXTURA_HUD_STEP = 16;
const int TEXTURA_HUD_ANCHORS = 8;
const int TEXTURA_HUD_PALETTE_SIZE = 16;

const float TEXTURA_HUD_BAR_TEXT_TOP = 3.0;
const float TEXTURA_HUD_LINE_HEIGHT = 9.0;
const float TEXTURA_HUD_MARGIN_X = 2.0;
const float TEXTURA_HUD_MARGIN_BOTTOM = 2.0;
const float TEXTURA_HUD_LIFT_STEP = 12.0;
const float TEXTURA_HUD_BAR_STEP = 19.0;
const float TEXTURA_HUD_CENTER_TOP = 32.0;
const float TEXTURA_HUD_FADE_STEPS = 16.0;

const vec3 TEXTURA_HUD_PALETTE[16] = vec3[16](
    vec3(0.0, 0.0, 0.0),
    vec3(0.0, 0.0, 170.0),
    vec3(0.0, 170.0, 0.0),
    vec3(0.0, 170.0, 170.0),
    vec3(170.0, 0.0, 0.0),
    vec3(170.0, 0.0, 170.0),
    vec3(255.0, 170.0, 0.0),
    vec3(170.0, 170.0, 170.0),
    vec3(85.0, 85.0, 85.0),
    vec3(85.0, 85.0, 255.0),
    vec3(85.0, 255.0, 85.0),
    vec3(85.0, 255.0, 255.0),
    vec3(255.0, 85.0, 85.0),
    vec3(255.0, 85.0, 255.0),
    vec3(255.0, 255.0, 85.0),
    vec3(255.0, 255.0, 255.0)
);

int texturaHudDecode(vec4 color, out int palette, out int lift, out bool shadow) {
    ivec3 c = ivec3(round(color.rgb * 255.0));
    palette = 0;
    lift = 0;
    shadow = false;

    int step = TEXTURA_HUD_STEP;
    if (c.r == TEXTURA_HUD_GLYPH_RED) {
        shadow = false;
    } else if (c.r == TEXTURA_HUD_SHADOW_RED) {
        shadow = true;
#if TEXTURA_HUD_QUARTER_SHADOW
    } else if (c.r == TEXTURA_HUD_QUARTER_RED) {
        shadow = true;
        step = TEXTURA_HUD_STEP / 4;
#endif
    } else {
        return 0;
    }

    if (c.g % step != 0) return 0;
    int anchor = c.g / step;
    int blue = c.b * (TEXTURA_HUD_STEP / step);
    int index = blue / TEXTURA_HUD_STEP;
    if (anchor < 1 || anchor > TEXTURA_HUD_ANCHORS || index >= TEXTURA_HUD_PALETTE_SIZE) return 0;

    palette = index;
    lift = blue % TEXTURA_HUD_STEP;
    return anchor;
}

vec2 texturaHudOffset(int anchor) {
    if (anchor == 7) return vec2(0.0, -TEXTURA_HUD_BAR_STEP);
    float guiWidth = 2.0 / abs(ProjMat[0][0]);
    float guiHeight = 2.0 / abs(ProjMat[1][1]);
    if (anchor == 8) return vec2(0.0, floor(guiHeight * 0.5) - TEXTURA_HUD_BAR_TEXT_TOP - TEXTURA_HUD_CENTER_TOP);
    float originX = floor(ceil(guiWidth - 0.01) * 0.5);

    int column = (anchor - 1) % 2;
    int row = (anchor - 1) / 2;

    float dx = column == 0
        ? TEXTURA_HUD_MARGIN_X - originX
        : guiWidth - TEXTURA_HUD_MARGIN_X - originX;

    float dy = 0.0;
    if (row == 1) {
        dy = floor(guiHeight * 0.5) - TEXTURA_HUD_BAR_TEXT_TOP;
    } else if (row == 2) {
        dy = guiHeight - TEXTURA_HUD_MARGIN_BOTTOM - TEXTURA_HUD_LINE_HEIGHT - TEXTURA_HUD_BAR_TEXT_TOP;
    }
    return vec2(dx, dy);
}
#endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif

#ifdef IS_GUI
    int hudPalette;
    int hudLift;
    bool hudShadow;
    int hudAnchor = texturaHudDecode(Color, hudPalette, hudLift, hudShadow);
    if (hudAnchor > 0) {
        vec2 offset;
        float fade = 1.0;
        if (hudAnchor == 8) {
            offset = texturaHudOffset(hudAnchor) - vec2(0.0, float(hudLift));
            fade = 1.0 - float(hudLift) / TEXTURA_HUD_FADE_STEPS;
        } else {
            offset = texturaHudOffset(hudAnchor) - vec2(0.0, float(hudLift) * TEXTURA_HUD_LIFT_STEP);
        }
        gl_Position.x += offset.x * ProjMat[0][0] * gl_Position.w;
        gl_Position.y += offset.y * ProjMat[1][1] * gl_Position.w;

        vec3 rgb = TEXTURA_HUD_PALETTE[hudPalette];
        if (hudShadow) rgb = floor(rgb * 0.25);
        vertexColor = vec4(rgb / 255.0, Color.a * fade);
    }
#endif

    texCoord0 = UV0;
}
