/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.worldwind.common.render.oit;

import java.io.InputStream;

import javax.media.opengl.GL;

import au.gov.ga.worldwind.common.render.Shader;

/**
 * Pass 2 shader for the weighted average order independent transparency
 * algorithm.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class Pass2Shader extends Shader
{
	@Override
	public void use(GL gl)
	{
		super.use(gl);
	}

	@Override
	protected InputStream getVertexSource()
	{
		return this.getClass().getResourceAsStream("wavg_final_vertex.glsl");
	}

	@Override
	protected InputStream getFragmentSource()
	{
		return this.getClass().getResourceAsStream("wavg_final_fragment.glsl");
	}

	@Override
	protected void getUniformLocations(GL gl)
	{
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "ColorTex0"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "ColorTex1"), 1);
	}
}
