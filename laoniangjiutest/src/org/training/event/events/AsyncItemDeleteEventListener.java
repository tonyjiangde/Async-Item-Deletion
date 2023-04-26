/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package org.training.event.events;

import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.OrderEntryModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.patches.utils.DbUtils;
import de.hybris.platform.servicelayer.event.impl.AbstractEventListener;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.type.TypeService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 *
 */
public class AsyncItemDeleteEventListener extends AbstractEventListener<AsyncItemDeleteEvent>
{

	@Autowired
	private ModelService modelService;

	@Autowired
	private FlexibleSearchService flexibleSearchService;

	@Autowired
	private TypeService typeService;

	private static final Logger LOG = LoggerFactory.getLogger(AsyncItemDeleteEventListener.class);


	@Override
	protected void onEvent(final AsyncItemDeleteEvent event)
	{
		LOG.info("Received event:" + event.getSource());
		final List<String> itemstodelete = (List<String>) event.getSource();
		String PKs = "";
		for (int i = 0; i < itemstodelete.size(); i++)
		{
			//LOG.info("Item needs to be deleted asynchronously:" + im.getItemtype() + "|" + im.getPk());
			LOG.info("Item needs to be deleted asynchronously:" + itemstodelete.get(i));
			if (i != 0)
			{
				PKs = PKs + "," + itemstodelete.get(i);
			}
			else
			{
				PKs = PKs + itemstodelete.get(i);
			}
		}
		try
		{
			//Thread.sleep(3000);
			final SearchResult<OrderEntryModel> result = flexibleSearchService
					.search("Select {PK} from {OrderEntry} where {PK} IN (" + PKs + ")");
			if (result.getCount() > 0)
			{
				modelService.removeAll(result.getResult());
			}
			final FlexibleSearchQuery query = new FlexibleSearchQuery("SELECT {PK} FROM {OrderEntry} Where {Order} IS NULL");
			final SearchResult<OrderEntryModel> orderentries = flexibleSearchService.search(query);
			LOG.info("Found also orderentries haaving referenced Order NULL:" + orderentries.getCount());
			if (orderentries.getCount() > 0)
			{
				//modelService.removeAll(orderentries.getResult());
				final ComposedTypeModel composedtype = typeService
						.getComposedType(modelService.getModelType(orderentries.getResult().get(0)));
				final String tablename = composedtype.getTable();
				final String column = typeService.getAttributeDescriptor(modelService.getModelType(orderentries.getResult().get(0)),
						AbstractOrderEntryModel.ORDER).getDatabaseColumn();
				String sqlquery = "DELETE FROM " + tablename + " WHERE PK IN (";
				for (final OrderEntryModel oe : orderentries.getResult())
				{
					sqlquery = sqlquery + oe.getPk() + ",";

				}
				sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
				LOG.info("SQL Query:" + sqlquery);
				LOG.info("Deleting:" + DbUtils.getJdbcTemplate().update(sqlquery));
			}
		}
		catch (final Exception e)
		{
			// XXX Auto-generated catch block
			e.printStackTrace();
		}


	}

}
