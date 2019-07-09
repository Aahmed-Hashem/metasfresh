package de.metas.email;

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

import javax.mail.internet.InternetAddress;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_AD_Client;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_C_DocType;

import de.metas.email.templates.MailTextBuilder;
import de.metas.email.templates.MailTemplate;
import de.metas.email.templates.MailTemplateId;
import de.metas.i18n.ITranslatableString;
import de.metas.util.ISingletonService;

/**
 * Mail configuration
 *
 * @author Cristina Ghita, Metas.RO
 * @see http://dewiki908/mediawiki/index.php/US901:_Postfächer_pro_Organisation_einstellen_können_%282010110510000031 %29
 */
public interface IMailBL extends ISingletonService
{
	/**
	 * @throws MailboxNotFoundException
	 */
	Mailbox findMailBox(I_AD_Client client,
			int AD_Org_ID,
			int AD_Process_ID,
			I_C_DocType docType,
			String customType,
			I_AD_User user);

	public static final class MailboxNotFoundException extends AdempiereException
	{
		private static final long serialVersionUID = 8412719309323544757L;

		public MailboxNotFoundException(ITranslatableString msg)
		{
			super(msg);
		}
	}

	EMail createEMail(I_AD_Client client,
			String mailCustomType,
			String to,
			String subject,
			String message,
			boolean html);

	EMail createEMail(I_AD_Client client,
			String mailCustomType,
			I_AD_User from,
			String to,
			String subject,
			String message,
			boolean html);

	/**
	 * @param html see the javadoc in {@link EMail}
	 */
	EMail createEMail(Properties ctx,
			Mailbox mailbox,
			String to,
			String subject,
			String message,
			boolean html);

	/**
	 * Gets the EMail TO address to be used when sending mails. If present, this address will be used to send all emails to a particular address instead of actual email addresses.
	 *
	 * @param ctx
	 * @return email address or null
	 */
	InternetAddress getDebugMailToAddressOrNull(Properties ctx);

	/**
	 * Send given email
	 *
	 * @param email
	 * @throws AdempiereException on any encountered error
	 */
	void send(EMail email);

	/**
	 *
	 * NOTE: this method is here temporary until we will properly refactor our Mail related BL
	 *
	 * @param e
	 * @return true if given exeption is about a connection error while trying to send the email
	 */
	boolean isConnectionError(Exception e);

	MailTextBuilder newMailTextBuilder(MailTemplate mailTemplate);

	MailTextBuilder newMailTextBuilder(MailTemplateId mailTemplateId);

	void validateEmail(String email);
}
