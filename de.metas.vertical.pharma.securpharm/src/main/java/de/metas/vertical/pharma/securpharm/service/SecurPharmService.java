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

package de.metas.vertical.pharma.securpharm.service;

import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.util.Env;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import de.metas.handlingunits.HuId;
import de.metas.handlingunits.model.I_M_Inventory;
import de.metas.i18n.IMsgBL;
import de.metas.inventory.InventoryId;
import de.metas.notification.INotificationBL;
import de.metas.notification.UserNotificationRequest;
import de.metas.util.Services;
import de.metas.vertical.pharma.securpharm.SecurPharmClient;
import de.metas.vertical.pharma.securpharm.SecurPharmClientFactory;
import de.metas.vertical.pharma.securpharm.SecurPharmConstants;
import de.metas.vertical.pharma.securpharm.model.DecommissionAction;
import de.metas.vertical.pharma.securpharm.model.SecurPharmActionResult;
import de.metas.vertical.pharma.securpharm.model.SecurPharmConfig;
import de.metas.vertical.pharma.securpharm.model.SecurPharmProductDataResult;
import de.metas.vertical.pharma.securpharm.model.SecurPharmProductDataResultId;
import de.metas.vertical.pharma.securpharm.repository.SecurPharmConfigRespository;
import lombok.NonNull;

@Service
public class SecurPharmService
{
	private final SecurPharmClientFactory clientFactory;
	private final SecurPharmResultService resultService;
	private final SecurPharmConfigRespository configRespository;

	public SecurPharmService(
			@NonNull final SecurPharmClientFactory clientFactory,
			@NonNull final SecurPharmResultService resultService,
			@NonNull final SecurPharmConfigRespository configRespository)
	{
		this.clientFactory = clientFactory;
		this.resultService = resultService;
		this.configRespository = configRespository;
	}

	public boolean hasConfig()
	{
		return configRespository.isConfigured();
	}

	public SecurPharmProductDataResult getAndSaveProductData(
			@NonNull final String datamatrix,
			@NonNull final HuId huId)
	{
		final SecurPharmClient client = clientFactory.createClient();

		final SecurPharmProductDataResult productDataResult = client.decodeDataMatrix(datamatrix);
		productDataResult.setHuId(huId);

		final SecurPharmProductDataResult result = resultService.createAndSaveResult(productDataResult);

		if (result.isError())
		{
			sendNotification(
					client.getConfig(),
					result.getRecordRef(),
					SecurPharmConstants.SECURPHARM_SCAN_RESULT_ERROR_NOTIFICATION_MESSAGE_KEY);
		}

		return result;
	}

	private void sendNotification(
			@NonNull final SecurPharmConfig config,
			@NonNull final TableRecordReference recordRef,
			@NonNull final String notificationMsgKey)
	{
		final String message = Services.get(IMsgBL.class).getMsg(Env.getCtx(), notificationMsgKey);

		final UserNotificationRequest userNotificationRequest = UserNotificationRequest.builder()
				.recipientUserId(config.getSupportUserId())
				.contentPlain(message)
				.targetAction(UserNotificationRequest.TargetRecordAction.of(recordRef))
				.build();

		Services.get(INotificationBL.class).sendAfterCommit(userNotificationRequest);
	}

	@Async
	public SecurPharmActionResult decommision(
			@NonNull final SecurPharmProductDataResult productDataResult,
			@NonNull final DecommissionAction action,
			@NonNull final InventoryId inventoryId)
	{
		final SecurPharmClient client = clientFactory.createClient();
		final SecurPharmActionResult actionResult = client.decommission(productDataResult.getProductData(), action);

		actionResult.setInventoryId(inventoryId);
		actionResult.setProductDataResult(productDataResult);

		return handleActionResult(actionResult, inventoryId, client.getConfig());
	}

	@Async
	public SecurPharmActionResult undoDecommision(
			@NonNull final SecurPharmActionResult initialActionResult,
			@NonNull final DecommissionAction action,
			@NonNull final InventoryId inventoryId)
	{
		final SecurPharmClient client = clientFactory.createClient();
		final SecurPharmProductDataResult productDataResult = initialActionResult.getProductDataResult();
		final SecurPharmActionResult actionResult = client.undoDecommission(
				productDataResult.getProductData(),
				action,
				initialActionResult.getRequestLogData().getServerTransactionId());

		actionResult.setInventoryId(inventoryId);
		actionResult.setProductDataResult(productDataResult);

		return handleActionResult(actionResult, inventoryId, client.getConfig());
	}

	private SecurPharmActionResult handleActionResult(
			@NonNull final SecurPharmActionResult productDataResult,
			@NonNull InventoryId inventoryId,
			@NonNull final SecurPharmConfig config)
	{
		productDataResult.setInventoryId(inventoryId);
		final SecurPharmActionResult result = resultService.createAndSaveResult(productDataResult);
		if (result.isError())
		{
			sendNotification(
					config,
					TableRecordReference.of(I_M_Inventory.Table_Name, inventoryId),
					SecurPharmConstants.SECURPHARM_ACTION_RESULT_ERROR_NOTIFICATION_MESSAGE_KEY);
		}
		return result;
	}

	public SecurPharmProductDataResult getProductDataResultById(@NonNull final SecurPharmProductDataResultId productDataResultId)
	{
		return resultService.getProductDataResultById(productDataResultId);
	}
}
