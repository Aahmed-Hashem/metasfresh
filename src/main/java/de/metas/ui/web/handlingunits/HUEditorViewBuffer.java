package de.metas.ui.web.handlingunits;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import de.metas.ui.web.exceptions.EntityNotFoundException;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.model.DocumentQueryOrderBy;

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
 * Buffer which contains all {@link HUEditorRow}s required for a given {@link HUEditorView} instance.
 *
 * Implementations of this interface are responsible for fetching the {@link HUEditorRow}s and (maybe)caching them.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
interface HUEditorViewBuffer
{
	ViewId getViewId();
	
	long size();

	void invalidateAll();

	boolean addHUIds(Collection<Integer> huIdsToAdd);

	boolean removeHUIds(Collection<Integer> huIdsToRemove);

	boolean containsAnyOfHUIds(Collection<Integer> huIdsToCheck);

	/** @return top level rows and included rows recursive stream */
	Stream<HUEditorRow> streamAllRecursive() throws UnsupportedOperationException;

	/**
	 * Stream all rows (including children) which match any of given <code>rowIds</code>.
	 * 
	 * If a rowId is included in another row (which will be returned by this method), then that row will be excluded.
	 * e.g.
	 * Consider having following structure: rowId=1 which includes rowId=2 which includes rowId=3.
	 * <ul>
	 * <li>When this method will be called with rowIds={1, 3}, only rowId=1 will be returned because rowId=3 is indirectly included in rowId=1.
	 * <li>When this method will be called with rowIds={3}, rowId=3 will be returned because it's not included in any of the rowIds we asked for.
	 * <li>
	 * </ul>
	 * 
	 */
	Stream<HUEditorRow> streamByIdsExcludingIncludedRows(Collection<DocumentId> rowIds);

	Stream<HUEditorRow> streamPage(int firstRow, int pageLength, List<DocumentQueryOrderBy> orderBys);

	HUEditorRow getById(DocumentId rowId) throws EntityNotFoundException;

	Set<DocumentId> getRowIdsMatchingBarcode(String barcode);
}
