/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package org.training.event.events;

import de.hybris.platform.servicelayer.event.ClusterAwareEvent;
import de.hybris.platform.servicelayer.event.events.AbstractEvent;

import java.io.Serializable;
import java.util.List;


/**
 *
 */
public class AsyncItemDeleteEvent extends AbstractEvent implements ClusterAwareEvent
{
	List<String> source;

	public AsyncItemDeleteEvent(final List<String> source)
	{
		super((Serializable) source);
		this.source = source;
	}

	@Override
	public boolean publish(final int sourceNodeId, final int targetNodeId)
	{
		return true;
	}

}
