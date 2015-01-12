package au.gov.ga.worldwind.openday;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.LayerList;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import au.gov.ga.worldwind.common.input.OrbitInputProviderManager;
import au.gov.ga.worldwind.common.input.ProviderOrbitViewInputHandler;
import au.gov.ga.worldwind.common.newt.NewtInputHandler;
import au.gov.ga.worldwind.common.newt.WorldWindowNewtAutoDrawable;
import au.gov.ga.worldwind.common.newt.WorldWindowNewtCanvas;
import au.gov.ga.worldwind.common.render.ExtendedSceneController;
import au.gov.ga.worldwind.common.retrieve.ExtendedRetrievalService;
import au.gov.ga.worldwind.common.terrain.ElevationModelFactory;
import au.gov.ga.worldwind.common.terrain.WireframeRectangularTessellator;
import au.gov.ga.worldwind.common.util.AVKeyMore;
import au.gov.ga.worldwind.common.util.GDALDataHelper;
import au.gov.ga.worldwind.common.view.oculus.RiftViewDistortionDelegate;
import au.gov.ga.worldwind.common.view.target.ITargetView;

public class Viewer
{
	static
	{
		//System.setProperty("http.proxyHost", "proxy.agso.gov.au");
		//System.setProperty("http.proxyPort", "8080");
		
		System.setProperty("jna.library.path", System.getProperty("java.library.path"));

		if (Configuration.isMacOS())
		{
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Viewer");
			System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
			System.setProperty("apple.awt.brushMetalLook", "true");
		}
		else if (Configuration.isWindowsOS())
		{
			System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
		}

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
		}

		Configuration.insertConfigurationDocument("layers.xml");

		Configuration.setValue(AVKey.LAYER_FACTORY, LayerFactory.class.getName());
		Configuration.setValue(AVKey.ELEVATION_MODEL_FACTORY, ElevationModelFactory.class.getName());
		Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlyOrbitView.class.getName());
		Configuration.setValue(AVKeyMore.DELEGATE_VIEW_DELEGATE_CLASS_NAME, RiftViewDistortionDelegate.class.getName());
		Configuration.setValue(AVKey.VIEW_INPUT_HANDLER_CLASS_NAME, ProviderOrbitViewInputHandler.class.getName());
		Configuration.setValue(AVKey.RETRIEVAL_SERVICE_CLASS_NAME, ExtendedRetrievalService.class.getName());
		Configuration.setValue(AVKey.SCENE_CONTROLLER_CLASS_NAME, ExtendedSceneController.class.getName());
		Configuration.setValue(AVKey.TESSELLATOR_CLASS_NAME, WireframeRectangularTessellator.class.getName());

		Configuration.setValue(AVKey.INITIAL_LATITUDE, -35.3075);
		Configuration.setValue(AVKey.INITIAL_LONGITUDE, 149.1244);
		Configuration.setValue(AVKey.INITIAL_ALTITUDE, 40000.0);
		Configuration.setValue(AVKey.INITIAL_PITCH, 80.0);
		Configuration.setValue(AVKey.INITIAL_HEADING, 10.0);
		Configuration.setValue(AVKey.VERTICAL_EXAGGERATION, 5.0);

		//Configuration.setValue(AVKey.FOV, 160.0);

		GDALDataHelper.init();
		//the JRiftLibrary must be loaded before JInput, otherwise the Oculus Rift goes undetected:
		//OculusSingleton.getInstance();

		OrbitInputProviderManager.getInstance().addProvider(new HydraFlyInputProvider());
	}

	public static void main(String[] args)
	{
		new Viewer();
	}

	private final JFrame frame;

	public Viewer()
	{
		frame = new JFrame("OpenDay Viewer");
		//frame.setIconImage();
		frame.setUndecorated(true);
		//frame.setAlwaysOnTop(true);

		Configuration.setValue(AVKey.WORLD_WINDOW_CLASS_NAME, WorldWindowNewtAutoDrawable.class.getName());
		Configuration.setValue(AVKey.INPUT_HANDLER_CLASS_NAME, NewtInputHandler.class.getName());
		WorldWindowNewtCanvas wwd = new WorldWindowNewtCanvas();

		Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
		//model.setLayers(new SectionListLayerList());
		//model.getGlobe().setElevationModel(new SectionListCompoundElevationModel());
		wwd.setModel(model);
		((ITargetView) wwd.getView()).setPrioritizeFarClipping(false);
		((ITargetView) wwd.getView()).setDrawAxisMarker(false);
		wwd.getSceneController().setScreenCreditController(null);

		//model.getGlobe().setElevationModel(new TerrainServiceElevationModel());

		LayerList layers = model.getLayers();
		LayerGroupEnabler.INSTANCE.setLayers(layers);

		//layers.add(new TerrainServiceLayer());

		JPanel panel = new JPanel(new BorderLayout());
		frame.setContentPane(panel);
		panel.add(wwd, BorderLayout.CENTER);

		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				frame.dispose();
			}
		});

		Action action = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e)
			{
				frame.dispose();
			}
		};
		panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), action);
		panel.getActionMap().put(action, action);

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = ge.getScreenDevices();
		GraphicsDevice device = ge.getDefaultScreenDevice();
		for (GraphicsDevice otherDevice : devices)
		{
			if (otherDevice != device)
			{
				device = otherDevice;
				break;
			}
		}
		Rectangle bounds = device.getDefaultConfiguration().getBounds();

		//TEMP
		//bounds = new Rectangle(100, 100, 640, 480);
		//TEMP

		frame.setExtendedState(JFrame.NORMAL);
		frame.setBounds(bounds);
		frame.setVisible(true);
	}
}
