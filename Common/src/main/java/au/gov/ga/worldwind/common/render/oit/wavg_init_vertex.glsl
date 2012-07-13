//--------------------------------------------------------------------------------------
// Order Independent Transparency with Average Color
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

varying vec3 N;
varying vec3 V;

void main(void)
{
	V = vec3(gl_ModelViewMatrix * gl_Vertex);
	N = normalize(gl_NormalMatrix * gl_Normal);
	
	gl_Position = ftransform();
	gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	gl_TexCoord[1] = gl_TextureMatrix[0] * gl_MultiTexCoord1;
	
	//float diffuse = abs(normalize(gl_NormalMatrix * gl_Normal).z);
	//gl_TexCoord[0].xyz = vec3(gl_Vertex.xy, diffuse);
}
