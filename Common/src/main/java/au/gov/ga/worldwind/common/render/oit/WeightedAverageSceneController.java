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

import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.Dimension;

import javax.media.opengl.GL;

import au.gov.ga.worldwind.common.render.ExtendedSceneController;
import au.gov.ga.worldwind.common.render.FrameBuffer;

/**
 * {@link SceneController} that implements weighted average order-independant
 * transparency.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public abstract class WeightedAverageSceneController extends ExtendedSceneController
{
	private FrameBuffer frameBuffer = new FrameBuffer(2, false);
	private Pass1Shader pass1 = new Pass1Shader();
	private Pass2Shader pass2 = new Pass2Shader();

	public WeightedAverageSceneController()
	{
		frameBuffer.getTextures()[0].setTarget(GL.GL_TEXTURE_RECTANGLE_ARB);
		frameBuffer.getTextures()[0].setInternalFormat(GL.GL_RGBA16F_ARB);
		frameBuffer.getTextures()[0].setType(GL.GL_FLOAT);
		frameBuffer.getTextures()[0].setMinificationFilter(GL.GL_NEAREST);
		frameBuffer.getTextures()[0].setMagnificationFilter(GL.GL_NEAREST);

		frameBuffer.getTextures()[1].setTarget(GL.GL_TEXTURE_RECTANGLE_ARB);
		frameBuffer.getTextures()[1].setInternalFormat(GL.GL_FLOAT_R32_NV);
		frameBuffer.getTextures()[1].setType(GL.GL_FLOAT);
		frameBuffer.getTextures()[1].setMinificationFilter(GL.GL_NEAREST);
		frameBuffer.getTextures()[1].setMagnificationFilter(GL.GL_NEAREST);
	}

	@Override
	protected void draw(DrawContext dc)
	{
		GL gl = dc.getGL();

		Dimension dimensions = new Dimension(dc.getDrawableWidth(), dc.getDrawableHeight());
		frameBuffer.resize(gl, dimensions);
		pass1.createIfRequired(gl);
		pass2.createIfRequired(gl);

		frameBuffer.bind(gl);
		gl.glDrawBuffers(2, new int[] { GL.GL_COLOR_ATTACHMENT0_EXT, GL.GL_COLOR_ATTACHMENT1_EXT }, 0);

		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		gl.glBlendEquation(GL.GL_FUNC_ADD);
		gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
		gl.glEnable(GL.GL_BLEND);

		pass1.use(gl);
		super.draw(dc);
		pass1.unuse(gl);

		gl.glDisable(GL.GL_BLEND);

		frameBuffer.unbind(gl);
		gl.glDrawBuffer(GL.GL_BACK);

		pass2.use(gl);
		FrameBuffer.renderTexturedQuadUsingTarget(gl, GL.GL_TEXTURE_RECTANGLE_ARB,
				frameBuffer.getTextures()[0].getId(), frameBuffer.getTextures()[1].getId());
		pass2.unuse(gl);
	}
}
