/*
 * #%L
 * de.metas.externalsystem
 * %%
 * Copyright (C) 2021 metas GmbH
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

package de.metas.externalsystem.woocommerce.interceptor;

import de.metas.externalsystem.ExternalSystemConfigRepo;
import de.metas.externalsystem.ExternalSystemParentConfigId;
import de.metas.externalsystem.ExternalSystemType;
import de.metas.externalsystem.model.I_ExternalSystem_Config_WooCommerce;
import lombok.NonNull;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.ModelValidator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Interceptor(I_ExternalSystem_Config_WooCommerce.class)
@Component
public class ExternalSystem_Config_WooCommerce
{
	public final ExternalSystemConfigRepo externalSystemConfigRepo;

	public ExternalSystem_Config_WooCommerce(@NonNull final ExternalSystemConfigRepo externalSystemConfigRepo)
	{
		this.externalSystemConfigRepo = externalSystemConfigRepo;
	}

	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_NEW, ModelValidator.TYPE_BEFORE_CHANGE },
			ifColumnsChanged = I_ExternalSystem_Config_WooCommerce.COLUMNNAME_ExternalSystem_Config_ID)
	public void checkType(final I_ExternalSystem_Config_WooCommerce woocommerceConfig)
	{
		final String parentType =
				externalSystemConfigRepo.getParentTypeById(ExternalSystemParentConfigId.ofRepoId(woocommerceConfig.getExternalSystem_Config_ID()));

		if (!ExternalSystemType.WOO.getCode().equals(parentType))
		{
			throw new AdempiereException("Invalid external system type!");
		}
	}

	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_NEW, ModelValidator.TYPE_BEFORE_CHANGE })
	public void generateAndSetUUID(final I_ExternalSystem_Config_WooCommerce woocommerceConfig)
	{
		if (woocommerceConfig.getCamelHttpResourceAuthKey() == null)
		{
			woocommerceConfig.setCamelHttpResourceAuthKey(UUID.randomUUID().toString());
		}
	}
}
