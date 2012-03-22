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
package au.gov.ga.worldwind.common.util;

import java.util.Date;

/**
 * Represents a date/time, with second precision. Stores a long value in
 * seconds, which represents the number of seconds since midnight, January 1,
 * 1970 UTC.
 * <p/>
 * The reason for the creation of a separate class (and not using {@link Date})
 * is because Date stores the time as a long in millisecond precision, which
 * gives approximately 292 million years either side of present. Temporal data
 * may possibly lie outside of that (eg Paleogeographic data); storing the time
 * with second precision gives us 292 billion years to play with.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class BigDate implements Comparable<BigDate>
{
	private final static double yearLengthInDays = 365.242196;
	private final static double yearLengthInSeconds = 60.0 * 60.0 * 24.0 * yearLengthInDays;
	private final static long minSecondsToRepresentAsDateObject = Long.MIN_VALUE / 1000;
	private final static long maxSecondsToRepresentAsDateObject = Long.MAX_VALUE / 1000;

	/**
	 * Number of seconds since midnight, January 1, 1970 UTC.
	 */
	public final long seconds;

	protected BigDate(long seconds)
	{
		this.seconds = seconds;
	}

	/**
	 * @return Number of seconds since midnight, January 1, 1970 UTC.
	 */
	public long getSeconds()
	{
		return seconds;
	}

	/**
	 * @return Number of years ago
	 */
	public long numberOfYearsAgo()
	{
		long currentTimeInSeconds = System.currentTimeMillis() / 1000;
		long secondsAgo = currentTimeInSeconds - seconds;
		double yearsAgo = secondsAgo / yearLengthInSeconds;
		return Math.round(yearsAgo);
	}

	/**
	 * @return Convert this value to a {@link Date}. Returns null if this date
	 *         lies outside the maximum range that can be represented by the
	 *         {@link Date} object (absolute value is greater than approximately
	 *         292 million years).
	 */
	public Date dateValue()
	{
		if (seconds < minSecondsToRepresentAsDateObject || seconds > maxSecondsToRepresentAsDateObject)
		{
			return null;
		}
		return new Date(seconds * 1000);
	}

	/**
	 * @param date
	 * @return New {@link BigDate} object from the given {@link Date}.
	 */
	public static BigDate fromDate(Date date)
	{
		return fromMillis(date.getTime());
	}

	/**
	 * @param seconds
	 *            Number of seconds since midnight, January 1, 1970 UTC.
	 * @return New {@link BigDate} object.
	 */
	public static BigDate fromSeconds(long seconds)
	{
		return new BigDate(seconds);
	}

	/**
	 * @param millis
	 *            Number of milliseconds since midnight, January 1, 1970 UTC.
	 * @return New {@link BigDate} object.
	 */
	public static BigDate fromMillis(long millis)
	{
		return new BigDate(millis / 1000);
	}

	/**
	 * Create a new {@link BigDate} from the given value, in
	 * millions-of-years-ago.
	 * 
	 * @param ma
	 *            Millions of years ago.
	 * @return New {@link BigDate} object.
	 */
	public static BigDate fromMa(double ma)
	{
		return new BigDate(Math.round(System.currentTimeMillis() / 1000 - ma * yearLengthInSeconds * 1e6d));
	}

	/**
	 * @return New {@link BigDate} with the value of the current time.
	 */
	public static BigDate now()
	{
		return fromMillis(System.currentTimeMillis());
	}

	@Override
	public int compareTo(BigDate o)
	{
		long thisVal = this.seconds;
		long anotherVal = o.seconds;
		return thisVal < anotherVal ? -1 : thisVal == anotherVal ? 0 : 1;
	}
}
