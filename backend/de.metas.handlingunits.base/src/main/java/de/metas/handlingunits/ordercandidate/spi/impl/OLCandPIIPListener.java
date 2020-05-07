package de.metas.handlingunits.ordercandidate.spi.impl;

/*
 * #%L
 * de.metas.handlingunits.base
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


import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.model.I_C_OrderLine;

import de.metas.adempiere.gui.search.impl.OLCandHUPackingAware;
import de.metas.ordercandidate.model.I_C_OLCand;
import de.metas.ordercandidate.spi.IOLCandListener;

/**
 * See {@link #onOrderLineCreated(I_C_OLCand, I_C_OrderLine)}.
 * 
 * @author ts
 *
 */
public class OLCandPIIPListener implements IOLCandListener
{
	/**
	 * Sets the new order line's <code>M_HU_PI_Item_Product_ID</code> from the olCand's effective <code>M_HU_PI_Item_Product_ID</code>, so that the sytem won't have to guess.
	 */
	@Override
	public void onOrderLineCreated(final I_C_OLCand olCand, final I_C_OrderLine newOrderLine)
	{
		final de.metas.handlingunits.model.I_C_OrderLine newOrderLineExt = InterfaceWrapperHelper.create(newOrderLine, de.metas.handlingunits.model.I_C_OrderLine.class);
		final OLCandHUPackingAware olCandHUPackingAware = new OLCandHUPackingAware(olCand);

		newOrderLineExt.setM_HU_PI_Item_Product_ID(olCandHUPackingAware.getM_HU_PI_Item_Product_ID());
	}
}
