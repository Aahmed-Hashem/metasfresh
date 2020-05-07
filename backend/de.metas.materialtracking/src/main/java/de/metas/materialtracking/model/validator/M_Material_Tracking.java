package de.metas.materialtracking.model.validator;

/*
 * #%L
 * de.metas.materialtracking
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

import org.adempiere.ad.callout.spi.IProgramaticCalloutProvider;
import org.adempiere.ad.modelvalidator.annotations.Init;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_M_AttributeValue;
import org.compiere.model.ModelValidator;

import de.metas.materialtracking.IMaterialTrackingAttributeBL;
import de.metas.materialtracking.model.I_M_Material_Tracking;

@Interceptor(I_M_Material_Tracking.class)
public class M_Material_Tracking
{
	@Init
	public void setupCallouts()
	{
		// Setup callout M_Material_Tracking
		final IProgramaticCalloutProvider calloutProvider = Services.get(IProgramaticCalloutProvider.class);
		calloutProvider.registerAnnotatedCallout(new de.metas.materialtracking.ch.lagerkonf.callout.M_Material_Tracking());
	}

	/**
	  * Note: ifColumnsChanged is adapted to the BL that is called. if you change the BL, pls check if ifColumnsChanged also needs to be changed
	  */
	@ModelChange(
			timings = { ModelValidator.TYPE_BEFORE_NEW,
					ModelValidator.TYPE_BEFORE_CHANGE },
			ifColumnsChanged = { I_M_Material_Tracking.COLUMNNAME_Lot,
					I_M_Material_Tracking.COLUMNNAME_C_BPartner_ID,
					I_M_Material_Tracking.COLUMNNAME_M_Product_ID })
	public void createMaterialTrackingAttributeValue(final I_M_Material_Tracking materialTracking)
	{
		final IMaterialTrackingAttributeBL materialTrackingAttributeBL = Services.get(IMaterialTrackingAttributeBL.class);
		materialTrackingAttributeBL.createOrUpdateMaterialTrackingAttributeValue(materialTracking);

		// NOTE: in case of a new material tracking, the generated M_AttributeValue.Value will be "0" (or a random number) because M_Material_Tracking is not yet saved
		// so we will need to update it after save (new)
	}

	/**
	 * After a new material tracking is saved, we need to make sure M_AttributeValue.Value is correct
	 *
	 * @param materialTracking
	 */
	@ModelChange(timings = { ModelValidator.TYPE_AFTER_NEW })
	public void updateMaterialTrackingAttributeValue_Value(final I_M_Material_Tracking materialTracking)
	{
		Check.assumeNotNull(materialTracking.getM_AttributeValue_ID() > 0, "material tracking has attribute value created");
		final I_M_AttributeValue attributeValue = materialTracking.getM_AttributeValue();

		final IMaterialTrackingAttributeBL materialTrackingAttributeBL = Services.get(IMaterialTrackingAttributeBL.class);
		final String attributeValue_Value = materialTrackingAttributeBL.getMaterialTrackingIdStr(materialTracking);
		attributeValue.setValue(attributeValue_Value);
		InterfaceWrapperHelper.save(attributeValue);
	}

	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_DELETE })
	public void deleteMaterialTrackingAttributeValue(final I_M_Material_Tracking materialTracking)
	{
		if (materialTracking.getM_AttributeValue_ID() <= 0)
		{
			// nothing to do
			return;
		}

		// NOTE: instead of deleting attribute value, just inactivate it because it might be that is used
		final I_M_AttributeValue attributeValue = materialTracking.getM_AttributeValue();
		attributeValue.setIsActive(false);
		InterfaceWrapperHelper.save(attributeValue);
	}

}
