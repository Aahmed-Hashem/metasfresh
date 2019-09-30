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

package de.metas.edi.esb.route.exports;

import java.text.DecimalFormat;

import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spi.DataFormat;
import org.springframework.stereotype.Component;

import de.metas.edi.esb.bean.desadv.StepComXMLDesadvBean;
import de.metas.edi.esb.commons.Constants;
import de.metas.edi.esb.commons.Util;
import de.metas.edi.esb.jaxb.metasfresh.EDIDesadvFeedbackType;
import de.metas.edi.esb.jaxb.metasfresh.EDIExpDesadvType;
import de.metas.edi.esb.jaxb.stepcom.desadv.ObjectFactory;
import de.metas.edi.esb.processor.feedback.EDIXmlSuccessFeedbackProcessor;
import de.metas.edi.esb.processor.feedback.helper.EDIXmlFeedbackHelper;
import de.metas.edi.esb.route.AbstractEDIRoute;

@Component
public class StepComXMLDesadvRoute extends AbstractEDIRoute
{
	private static final String ROUTE_ID_AGGREGATE = "XML-InOut-To-XML-EDI-DESADV";

	private static final String EDI_DESADV_XML_FILENAME_PATTERN = "edi.file.desadv.stepcom-xml.filename";

	public static final String EP_EDI_STEPCOM_XML_DESADV_CONSUMER = "direct:edi.xml.desadv.consumer";

	public static final String EDI_XML_DESADV_IS_TEST = "edi.props.desadv.stepcom-xml.isTest";

	public static final String EDI_XML_OWNER_ID = "edi.props.stepcom.owner.id";
	public static final String EDI_XML_APPLICATION_REF = "edi.props.stepcom.application.ref";

	public static final String EDI_XML_SUPPLIER_GLN = "edi.props.desadv.stepcom.supplier.gln";

	private final static QName EDIDesadvFeedback_QNAME = Constants.JAXB_ObjectFactory.createEDIDesadvFeedback(null).getName();

	private static final String METHOD_setEDIDesadvID = "setEDIDesadvID";

	private static final String EP_EDI_FILE_DESADV_XML = "{{edi.file.desadv.stepcom-xml}}";

	private static final String JAXB_DESADV_CONTEXTPATH = ObjectFactory.class.getPackage().getName();

	@Override
	public void configureEDIRoute(final DataFormat jaxb, final DecimalFormat decimalFormat)
	{
		final String charset = Util.resolvePropertyPlaceholders(getContext(), AbstractEDIRoute.EDI_STEPCOM_CHARSET_NAME);

		final JaxbDataFormat dataFormat = new JaxbDataFormat(JAXB_DESADV_CONTEXTPATH);
		dataFormat.setCamelContext(getContext());
		dataFormat.setEncoding(charset);

		// FRESH-360: provide our own converter, so we don't anymore need to rely on the system's default charset when writing the EDI data to file.
		final ReaderTypeConverter readerTypeConverter = new ReaderTypeConverter();
		getContext().getTypeConverterRegistry().addTypeConverters(readerTypeConverter);

		final String desadvFilenamePattern = Util.resolvePropertyPlaceholders(getContext(), StepComXMLDesadvRoute.EDI_DESADV_XML_FILENAME_PATTERN);

		final String isTest = Util.resolvePropertyPlaceholders(getContext(), StepComXMLDesadvRoute.EDI_XML_DESADV_IS_TEST);
		final String ownerId = Util.resolvePropertyPlaceholders(getContext(), StepComXMLDesadvRoute.EDI_XML_OWNER_ID);
		final String applicationRef = Util.resolvePropertyPlaceholders(getContext(), StepComXMLDesadvRoute.EDI_XML_APPLICATION_REF);
		final String supplierGln = Util.resolvePropertyPlaceholders(getContext(), StepComXMLDesadvRoute.EDI_XML_SUPPLIER_GLN);

		final String defaultEDIMessageDatePattern = Util.resolvePropertyPlaceholders(getContext(), StepComXMLDesadvRoute.EDI_ORDER_EDIMessageDatePattern);
		final String feedbackMessageRoutingKey = Util.resolvePropertyPlaceholders(getContext(), Constants.EP_AMQP_TO_AD_DURABLE_ROUTING_KEY);

		from(StepComXMLDesadvRoute.EP_EDI_STEPCOM_XML_DESADV_CONSUMER)
				.routeId(ROUTE_ID_AGGREGATE)

				.log(LoggingLevel.INFO, "EDI: Setting defaults as exchange properties...")
				.setProperty(StepComXMLDesadvRoute.EDI_XML_DESADV_IS_TEST).constant(isTest)
				.setProperty(StepComXMLDesadvRoute.EDI_XML_OWNER_ID).constant(ownerId)
				.setProperty(StepComXMLDesadvRoute.EDI_XML_APPLICATION_REF).constant(applicationRef)
				.setProperty(StepComXMLDesadvRoute.EDI_XML_SUPPLIER_GLN).constant(supplierGln)
				.setProperty(StepComXMLDesadvRoute.EDI_ORDER_EDIMessageDatePattern).constant(defaultEDIMessageDatePattern)

				.log(LoggingLevel.INFO, "EDI: Setting EDI feedback headers...")
				.process(exchange -> {
					// i'm sure that there are better ways, but we want the EDIFeedbackRoute to identify that the error is coming from *this* route.
					exchange.getIn().setHeader(EDIXmlFeedbackHelper.HEADER_ROUTE_ID, ROUTE_ID_AGGREGATE);

					final EDIExpDesadvType xmlDesadv = exchange.getIn().getBody(EDIExpDesadvType.class); // throw exceptions if mandatory fields are missing

					exchange.getIn().setHeader(EDIXmlFeedbackHelper.HEADER_ADClientValueAttr, xmlDesadv.getADClientValueAttr());
					exchange.getIn().setHeader(EDIXmlFeedbackHelper.HEADER_RecordID, xmlDesadv.getEDIDesadvID().longValue());
				})

				.log(LoggingLevel.INFO, "EDI: Converting metasfresh-XML Java Object -> stepCOM-XML Java Object...")
				.bean(StepComXMLDesadvBean.class, StepComXMLDesadvBean.METHOD_createXMLEDIData)
				.log(LoggingLevel.INFO, "EDI: Marshalling stepCOM-XML Java Object -> XML...")
				.marshal(dataFormat)

				.log(LoggingLevel.INFO, "EDI: Setting output filename pattern from properties...")
				.setHeader(Exchange.FILE_NAME).simple(desadvFilenamePattern)

				.log(LoggingLevel.INFO, "EDI: Sending the stepCOM-XML to the FILE component...")
				.to(StepComXMLDesadvRoute.EP_EDI_FILE_DESADV_XML)

				.log(LoggingLevel.INFO, "EDI: Creating metasfresh feedback XML Java Object...")
				.process(new EDIXmlSuccessFeedbackProcessor<>(EDIDesadvFeedbackType.class, StepComXMLDesadvRoute.EDIDesadvFeedback_QNAME, StepComXMLDesadvRoute.METHOD_setEDIDesadvID))

				.log(LoggingLevel.INFO, "EDI: Marshalling metasfresh feedback XML Java Object -> XML...")
				.marshal(jaxb)

				.log(LoggingLevel.INFO, "EDI: Sending success response to metasfresh...")
				.setHeader("rabbitmq.ROUTING_KEY").simple(feedbackMessageRoutingKey) // https://github.com/apache/camel/blob/master/components/camel-rabbitmq/src/main/docs/rabbitmq-component.adoc
				.to(Constants.EP_AMQP_TO_AD);
	}
}
