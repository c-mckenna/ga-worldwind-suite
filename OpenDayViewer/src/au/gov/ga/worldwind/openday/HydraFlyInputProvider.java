package au.gov.ga.worldwind.openday;

import gov.nasa.worldwind.awt.ViewInputAttributes.ActionAttributes;
import gov.nasa.worldwind.awt.ViewInputAttributes.DeviceAttributes;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.view.ViewUtil;
import gov.nasa.worldwind.view.orbit.OrbitView;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import au.gov.ga.worldwind.common.input.IOrbitInputProvider;
import au.gov.ga.worldwind.common.input.IProviderOrbitViewInputHandler;
import au.gov.ga.worldwind.common.input.hydra.Hydra;
import au.gov.ga.worldwind.common.input.hydra.HydraButtonEvent;
import au.gov.ga.worldwind.common.input.hydra.HydraEvent;
import au.gov.ga.worldwind.common.input.hydra.HydraStickEvent;
import au.gov.ga.worldwind.common.input.hydra.HydraTriggerEvent;
import au.gov.ga.worldwind.common.input.hydra.IHydraListener;
import au.gov.ga.worldwind.common.sun.SunPositionService;
import au.gov.ga.worldwind.common.sun.SunPositionService.SunPositionType;
import au.gov.ga.worldwind.common.view.orbit.AbstractView;
import au.gov.ga.worldwind.common.view.orbit.BaseOrbitView;

public class HydraFlyInputProvider implements IOrbitInputProvider, IHydraListener
{
	private final static ActionAttributes horizontalAttributes = new ActionAttributes(1e4 * 0.1, 1e8 * 0.1, false, 0.4);
	private final static ActionAttributes headingAttributes = new ActionAttributes(1e2, 1e2, false, 0.85);
	private final static ActionAttributes pitchAttributes = new ActionAttributes(5e1, 1e2, false, 0.85);
	private final static DeviceAttributes deviceAttributes = new DeviceAttributes(1.0);

	private float x1, y1, x2, y2, z;
	private long lastNanos;
	private float timeChange;
	private boolean goHome = false;

	private enum TimeType
	{
		MONTH(GregorianCalendar.MONTH),
		DAY(GregorianCalendar.DAY_OF_YEAR),
		HOUR(GregorianCalendar.HOUR_OF_DAY),
		MINUTE(GregorianCalendar.MINUTE);

		public final int field;

		private TimeType(int field)
		{
			this.field = field;
		}
	}

	private TimeType timeType = TimeType.MINUTE;

	public HydraFlyInputProvider()
	{
		Hydra.getInstance().addListener(this);
		lastNanos = System.nanoTime();

		SunPositionService.INSTANCE.setType(SunPositionType.SpecificTime);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(GregorianCalendar.HOUR_OF_DAY, 12); //set time to midday (in computer's time zone)
		calendar.set(GregorianCalendar.MINUTE, 0);
		SunPositionService.INSTANCE.setTime(calendar);
	}

	@Override
	public void updated(HydraEvent event)
	{
	}

	@Override
	public void stickChanged(HydraStickEvent event)
	{
		x1 = event.stick1[0];
		y1 = event.stick1[1];
		x2 = event.stick2[0];
		y2 = event.stick2[1];
	}

	@Override
	public void triggerChanged(HydraTriggerEvent event)
	{
		z = event.trigger2 - event.trigger1;
	}

	@Override
	public void buttonChanged(HydraButtonEvent event)
	{
		if (!event.down)
		{
			switch (event.button)
			{
			/*case HydraButtonEvent.BUTTON1:
			case HydraButtonEvent.BUTTON3:
				LayerGroupEnabler.INSTANCE.previous();
				break;
			case HydraButtonEvent.BUTTON2:
			case HydraButtonEvent.BUTTON4:
				LayerGroupEnabler.INSTANCE.next();
				break;*/
			case HydraButtonEvent.BUTTON1:
				LayerGroupEnabler.INSTANCE.set(0);
				break;
			case HydraButtonEvent.BUTTON2:
				LayerGroupEnabler.INSTANCE.set(1);
				break;
			case HydraButtonEvent.BUTTON3:
				LayerGroupEnabler.INSTANCE.set(2);
				break;
			case HydraButtonEvent.BUTTON4:
				LayerGroupEnabler.INSTANCE.set(3);
				break;
			case HydraButtonEvent.BACK:
				int timeTypeChange = event.controller == 1 ? -1 : 1;
				int timeTypeOrdinal =
						(((timeType.ordinal() + timeTypeChange) % TimeType.values().length) + TimeType.values().length)
								% TimeType.values().length;
				timeType = TimeType.values()[timeTypeOrdinal];
				System.out.println("Time type changed to: " + timeType);
				break;
			case HydraButtonEvent.START:
				goHome = true;
				break;
			}
		}
	}

	protected double getScaleValue(double minValue, double maxValue, double value, double range, boolean isExp)
	{
		double t = value / range;
		t = t < 0 ? 0 : (t > 1 ? 1 : t);
		if (isExp)
		{
			t = Math.pow(2.0, t) - 1.0;
		}
		return (minValue * (1.0 - t) + maxValue * t);
	}

	@Override
	public void apply(IProviderOrbitViewInputHandler inputHandler)
	{
		OrbitView view = inputHandler.getView();
		Globe globe = view.getGlobe();
		DrawContext dc = ((AbstractView) view).getDC(); //TODO dodgy!
		if (globe == null || dc == null)
		{
			return;
		}

		if (goHome)
		{
			goHome = false;
			view.setCenterPosition(new Position(view.getCenterPosition(), 0));
			((BaseOrbitView) view).loadConfigurationValues();
			view.setPitch(Angle.POS90);
			return;
		}

		long currentNanos = System.nanoTime();
		double time = (currentNanos - lastNanos) / 1e9d;
		lastNanos = currentNanos;

		Position eyePosition = view.getEyePosition();
		Vec4 eyePoint = view.getEyePoint();
		Vec4 forward = view.getForwardVector();
		Vec4 up = view.getUpVector();
		Vec4 side = forward.cross3(up);

		double altitude = ViewUtil.computeElevationAboveSurface(dc, eyePosition);
		double radius = globe.getRadius();
		double[] range = horizontalAttributes.getValues();
		double speed = getScaleValue(range[0], range[1], altitude, 3.0 * radius, true) * time;

		Vec4 centerPoint = eyePoint.add3(forward);
		centerPoint = centerPoint.add3(forward.multiply3(y2 * speed));
		centerPoint = centerPoint.add3(side.multiply3(x2 * speed));

		Position centerPosition = globe.computePositionFromPoint(centerPoint);

		double minimumAltitude = 500;
		double maximumAltitude = radius * 0.5;
		double centerAltitude = ViewUtil.computeElevationAboveSurface(dc, centerPosition);
		if (centerAltitude < minimumAltitude)
		{
			centerPosition = new Position(centerPosition, centerPosition.elevation - centerAltitude + minimumAltitude);
		}
		else if (centerAltitude > maximumAltitude)
		{
			centerPosition = new Position(centerPosition, centerPosition.elevation - centerAltitude + maximumAltitude);
		}

		view.setZoom(1.0);
		view.setCenterPosition(centerPosition);
		view.setPitch(Angle.POS90);

		view.setHeading(view.getHeading().addDegrees(x1 * time * 100d));

		//rendering constantly:
		inputHandler.markViewChanged();

		if (z != 0)
		{
			timeChange += z * time * 20f;

			if (Math.abs(timeChange) >= 1)
			{
				int change = (int) timeChange;
				timeChange %= 1f;

				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTimeInMillis(SunPositionService.INSTANCE.getTime().getTimeInMillis());
				int value = calendar.get(timeType.field);
				value += change;
				calendar.set(timeType.field, value);

				//rotate year around 2010-2020
				int year = calendar.get(GregorianCalendar.YEAR);
				int minYear = 2010;
				int maxYear = 2020;
				int yearRange = maxYear - minYear + 1;
				int rotatedYear = (((year - minYear) % yearRange) + yearRange) % yearRange + minYear;
				if (year != rotatedYear)
				{
					calendar.set(GregorianCalendar.YEAR, rotatedYear);
				}

				SunPositionService.INSTANCE.setTime(calendar);

				SimpleDateFormat format = new SimpleDateFormat();
				System.out.println("Current time = " + format.format(calendar.getTime()));
			}
		}
	}
}
