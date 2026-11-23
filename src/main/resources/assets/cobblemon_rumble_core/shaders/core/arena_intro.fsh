#version 150
in vec2 uv;
uniform vec4 Scene;
out vec4 fragColor;

void main() {
    vec2 p = uv;
    float t = Scene.x;
    float aspect = Scene.y;
    float seam = p.x - 0.5 + (p.y - 0.5) * 0.17;
    float right = smoothstep(-0.001, 0.001, seam);
    vec3 dark = mix(vec3(0.035, 0.066, 0.14), vec3(0.09, 0.042, 0.15), right);
    vec3 light = mix(vec3(0.13, 0.48, 0.64), vec3(0.61, 0.19, 0.41), right);
    vec2 d = p - vec2(mix(0.26, 0.74, right), 0.52);
    d.x *= aspect * 0.76;
    float glow = exp(-6.8 * dot(d,d));
    vec3 col = mix(dark,light,glow);
    float streak = p.y * 29.0 - p.x * 15.0 + t * 1.5;
    float line = 1.0 - smoothstep(0.02, 0.10, abs(fract(streak)-0.5));
    col += light * line * (0.12 + 0.16 * Scene.z);
    float fine = sin((p.y * 2.0 - p.x) * 570.0) * 0.012;
    col += fine;
    float edge = abs(seam);
    col += vec3(0.62,0.27,0.13) * exp(-edge*110.0) * (0.28 + Scene.z * 0.52);
    col = mix(col, vec3(1.0,0.81,0.46), 1.0 - smoothstep(0.0007,0.0025,edge));
    col *= 1.0 - 0.32 * smoothstep(0.34,0.94,length((p-0.5)*vec2(1.2,1.0)));
    fragColor = vec4(col, 1.0);
}
