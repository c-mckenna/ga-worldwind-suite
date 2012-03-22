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
package au.gov.ga.worldwind.common.layers.temporal;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ScreenCredit;

import java.awt.Point;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.xpath.XPath;

import org.w3c.dom.Element;

import au.gov.ga.worldwind.common.layers.tiled.image.delegate.DelegatorTiledImageLayer;
import au.gov.ga.worldwind.common.util.AVKeyMore;
import au.gov.ga.worldwind.common.util.BigDate;
import au.gov.ga.worldwind.common.util.XMLUtil;

/**
 * {@link TemporalLayer} implementation that fades between multiple
 * {@link DelegatorTiledImageLayer}s. Each layer has an associated
 * {@link BigDate}, and this layer renders the two layers either side of the
 * specified date, with appropriate opacity.
 * <p/>
 * Layer definition is the same for a standard {@link DelegatorTiledImageLayer},
 * with the following modification:
 * 
 * <pre>
 * &lt;Dates&gt;
 *   &lt;Date mya="mya" id="id" label="label" /&gt;
 *   &lt;Date mya="mya" id="id" label="label" /&gt;
 * &lt;/Date&gt;
 * </pre>
 * 
 * Where:
 * <ul>
 * <li>mya = Date (in millions of years ago) to associate with the layer.
 * <li>id = Relative directory of the layer's dataset. This is appended on to
 * the end of the layer's DatasetName, separated by a '/'.
 * <li>label = Layer's display name.
 * </ul>
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class TemporalTiledImageLayer extends AbstractLayer implements TemporalLayer
{
	private final NavigableMap<BigDate, Layer> layers;
	private BigDate date = BigDate.now();

	@SuppressWarnings("unchecked")
	public TemporalTiledImageLayer(AVList params)
	{
		String s = params.getStringValue(AVKey.DISPLAY_NAME);
		if (s != null)
			this.setName(s);

		Double d = (Double) params.getValue(AVKey.OPACITY);
		if (d != null)
			this.setOpacity(d);

		d = (Double) params.getValue(AVKey.MAX_ACTIVE_ALTITUDE);
		if (d != null)
			this.setMaxActiveAltitude(d);

		d = (Double) params.getValue(AVKey.MIN_ACTIVE_ALTITUDE);
		if (d != null)
			this.setMinActiveAltitude(d);

		d = (Double) params.getValue(AVKey.MAP_SCALE);
		if (d != null)
			this.setValue(AVKey.MAP_SCALE, d);

		Boolean b = (Boolean) params.getValue(AVKey.NETWORK_RETRIEVAL_ENABLED);
		if (b != null)
			this.setNetworkRetrievalEnabled(b);

		ScreenCredit sc = (ScreenCredit) params.getValue(AVKey.SCREEN_CREDIT);
		if (sc != null)
			this.setScreenCredit(sc);

		this.setValue(AVKey.CONSTRUCTION_PARAMETERS, params.copy());

		this.layers = (NavigableMap<BigDate, Layer>) params.getValue(AVKeyMore.TEMPORAL_LAYERS);

		for (Layer layer : layers.values())
		{
			layer.addPropertyChangeListener(this);
		}

		//TODO TEMP: this is a temporary JSlider to control the time of this layer
		int min = (int) (layers.firstKey().numberOfYearsAgo() / -1e6);
		int max = (int) (layers.lastKey().numberOfYearsAgo() / -1e6);
		JFrame frame = new JFrame(getName() + " time slider");
		final JSlider slider = new JSlider(min, max, 0);
		frame.add(slider);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				setDate(BigDate.fromMa(-slider.getValue()));
				firePropertyChange(AVKey.LAYER, null, TemporalTiledImageLayer.this);
			}
		});
		//TEMP: end temporary JSlider
	}

	public TemporalTiledImageLayer(Element domElement, AVList params)
	{
		this(getParamsFromDocument(domElement, params));
	}

	public static AVList getParamsFromDocument(Element domElement, AVList params)
	{
		if (params == null)
			params = new AVListImpl();
		params = DelegatorTiledImageLayer.getParamsFromDocument(domElement, params);

		String displayName = (String) params.getValue(AVKey.DISPLAY_NAME);
		String datasetName = (String) params.getValue(AVKey.DATASET_NAME);
		String dataCacheName = (String) params.getValue(AVKey.DATA_CACHE_NAME);

		NavigableMap<BigDate, Layer> layers = new TreeMap<BigDate, Layer>();
		XPath xpath = XMLUtil.makeXPath();
		Element[] dateElements = XMLUtil.getElements(domElement, "Dates/Date", xpath);
		if (dateElements != null)
		{
			for (Element dateElement : dateElements)
			{
				Double mya = XMLUtil.getDouble(dateElement, "@mya", xpath);
				String id = XMLUtil.getText(dateElement, "@id", xpath);
				String label = XMLUtil.getText(dateElement, "@label", xpath);

				if (mya != null)
				{
					BigDate date = BigDate.fromMa(mya);
					params.setValue(AVKey.DATASET_NAME, datasetName + "/" + id);
					params.setValue(AVKey.DATA_CACHE_NAME, dataCacheName + "/" + id);
					params.setValue(AVKey.DISPLAY_NAME, label);
					Layer layer = new DelegatorTiledImageLayer(params);
					layers.put(date, layer);
				}
			}
		}
		params.setValue(AVKeyMore.TEMPORAL_LAYERS, layers);

		params.setValue(AVKey.DISPLAY_NAME, displayName);
		params.setValue(AVKey.DATASET_NAME, datasetName);
		params.setValue(AVKey.DATA_CACHE_NAME, dataCacheName);

		return params;
	}

	@Override
	public BigDate getDate()
	{
		return date;
	}

	@Override
	public void setDate(BigDate date)
	{
		this.date = date;
	}

	@Override
	protected void doRender(DrawContext dc)
	{
		renderOrPick(dc, false, null);
	}

	@Override
	protected void doPick(DrawContext dc, Point point)
	{
		renderOrPick(dc, true, point);
	}

	protected void renderOrPick(DrawContext dc, boolean pick, Point pickPoint)
	{
		Entry<BigDate, Layer> floor = layers.floorEntry(date);
		Entry<BigDate, Layer> ceiling = layers.ceilingEntry(date);

		if (floor == null && ceiling == null)
			return;

		if (floor == null || ceiling == null || floor.getKey().seconds == ceiling.getKey().seconds)
		{
			Layer layer = floor != null ? floor.getValue() : ceiling.getValue();
			layer.setOpacity(getOpacity());
			if (pick)
			{
				layer.pick(dc, pickPoint);
			}
			else
			{
				layer.render(dc);
			}
		}
		else
		{
			Layer floorLayer = floor.getValue();
			Layer ceilingLayer = ceiling.getValue();
			double percent =
					(date.seconds - floor.getKey().seconds)
							/ (double) (ceiling.getKey().seconds - floor.getKey().seconds);
			floorLayer.setOpacity(getOpacity());
			ceilingLayer.setOpacity(getOpacity() * percent);
			if (pick)
			{
				floorLayer.pick(dc, pickPoint);
				ceilingLayer.pick(dc, pickPoint);
			}
			else
			{
				floorLayer.render(dc);
				ceilingLayer.render(dc);
			}
		}

		if (!pick)
		{
			//Render the layer before the floor, and the layer after the ceiling, so
			//that the layers either side of the current date have data loaded too.
			//This makes the data loading quicker when dragging the time slider.
			double smallOpacity = 1e-6;
			if (floor != null)
			{
				Entry<BigDate, Layer> previous = layers.lowerEntry(floor.getKey());
				if (previous != null)
				{
					previous.getValue().setOpacity(smallOpacity);
					previous.getValue().render(dc);
				}
			}
			if (ceiling != null)
			{
				Entry<BigDate, Layer> next = layers.higherEntry(ceiling.getKey());
				if (next != null)
				{
					next.getValue().setOpacity(smallOpacity);
					next.getValue().render(dc);
				}
			}
		}
	}
}
