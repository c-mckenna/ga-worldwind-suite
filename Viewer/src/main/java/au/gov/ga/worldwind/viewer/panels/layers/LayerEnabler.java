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
package au.gov.ga.worldwind.viewer.panels.layers;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.tree.TreeNode;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;
import gov.nasa.worldwindx.applications.worldwindow.core.WMSLayerInfo;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import au.gov.ga.worldwind.common.downloader.Downloader;
import au.gov.ga.worldwind.common.downloader.RetrievalHandler;
import au.gov.ga.worldwind.common.downloader.RetrievalResult;
import au.gov.ga.worldwind.common.layers.Bounded;
import au.gov.ga.worldwind.common.layers.Bounds;
import au.gov.ga.worldwind.common.layers.Hierarchical;
import au.gov.ga.worldwind.common.layers.Hierarchical.HierarchicalListener;
import au.gov.ga.worldwind.common.util.DaemonThreadFactory;
import au.gov.ga.worldwind.common.util.FileUtil;
import au.gov.ga.worldwind.common.util.Loader;
import au.gov.ga.worldwind.common.util.URLUtil;
import au.gov.ga.worldwind.viewer.panels.layers.FileLoader.FileLoadListener;
import au.gov.ga.worldwind.wmsbrowser.wmsserver.WmsCapabilitiesServiceAccessor;

/**
 * This class acts as a joiner between the WorldWind layers list & elevation
 * model, and the layer tree. It is responsible for adding/removing elevation
 * models and layers to the World Wind lists whenever they are enabled/disabled
 * in the layer tree.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class LayerEnabler implements HierarchicalListener
{
	private final LayerTree tree;

	private WorldWindow wwd;

	private SectionList<Layer> layerList;
	private SectionList<ElevationModel> elevationModel;

	private List<ILayerNode> nodes = new ArrayList<ILayerNode>();
	private List<Wrapper> wrappers = new ArrayList<Wrapper>();
	private Map<ILayerNode, Wrapper> nodeMap = new HashMap<ILayerNode, Wrapper>();

	private List<Layer> layers = new ArrayList<Layer>();
	private List<ElevationModel> elevationModels = new ArrayList<ElevationModel>();
	private Map<Layer, ILayerNode> layerMap = new HashMap<Layer, ILayerNode>();
	private Map<ElevationModel, ILayerNode> elevationModelMap = new HashMap<ElevationModel, ILayerNode>();

	private List<RefreshListener> listeners = new ArrayList<RefreshListener>();
	private final Set<Hierarchical> hierarchicalListenees = new HashSet<Hierarchical>();
	private final Set<ILayerNode> connectedHierarchicalLayerNodes = new HashSet<ILayerNode>();

	private static ExecutorService loaderService = Executors.newSingleThreadExecutor(new DaemonThreadFactory(
			"WMS layer loader"));

	public LayerEnabler(WorldWindow wwd)
	{
		this.tree = null;
		setWwd(wwd);
	}

	public LayerEnabler(LayerTree tree, WorldWindow wwd)
	{
		this.tree = tree;
		setWwd(wwd);
	}

	public LayerTree getTree()
	{
		return tree;
	}

	public void addRefreshListener(RefreshListener listener)
	{
		listeners.add(listener);
	}

	public void removeRefreshListener(RefreshListener listener)
	{
		listeners.remove(listener);
	}

	@SuppressWarnings("unchecked")
	protected synchronized void setWwd(WorldWindow wwd)
	{
		this.wwd = wwd;

		LayerList ll = wwd.getModel().getLayers();
		if (ll instanceof SectionList<?>)
		{
			layerList = (SectionList<Layer>) ll;
		}
		else
		{
			throw new IllegalStateException("Model's layer list must implement SectionList<Layer>");
		}

		ElevationModel em = wwd.getModel().getGlobe().getElevationModel();
		if (em instanceof SectionList<?>)
		{
			elevationModel = (SectionList<ElevationModel>) em;
		}
		else
		{
			throw new IllegalStateException("Globe's elevation model must implement SectionList<ElevationModel>");
		}

		layerList.registerSectionObject(this);
		elevationModel.registerSectionObject(this);

		refreshLists();
	}

	//called by LayerTreeModel
	public synchronized void enable(List<ILayerNode> nodes)
	{
		//check if the node list has changed (if not, simply call refreshLists() to enable/disable layers)
		if (!nodes.equals(this.nodes))
		{
			//build a set of added nodes
			Set<ILayerNode> added = new HashSet<ILayerNode>(nodes);
			added.removeAll(this.nodes);

			if (!added.isEmpty() || nodes.size() != this.nodes.size())
			{
				//if any nodes have been added, or the node count is not the same (therefore some
				//may have been removed; calculate the removed set, and then refresh the nodeMap

				Set<ILayerNode> removed = new HashSet<ILayerNode>(this.nodes);
				removed.removeAll(nodes);

				for (ILayerNode remove : removed)
				{
					nodeMap.remove(remove);
				}
				for (ILayerNode add : added)
				{
					Wrapper wrapper = new Wrapper(add);
					nodeMap.put(add, wrapper);
				}
			}

			//set the global here, so that handleResult can find the index of it's node in the list
			this.nodes = nodes;

			//rebuild the wrappers list so that it contains wrappers in the same order as the nodes list
			wrappers.clear();
			for (ILayerNode node : nodes)
			{
				wrappers.add(nodeMap.get(node));

				//load the layer if it has been added in this refresh
				if (added.contains(node))
				{
					loadLayer(node, true);
				}
			}
		}

		//build the layer lists and redraw
		this.nodes = nodes;
		refreshLists();
	}

	public void reloadLayer(ILayerNode node)
	{
		loadLayer(node, false);
	}

	private void loadLayer(ILayerNode node, boolean onlyIfModified)
	{
		URL url = node.getLayerURL();
		//url could possibly be null (eg KML layer's children); ignore a load request if this occurs
		if (url == null)
			return;

		File file = URLUtil.urlToFile(url);
		boolean isFile = file != null && file.isFile();
		boolean isXml = FileUtil.hasExtension(url.toString(), "xml");
		boolean isWmsLayer = node instanceof WmsLayerNode;

		setLayerLoading(node, true, true);

		if (isWmsLayer)
		{
			loadWmsLayer((WmsLayerNode) node);
		}
		else if (isFile && !isXml)
		{
			loadFile(node, file);
		}
		else
		{
			downloadLayer(node, url, onlyIfModified);
		}
	}

	private void loadWmsLayer(final WmsLayerNode node)
	{
		final int index = nodes.indexOf(node);
		if (index < 0) //layer must have been removed during loading
		{
			return;
		}

		loaderService.submit(new Runnable()
		{
			@Override
			public void run()
			{
				LoadedLayer loadedLayer;
				if (!node.isLayerInfoLoaded())
				{

					try
					{
						WMSCapabilities capabilities =
								WmsCapabilitiesServiceAccessor.getService().retrieveCapabilities(node.getLayerURL());
						capabilities.parse();
						WMSLayerCapabilities layerCapabilities = capabilities.getLayerByName(node.getLayerId());
						List<WMSLayerInfo> layerInfos = WMSLayerInfo.createLayerInfos(capabilities, layerCapabilities);
						node.setLayerInfo(layerInfos.get(0));
					}
					catch (Exception e)
					{
						e.printStackTrace();
						setLayerLoading(node, false, false);
						setError(node, e);
						return;
					}
				}

				loadedLayer =
						new LoadedLayer(new WMSTiledImageLayer(node.getWmsCapabilities(), node.getWmsParams()), node
								.getWmsParams());
				loadedLayer.setLegendURL(node.getLegendURL());

				Wrapper wrapper = wrappers.get(index);
				wrapper.setLoaded(loadedLayer);

				setLayerLoading(node, false, true);

				refreshLists();
			}
		});
	}

	private void loadFile(final ILayerNode node, File file)
	{
		FileLoadListener listener = new FileLoadListener()
		{
			@Override
			public void loaded(LoadedLayer loaded)
			{
				handleLoad(node, loaded);
			}

			@Override
			public void error(Exception e)
			{
				setLayerLoading(node, false, false);
				setError(node, e);
			}

			@Override
			public void cancelled()
			{
				setLayerLoading(node, false, false);
				setError(node, new Exception("Cancelled"));
			}
		};

		//clear any errors before attempting to load
		setError(node, null);

		//load the file using the FileLoader
		FileLoader.loadFile(file, listener, tree, WorldWind.getDataFileStore());
	}

	private synchronized void handleLoad(ILayerNode node, LoadedLayer loaded)
	{
		setLayerLoading(node, false, true);

		int index = nodes.indexOf(node);
		if (index < 0) //layer must have been removed during loading
		{
			return;
		}

		Wrapper wrapper = wrappers.get(index);
		wrapper.setLoaded(loaded);

		refreshLists();
	}

	private void downloadLayer(final ILayerNode node, URL url, boolean onlyIfModified)
	{
		RetrievalHandler handler = new RetrievalHandler()
		{
			@Override
			public void handle(RetrievalResult result)
			{
				handleResult(node, result);
			}
		};
		if (onlyIfModified)
		{
			Downloader.downloadIfModified(url, handler, handler, true);
		}
		else
		{
			Downloader.downloadIgnoreCache(url, handler, true);
		}
	}

	private void setError(ILayerNode node, Exception error)
	{
		node.setError(error);
		if (tree != null)
		{
			tree.relayoutOnEDT();
		}
	}

	private void setLayerLoading(ILayerNode node, boolean loading, boolean repaintTree)
	{
		node.setLayerLoading(loading);
		if (repaintTree && tree != null)
		{
			tree.relayoutOnEDT();
		}
	}

	private synchronized void handleResult(ILayerNode node, RetrievalResult result)
	{
		if (result.getError() != null)
		{
			setLayerLoading(node, false, false);
			setError(node, result.getError());
			return;
		}

		if (!result.isFromCache())
		{
			setLayerLoading(node, false, true);
		}

		//data was not modified (already created layer from cache)
		if (result.isNotModified())
		{
			return;
		}

		if (!result.hasData())
		{
			//shouldn't get here
			setError(node, new Exception("Error downloading layer"));
			return;
		}

		//create a layer or elevation model from the downloaded result
		LoadedLayer loaded;
		try
		{
			loaded = LayerLoader.load(result.getSourceURL(), result.getAsInputStream());
		}
		catch (Exception e)
		{
			Logging.logger().log(Level.SEVERE, "Error loading layer", e);
			setError(node, e);
			return;
		}
		if (loaded == null)
		{
			return;
		}

		int index = nodes.indexOf(node);
		if (index < 0) //layer must have been removed during loading
		{
			return;
		}

		Wrapper wrapper = wrappers.get(index);
		wrapper.setLoaded(loaded);

		//must've been a download, so have to refresh the layer list
		if (!result.isFromCache())
		{
			refreshLists();
			if (node.hasError())
			{
				setError(node, null);
			}
		}
	}

	private void refreshLists()
	{
		//TODO instead of clearing layers and reading, only remove those that need to be removed,
		//and only add those that need to be added, and move those that need to be moved

		if (wwd == null)
		{
			return;
		}

		//remove all that we added last time
		layerList.removeAllFromSection(this, layers);
		elevationModel.removeAllFromSection(this, elevationModels);

		//clear the lists
		layers.clear();
		elevationModels.clear();

		//list of hierarchicals to setup outside of the main loop (to ensure
		//we don't get a ConcurrentModificationException on the wrappers list)
		List<Hierarchical> hierarchicalsToSetup = new ArrayList<Hierarchical>();

		//rebuild the lists
		for (Wrapper wrapper : wrappers)
		{
			if (wrapper.node.isEnabled())
			{
				if (wrapper.hasLayer())
				{
					Layer layer = wrapper.getLayer();
					layer.setEnabled(wrapper.node.isEnabled());
					layer.setOpacity(wrapper.node.getOpacity());
					layers.add(layer);
					layerMap.put(layer, wrapper.node);

					if (layer instanceof Hierarchical && !hierarchicalListenees.contains(layer))
					{
						hierarchicalsToSetup.add((Hierarchical) layer);
						hierarchicalListenees.add((Hierarchical) layer);
					}
				}
				else if (wrapper.hasElevationModel())
				{
					ElevationModel elevationModel = wrapper.getElevationModel();
					elevationModels.add(elevationModel);
					mapChildElevationModelsToNode(elevationModel, wrapper.node);
				}
			}

			if (wrapper.isLoaded())
			{
				wrapper.node.setLegendURL(wrapper.getLoaded().getLegendURL());
				wrapper.node.setQueryURL(wrapper.getLoaded().getQueryURL());

				wrapper.updateExpiryTime();
			}
		}

		layerList.addAllFromSection(this, layers);
		elevationModel.addAllFromSection(this, elevationModels);

		//now setup the hierarchicals
		for (Hierarchical hierarchical : hierarchicalsToSetup)
		{
			hierarchical.addHierarchicalListener(this);
		}

		//tell the listeners that the list has been refreshed
		for (RefreshListener listener : listeners)
		{
			listener.refreshed();
		}

		//relayout and repaint the tree, as the labels may have changed (maybe legend button added)
		tree.relayoutOnEDT();
	}

	private void mapChildElevationModelsToNode(ElevationModel elevationModel, ILayerNode node)
	{
		elevationModelMap.put(elevationModel, node);
		if (elevationModel instanceof CompoundElevationModel)
		{
			CompoundElevationModel cem = (CompoundElevationModel) elevationModel;
			for (ElevationModel em : cem.getElevationModels())
			{
				mapChildElevationModelsToNode(em, node);
			}
		}
	}

	public synchronized boolean hasLayer(ILayerNode node)
	{
		if (nodeMap.containsKey(node))
		{
			return nodeMap.get(node).hasLayer();
		}
		return false;
	}

	public synchronized boolean hasLayer(Layer layer)
	{
		return layerMap.containsKey(layer);
	}

	public synchronized Bounds getLayerExtents(ILayerNode node)
	{
		if (!nodeMap.containsKey(node))
		{
			return null;
		}

		Wrapper wrapper = nodeMap.get(node);
		Object wrapped =
				wrapper.hasLayer() ? wrapper.getLayer() : wrapper.hasElevationModel() ? wrapper.getElevationModel()
						: null;

		return Bounded.Reader.getBounds(wrapped);
	}

	public synchronized Layer getLayer(ILayerNode node)
	{
		if (!nodeMap.containsKey(node))
		{
			return null;
		}

		Wrapper wrapper = nodeMap.get(node);
		return wrapper.hasLayer() ? wrapper.getLayer() : null;
	}

	public ILayerNode getLayerNode(Layer layer)
	{
		return layerMap.get(layer);
	}

	public ILayerNode getLayerNode(ElevationModel elevationModel)
	{
		return elevationModelMap.get(elevationModel);
	}

	public void redrawWwd()
	{
		if (wwd != null)
		{
			wwd.redraw();
		}
	}

	private class Wrapper
	{
		public final ILayerNode node;
		private LoadedLayer loaded;

		public Wrapper(ILayerNode node)
		{
			this.node = node;
		}

		public ElevationModel getElevationModel()
		{
			return loaded != null ? loaded.getElevationModel() : null;
		}

		public boolean hasElevationModel()
		{
			return loaded != null && loaded.isElevationModel();
		}

		public Layer getLayer()
		{
			return loaded != null ? loaded.getLayer() : null;
		}

		public boolean hasLayer()
		{
			return loaded != null && loaded.isLayer();
		}

		public boolean isLoaded()
		{
			return loaded != null;
		}

		public LoadedLayer getLoaded()
		{
			return loaded;
		}

		public void setLoaded(LoadedLayer loaded)
		{
			this.loaded = loaded;

			//ensure the loaded layer/elevationModel has the same name as the tree node
			if (hasLayer())
			{
				getLayer().setName(node.getName());
			}
			else if (hasElevationModel())
			{
				getElevationModel().setName(node.getName());
			}

			if (getLoaded().getLoadedObject() instanceof Loader)
			{
				Loader loader = (Loader) getLoaded().getLoadedObject();
				node.setLoader(loader);
				if (tree != null)
				{
					loader.addLoadingListener(tree);
				}
			}
		}

		public void updateExpiryTime()
		{
			if (node.getExpiryTime() != null)
			{
				if (useNodesExpiryTime())
				{
					if (hasLayer())
					{
						getLayer().setExpiryTime(node.getExpiryTime());
					}
					else if (hasElevationModel())
					{
						getElevationModel().setExpiryTime(node.getExpiryTime());
					}
				}
				else
				{
					node.setExpiryTime(null);
				}
			}
		}

		private boolean useNodesExpiryTime()
		{
			if (node.getExpiryTime() == null)
			{
				return false;
			}

			if (loaded.getParams() == null)
			{
				return true;
			}

			Object o = loaded.getParams().getValue(AVKey.EXPIRY_TIME);
			if (o == null || !(o instanceof Long))
			{
				return true;
			}

			Long l = (Long) o;
			if (l < node.getExpiryTime())
			{
				return true;
			}

			return false;
		}
	}

	public static interface RefreshListener
	{
		public void refreshed();
	}

	@Override
	public void hierarchyChanged(Layer layer, TreeNode node)
	{
		ILayerNode layerNode = layerMap.get(layer);
		if (layerNode != null && !connectedHierarchicalLayerNodes.contains(layerNode))
		{
			WWTreeToLayerTreeConnector.connect(tree.getLayerModel(), layerNode, node);
			connectedHierarchicalLayerNodes.add(layerNode);
		}
	}
}
