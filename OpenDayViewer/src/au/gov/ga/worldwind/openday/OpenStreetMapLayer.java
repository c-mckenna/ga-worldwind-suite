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
package au.gov.ga.worldwind.openday;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.layers.mercator.MercatorTextureTile;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

import java.net.MalformedURLException;
import java.net.URL;

import javax.media.opengl.GLProfile;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Mercator tiled image layer that displays maps from the OpenStreetMap.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class OpenStreetMapLayer extends BasicMercatorTiledImageLayer
{
	public OpenStreetMapLayer()
	{
		super(makeLevels());
		this.setForceLevelZeroLoads(true);
		this.setRetainLevelZeroTiles(true);
		this.setSplitScale(1.2);
	}

	private static LevelSet makeLevels()
	{
		AVList params = new AVListImpl();

		params.setValue(AVKey.TILE_WIDTH, 256);
		params.setValue(AVKey.TILE_HEIGHT, 256);
		params.setValue(AVKey.DATA_CACHE_NAME, "OpenStreetMap Mapnik");
		params.setValue(AVKey.SERVICE, "http://a.tile.openstreetmap.org/");
		params.setValue(AVKey.DATASET_NAME, "mapnik");
		params.setValue(AVKey.FORMAT_SUFFIX, ".png");
		params.setValue(AVKey.NUM_LEVELS, 16);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
		params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
		params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());

		return new LevelSet(params);
	}

	protected boolean loadTexture(MercatorTextureTile tile, java.net.URL textureURL)
	{
		TextureData textureData;

		synchronized (this.fileLock)
		{
			textureData = readTexture(textureURL, this.isUseMipMaps());
		}

		if (textureData == null)
			return false;

		tile.setTextureData(textureData);
		if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
			this.addTileToCache(tile);

		return true;
	}

	private static TextureData readTexture(java.net.URL url, boolean useMipMaps)
	{
		try
		{
			return TextureIO.newTextureData(GLProfile.get(GLProfile.GL2), url, useMipMaps, null);
		}
		catch (Exception e)
		{
			try
			{
				URL url2 = OpenStreetMapLayer.class.getResource("OpenStreetMapBlankTile.png");
				return TextureIO.newTextureData(GLProfile.get(GLProfile.GL2), url2, useMipMaps, null);
			}
			catch (Exception e1)
			{
			}

			String msg = Logging.getMessage("layers.TextureLayer.ExceptionAttemptingToReadTextureFile", url.toString());
			Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);
			return null;
		}
	}

	private static class URLBuilder implements TileUrlBuilder
	{
		@Override
		public URL getURL(Tile tile, String imageFormat) throws MalformedURLException
		{
			int level = tile.getLevelNumber() + 3;
			int column = tile.getColumn();
			int row = (1 << (tile.getLevelNumber()) + 3) - 1 - tile.getRow();
			URL url = new URL(tile.getLevel().getService() + level + "/" + column + "/" + row + ".png");
			return url;
		}
	}

	@Override
	public String toString()
	{
		return "OpenStreetMap Mapnik";
	}
}
