package de.metas.document.archive;

import java.util.Optional;

import org.adempiere.user.User;

import de.metas.document.archive.model.I_C_Doc_Outbound_Log;

/*
 * #%L
 * de.metas.document.archive.base
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public interface DocOutboundLogMailRecipientProvider
{
	/** *One* registered provider may return {@code true}. */
	public boolean isDefault();

	/**
	 *
	 * Will return null if {@link #isDefault()}, otherwise will never return null;
	 * Feel free to add other discriminatory methods like isSOTrx(), getDocBaseType() etc, if and when needed.
	 */
	public String getTableName();

	/**
	 * Unless the implementor has {@link #isDefault()} {@code == true}, it can safely assume that the docOutboundLogRecord's {@code AD_Table_ID}'s name is {@link #getTableName()}.
	 */
	public Optional<User> provideMailRecipient(I_C_Doc_Outbound_Log docOutboundLogRecord);
}
