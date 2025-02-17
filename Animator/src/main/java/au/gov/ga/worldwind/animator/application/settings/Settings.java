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
package au.gov.ga.worldwind.animator.application.settings;

import static au.gov.ga.worldwind.common.util.Util.isBlank;
import gov.nasa.worldwind.util.WWXML;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import au.gov.ga.worldwind.animator.layers.LayerIdentifier;
import au.gov.ga.worldwind.animator.layers.LayerIdentifierFactory;
import au.gov.ga.worldwind.animator.layers.LayerIdentifierImpl;
import au.gov.ga.worldwind.animator.terrain.ElevationModelIdentifier;
import au.gov.ga.worldwind.animator.terrain.ElevationModelIdentifierImpl;
import au.gov.ga.worldwind.animator.util.ExceptionLogger;
import au.gov.ga.worldwind.common.util.Proxy;
import au.gov.ga.worldwind.common.util.Proxy.ProxyType;
import au.gov.ga.worldwind.common.util.Util;
import au.gov.ga.worldwind.common.util.XMLUtil;

/**
 * A class that holds global settings for the Animator application.
 * <p/>
 * These settings can be persisted between executions of the application.
 * 
 * @author James Navin (james.navin@ga.gov.au)
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class Settings
{
	/** The settings file name to use */
	private static final String SETTINGS_FILE_NAME = "animatorSettings.xml";

	/** The Singleton Settings instance */
	private static Settings instance;

	/**
	 * Gets the Singleton instance of the global application {@link Settings}.
	 * <p/>
	 * If one has not yet been loaded, will lazy-load the settings from the
	 * configured location.
	 * 
	 * @return The current global settings
	 */
	public static Settings get()
	{
		if (instance == null)
		{
			loadSettings();
		}
		return instance;
	}

	/**
	 * Persists the settings to the configured xml file location
	 */
	public static void save()
	{
		if (instance == null)
		{
			return;
		}

		try
		{
			Document document = WWXML.createDocumentBuilder(false).newDocument();

			Element rootElement = document.createElement("animationSettings");
			document.appendChild(rootElement);

			saveLastUsedLocation(rootElement);
			saveRecentFilesList(rootElement);
			saveSplitLocation(rootElement);
			saveDefaultAnimationLayers(rootElement);
			saveKnownLayers(rootElement);
			saveDefaultElevationModels(rootElement);
			saveUtilityLayerFlags(rootElement);
			saveDebugFlags(rootElement);
			saveAutoSaveConfig(rootElement);
			saveProxy(rootElement);

			XMLUtil.saveDocumentToFormattedStream(document,
					new FileOutputStream(new File(Util.getUserGAWorldWindDirectory(), SETTINGS_FILE_NAME)));
		}
		catch (Exception e)
		{
			ExceptionLogger.logException(e);
		}
	}

	private static void saveDebugFlags(Element rootElement)
	{
		WWXML.appendBoolean(rootElement, "logAnimationEvents", instance.isAnimationEventsLogged());
	}

	private static void saveUtilityLayerFlags(Element rootElement)
	{
		WWXML.appendBoolean(rootElement, "showCameraPath", instance.isCameraPathShown());
		WWXML.appendBoolean(rootElement, "showGrid", instance.isGridShown());
		WWXML.appendBoolean(rootElement, "showRuleOfThirds", instance.isRuleOfThirdsShown());
		WWXML.appendBoolean(rootElement, "showCrosshairs", instance.isCrosshairsShown());
	}

	private static void saveDefaultElevationModels(Element rootElement)
	{
		Element animationLayersContainer = WWXML.appendElement(rootElement, "defaultModels");
		List<ElevationModelIdentifier> modelList = instance.getDefaultElevationModels();
		for (int i = 0; i < modelList.size(); i++)
		{
			Element layerElement = WWXML.appendElement(animationLayersContainer, "model");
			WWXML.setIntegerAttribute(layerElement, "index", i);
			WWXML.setTextAttribute(layerElement, "name", modelList.get(i).getName());
			WWXML.setTextAttribute(layerElement, "url", modelList.get(i).getLocation());
		}
	}

	private static void saveKnownLayers(Element rootElement)
	{
		Element animationLayersContainer = WWXML.appendElement(rootElement, "knownLayers");
		List<LayerIdentifier> layersList = instance.getKnownLayers();
		for (int i = 0; i < layersList.size(); i++)
		{
			Element layerElement = WWXML.appendElement(animationLayersContainer, "layer");
			WWXML.setIntegerAttribute(layerElement, "index", i);
			WWXML.setTextAttribute(layerElement, "name", layersList.get(i).getName());
			WWXML.setTextAttribute(layerElement, "url", layersList.get(i).getLocation());
		}

	}

	private static void saveDefaultAnimationLayers(Element rootElement)
	{
		Element animationLayersContainer = WWXML.appendElement(rootElement, "defaultLayers");
		List<LayerIdentifier> layersList = instance.getDefaultAnimationLayers();
		for (int i = 0; i < layersList.size(); i++)
		{
			Element layerElement = WWXML.appendElement(animationLayersContainer, "layer");
			WWXML.setIntegerAttribute(layerElement, "index", i);
			WWXML.setTextAttribute(layerElement, "name", layersList.get(i).getName());
			WWXML.setTextAttribute(layerElement, "url", layersList.get(i).getLocation());
		}
	}

	private static void saveSplitLocation(Element rootElement)
	{
		Element splitLocationElement = WWXML.appendElement(rootElement, "splitLocation");
		WWXML.setIntegerAttribute(splitLocationElement, "value", instance.getSplitLocation());
	}

	private static void saveRecentFilesList(Element rootElement)
	{
		Element recentFilesContainer = WWXML.appendElement(rootElement, "recentFiles");
		List<File> recentFiles = instance.getRecentFiles();
		for (int i = 0; i < recentFiles.size(); i++)
		{
			Element recentFileElement = WWXML.appendElement(recentFilesContainer, "file");
			WWXML.setIntegerAttribute(recentFileElement, "index", i);
			WWXML.setTextAttribute(recentFileElement, "path", recentFiles.get(i).getAbsolutePath());
		}
	}

	private static void saveLastUsedLocation(Element rootElement)
	{
		File lastUsedLocation = instance.getLastUsedLocation();
		if (lastUsedLocation != null)
		{
			WWXML.appendText(rootElement, "lastUsedLocation", lastUsedLocation.getAbsolutePath());
		}
	}

	private static void saveAutoSaveConfig(Element rootElement)
	{
		Element autoSaveContainer = WWXML.appendElement(rootElement, "autoSave");
		WWXML.appendBoolean(autoSaveContainer, "enabled", instance.isAutoSaveEnabled());
		if (instance.getSaveIntervalInSeconds() != null)
		{
			WWXML.appendLong(autoSaveContainer, "saveInterval", instance.getSaveIntervalInSeconds());
		}
		WWXML.appendInteger(autoSaveContainer, "maxNumberSaves", instance.getMaxNumberOfAutoSaves());
	}

	private static void saveProxy(Element rootElement)
	{
		Proxy proxy = instance.getProxy();
		Element proxyContainer = WWXML.appendElement(rootElement, "proxy");
		WWXML.appendBoolean(proxyContainer, "enabled", proxy.isEnabled());
		WWXML.appendBoolean(proxyContainer, "useSystem", proxy.isUseSystem());
		if (proxy.getHost() != null)
		{
			WWXML.appendText(proxyContainer, "host", proxy.getHost());
		}
		if (proxy.getType() != null)
		{
			WWXML.appendText(proxyContainer, "type", proxy.getType().toString());
		}
		if (proxy.getNonProxyHosts() != null)
		{
			WWXML.appendText(proxyContainer, "nonProxyHosts", proxy.getType().toString());
		}
		WWXML.appendInteger(proxyContainer, "port", proxy.getPort());
	}

	/**
	 * Load the settings instance from the persisted xml file
	 */
	private static void loadSettings()
	{
		instance = new Settings();

		// If no file is detected, continue with the vanilla instance
		File settingsFile = new File(Util.getUserGAWorldWindDirectory(), SETTINGS_FILE_NAME);
		if (!settingsFile.exists())
		{
			return;
		}

		// Otherwise load the settings from the file
		Document xmlDocument = WWXML.openDocument(settingsFile);
		Element rootElement = xmlDocument.getDocumentElement();

		loadLastUsedLocation(rootElement);
		loadRecentFilesList(rootElement);
		loadSplitLocation(rootElement);
		loadDefaultAnimationLayers(rootElement);
		loadKnownLayers(rootElement);
		loadDefaultElevationModels(rootElement);
		loadUtilityLayerFlags(rootElement);
		loadDebugFlags(rootElement);
		loadAutoSaveConfig(rootElement);
		loadProxy(rootElement);
	}

	private static void loadDebugFlags(Element rootElement)
	{
		instance.setAnimationEventsLogged(XMLUtil.getBoolean(rootElement, "//logAnimationEvents", false));
	}

	private static void loadUtilityLayerFlags(Element rootElement)
	{
		instance.setCameraPathShown(XMLUtil.getBoolean(rootElement, "//showCameraPath", true));
		instance.setGridShown(XMLUtil.getBoolean(rootElement, "//showGrid", true));
		instance.setRuleOfThirdsShown(XMLUtil.getBoolean(rootElement, "//showRuleOfThirds", true));
		instance.setCrosshairsShown(XMLUtil.getBoolean(rootElement, "//showCrosshairs", true));
	}

	private static void loadDefaultElevationModels(Element rootElement)
	{
		List<ElevationModelIdentifier> loadedIdentifiers = new ArrayList<ElevationModelIdentifier>();
		Integer layerCount = WWXML.getInteger(rootElement, "count(//defaultModels/model)", null);
		for (int i = 0; i < layerCount; i++)
		{
			String modelName = WWXML.getText(rootElement, "//defaultModels/model[@index='" + i + "']/@name");
			String modelUrl = WWXML.getText(rootElement, "//defaultModels/model[@index='" + i + "']/@url");
			if (!isBlank(modelUrl) && !isBlank(modelName))
			{
				loadedIdentifiers.add(new ElevationModelIdentifierImpl(modelName, modelUrl));
			}
		}
		if (!loadedIdentifiers.isEmpty())
		{
			instance.setDefaultElevationModels(loadedIdentifiers);
		}
	}

	private static void loadKnownLayers(Element rootElement)
	{
		List<LayerIdentifier> loadedIdentifiers = new ArrayList<LayerIdentifier>();
		Integer layerCount = WWXML.getInteger(rootElement, "count(//knownLayers/layer)", null);
		for (int i = 0; i < layerCount; i++)
		{
			String layerName = WWXML.getText(rootElement, "//knownLayers/layer[@index='" + i + "']/@name");
			String layerUrl = WWXML.getText(rootElement, "//knownLayers/layer[@index='" + i + "']/@url");
			if (!isBlank(layerUrl) && !isBlank(layerName))
			{
				loadedIdentifiers.add(new LayerIdentifierImpl(layerName, layerUrl));
			}
		}
		if (!loadedIdentifiers.isEmpty())
		{
			instance.setKnownLayers(loadedIdentifiers);
		}

	}

	private static void loadDefaultAnimationLayers(Element rootElement)
	{
		List<LayerIdentifier> loadedIdentifiers = new ArrayList<LayerIdentifier>();
		Integer layerCount = WWXML.getInteger(rootElement, "count(//defaultLayers/layer)", null);
		for (int i = 0; i < layerCount; i++)
		{
			String layerName = WWXML.getText(rootElement, "//defaultLayers/layer[@index='" + i + "']/@name");
			String layerUrl = WWXML.getText(rootElement, "//defaultLayers/layer[@index='" + i + "']/@url");
			if (!isBlank(layerUrl) && !isBlank(layerName))
			{
				loadedIdentifiers.add(new LayerIdentifierImpl(layerName, layerUrl));
			}
		}
		if (!loadedIdentifiers.isEmpty())
		{
			instance.setDefaultAnimationLayers(loadedIdentifiers);
		}
	}

	private static void loadLastUsedLocation(Element rootElement)
	{
		String lastUsedLocationPath = WWXML.getText(rootElement, "//lastUsedLocation");
		if (!isBlank(lastUsedLocationPath))
		{
			instance.setLastUsedLocation(new File(lastUsedLocationPath));
		}
	}

	private static void loadRecentFilesList(Element rootElement)
	{
		for (int i = MAX_NUMBER_RECENT_FILES - 1; i >= 0; i--)
		{
			String recentFileName = WWXML.getText(rootElement, "//recentFiles/file[@index='" + i + "']/@path");
			if (!isBlank(recentFileName))
			{
				instance.addRecentFile(new File(recentFileName));
			}
		}
	}

	private static void loadSplitLocation(Element rootElement)
	{
		Integer splitLocation = WWXML.getInteger(rootElement, "//splitLocation/@value", null);
		if (splitLocation != null)
		{
			instance.setSplitLocation(splitLocation);
		}
	}

	private static void loadAutoSaveConfig(Element rootElement)
	{
		instance.setAutoSaveEnabled(XMLUtil.getBoolean(rootElement, "//autoSave/enabled", true));
		instance.setMaxNumberOfAutoSaves(XMLUtil.getInteger(rootElement, "//autoSave/maxNumberSaves", 5));
		instance.setSaveIntervalInSeconds(XMLUtil.getLong(rootElement, "//autoSave/saveInterval", 300L));
	}

	private static void loadProxy(Element rootElement)
	{
		Proxy proxy = new Proxy();
		if (XMLUtil.getElement(rootElement, "//proxy", null) != null)
		{
			proxy.setEnabled(XMLUtil.getBoolean(rootElement, "//proxy/enabled", true));
			proxy.setUseSystem(XMLUtil.getBoolean(rootElement, "//proxy/useSystem", true));
			proxy.setHost(XMLUtil.getText(rootElement, "//proxy/host", (String) null));
			proxy.setNonProxyHosts(XMLUtil.getText(rootElement, "//proxy/nonProxyHosts", ""));

			Integer port = XMLUtil.getInteger(rootElement, "//proxy/port", null);
			if (port != null)
			{
				proxy.setPort(port);
			}

			ProxyType type = ProxyType.fromString(XMLUtil.getText(rootElement, "//proxy/type", (String) null));
			if (type != null)
			{
				proxy.setType(type);
			}
		}
		instance.setProxy(proxy);
	}

	// ----------------------------------
	// Instance members
	// ----------------------------------

	/** The maximum number of recent files to remember */
	public static final int MAX_NUMBER_RECENT_FILES = 5;

	/**
	 * The last used location (i.e the last location used to open or save an
	 * animation file)
	 */
	private File lastUsedLocation = null;

	/** The list of recently used files */
	private List<File> recentFiles = new ArrayList<File>(MAX_NUMBER_RECENT_FILES);

	/** The location of the split in the split pane */
	private int splitLocation = 300;

	/** The default set of animation layers to include in new animations */
	private List<LayerIdentifier> defaultAnimationLayers = new ArrayList<LayerIdentifier>(
			Arrays.asList(new LayerIdentifier[] {
					new LayerIdentifierImpl("Stars",
							"http://www.ga.gov.au/apps/world-wind/dataset/standard/layers/stars.xml"),
					new LayerIdentifierImpl("Sky",
							"http://www.ga.gov.au/apps/world-wind/dataset/standard/layers/sky.xml"),
					new LayerIdentifierImpl("Blue Marble",
							"http://www.ga.gov.au/apps/world-wind/dataset/standard/layers/blue_marble.xml"),
					new LayerIdentifierImpl("Landsat",
							"http://www.ga.gov.au/apps/world-wind/dataset/standard/layers/landsat.xml"), }));

	/** The list of known layer locations for populating the layer palette */
	private List<LayerIdentifier> knownLayers = LayerIdentifierFactory
			.readFromPropertiesFile("au.gov.ga.worldwind.animator.layers.worldwindLayerIdentities");

	/** The default set of elevation models to include in new animations */
	private List<ElevationModelIdentifier> defaultElevationModels = new ArrayList<ElevationModelIdentifier>(
			Arrays.asList(new ElevationModelIdentifier[] { new ElevationModelIdentifierImpl("Earth",
					"http://www.ga.gov.au/apps/world-wind/dataset/standard/layers/earth_elevation_model.xml"), }));

	private boolean crosshairsShown = true;
	private boolean gridShown = true;
	private boolean cameraPathShown = true;
	private boolean ruleOfThirdsShown = true;
	private boolean animationEventsLogged = false;

	// Autosave settings
	private boolean autoSaveEnabled = true;
	private Long saveIntervalInSeconds = 300L; //5mins
	private int maxNumberOfAutoSaves = 5;

	private Proxy proxy = new Proxy();

	/**
	 * Private constructor. Use the Singleton accessor {@link #get()}.
	 */
	private Settings()
	{
	};

	/**
	 * @return The last used location. If not <code>null</code>, will always be
	 *         a directory location.
	 */
	public File getLastUsedLocation()
	{
		return lastUsedLocation;
	}

	public void setLastUsedLocation(File lastUsedLocation)
	{
		if (lastUsedLocation == null)
		{
			return;
		}

		if (lastUsedLocation.isDirectory())
		{
			this.lastUsedLocation = lastUsedLocation;
			return;
		}

		this.lastUsedLocation = lastUsedLocation.getParentFile();
	}

	/**
	 * @return An unmodifiable view on the list of recent files, ordered most
	 *         recent to least recent.
	 */
	public List<File> getRecentFiles()
	{
		return Collections.unmodifiableList(recentFiles);
	}

	public void addRecentFile(File recentFile)
	{
		if (recentFile == null || recentFiles.contains(recentFile))
		{
			return;
		}

		if (recentFiles.size() == MAX_NUMBER_RECENT_FILES)
		{
			recentFiles.remove(recentFiles.size() - 1);
		}

		recentFiles.add(0, recentFile);
	}

	public int getSplitLocation()
	{
		return splitLocation;
	}

	public void setSplitLocation(int splitLocation)
	{
		this.splitLocation = splitLocation;
	}

	public void setDefaultAnimationLayers(List<LayerIdentifier> defaultAnimationLayers)
	{
		this.defaultAnimationLayers = defaultAnimationLayers;
	}

	public List<LayerIdentifier> getDefaultAnimationLayers()
	{
		return defaultAnimationLayers;
	}

	public void addDefaultAnimationLayer(LayerIdentifier layer)
	{
		if (defaultAnimationLayers.contains(layer))
		{
			return;
		}
		defaultAnimationLayers.add(layer);
	}

	public List<LayerIdentifier> getKnownLayers()
	{
		return knownLayers;
	}

	public void setKnownLayers(List<LayerIdentifier> knownLayers)
	{
		this.knownLayers = knownLayers;
	}

	public void addKnownLayer(LayerIdentifier layer)
	{
		if (knownLayers.contains(layer))
		{
			return;
		}
		knownLayers.add(layer);
	}

	public void removeKnownLayer(LayerIdentifier identifier)
	{
		knownLayers.remove(identifier);
	}

	public List<ElevationModelIdentifier> getDefaultElevationModels()
	{
		return defaultElevationModels;
	}

	public void setDefaultElevationModels(List<ElevationModelIdentifier> defaultElevationModels)
	{
		this.defaultElevationModels = defaultElevationModels;
	}

	public boolean isGridShown()
	{
		return gridShown;
	}

	public void setGridShown(boolean gridShown)
	{
		this.gridShown = gridShown;
	}

	public boolean isRuleOfThirdsShown()
	{
		return ruleOfThirdsShown;
	}

	public void setRuleOfThirdsShown(boolean ruleOfThirdsShown)
	{
		this.ruleOfThirdsShown = ruleOfThirdsShown;
	}

	public boolean isCameraPathShown()
	{
		return cameraPathShown;
	}

	public void setCameraPathShown(boolean cameraPathShown)
	{
		this.cameraPathShown = cameraPathShown;
	}

	public boolean isCrosshairsShown()
	{
		return crosshairsShown;
	}

	public void setCrosshairsShown(boolean crosshairsShown)
	{
		this.crosshairsShown = crosshairsShown;
	}

	public boolean isAnimationEventsLogged()
	{
		return animationEventsLogged;
	}

	public void setAnimationEventsLogged(boolean animationEventsLogged)
	{
		this.animationEventsLogged = animationEventsLogged;
	}

	public int getMaxNumberOfAutoSaves()
	{
		return maxNumberOfAutoSaves;
	}

	public void setMaxNumberOfAutoSaves(int maxNumberOfAutoSaves)
	{
		this.maxNumberOfAutoSaves = maxNumberOfAutoSaves;
	}

	public Long getSaveIntervalInSeconds()
	{
		return saveIntervalInSeconds;
	}

	public Long getSaveIntervalInMilliseconds()
	{
		return saveIntervalInSeconds * 1000;
	}

	public void setSaveIntervalInSeconds(Long saveIntervalInSeconds)
	{
		this.saveIntervalInSeconds = saveIntervalInSeconds;
	}

	public boolean isAutoSaveEnabled()
	{
		return autoSaveEnabled;
	}

	public void setAutoSaveEnabled(boolean autoSaveEnabled)
	{
		this.autoSaveEnabled = autoSaveEnabled;
	}

	public Proxy getProxy()
	{
		return proxy;
	}

	public void setProxy(Proxy proxy)
	{
		if (proxy == null)
		{
			proxy = new Proxy();
		}
		this.proxy = proxy;
		updateProxyConfiguration();
	}

	private void updateProxyConfiguration()
	{
		proxy.set();
	}
}
