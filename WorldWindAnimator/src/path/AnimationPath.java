package path;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

public class AnimationPath implements Serializable
{
	public final SortedSet<AnimationPoint> points = new TreeSet<AnimationPoint>();

	public Point getPositionAt(double time)
	{
		if (points.first().time > time)
		{
			return points.first();
		}
		else if (points.last().time <= time)
		{
			return points.last();
		}

		AnimationPoint p1 = null;
		AnimationPoint p2 = null;

		for (AnimationPoint point : points)
		{
			p2 = p1;
			p1 = point;
			if (p2 != null)
			{
				if (p1.time <= time && p2.time > time)
					break;
			}
		}

		if (p1 != null && p2 != null)
		{
			double percent = (time - p1.time) / (p2.time - p1.time);
			return AnimationPoint.bezierInterpolate(p1, p2, percent);
		}

		return null;
	}
}