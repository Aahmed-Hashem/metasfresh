package de.metas.attachments;

/*
 * #%L
 * metasfresh-invoice_gateway.spi
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

public class AttachmentConstants
{
	public static final String TAGNAME_IS_DOCUMENT = "IsDocument";

	/**
	 * if set to {@code true}, it advises the system that
	 * the respective attachment is a PDF
	 * and when a PDF is created for the invoice to which it is attached, then this attachment's PDF shall be appended to that invoice's PDF.
	 */
	public static final String TAGNAME_CONCATENATE_PDF_TO_INVOICE_PDF = "Concatenate_Pdf_to_InvoicePdf";

	public static final String TAGNAME_BPARTNER_RECIPIENT_ID = "C_BPartner_Recipient_ID";

	public static final String TAGNAME_STORED_PREFIX = "Stored_";

	public static final String TAGS_SEPARATOR = "\n";
	
	public static final String TAGS_KEY_VALUE_SEPARATOR = "=";
}
