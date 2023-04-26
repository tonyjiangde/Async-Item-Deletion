/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package org.training.setup;

import static org.training.constants.LaoniangjiutestConstants.PLATFORM_LOGO_CODE;

import de.hybris.platform.core.initialization.SystemSetup;

import java.io.InputStream;

import org.training.constants.LaoniangjiutestConstants;
import org.training.service.LaoniangjiutestService;


@SystemSetup(extension = LaoniangjiutestConstants.EXTENSIONNAME)
public class LaoniangjiutestSystemSetup
{
	private final LaoniangjiutestService laoniangjiutestService;

	public LaoniangjiutestSystemSetup(final LaoniangjiutestService laoniangjiutestService)
	{
		this.laoniangjiutestService = laoniangjiutestService;
	}

	@SystemSetup(process = SystemSetup.Process.INIT, type = SystemSetup.Type.ESSENTIAL)
	public void createEssentialData()
	{
		laoniangjiutestService.createLogo(PLATFORM_LOGO_CODE);
	}

	private InputStream getImageStream()
	{
		return LaoniangjiutestSystemSetup.class.getResourceAsStream("/laoniangjiutest/sap-hybris-platform.png");
	}
}
