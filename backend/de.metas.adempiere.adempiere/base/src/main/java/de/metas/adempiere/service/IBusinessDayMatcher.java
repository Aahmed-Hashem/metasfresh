package de.metas.adempiere.service;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.util.Date;
import java.util.Set;

/**
 * Implementations of this interface are responsible of:
 * <ul>
 * <li>validating if a given date is a bussiness-day
 * <li>getting next business day
 * </ul>
 * 
 * @author tsa
 *
 */
public interface IBusinessDayMatcher
{
	/**
	 * Gets next business day.
	 * 
	 * If given date is a business day then that date will be returned.
	 * 
	 * @param date
	 * @return next business day
	 */
	Date getNextBusinessDay(final Date date);

	/**
	 * 
	 * @param date
	 * @return true if given date is a business day
	 */
	boolean isBusinessDay(final Date date);

	/**
	 * Sets week days which shall be considered as non-business day.
	 * 
	 * 
	 * NOTE: I Don't understand why this one was made this way. And it is never used in the code
	 * Replaced it with de.metas.adempiere.service.IBusinessDayMatcher.setWeekendDays(Set<Integer>)
	 * 
	 * @param daysOfWeek
	 * @deprecated
	 */
	void setWeekendDays(final int... daysOfWeek);
	
	/**
	 *  Sets week days which shall be considered as non-business day.
	 *  
	 * @param daysOfWeek
	 */
	void setWeekendDays(Set<Integer> daysOfWeek);

	/**
	 * 
	 * @return days of week which will be considered non-business day
	 */
	Set<Integer> getWeekendDays();

	
}
