package application;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.FogLayer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.StarsLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.layers.Earth.BMNGWMSLayer;
import gov.nasa.worldwind.layers.Earth.EarthNASAPlaceNameLayer;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.OrbitView;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import path.AnimationPath;
import path.AnimationPoint;
import path.Point;

import camera.CameraPath;
import camera.motion.Motion;
import camera.motion.MotionParams;
import camera.params.Heading;
import camera.params.LatLon;
import camera.params.Pitch;
import camera.params.Zoom;
import camera.vector.Vector2;
import camera.vector.Vector3;

import com.sun.opengl.util.BufferUtil;

public class Animator
{
	static
	{
		if (Configuration.isWindowsOS())
		{
			System.setProperty("sun.java2d.noddraw", "true");
		}
		else if (Configuration.isMacOS())
		{
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty(
					"com.apple.mrj.application.apple.menu.about.name",
					"World Wind Application");
			System.setProperty("com.apple.mrj.application.growbox.intrudes",
					"false");
			System.setProperty("apple.awt.brushMetalLook", "true");
		}

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
		}

		System.setProperty("http.proxyHost", "proxy.agso.gov.au");
		System.setProperty("http.proxyPort", "8080");
		System.setProperty("http.nonProxyHosts", "localhost");
	}

	public static void main(String[] args)
	{
		new Animator();
	}

	private JFrame frame;
	private WorldWindowGLCanvas wwd;
	private boolean takingScreenshot = false;

	public Animator()
	{
		Configuration.setValue(AVKey.LAYERS_CLASS_NAMES, "");

		frame = new JFrame("World Wind");

		frame.setLayout(new BorderLayout());
		wwd = new WorldWindowGLCanvas();
		Model model = new BasicModel();
		wwd.setModel(model);
		wwd.setPreferredSize(new Dimension(800, 600));
		frame.add(wwd, BorderLayout.CENTER);

		LayerList layers = model.getLayers();

		layers.add(new StarsLayer());
		layers.add(new SkyGradientLayer());
		layers.add(new FogLayer());
		//layers.add(new BMNGOneImage());
		layers.add(new BMNGWMSLayer());
		//layers.add(new LandsatI3WMSLayer());
		layers.add(new EarthNASAPlaceNameLayer());
		layers.add(new CompassLayer());
		layers.add(new WorldMapLayer());
		layers.add(new ScalebarLayer());
		//layers.add(new MGRSGraticuleLayer());

		JPanel left = new JPanel(new GridLayout(0, 1));
		frame.add(left, BorderLayout.WEST);

		JButton button = new JButton("Take screenshot");
		left.add(button);
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				takeScreenshot("screenshot.png");
			}
		});

		button = new JButton("Animate");
		left.add(button);
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				animate();
			}
		});

		StatusBar statusBar = new StatusBar();
		frame.add(statusBar, BorderLayout.PAGE_END);
		statusBar.setEventSource(wwd);

		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				quit();
			}
		});

		frame.pack();
		frame.setVisible(true);
	}

	public void quit()
	{
		frame.dispose();
		System.exit(0);
	}

	private void takeScreenshot(String filename)
	{
		if (!EventQueue.isDispatchThread())
		{
			takingScreenshot = true;
		}

		wwd.addRenderingListener(new Screenshotter(filename));
		wwd.redraw();

		if (!EventQueue.isDispatchThread())
		{
			while (takingScreenshot)
			{
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private double e2a(double elevation)
	{
		return Math.log(elevation + 1);
	}

	private double a2e(double animation)
	{
		return Math.pow(Math.E, animation) - 1;
	}

	private void animate()
	{
		LatLon l1 = LatLon.fromDegrees(-27, 133.5);
		LatLon l2 = LatLon.fromDegrees(-21.0474, 119.6494);

		Zoom zoom1 = Zoom.fromCameraZoom(6378137);
		Zoom zoom2 = Zoom.fromCameraZoom(559794);

		Heading heading1 = Heading.fromDegrees(-170);
		Heading heading2 = Heading.fromDegrees(180);
		Pitch pitch1 = Pitch.fromDegrees(0);
		Pitch pitch2 = Pitch.fromDegrees(70);
		Pitch pitch3 = Pitch.fromDegrees(45);

		MotionParams motion1 = new MotionParams(0.2, 0.2, 0, 0);
		MotionParams motion2 = new MotionParams(5, 5, 0, 0);

		final CameraPath path = new CameraPath(l1, l1, zoom1, heading1, pitch1);

		path.addCenter(l2, l2, null, 20, motion1);
		path.addZoom(zoom2, 10, motion1);
		path.addHeading(heading2, 20, motion2);
		path.addPitch(pitch2, 10, motion2);
		path.addPitch(pitch3, 20, motion2);


		Thread thread = new Thread(new Runnable()
		{
			public void run()
			{
				View v = wwd.getSceneController().getView();
				if (!(v instanceof OrbitView))
					return;
				OrbitView view = (OrbitView) v;
				boolean detectCollisions = view.isDetectCollisions();
				view.setDetectCollisions(false);

				double totalTime = path.getTime();
				double startTime = System.currentTimeMillis() / 1000d;
				double currentTime = 0;
				while (currentTime <= totalTime)
				{
					currentTime = System.currentTimeMillis() / 1000d
							- startTime;
					//currentTime += 1;

					LatLon center = path.getCenter(currentTime);
					Zoom zoom = path.getZoom(currentTime);
					Heading heading = path.getHeading(currentTime);
					Pitch pitch = path.getPitch(currentTime);

					view.setCenterPosition(new Position(center.getLatLon(), 0));
					view.setZoom(zoom.toCameraZoom());
					view.setHeading(heading.getAngle());
					view.setPitch(pitch.getAngle());

					wwd.redrawNow();

					//takeScreenshot("frames/screen" + (currentTime) + ".png");

					//System.out.println(currentTime + " = " + position + " zoom = " + zoom);
				}

				view.setDetectCollisions(detectCollisions);
			}
		});
		thread.start();
	}

	private void animate2()
	{
		Vector3 pos = new Vector3(0, 0, 40);
		Vector3 zero = Vector3.ZERO;

		final AnimationPath path = new AnimationPath();
		/*AnimationPoint p1 = new AnimationPoint(0, new Position(41.68695,
				-87.70575, e2a(374070)), 0d, 0d, zero, zero);
		AnimationPoint p2 = new AnimationPoint(4, new Position(41.68695,
				-87.70575, e2a(6889382)), 0d, 0d, zero, pos);
		AnimationPoint p3 = new AnimationPoint(8, new Position(51.44871,
				-0.01974, e2a(6889382)), 0d, 0d, pos, zero);
		AnimationPoint p4 = new AnimationPoint(12, new Position(51.44871,
				-0.01974, e2a(374070)), 0d, 0d, zero, zero);
		path.points.add(p1);
		path.points.add(p2);
		path.points.add(p3);
		path.points.add(p4);*/


		Vector3 v1 = new Vector3(-27, 133.5, e2a(6378137 * 3));
		Vector3 v2 = new Vector3(-21.0474, 119.6494 - 1, e2a(559794));
		Vector3 v3 = new Vector3(-21.0474 + 1, 119.6494, e2a(559794));
		Vector3 v4 = new Vector3(-21.0474, 119.6494 + 1, e2a(559794));
		Vector3 v5 = new Vector3(-21.0474 - 1, 119.6494, e2a(559794));

		Vector3 xaxis = new Vector3(0.5, 0, 0);
		Vector3 yaxis = new Vector3(0, 0.5, 0);

		double pitch = 60;
		Vector2 orientation1 = new Vector2(0, 0);
		Vector2 orientation2 = new Vector2(90, pitch);
		Vector2 orientation3 = new Vector2(180, pitch);
		Vector2 orientation4 = new Vector2(270, pitch);
		Vector2 orientation5 = new Vector2(360, pitch);

		AnimationPoint p1 = new AnimationPoint(v1, orientation1, zero, zero);
		AnimationPoint p2 = new AnimationPoint(v2, orientation2, xaxis.mult(2)
				.negateLocal(), xaxis);
		AnimationPoint p3 = new AnimationPoint(v3, orientation3,
				yaxis.negate(), yaxis);
		AnimationPoint p4 = new AnimationPoint(v4, orientation4, xaxis, xaxis
				.negate());
		AnimationPoint p5 = new AnimationPoint(v5, orientation5, yaxis, zero);

		/*AnimationPoint p1 = new AnimationPoint(v1, orientation1, zero, zero);
		AnimationPoint p2 = new AnimationPoint(v2, orientation1, zero, zero);
		AnimationPoint p3 = new AnimationPoint(v3, orientation1, zero, zero);
		AnimationPoint p4 = new AnimationPoint(v4, orientation1, zero, zero);
		AnimationPoint p5 = new AnimationPoint(v5, orientation1, zero, zero);*/

		p1.velocityAt = 0;
		p1.velocityAfter = 50;
		p2.velocityAt = 5;
		p2.velocityAfter = 50;
		p3.velocityAt = 50;
		p3.velocityAfter = 50;
		p4.velocityAt = 50;
		p4.velocityAfter = 50;
		p5.velocityAt = 0;

		p1.accelerationIn = 100;
		p1.accelerationOut = 100;
		p2.accelerationIn = 100;
		p2.accelerationOut = 100;
		p3.accelerationIn = 100;
		p3.accelerationOut = 100;
		p4.accelerationIn = 100;
		p4.accelerationOut = 100;

		path.addPoint(p1);
		path.addPoint(p2);
		path.addPoint(p3);
		//path.addPoint(p4);
		//path.addPoint(p5);

		Thread thread = new Thread(new Runnable()
		{
			public void run()
			{
				View v = wwd.getSceneController().getView();
				if (!(v instanceof OrbitView))
					return;
				OrbitView view = (OrbitView) v;
				boolean detectCollisions = view.isDetectCollisions();
				view.setDetectCollisions(false);

				long totalTime = 5000;
				long startTime = System.currentTimeMillis();
				long currentTime = 0;
				double percent = 0;
				while (percent <= 1)
				{
					currentTime = System.currentTimeMillis() - startTime;
					//currentTime += 1;

					percent = (double) currentTime / (double) totalTime;

					Point point = path.getPositionAt(percent);

					Position position = Position.fromDegrees(point.position.x,
							point.position.y, 0);
					double zoom = a2e(point.position.z);
					double heading = point.orientation.x;
					double pitch = point.orientation.y;

					view.setCenterPosition(position);
					view.setZoom(zoom);
					view.setHeading(Angle.fromDegrees(heading));
					view.setPitch(Angle.fromDegrees(pitch));

					wwd.redrawNow();

					//takeScreenshot("frames/screen" + (currentTime) + ".png");

					//System.out.println(currentTime + " = " + position + " zoom = " + zoom);
				}

				view.setDetectCollisions(detectCollisions);
			}
		});
		thread.start();
	}

	private class Screenshotter implements RenderingListener
	{
		private final String filename;

		public Screenshotter(String filename)
		{
			this.filename = filename;
		}

		public void stageChanged(RenderingEvent event)
		{
			if (WorldWind.getTaskService().hasActiveTasks()
					|| WorldWind.getRetrievalService().hasActiveTasks())
			{
				sleep();
				wwd.redraw();
			}
			else if (event.getStage() == RenderingEvent.BEFORE_BUFFER_SWAP)
			{
				wwd.removeRenderingListener(this);
				saveFrame(filename);
				takingScreenshot = false;
			}
		}

		private void sleep()
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	};

	private void saveFrame(String filename)
	{
		GL gl = wwd.getGL();
		int width = wwd.getWidth();
		int height = wwd.getHeight();
		ByteBuffer buffer = BufferUtil.newByteBuffer(width * height * 3);
		gl.glReadPixels(0, 0, width, height, GL.GL_RGB, GL.GL_UNSIGNED_BYTE,
				buffer);
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				int index = 3 * ((height - y - 1) * width + x);
				int argb = (((int) (buffer.get(index + 0)) & 0xFF) << 16) //r
						| (((int) (buffer.get(index + 1)) & 0xFF) << 8) //g
						| (((int) (buffer.get(index + 2)) & 0xFF)); //b

				img.setRGB(x, y, argb);
			}
		}

		File out = new File(filename);
		if (!out.getParentFile().exists())
		{
			out.getParentFile().mkdirs();
		}
		try
		{
			ImageIO.write(img, "png", out);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println("Screenshot saved to " + out);
	}
}
