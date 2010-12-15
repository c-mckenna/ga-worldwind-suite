package au.gov.ga.worldwind.common.layers.point.providers;

import gov.nasa.worldwind.formats.shapefile.DBaseRecord;
import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.formats.shapefile.ShapefileUtils;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.VecBuffer;

import java.net.URL;
import java.util.logging.Level;

import au.gov.ga.worldwind.common.layers.point.AbstractPointProvider;
import au.gov.ga.worldwind.common.layers.point.PointLayer;
import au.gov.ga.worldwind.common.layers.point.PointProvider;
import au.gov.ga.worldwind.common.util.URLUtil;

/**
 * {@link PointProvider} implementation which loads points from a zipped
 * shapefile.
 * 
 * @author Michael de Hoog
 */
public class ShapefilePointProvider extends AbstractPointProvider
{
	private Sector sector;

	@Override
	protected boolean doLoadPoints(URL url, PointLayer layer)
	{
		try
		{
			Shapefile shapefile = ShapefileUtils.openZippedShapefile(URLUtil.urlToFile(url));
			while (shapefile.hasNext())
			{
				ShapefileRecord record = shapefile.nextRecord();
				DBaseRecord values = record.getAttributes();

				for (int part = 0; part < record.getNumberOfParts(); part++)
				{
					VecBuffer buffer = record.getPointBuffer(part);
					int size = buffer.getSize();
					for (int i = 0; i < size; i++)
					{
						layer.addPoint(buffer.getPosition(i), values);
					}
				}
			}

			sector = Sector.fromDegrees(shapefile.getBoundingRectangle());
			layer.loadComplete();
		}
		catch (Exception e)
		{
			String message = "Error loading points";
			Logging.logger().log(Level.SEVERE, message, e);
			return false;
		}
		return true;
	}

	@Override
	public Sector getSector()
	{
		return sector;
	}
}
