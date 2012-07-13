//--------------------------------------------------------------------------------------
// Order Independent Transparency with Average Color
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#extension ARB_draw_buffers : require

uniform sampler2D tex0;
uniform sampler2D tex1;

varying vec3 N;
varying vec3 V;

void main(void)
{
	//vec4 alpha = texture2D(tex1, gl_TexCoord[1].st);
	//alpha mask - drop frags less than 1
	//if(alpha.a < 1.0) discard;
	
	//vec4 color = texture2D(tex0, gl_TexCoord[0].st);
	//vec4 color = clamp(gl_Color + vec4(0.1), 0.0, 1.0);
	//vec4 color = fract(gl_Color + vec4(0.1));
	
	vec3 L = normalize(gl_LightSource[1].position.xyz - V);
	vec3 E = normalize(-V);
	vec3 R = normalize(-reflect(L, N));
	
	vec4 Iamb = gl_FrontLightProduct[1].ambient;
	
	vec4 Idiff = gl_FrontLightProduct[1].diffuse * max(dot(N, L), 0.0);
	Idiff = clamp(Idiff, 0.0, 1.0);
	
	//vec4 Ispec = gl_FrontLightProduct[1].specular * pow(max(dot(R, E), 0.0), 0.3 * gl_FrontMaterial.shininess);
	//Ispec = clamp(Ispec, 0.0, 1.0);
	
	//vec4 color = gl_FrontLightModelProduct.sceneColor +
	//	gl_FrontMaterial.emission +
	//	gl_FrontMaterial.diffuse * Idiff +
	//	gl_FrontMaterial.ambient * Iamb + 
	//	gl_FrontMaterial.specular * Ispec;
	vec4 color = gl_FrontLightModelProduct.sceneColor +
		gl_FrontMaterial.emission +
		gl_FrontMaterial.diffuse * Idiff +
		gl_FrontMaterial.ambient * Iamb;
	color = clamp(color, 0.0, 1.0);

	gl_FragData[0] = vec4(color.rgb * color.a, color.a);
	gl_FragData[1] = vec4(1.0);
}
