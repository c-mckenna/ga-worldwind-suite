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
package au.gov.ga.worldwind.common.layers.temporal;

import au.gov.ga.worldwind.common.util.BigDate;
import gov.nasa.worldwind.layers.Layer;

/**
 * Represents a layer that has an extra dimension: time. This means that the
 * layer's visual representation could change over time.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public interface TemporalLayer extends Layer
{
	/**
	 * @return The date that this layer is displaying.
	 */
	BigDate getDate();

	/**
	 * Set the date that this layer should display.
	 * 
	 * @param date
	 */
	void setDate(BigDate date);

	/**
	 * @return Collection of dates at which this temporal layer has data.
	 */
	Iterable<BigDate> dataDates();

	/**
	 * @param date
	 * @return Label of this layer for the given date. Returns null if there's
	 *         no label at the given date.
	 */
	String labelAtDate(BigDate date);
}
