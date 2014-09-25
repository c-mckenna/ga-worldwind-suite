package au.gov.ga.worldwind.openday;

import au.gov.ga.worldwind.common.view.delegate.DelegateOrbitView;

public class FlyOrbitView extends DelegateOrbitView
{
	@Override
	protected void markOutOfFocus()
	{
		//don't ever mark out of focus, so that center point is never on center of globe
	}
}
