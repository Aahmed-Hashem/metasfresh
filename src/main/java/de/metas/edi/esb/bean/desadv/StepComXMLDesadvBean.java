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

package de.metas.edi.esb.bean.desadv;

import static de.metas.edi.esb.commons.Util.formatNumber;
import static de.metas.edi.esb.commons.Util.toDate;
import static de.metas.edi.esb.commons.Util.toFormattedStringDate;
import static de.metas.edi.esb.commons.ValidationHelper.validateString;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;

import org.apache.camel.Exchange;
import org.apache.commons.lang.StringUtils;

import de.metas.edi.esb.commons.Constants;
import de.metas.edi.esb.commons.StepComUtil;
import de.metas.edi.esb.commons.SystemTime;
import de.metas.edi.esb.commons.Util;
import de.metas.edi.esb.jaxb.metasfresh.EDIExpCBPartnerLocationType;
import de.metas.edi.esb.jaxb.metasfresh.EDIExpDesadvLineType;
import de.metas.edi.esb.jaxb.metasfresh.EDIExpDesadvType;
import de.metas.edi.esb.jaxb.stepcom.desadv.DETAILXlief;
import de.metas.edi.esb.jaxb.stepcom.desadv.DPRDE1;
import de.metas.edi.esb.jaxb.stepcom.desadv.DPRIN1;
import de.metas.edi.esb.jaxb.stepcom.desadv.DQUAN1;
import de.metas.edi.esb.jaxb.stepcom.desadv.DQVAR1;
import de.metas.edi.esb.jaxb.stepcom.desadv.DREFE1;
import de.metas.edi.esb.jaxb.stepcom.desadv.Document;
import de.metas.edi.esb.jaxb.stepcom.desadv.HADRE1;
import de.metas.edi.esb.jaxb.stepcom.desadv.HDATE1;
import de.metas.edi.esb.jaxb.stepcom.desadv.HEADERXlief;
import de.metas.edi.esb.jaxb.stepcom.desadv.HREFE1;
import de.metas.edi.esb.jaxb.stepcom.desadv.HRFAD1;
import de.metas.edi.esb.jaxb.stepcom.desadv.ObjectFactory;
import de.metas.edi.esb.jaxb.stepcom.desadv.PACKINXlief;
import de.metas.edi.esb.jaxb.stepcom.desadv.PPACK1;
import de.metas.edi.esb.jaxb.stepcom.desadv.TRAILR;
import de.metas.edi.esb.jaxb.stepcom.desadv.Xlief4H;
import de.metas.edi.esb.pojo.common.MeasurementUnit;
import de.metas.edi.esb.pojo.desadv.qualifier.AddressQual;
import de.metas.edi.esb.pojo.desadv.qualifier.ControlQual;
import de.metas.edi.esb.pojo.desadv.qualifier.DateQual;
import de.metas.edi.esb.pojo.desadv.qualifier.DiscrepencyCode;
import de.metas.edi.esb.pojo.desadv.qualifier.DocumentFunction;
import de.metas.edi.esb.pojo.desadv.qualifier.DocumentType;
import de.metas.edi.esb.pojo.desadv.qualifier.PackIdentificationQual;
import de.metas.edi.esb.pojo.desadv.qualifier.PackagingCode;
import de.metas.edi.esb.pojo.desadv.qualifier.PackagingLevel;
import de.metas.edi.esb.pojo.desadv.qualifier.ProductDescQual;
import de.metas.edi.esb.pojo.desadv.qualifier.ProductQual;
import de.metas.edi.esb.pojo.desadv.qualifier.QuantityQual;
import de.metas.edi.esb.pojo.desadv.qualifier.ReferenceQual;
import de.metas.edi.esb.route.AbstractEDIRoute;
import de.metas.edi.esb.route.exports.StepComXMLDesadvRoute;
import lombok.NonNull;

public class StepComXMLDesadvBean
{
	public static final String METHOD_createXMLEDIData = "createXMLEDIData";

	private static final String DEFAULT_PACK_DETAIL = "1";
	private static final ObjectFactory DESADV_objectFactory = new ObjectFactory();

	public final void createXMLEDIData(final Exchange exchange)
	{
		final EDIDesadvValidation validation = new EDIDesadvValidation();

		// validate mandatory exchange properties
		validateString(
				exchange.getProperty(StepComXMLDesadvRoute.EDI_XML_DESADV_IS_TEST, String.class),
				"exchange property " + StepComXMLDesadvRoute.EDI_XML_DESADV_IS_TEST + " cannot be null or empty");

		final EDIExpDesadvType xmlDesadv = validation.validateExchange(exchange); // throw exceptions if mandatory fields are missing
		final Document desadvDocument = createDesadvDocumentFromXMLBean(xmlDesadv, exchange);

		exchange
				.getIn()
				.setBody(DESADV_objectFactory.createDocument(desadvDocument));
	}

	private Document createDesadvDocumentFromXMLBean(@NonNull final EDIExpDesadvType xmlDesadv, @NonNull final Exchange exchange)
	{
		final DecimalFormat decimalFormat = exchange.getProperty(Constants.DECIMAL_FORMAT, DecimalFormat.class);
		final String isTest = exchange.getProperty(StepComXMLDesadvRoute.EDI_XML_DESADV_IS_TEST, String.class);
		final String dateFormat = (String)exchange.getProperty(AbstractEDIRoute.EDI_ORDER_EDIMessageDatePattern);

		final String ownerId = exchange.getProperty(StepComXMLDesadvRoute.EDI_XML_OWNER_ID, String.class);
		final String applicationRef = exchange.getProperty(StepComXMLDesadvRoute.EDI_XML_APPLICATION_REF, String.class);
		final String supplierGln = exchange.getProperty(StepComXMLDesadvRoute.EDI_XML_SUPPLIER_GLN, String.class);
		final String supplierAdditionalId = exchange.getProperty(StepComXMLDesadvRoute.EDI_XML_SUPPLIER_ADDITIONAL_ID, String.class);

		xmlDesadv.getEDIExpDesadvLine().sort(Comparator.comparing(EDIExpDesadvLineType::getLine));
		final Document document = DESADV_objectFactory.createDocument();
		final Xlief4H xlief4H = DESADV_objectFactory.createXlief4H();

		final HEADERXlief header = DESADV_objectFactory.createHEADERXlief();
		final String documentId = xmlDesadv.getDocumentNo();

		final String partnerId = StepComUtil.resolvePartnerId(
				exchange.getContext(),
				xmlDesadv.getCBPartnerID().getEdiRecipientGLN());

		header.setDOCUMENTID(documentId);
		header.setPARTNERID(partnerId);
		header.setOWNERID(ownerId);
		header.setTESTINDICATOR(isTest);
		header.setMESSAGEREF(formatNumber(xmlDesadv.getSequenceNoAttr(), decimalFormat));
		header.setAPPLICATIONREF(applicationRef);
		header.setDOCUMENTTYP(DocumentType.DADV.name());
		header.setDOCUMENTFUNCTION(DocumentFunction.ORIG.name());

		mapDates(xmlDesadv, dateFormat, header);

		mapReferences(xmlDesadv, dateFormat, header);

		mapAddresses(xmlDesadv, header, supplierGln, supplierAdditionalId);

		mapPackaging(xmlDesadv, decimalFormat, header);

		final TRAILR trailr = DESADV_objectFactory.createTRAILR();
		trailr.setDOCUMENTID(documentId);
		trailr.setCONTROLQUAL(ControlQual.LINE.name());
		trailr.setCONTROLVALUE(formatNumber(xmlDesadv.getEDIExpDesadvLine().size(), decimalFormat));

		xlief4H.setHEADER(header);
		xlief4H.setTRAILR(trailr);
		document.getXlief4H().add(xlief4H);
		return document;
	}

	private DiscrepencyCode getDiscrepancyCode(final String isSubsequentDeliveryPlanned, final BigDecimal diff)
	{
		DiscrepencyCode discrepancyCode;
		if (diff.signum() == -1)
		{
			discrepancyCode = DiscrepencyCode.OVSH; // = Over-shipped
			return discrepancyCode;
		}
		if (Boolean.parseBoolean(isSubsequentDeliveryPlanned))
		{
			discrepancyCode = DiscrepencyCode.BFOL; // = Shipment partial - back order to follow
		}
		else
		{
			discrepancyCode = DiscrepencyCode.BCOM; // = shipment partial - considered complete, no backorder;
		}
		return discrepancyCode;
	}

	private void mapPackaging(final EDIExpDesadvType xmlDesadv, final DecimalFormat decimalFormat, final HEADERXlief header)
	{
		final PACKINXlief packaging = DESADV_objectFactory.createPACKINXlief();
		packaging.setDOCUMENTID(header.getDOCUMENTID());
		final int orderLineCount = xmlDesadv.getEDIExpDesadvLine().size();
		packaging.setPACKAGINGTOTAL(formatNumber(orderLineCount, decimalFormat));

		for (EDIExpDesadvLineType ediExpDesadvLineType : xmlDesadv.getEDIExpDesadvLine())
		{
			final PPACK1 packDetail = DESADV_objectFactory.createPPACK1();
			packDetail.setDOCUMENTID(header.getDOCUMENTID());
			// usually one, as per spec
			packDetail.setPACKAGINGDETAIL(DEFAULT_PACK_DETAIL);
			// ISO1 as default for now
			packDetail.setPACKAGINGCODE(PackagingCode.ISO1.name());
			// full pallet as default for now
			packDetail.setPACKAGINGLEVEL(PackagingLevel.OUTE.name());
			packDetail.setIDENTIFICATIONQUAL(PackIdentificationQual.SSCC.name());
			final String sscc18Value = Util.removePrecedingZeros(ediExpDesadvLineType.getIPASSCC18());
			packDetail.setIDENTIFICATIONCODE(sscc18Value);

			mapDetail(decimalFormat, ediExpDesadvLineType, packDetail);
			packaging.getPPACK1().add(packDetail);
		}
		header.setPACKIN(packaging);
	}

	private void mapDetail(final DecimalFormat decimalFormat, final EDIExpDesadvLineType ediExpDesadvLineType, final PPACK1 packDetail)
	{
		final String documentId = packDetail.getDOCUMENTID();
		final DETAILXlief detail = DESADV_objectFactory.createDETAILXlief();
		final String lineNumber = formatNumber(ediExpDesadvLineType.getLine(), decimalFormat);
		detail.setDOCUMENTID(documentId);
		detail.setLINENUMBER(lineNumber);

		if (StringUtils.isNotEmpty(ediExpDesadvLineType.getUPC()))
		{
			final DPRIN1 prodInfo = DESADV_objectFactory.createDPRIN1();
			prodInfo.setDOCUMENTID(documentId);
			prodInfo.setPRODUCTQUAL(ProductQual.GTIN.name());
			prodInfo.setLINENUMBER(lineNumber);
			prodInfo.setPRODUCTID(ediExpDesadvLineType.getUPC());
			detail.getDPRIN1().add(prodInfo);
		}

		if (StringUtils.isNotEmpty(ediExpDesadvLineType.getProductNo()))
		{
			final DPRIN1 prodInfo = DESADV_objectFactory.createDPRIN1();
			prodInfo.setDOCUMENTID(documentId);
			prodInfo.setPRODUCTQUAL(ProductQual.BUYR.name());
			prodInfo.setLINENUMBER(lineNumber);
			prodInfo.setPRODUCTID(ediExpDesadvLineType.getProductNo());
			detail.getDPRIN1().add(prodInfo);
		}

		if (StringUtils.isNotEmpty(ediExpDesadvLineType.getProductDescription()))
		{
			final DPRDE1 prodDescr = DESADV_objectFactory.createDPRDE1();
			prodDescr.setDOCUMENTID(documentId);
			prodDescr.setLINENUMBER(lineNumber);
			prodDescr.setPRODUCTDESCQUAL(ProductDescQual.PROD.name());
			prodDescr.setPRODUCTDESCTEXT(ediExpDesadvLineType.getProductDescription());
			detail.getDPRDE1().add(prodDescr);
		}

		final DQUAN1 quantity = DESADV_objectFactory.createDQUAN1();
		quantity.setDOCUMENTID(documentId);
		quantity.setLINENUMBER(lineNumber);
		quantity.setQUANTITYQUAL(QuantityQual.DELV.name());
		BigDecimal qtyDelivered = ediExpDesadvLineType.getQtyDeliveredInUOM();
		if (qtyDelivered == null)
		{
			qtyDelivered = BigDecimal.ZERO;
		}
		quantity.setQUANTITY(formatNumber(qtyDelivered, decimalFormat));
		MeasurementUnit measurementUnit = MeasurementUnit.fromCUOM(ediExpDesadvLineType.getCUOMID().getX12DE355());
		if (measurementUnit != null)
		{
			quantity.setMEASUREMENTUNIT(measurementUnit.name());
		}
		detail.getDQUAN1().add(quantity);

		final DREFE1 detailRef = DESADV_objectFactory.createDREFE1();
		detailRef.setDOCUMENTID(documentId);
		detailRef.setLINENUMBER(lineNumber);
		detailRef.setREFERENCEQUAL(ReferenceQual.LIRN.name());
		// line number for now
		detailRef.setREFERENCELINE(lineNumber);
		detail.getDREFE1().add(detailRef);

		BigDecimal quantityDiff = ediExpDesadvLineType.getQtyEntered().subtract(qtyDelivered);
		if (quantityDiff.signum() != 0)
		{
			final DQVAR1 varianceQuantity = DESADV_objectFactory.createDQVAR1();
			varianceQuantity.setDOCUMENTID(documentId);
			varianceQuantity.setLINENUMBER(lineNumber);
			varianceQuantity.setQUANTITY(formatNumber(quantityDiff, decimalFormat));
			varianceQuantity.setDISCREPANCYCODE(getDiscrepancyCode(ediExpDesadvLineType.getIsSubsequentDeliveryPlanned(), quantityDiff).name());
			detail.setDQVAR1(varianceQuantity);
		}
		packDetail.getDETAIL().add(detail);
	}

	private void mapAddresses(final EDIExpDesadvType xmlDesadv, final HEADERXlief header, final String supplierGln, final String supplierAdditionalId)
	{
		final HADRE1 buyerAddress = DESADV_objectFactory.createHADRE1();
		buyerAddress.setDOCUMENTID(header.getDOCUMENTID());
		buyerAddress.setADDRESSQUAL(AddressQual.BUYR.name());
		buyerAddress.setPARTYIDGLN(xmlDesadv.getBillLocationID().getGLN());
		header.getHADRE1().add(buyerAddress);

		EDIExpCBPartnerLocationType deliveryLocation = xmlDesadv.getHandOverLocationID();
		if (deliveryLocation == null)
		{
			deliveryLocation = xmlDesadv.getCBPartnerLocationID();
		}

		final HADRE1 deliveryAddress = DESADV_objectFactory.createHADRE1();
		deliveryAddress.setDOCUMENTID(header.getDOCUMENTID());
		deliveryAddress.setADDRESSQUAL(AddressQual.DELV.name());
		deliveryAddress.setPARTYIDGLN(deliveryLocation.getGLN());
		header.getHADRE1().add(deliveryAddress);

		final HADRE1 supplierAddress = DESADV_objectFactory.createHADRE1();
		supplierAddress.setDOCUMENTID(header.getDOCUMENTID());
		supplierAddress.setADDRESSQUAL(AddressQual.SUPL.name());
		supplierAddress.setPARTYIDGLN(supplierGln);
		final HRFAD1 hrfad = DESADV_objectFactory.createHRFAD1();
		hrfad.setDOCUMENTID(header.getDOCUMENTID());
		hrfad.setADDRESSQUAL(AddressQual.SUPL.name());
		hrfad.setREFERENCEQUAL(ReferenceQual.APAI.name());
		hrfad.setREFERENCE(supplierAdditionalId);
		supplierAddress.setHRFAD1(hrfad);

		// address HCTAD1 contact not mapped for now

		header.getHADRE1().add(supplierAddress);

		if (xmlDesadv.getCBPartnerLocationID() != null)
		{
			HADRE1 ucAddress = DESADV_objectFactory.createHADRE1();
			ucAddress.setDOCUMENTID(header.getDOCUMENTID());
			ucAddress.setADDRESSQUAL(AddressQual.ULCO.name());
			ucAddress.setPARTYIDGLN(xmlDesadv.getCBPartnerLocationID().getGLN());
			header.getHADRE1().add(ucAddress);
		}

		// transport details HTRSD1 not mapped for now
	}

	private void mapReferences(final EDIExpDesadvType xmlDesadv, final String dateFormat, final HEADERXlief header)
	{
		final HREFE1 orderReference = DESADV_objectFactory.createHREFE1();
		orderReference.setDOCUMENTID(header.getDOCUMENTID());
		orderReference.setREFERENCEQUAL(ReferenceQual.ORBU.name());
		String reference;
		if (StringUtils.isNotEmpty(xmlDesadv.getPOReference()))
		{
			reference = xmlDesadv.getPOReference();
		}
		else
		{
			reference = xmlDesadv.getDocumentNo();
		}
		orderReference.setREFERENCE(reference);
		orderReference.setREFERENCEDATE1(toFormattedStringDate(toDate(xmlDesadv.getDateOrdered()), dateFormat));
		// ORIG same as ORBU for now
		final HREFE1 origReference = DESADV_objectFactory.createHREFE1();
		origReference.setDOCUMENTID(header.getDOCUMENTID());
		origReference.setREFERENCEQUAL(ReferenceQual.ORIG.name());
		origReference.setREFERENCE(reference);
		origReference.setREFERENCEDATE1(toFormattedStringDate(toDate(xmlDesadv.getDateOrdered()), dateFormat));
		header.getHREFE1().add(orderReference);
		header.getHREFE1().add(origReference);
	}

	private void mapDates(final EDIExpDesadvType xmlDesadv, final String dateFormat, final HEADERXlief header)
	{
		final HDATE1 messageDate = DESADV_objectFactory.createHDATE1();
		messageDate.setDOCUMENTID(header.getDOCUMENTID());
		messageDate.setDATEQUAL(DateQual.CREA.name());
		messageDate.setDATEFROM(toFormattedStringDate(SystemTime.asDate(), dateFormat));
		final HDATE1 deliveryDate = DESADV_objectFactory.createHDATE1();
		deliveryDate.setDOCUMENTID(header.getDOCUMENTID());
		deliveryDate.setDATEQUAL(DateQual.DELV.name());
		deliveryDate.setDATEFROM(toFormattedStringDate(toDate(xmlDesadv.getMovementDate()), dateFormat));

		header.getHDATE1().add(messageDate);
		header.getHDATE1().add(deliveryDate);
	}
}
