package au.gov.ga.worldwind.openday;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

import java.net.MalformedURLException;
import java.net.URL;

import au.gov.ga.worldwind.common.terrain.SharedLockBasicElevationModel;

public class TerrainServiceElevationModel extends SharedLockBasicElevationModel
{
	public TerrainServiceElevationModel()
	{
		super(createParams());
	}

	public static AVList createParams()
	{
		AVList params = new AVListImpl();

		params.setValue(AVKey.TILE_WIDTH, 256);
		params.setValue(AVKey.TILE_HEIGHT, 256);
		params.setValue(AVKey.DATA_CACHE_NAME, "GA/Elevation/terrain-service");
		params.setValue(AVKey.SERVICE, "http://localhost:8080/tile/");
		params.setValue(AVKey.DATASET_NAME, "elevation");
		params.setValue(AVKey.FORMAT_SUFFIX, ".bil");
		params.setValue(AVKey.NUM_LEVELS, 11);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(180d), Angle.fromDegrees(180d)));
		params.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);
		params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());
		params.setValue(AVKey.DETAIL_HINT, 0.5);

		return params;
	}

	private static class URLBuilder implements TileUrlBuilder
	{
		@Override
		public URL getURL(Tile tile, String imageFormat) throws MalformedURLException
		{
			int level = tile.getLevelNumber();
			int column = tile.getColumn();
			int row = tile.getRow();
			URL url = new URL(tile.getLevel().getService() + level + "/" + column + "/" + row + ".bil");
			return url;
		}
	}
}
