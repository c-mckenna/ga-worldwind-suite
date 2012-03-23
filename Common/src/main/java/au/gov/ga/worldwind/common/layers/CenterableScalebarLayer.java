package au.gov.ga.worldwind.common.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.Point;

public class CenterableScalebarLayer extends ScalebarLayer
{
	@Override
	public void draw(DrawContext dc)
	{
		if (getPosition().equals(AVKey.CENTER))
		{
			Point center = dc.getViewportCenterScreenPoint();
			setLocationCenter(new Vec4(center.x, center.y));
		}
		super.draw(dc);
	}
}
