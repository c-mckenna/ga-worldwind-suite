package au.gov.ga.worldwind.openday;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.WWIO;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import au.gov.ga.worldwind.common.util.AVKeyMore;

public class LayerFactory extends au.gov.ga.worldwind.common.layers.LayerFactory
{
	public Object createFromConfigSource(Object configSource, AVList params)
	{
		if (params == null || params.getValue(AVKeyMore.CONTEXT_URL) == null)
		{
			URL url = WWIO.makeURL(configSource);
			if (url == null && configSource instanceof String)
			{
				url = getClass().getResource("/" + configSource);
				if (url == null)
				{
					try
					{
						URL context = new File(".").getAbsoluteFile().toURI().toURL();
						url = new URL(context, (String) configSource);
					}
					catch (MalformedURLException e)
					{
					}
				}
			}
			if (url != null)
			{
				if (params == null)
				{
					params = new AVListImpl();
				}
				params.setValue(AVKeyMore.CONTEXT_URL, url);
			}
		}
		return super.createFromConfigSource(configSource, params);
	}
}
