#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float alpha = texture(Sampler0, texCoord0).a * vertexColor.a * ColorModulator.a;
    if (alpha == 0.0) {
        discard;
    }

    // GUI_TEXTURED_PREMULTIPLIED_ALPHA expects premultiplied output.
    fragColor = vec4(ColorModulator.rgb * alpha, alpha);
}
