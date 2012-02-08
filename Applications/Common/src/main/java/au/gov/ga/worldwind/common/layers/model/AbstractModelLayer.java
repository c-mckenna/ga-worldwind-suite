package au.gov.ga.worldwind.common.layers.model;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.WWTexture;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import au.gov.ga.worldwind.common.util.FastShape;

public abstract class AbstractModelLayer extends AbstractLayer implements ModelLayer
{
	protected final List<FastShape> shapes = new ArrayList<FastShape>();
	protected WWTexture pointTexture;
	protected WWTexture blankTexture;

	protected boolean sectorDirty = true;
	protected Sector sector;
	protected Double minimumDistance;

	protected Color color;
	protected Double lineWidth;
	protected Double pointSize;
	protected boolean wireframe = false;

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
	public Sector getSector()
	{
		synchronized (shapes)
		{
			if (sectorDirty)
			{
				sector = null;
				for (FastShape shape : shapes)
				{
					sector = Sector.union(sector, shape.getSector());
				}
				sectorDirty = false;
			}
		}
		return sector;
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
	public void setup(WorldWindow wwd)
	{
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
		shape.setPointTextureUrl(this.getClass().getResource("sprite.png"));
		shape.setWireframe(isWireframe());
		
		synchronized (shapes)
		{
			shapes.add(shape);
		}
		sectorDirty = true;
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
		sectorDirty = true;
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