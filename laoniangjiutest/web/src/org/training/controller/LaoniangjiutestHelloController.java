/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package org.training.controller;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.OrderEntryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.product.UnitModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.order.OrderService;
import de.hybris.platform.patches.utils.DbUtils;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.product.UnitService;
import de.hybris.platform.servicelayer.event.EventService;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.session.SessionExecutionBody;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.type.TypeService;
import de.hybris.platform.servicelayer.user.UserService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.training.event.events.AsyncItemDeleteEvent;

import com.microsoft.sqlserver.jdbc.StringUtils;


@Controller
public class LaoniangjiutestHelloController
{
	private static final Logger LOG = LoggerFactory.getLogger(LaoniangjiutestHelloController.class);
	@Autowired
	private FlexibleSearchService flexibleSearchService;
	@Autowired
	private UnitService unitService;
	@Autowired
	private UserService userService;
	@Autowired
	private CommonI18NService commonI18NService;
	@Autowired
	private ModelService modelService;
	@Autowired
	private CatalogVersionService catalogVersionService;
	@Autowired
	private OrderService orderService;
	@Autowired
	private CalculationService calculationService;
	@Autowired
	private ProductService productService;
	@Autowired
	private SessionService sessionService;
	@Autowired
	private EventService eventService;
	@Autowired
	private TypeService typeService;



	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String printWelcome(final ModelMap model)
	{
		//model.addAttribute("logoUrl", laoniangjiutestService.getHybrisLogoUrl(PLATFORM_LOGO_CODE));
		return "welcome";
	}

	@RequestMapping(value = "/testOrder", params =
	{ "code", "async" }, method = RequestMethod.GET)
	public @ResponseBody String testOrder(@RequestParam(value = "code")
	final String code, @RequestParam(value = "async")
	final Boolean async, final ModelMap model)
	{

		if (!StringUtils.isEmpty(code))
		{
			final CatalogVersionModel cv = catalogVersionService.getCatalogVersion("electronicsProductCatalog", "Online");

			final String[] productcodes =
			{ "149243", "300938", "65652", "280916", "266899", "301233" };

			final UserModel adminuserModel = userService.getAdminUser();
			final List<ProductModel> products = sessionService.executeInLocalView(new SessionExecutionBody()
			{
				@Override
				public Object execute()
				{
					final List<ProductModel> products = new ArrayList<>();

					try
					{
						for (final String s : productcodes)
						{
							final ProductModel product = productService.getProductForCode(cv, s);
							LOG.info("found product:" + product.getCode());
							products.add(product);
						}


					}
					catch (final Exception e2)
					{
						e2.printStackTrace();
					}
					return products;
				}
			}, adminuserModel);
			//for (final String s : productcodes)
			//{

			//}

			final UnitModel unit = unitService.getUnitForCode("pieces");
			final UserModel user = userService.getUserForUID("women@hybris.com");
			final CurrencyModel curr = commonI18NService.getCurrency("USD");

			final FlexibleSearchQuery query = new FlexibleSearchQuery("SELECT {PK} FROM {Order} Where {Code}=?code");
			query.addQueryParameter("code", code);
			LOG.info(query.getQuery());
			try
			{
				final OrderModel testOrder = flexibleSearchService.searchUnique(query);
				LOG.info("Found order with code:" + code + " created at " + testOrder.getCreationtime());
				LOG.info("Order entry size:" + testOrder.getEntries().size());
				LOG.info("Order totalprice:" + testOrder.getTotalPrice());
				final StopWatch stopWatch = new StopWatch();
				stopWatch.start("Catering Order Update");
				for (int j = 0; j < 1; j++)
				{
					LOG.info("Update round:" + j);
					testOrder.setDate(new Date());
					final double otp = 1000000.0;
					testOrder.setTotalPrice(otp);
					modelService.save(testOrder);
					modelService.refresh(testOrder);
					final List<AbstractOrderEntryModel> neworderEntries = new ArrayList<>();
					for (var i = 0; i < 20; i++)
					{
						final OrderEntryModel entryModel = modelService.create(OrderEntryModel.class);
						entryModel.setProduct(products.get(i % 6));
						final double oetp = 10000.0;
						entryModel.setTotalPrice(oetp);
						entryModel.setQuantity(5L);
						final double oebp = 2000.0;
						entryModel.setBasePrice(oebp);
						entryModel.setOrder(testOrder);
						entryModel.setUnit(unit);
						neworderEntries.add(entryModel);
					}

					final OrderEntryModel exampel = new OrderEntryModel();
					exampel.setOrder(testOrder);
					final List<OrderEntryModel> modelsByExample = flexibleSearchService.getModelsByExample(exampel);
					LOG.info("modelsByExample size: " + modelsByExample.size());
					if (async)
					{
						final List<String> l = new ArrayList();
						for (final OrderEntryModel oem : modelsByExample)
						{
							l.add(oem.getPk().toString());
						}
						final ComposedTypeModel composedtype = typeService
								.getComposedType(modelService.getModelType(modelsByExample.get(0)));
						final String tablename = composedtype.getTable();
						final String column = typeService
								.getAttributeDescriptor(modelService.getModelType(modelsByExample.get(0)), AbstractOrderEntryModel.ORDER)
								.getDatabaseColumn();
						String sqlquery = "UPDATE " + tablename + " SET " + column + "=NULL WHERE PK IN (";
						for (final OrderEntryModel oe : modelsByExample)
						{
							sqlquery = sqlquery + oe.getPk() + ",";

						}
						sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
						LOG.info("SQL Query:" + sqlquery);
						LOG.info("Updating:" + DbUtils.getJdbcTemplate().update(sqlquery));
						//modelService.refresh(modelsByExample);
						//final List<ItemModel> l = new ArrayList();
						//l.addAll(modelsByExample);
						final AsyncItemDeleteEvent ade = new AsyncItemDeleteEvent(l);
						eventService.publishEvent(ade);

					}
					else
					{
						Thread.sleep(3000);
						modelService.removeAll(modelsByExample);
						//modelService.refresh(testOrder);
					}

					modelService.saveAll(neworderEntries);


				}
				modelService.refresh(testOrder);
				stopWatch.stop();
				LOG.info(stopWatch.prettyPrint());
				return "Order:" + code + "has been updated 100 times hand having now entry size:" + testOrder.getEntries().size()
						+ " and total time spent is:" + stopWatch.getTotalTimeSeconds();

			}
			catch (final Exception e)
			{
				e.printStackTrace();
				LOG.info("can not found order with code:" + code + " due to" + e.getMessage() + ", a new one will be created");

				final OrderModel testOrder = modelService.create(OrderModel.class);
				testOrder.setCode(code);
				testOrder.setUser(user);
				testOrder.setCurrency(curr);
				testOrder.setDate(new Date());
				testOrder.setNet(Boolean.FALSE);
				modelService.save(testOrder);


				final List<AbstractOrderEntryModel> orderEntries = new ArrayList();
				for (final ProductModel p : products)
				{
					final AbstractOrderEntryModel newOrderEntry1 = orderService.addNewEntry(testOrder, p, 1, unit);
					orderEntries.add(newOrderEntry1);
					LOG.info(newOrderEntry1.toString());
					LOG.info("EntryNumder:" + newOrderEntry1.getEntryNumber());
				}
				sessionService.executeInLocalView(new SessionExecutionBody()
				{
					@Override
					public void executeWithoutResult()
					{

						try
						{
							modelService.save(testOrder);
							modelService.saveAll(orderEntries);
							calculationService.calculate(testOrder);

						}
						catch (final Exception e2)
						{
							e2.printStackTrace();
						}

					}
				}, adminuserModel);
				/*
				 * try { modelService.save(testOrder); modelService.saveAll(orderEntries);
				 * calculationService.calculate(testOrder); } catch (final CalculationException ce) { // XXX Auto-generated
				 * catch block ce.printStackTrace(); }
				 */




				LOG.info("Order entry size:" + testOrder.getEntries().size());
				LOG.info("Order totalprice:" + testOrder.getTotalPrice());
				final StopWatch stopWatch = new StopWatch();
				stopWatch.start("Catering Order Update");
				for (int j = 0; j < 1; j++)
				{
					LOG.info("Update round:" + j);
					testOrder.setDate(new Date());
					final double otp = 1000000.0;
					testOrder.setTotalPrice(otp);
					modelService.save(testOrder);
					modelService.refresh(testOrder);
					final List<AbstractOrderEntryModel> neworderEntries = new ArrayList<>();
					for (var i = 0; i < 20; i++)
					{
						final OrderEntryModel entryModel = modelService.create(OrderEntryModel.class);
						entryModel.setProduct(products.get(i % 6));
						final double oetp = 10000.0;
						entryModel.setTotalPrice(oetp);
						entryModel.setQuantity(5L);
						final double oebp = 2000.0;
						entryModel.setBasePrice(oebp);
						entryModel.setOrder(testOrder);
						entryModel.setUnit(unit);
						neworderEntries.add(entryModel);
					}

					final OrderEntryModel exampel = new OrderEntryModel();
					exampel.setOrder(testOrder);
					final List<OrderEntryModel> modelsByExample = flexibleSearchService.getModelsByExample(exampel);
					LOG.info("modelsByExample size: " + modelsByExample.size());
					if (async)
					{
						final List<String> l = new ArrayList();
						for (final OrderEntryModel oem : modelsByExample)
						{
							l.add(oem.getPk().toString());
						}

						final ComposedTypeModel composedtype = typeService
								.getComposedType(modelService.getModelType(modelsByExample.get(0)));
						final String tablename = composedtype.getTable();
						final String column = typeService
								.getAttributeDescriptor(modelService.getModelType(modelsByExample.get(0)), AbstractOrderEntryModel.ORDER)
								.getDatabaseColumn();
						String sqlquery = "UPDATE " + tablename + " SET " + column + "=NULL WHERE PK IN (";
						for (final OrderEntryModel oe : modelsByExample)
						{
							sqlquery = sqlquery + oe.getPk() + ",";

						}
						sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
						LOG.info("SQL Query:" + sqlquery);
						DbUtils.getJdbcTemplate().update(sqlquery);
						//modelService.refresh(modelsByExample);

						final AsyncItemDeleteEvent ade = new AsyncItemDeleteEvent(l);
						eventService.publishEvent(ade);

					}
					else
					{
						try
						{
							Thread.sleep(3000);
							modelService.removeAll(modelsByExample);
						}
						catch (final Exception e1)
						{
							// XXX Auto-generated catch block
							e1.printStackTrace();
						}

						//modelService.refresh(testOrder);
					}
					modelService.saveAll(neworderEntries);


				}
				modelService.refresh(testOrder);
				stopWatch.stop();
				LOG.info(stopWatch.prettyPrint());
				return "Order:" + code + " has been created and updated 100 times hand having now entry size:"
						+ testOrder.getEntries().size() + " and total time spent is:" + stopWatch.getTotalTimeSeconds();

			}



		}
		else
		{
			return "Code is empty!";
		}

	}
}
