/*******************************************************************************
 * urmusic - The Free and Open Source Music Visualizer Tool
 * Copyright (C) 2018  nasso (https://github.com/nasso)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * Contact "nasso": nassomails -at- gmail dot com
 ******************************************************************************/
#version 330 core

uniform vec4 sigma_radius_offsetScale;
uniform sampler2D inputTex;

#define uSigma sigma_radius_offsetScale.x
#define uRadius sigma_radius_offsetScale.y
#define uOffsetScale sigma_radius_offsetScale.zw

in vec2 pass_quad_uv;

out vec4 out_color;

// TODO:	Make use of the bilinear sampling trick to reduce
//  		the number of texture fetches needed for a gaussian blur.

void main() {
	vec4 original_color = texture(inputTex, pass_quad_uv);
	
	int iRadius = int(uRadius);
	
    if (iRadius == 0) {
        out_color = original_color;
        return;
	}
	
	// Incremental Gaussian Coefficient Calculation (See GPU Gems 3 pp. 877 - 889)
	// Also: https://github.com/servo/webrender/blob/master/webrender/res/cs_blur.glsl
	vec3 gauss_coefficient;
    gauss_coefficient.x = 1.0 / (sqrt(2.0 * 3.14159265) * uSigma);
    gauss_coefficient.y = exp(-0.5 / (uSigma * uSigma));
    gauss_coefficient.z = gauss_coefficient.y * gauss_coefficient.y;

    float gauss_coefficient_sum = 0.0;
    vec4 avg_color = original_color * gauss_coefficient.x;
    gauss_coefficient_sum += gauss_coefficient.x;
    gauss_coefficient.xy *= gauss_coefficient.yz;

    for (int i=1 ; i <= iRadius ; ++i) {
        vec2 offset = uOffsetScale * float(i);

        vec2 st0 = pass_quad_uv.xy - offset;
        avg_color += texture(inputTex, st0) * gauss_coefficient.x;

        vec2 st1 = pass_quad_uv.xy + offset;
        avg_color += texture(inputTex, st1) * gauss_coefficient.x;

        gauss_coefficient_sum += 2.0 * gauss_coefficient.x;
        gauss_coefficient.xy *= gauss_coefficient.yz;
    }

	out_color = avg_color / gauss_coefficient_sum;
}
