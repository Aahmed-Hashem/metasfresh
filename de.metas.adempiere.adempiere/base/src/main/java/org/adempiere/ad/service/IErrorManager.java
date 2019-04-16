package org.adempiere.ad.service;

import org.adempiere.ad.element.api.AdWindowId;
import org.compiere.model.I_AD_Issue;

import de.metas.util.ISingletonService;

/**
 * System Error Manager. This subsystem is responsible with error logging, AD_Issue creation etc.
 *
 * @author tsa
 *
 */
public interface IErrorManager extends ISingletonService
{

	public static final AdWindowId AD_ISSUE_WINDOW_ID = AdWindowId.ofRepoId(363);

	/**
	 * Creates, saves and returns and {@link I_AD_Issue} based on given {@link Throwable} object.
	 *
	 * @param name issue name; if null then default issue name will be used (i.e. Error)
	 */
	I_AD_Issue createIssue(String name, Throwable t);

	default I_AD_Issue createIssue(Throwable t)
	{
		final String name = null;
		return createIssue(name, t);
	}

}
