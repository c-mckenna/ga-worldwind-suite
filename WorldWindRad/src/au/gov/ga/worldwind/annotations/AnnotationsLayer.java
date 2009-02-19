package au.gov.ga.worldwind.annotations;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.examples.BasicDragger;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.AnnotationRenderer;
import gov.nasa.worldwind.render.BasicAnnotationRenderer;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import au.gov.ga.worldwind.settings.Settings;


public class AnnotationsLayer extends AbstractLayer
{
	private AnnotationRenderer renderer = new BasicAnnotationRenderer();
	private List<Annotation> annotations = new ArrayList<Annotation>();
	private RenderableAnnotation selectedAnnotation;
	private RenderableAnnotation lastPickedAnnotation;

	private Color savedBorderColor;
	private WorldWindow wwd;
	private AnnotationsPanel annotationsPanel;

	public AnnotationsLayer(WorldWindow wwd, AnnotationsPanel annotationsPanel)
	{
		this.wwd = wwd;
		this.annotationsPanel = annotationsPanel;
		addSelectListener();
		refresh();
	}

	private void addSelectListener()
	{
		wwd.addSelectListener(new SelectListener()
		{
			private BasicDragger dragger = new BasicDragger(wwd);

			public void selected(SelectEvent event)
			{
				// Select/unselect on left click on annotations
				if (event.getEventAction().equals(SelectEvent.LEFT_CLICK))
				{
					if (event.hasObjects())
					{
						if (event.getTopObject() instanceof RenderableAnnotation)
						{
							RenderableAnnotation a = (RenderableAnnotation) event
									.getTopObject();
							// Left click on an annotation - select
							selectAnnotation(a, true);
							if (isSelected(a))
								annotationsPanel.selectAnnotation(a
										.getAnnotation());
						}
					}
				}
				// Highlight on rollover
				else if (event.getEventAction().equals(SelectEvent.ROLLOVER)
						&& !this.dragger.isDragging())
				{
					highlight(event.getTopObject());
				}
				// Have drag events drag the selected object.
				else if (event.getEventAction().equals(SelectEvent.DRAG_END)
						|| event.getEventAction().equals(SelectEvent.DRAG))
				{
					if (event.hasObjects())
					{
						// If selected annotation delegate dragging computations to a dragger.
						if (event.getTopObject() == selectedAnnotation)
						{
							this.dragger.selected(event);
							selectedAnnotation.setDragging(event
									.getEventAction().equals(SelectEvent.DRAG));
						}
					}

					// We missed any roll-over events while dragging, so highlight any under the cursor now,
					// or de-highlight the dragged shape if it's no longer under the cursor.
					if (event.getEventAction().equals(SelectEvent.DRAG_END))
					{
						PickedObjectList pol = wwd
								.getObjectsAtCurrentPosition();
						if (pol != null)
						{
							highlight(pol.getTopObject());
							wwd.redraw();
						}
					}
				}

			}
		});
	}

	private void highlight(Object o)
	{
		// Manage highlighting of Annotations.
		if (lastPickedAnnotation == o)
			return; // same thing selected

		// Turn off highlight if on.
		if (lastPickedAnnotation != null) // && this.lastPickedObject != this.currentAnnotation)
		{
			lastPickedAnnotation.getAttributes().setHighlighted(false);
			lastPickedAnnotation = null;
		}

		// Turn on highlight if object selected.
		if (o != null && o instanceof RenderableAnnotation)
		{
			lastPickedAnnotation = (RenderableAnnotation) o;
			lastPickedAnnotation.getAttributes().setHighlighted(true);
		}
	}

	public void refresh()
	{
		annotations.clear();
		for (au.gov.ga.worldwind.annotations.Annotation annotation : Settings
				.get().getAnnotations())
		{
			RenderableAnnotation a = new RenderableAnnotation(annotation);
			annotations.add(a);
		}
	}

	@Override
	protected void doRender(DrawContext dc)
	{
		renderer.render(dc, annotations);
	}

	@Override
	protected void doPick(DrawContext dc, Point pickPoint)
	{
		renderer.pick(dc, annotations, pickPoint, this);
	}

	public void selectAnnotation(
			au.gov.ga.worldwind.annotations.Annotation annotation)
	{
		for (Annotation a : annotations)
		{
			RenderableAnnotation ra = (RenderableAnnotation) a;
			if (ra.getAnnotation() == annotation)
			{
				selectAnnotation(ra, false);
				break;
			}
		}
	}

	protected boolean isSelected(RenderableAnnotation annotation)
	{
		return annotation == selectedAnnotation;
	}

	protected void selectAnnotation(RenderableAnnotation annotation,
			boolean toggle)
	{
		if (selectedAnnotation != null)
		{
			selectedAnnotation.getAttributes().setBorderColor(savedBorderColor);
		}
		if (selectedAnnotation == annotation && toggle)
		{
			selectedAnnotation = null;
		}
		else
		{
			selectedAnnotation = annotation;
			savedBorderColor = selectedAnnotation.getAttributes()
					.getBorderColor();
			selectedAnnotation.getAttributes().setBorderColor(Color.YELLOW);
		}
	}
}
