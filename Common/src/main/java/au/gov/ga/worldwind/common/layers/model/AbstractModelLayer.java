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
package au.gov.ga.worldwind.common.layers.model;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.WWTexture;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import au.gov.ga.worldwind.common.layers.Bounds;
import au.gov.ga.worldwind.common.render.fastshape.FastShape;

/**
 * Abstract implementation of the {@link ModelLayer}. Contains the common
 * functionality for setting up and rendering.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public abstract class AbstractModelLayer extends AbstractLayer implements ModelLayer
{
	protected final List<FastShape> shapes = new ArrayList<FastShape>();
	protected WWTexture pointTexture;
	protected WWTexture blankTexture;

	protected Bounds bounds;
	protected Double minimumDistance;

	protected Color color;
	protected Double lineWidth;
	protected Double pointSize;
	protected boolean wireframe = false;
	protected boolean useOrderedRendering = false;

	protected boolean reverseNormals = false;
	protected boolean pointSprite = false;
	protected Double pointMinSize = 2d;
	protected Double pointMaxSize = 1000d;
	protected Double pointConstantAttenuation = 0d;
	protected Double pointLinearAttenuation = 0d;
	protected Double pointQuadraticAttenuation = 6E-12d;

	protected final HierarchicalListenerList hierarchicalListenerList = new HierarchicalListenerList();
	protected final ModelLayerTreeNode treeNode = new ModelLayerTreeNode(this);

	protected abstract void requestData();

	@Override
	protected void doRender(DrawContext dc)
	{
		if (!isEnabled())
		{
			return;
		}

		requestData();

		synchronized (shapes)
		{
			for (FastShape shape : shapes)
			{
				if (minimumDistance != null)
				{
					Extent extent = shape.getExtent();
					if (extent != null)
					{
						double distanceToEye =
								extent.getCenter().distanceTo3(dc.getView().getEyePoint()) - extent.getRadius();
						if (distanceToEye > minimumDistance)
						{
							continue;
						}
					}
				}

				shape.render(dc);
			}
		}
	}

	@Override
	public Bounds getBounds()
	{
		synchronized (shapes)
		{
			if (bounds == null)
			{
				for (FastShape shape : shapes)
				{
					bounds = Bounds.union(bounds, shape.getBounds());
				}
			}
		}
		return bounds;
	}

	@Override
	public boolean isFollowTerrain()
	{
		for (FastShape shape : shapes)
		{
			return shape.isFollowTerrain();
		}
		return false;
	}

	public Double getMinimumDistance()
	{
		return minimumDistance;
	}

	public void setMinimumDistance(Double minimumDistance)
	{
		this.minimumDistance = minimumDistance;
	}

	@Override
	public boolean isWireframe()
	{
		return wireframe;
	}

	@Override
	public void setWireframe(boolean wireframe)
	{
		this.wireframe = wireframe;
		synchronized (shapes)
		{
			for (FastShape shape : shapes)
			{
				shape.setWireframe(wireframe);
			}
		}
	}

	public boolean isPointSprite()
	{
		return pointSprite;
	}

	public void setPointSprite(boolean pointSprite)
	{
		this.pointSprite = pointSprite;
		synchronized (shapes)
		{
			for (FastShape shape : shapes)
			{
				shape.setPointSprite(pointSprite);
			}
		}
	}

	@Override
	public void addShape(FastShape shape)
	{
		if (color != null)
		{
			shape.setColor(color);
		}
		shape.setLineWidth(lineWidth);
		shape.setPointSize(pointSize);
		shape.setPointMinSize(pointMinSize);
		shape.setPointMaxSize(pointMaxSize);
		shape.setPointConstantAttenuation(pointConstantAttenuation);
		shape.setPointLinearAttenuation(pointLinearAttenuation);
		shape.setPointQuadraticAttenuation(pointQuadraticAttenuation);
		shape.setPointSprite(pointSprite);
		shape.setPointTextureUrl(getClass().getResource("/images/pointsprite.png"));
		shape.setWireframe(isWireframe());
		shape.setReverseNormals(reverseNormals);
		shape.setUseOrderedRendering(useOrderedRendering);

		synchronized (shapes)
		{
			shapes.add(shape);
		}
		bounds = null;
		treeNode.addChild(shape);
		hierarchicalListenerList.notifyListeners(this, treeNode);
	}

	@Override
	public void removeShape(FastShape shape)
	{
		synchronized (shapes)
		{
			shapes.remove(shape);
		}
		bounds = null;
		treeNode.removeChild(shape);
		hierarchicalListenerList.notifyListeners(this, treeNode);
	}

	@Override
	public void addHierarchicalListener(HierarchicalListener listener)
	{
		hierarchicalListenerList.add(listener);
		hierarchicalListenerList.notifyListeners(this, treeNode);
	}

	@Override
	public void removeHierarchicalListener(HierarchicalListener listener)
	{
		hierarchicalListenerList.remove(listener);
	}
}
