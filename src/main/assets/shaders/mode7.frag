#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform float u_cameraX;
uniform float u_cameraY;
uniform float u_angle;
uniform float u_scale;
uniform float u_fov;
uniform float u_screenWidth;
uniform float u_screenHeight;
uniform float u_horizon;

void main() {
    vec2 fragCoord = gl_FragCoord.xy;

    float row = u_screenHeight - fragCoord.y;
    float rowDistance = u_scale / row;

    float cosA = cos(u_angle);
    float sinA = sin(u_angle);

    float offset = (fragCoord.x - u_screenWidth / 2.0) / (u_screenWidth / 2.0) * u_fov;

    float sampleX = u_cameraX + rowDistance * cosA - offset * rowDistance * sinA;
    float sampleY = u_cameraY + rowDistance * sinA + offset * rowDistance * cosA;

    vec2 sampleUV = vec2(mod(sampleX, 1.0), mod(sampleY, 1.0));

    gl_FragColor = texture2D(u_texture, sampleUV);
}
