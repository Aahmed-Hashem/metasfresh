package de.metas.ui.web.process;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.window.datatypes.DocumentIdsSelection;
import de.metas.ui.web.window.datatypes.DocumentPath;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

/**
 * Request for creating a new process instance.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Value
@Immutable
public final class CreateProcessInstanceRequest
{
	private ProcessId processId;

	private final DocumentPath singleDocumentPath;

	private ViewId viewId;
	private DocumentIdsSelection viewDocumentIds;

	@Builder
	private CreateProcessInstanceRequest( //
			@NonNull final ProcessId processId //
			, final DocumentPath singleDocumentPath //
			, final ViewId viewId //
			, final DocumentIdsSelection viewDocumentIds //
	)
	{
		this.processId = processId;

		this.singleDocumentPath = singleDocumentPath;

		this.viewId = viewId;
		this.viewDocumentIds = viewDocumentIds;
	}

	public void assertProcessIdEquals(final ProcessId expectedProcessId)
	{
		if (!Objects.equals(processId, expectedProcessId))
		{
			throw new IllegalArgumentException("Request's processId is not valid. It shall be " + expectedProcessId + " but it was " + processId);
		}
	}

	public int getProcessIdAsInt()
	{
		return processId.getProcessIdAsInt();
	}

}
