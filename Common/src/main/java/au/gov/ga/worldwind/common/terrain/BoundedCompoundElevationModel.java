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
package au.gov.ga.worldwind.common.terrain;

import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import au.gov.ga.worldwind.common.layers.Bounded;
import au.gov.ga.worldwind.common.layers.Bounds;

/**
 * Extension to {@link CompoundElevationModel} that implements the
 * {@link Bounded} interface.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class BoundedCompoundElevationModel extends CompoundElevationModel implements Bounded
{
	@Override
	public Bounds getBounds()
	{
		Bounds bounds = null;
		for (ElevationModel model : getElevationModels())
		{
			if (model instanceof Bounded)
			{
				bounds = Bounds.union(bounds, ((Bounded) model).getBounds());
			}
		}
		return bounds;
	}

	@Override
	public boolean isFollowTerrain()
	{
		return true;
	}
}
