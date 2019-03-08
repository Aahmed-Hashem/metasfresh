package de.metas.user.api;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_AD_Org;
import org.compiere.model.I_AD_User;
import org.compiere.util.Env;

import de.metas.i18n.ITranslatableString;
import de.metas.i18n.Language;
import de.metas.util.ISingletonService;
import de.metas.util.hash.HashableString;

public interface IUserBL extends ISingletonService
{
	HashableString getUserPassword(I_AD_User user);

	boolean isPasswordMatching(I_AD_User user, HashableString password);

	void createResetPasswordByEMailRequest(I_AD_User user);

	void createResetPasswordByEMailRequest(String userId);

	I_AD_User resetPassword(String passwordResetCode, String newPassword);

	/**
	 * Change given user's password.
	 *
	 * @param ctx context, IMPORTANT because will be used to fetch current logged in user credentials
	 * @param adUserId the AD_User_ID of which the password shall be changed
	 * @param oldPassword old/current password
	 * @param newPassword new password
	 * @param newPasswordRetype new password again
	 */
	void changePassword(final Properties ctx, final int adUserId, final HashableString oldPassword, final String newPassword, final String newPasswordRetype);

	void changePasswordAndSave(I_AD_User user, String newPassword);

	/**
	 * Generates and sets a new password for given user. The user will be also saved.
	 *
	 * @param user
	 * @return new password
	 */
	String generatedAndSetPassword(I_AD_User user);

	/**
	 * Asserts given user password is valid according to our security settings.
	 */
	void assertValidPassword(String passwordPlain);

	/**
	 * create a new user in specified org with the specified name
	 *
	 * @param name
	 * @param org
	 * @return
	 */
	I_AD_User createUser(String name, I_AD_Org org);

	/**
	 * Checks if given user has a C_BPartner which is an employee.
	 *
	 * @param user
	 * @return true if is employee
	 */
	boolean isEmployee(final org.compiere.model.I_AD_User user);

	String buildContactName(final String firstName, final String lastName);

	/**
	 * Is the email valid
	 *
	 * @return return true if email is valid (artificial check)
	 */
	boolean isEMailValid(I_AD_User user);

	/**
	 * Could we send an email from this user
	 *
	 * @return <code>null</code> if OK, error message if not ok
	 */
	ITranslatableString checkCanSendEMail(I_AD_User user);

	ITranslatableString checkCanSendEMail(int adUserId);

	default void assertCanSendEMail(final int adUserId)
	{
		final ITranslatableString errmsg = checkCanSendEMail(adUserId);
		if (errmsg != null)
		{
			throw new AdempiereException("User cannot send emails: " + errmsg.translate(Env.getAD_Language(Env.getCtx())));
		}
	}

	/** @return the user's language or fallbacks; never returns {@code null}. */
	Language getUserLanguage(I_AD_User userRecord);
}
