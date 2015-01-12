package au.gov.ga.worldwind.openday;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ScreenAnnotation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import au.gov.ga.worldwind.common.sun.SunPositionService;
import au.gov.ga.worldwind.common.view.delegate.IDelegateView;
import au.gov.ga.worldwind.common.view.delegate.IViewDelegate;

public class TimeLayer extends AbstractLayer
{
	private ScreenAnnotation annotation;
	private SimpleDateFormat format = new SimpleDateFormat("hh:mm a dd/MM/yyyy");

	public TimeLayer()
	{
		annotation = new ScreenAnnotation("", new Point(0, 0));
		annotation.setAlwaysOnTop(true);

		AnnotationAttributes attributes = new AnnotationAttributes();
		annotation.setAttributes(attributes);
		attributes.setCornerRadius(0);
		attributes.setFrameShape(AVKey.SHAPE_NONE);
		attributes.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
		attributes.setTextColor(Color.white);
		attributes.setTextAlign(AVKey.LEFT);
		attributes.setSize(new Dimension(420, 0));

		Font font = new Font("Arial", 0, 40);
		attributes.setFont(font);
	}

	@Override
	protected void doRender(DrawContext dc)
	{
		Calendar calendar = SunPositionService.INSTANCE.getTime();
		String text = format.format(calendar.getTime());
		annotation.setText(text);
		Point centerPoint = dc.getViewportCenterScreenPoint();

		int offset = 0;
		int hmdEyeOffset = 100;
		IViewDelegate viewDelegate = ((IDelegateView) dc.getView()).getDelegate();
		/*if (viewDelegate instanceof HMDViewDelegate && ((HMDViewDelegate) viewDelegate).isRenderEyes())
		{
			Eye eye = ((HMDViewDelegate) viewDelegate).getEye();
			offset = eye == Eye.LEFT ? hmdEyeOffset : -hmdEyeOffset;
		}*/

		Point point = new Point(centerPoint.x - 50 + offset, centerPoint.y + 100);
		annotation.setScreenPoint(point);
		annotation.render(dc);
	}
}
