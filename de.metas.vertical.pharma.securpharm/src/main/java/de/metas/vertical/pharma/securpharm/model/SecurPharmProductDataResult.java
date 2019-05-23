/*
 *
 * * #%L
 * * %%
 * * Copyright (C) <current year> metas GmbH
 * * %%
 * * This program is free software: you can redistribute it and/or modify
 * * it under the terms of the GNU General Public License as
 * * published by the Free Software Foundation, either version 2 of the
 * * License, or (at your option) any later version.
 * *
 * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * * GNU General Public License for more details.
 * *
 * * You should have received a copy of the GNU General Public
 * * License along with this program. If not, see
 * * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * * #L%
 *
 */

package de.metas.vertical.pharma.securpharm.model;

import javax.annotation.Nullable;

import org.adempiere.util.lang.impl.TableRecordReference;

import de.metas.handlingunits.HuId;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Data
@FieldDefaults(makeFinal = true)
@Builder
public class SecurPharmProductDataResult
{
	boolean error;

	@Nullable
	private ProductData productData;

	@NonNull
	private SecurPharmRequestLogData requestLogData;

	@Nullable
	@NonFinal
	private HuId huId;

	@Nullable
	@NonFinal
	private SecurPharmProductDataResultId id;

	public TableRecordReference getRecordRef()
	{
		return TableRecordReference.of(I_M_Securpharm_Productdata_Result.Table_Name, getId());
	}
}
