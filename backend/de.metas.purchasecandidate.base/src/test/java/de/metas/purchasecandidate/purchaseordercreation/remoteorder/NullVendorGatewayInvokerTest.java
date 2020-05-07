package de.metas.purchasecandidate.purchaseordercreation.remoteorder;

import static java.math.BigDecimal.TEN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.adempiere.util.time.SystemTime;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import de.metas.purchasecandidate.PurchaseCandidate;
import de.metas.purchasecandidate.VendorProductInfo;
import de.metas.purchasecandidate.purchaseordercreation.remotepurchaseitem.PurchaseItem;
import de.metas.purchasecandidate.purchaseordercreation.remotepurchaseitem.PurchaseOrderItem;

/*
 * #%L
 * de.metas.purchasecandidate.base
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

public class NullVendorGatewayInvokerTest
{
	@Test
	public void placeRemotePurchaseOrder()
	{
		final int productId = 20;
		final int vendorBPartnerId = 30;

		final VendorProductInfo vendorProductInfo = VendorProductInfo.builder()
				.bPartnerProductId(10)
				.productId(productId)
				.vendorBPartnerId(vendorBPartnerId)
				.productName("productName")
				.productNo("productNo").build();

		final PurchaseCandidate purchaseCandidate = PurchaseCandidate.builder()
				.orgId(10)
				.dateRequired(SystemTime.asTimestamp())
				.vendorProductInfo(vendorProductInfo)
				.vendorBPartnerId(vendorBPartnerId)
				.productId(productId)
				.qtyToPurchase(TEN)
				.salesOrderId(40)
				.salesOrderLineId(50)
				.warehouseId(60)
				.uomId(70)
				.build();

		final List<PurchaseItem> purchaseItems = NullVendorGatewayInvoker.INSTANCE.placeRemotePurchaseOrder(ImmutableList.of(purchaseCandidate));

		assertThat(purchaseItems).hasSize(1);
		assertThat(purchaseItems.get(0)).isInstanceOf(PurchaseOrderItem.class);

		final PurchaseOrderItem purchaseOrderItem = (PurchaseOrderItem)purchaseItems.get(0);
		assertThat(purchaseOrderItem.getRemotePurchaseOrderId()).isEqualTo(NullVendorGatewayInvoker.NO_REMOTE_PURCHASE_ID);
	}

}
