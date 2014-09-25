package au.gov.ga.worldwind.openday;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

import java.util.HashSet;
import java.util.Set;

public enum LayerGroupEnabler
{
	INSTANCE;

	public final static String GROUP_KEY = "Group";

	private LayerList layers;
	private int group = 0;
	private int count = 1;

	public LayerList getLayers()
	{
		return layers;
	}

	public void setLayers(LayerList layers)
	{
		this.layers = layers;
		count = calculateCount();
		move(0);
	}

	public void next()
	{
		move(1);
	}

	public void previous()
	{
		move(-1);
	}
	
	public int get()
	{
		return group;
	}
	
	public void set(int group)
	{
		this.group = ((group % count) + count) % count;

		if (layers == null)
		{
			return;
		}

		for (Layer layer : layers)
		{
			String layerGroupValue = (String) layer.getValue(GROUP_KEY);
			if (layerGroupValue != null)
			{
				boolean enable = false;
				String[] layerGroups = layerGroupValue.split("\\s*,\\s*");
				for (String layerGroup : layerGroups)
				{
					layerGroup = layerGroup.trim();
					if (layerGroup.equals("" + this.group))
					{
						enable = true;
					}
				}
				layer.setEnabled(enable);
			}
		}
	}

	protected int calculateCount()
	{
		if (layers == null)
		{
			return 1;
		}

		Set<Integer> set = new HashSet<Integer>();
		for (Layer layer : layers)
		{
			String layerGroupValue = (String) layer.getValue(GROUP_KEY);
			if (layerGroupValue != null)
			{
				String[] layerGroups = layerGroupValue.split("\\s*,\\s*");
				for (String layerGroup : layerGroups)
				{
					try
					{
						int i = Integer.parseInt(layerGroup);
						set.add(i);
					}
					catch (Exception e)
					{
						//ignore
					}
				}
			}
		}
		return Math.max(1, set.size());
	}

	public void move(int steps)
	{
		set(group + steps);
	}
}
