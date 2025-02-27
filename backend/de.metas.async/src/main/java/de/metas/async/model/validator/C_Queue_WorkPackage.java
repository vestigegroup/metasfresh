/**
 *
 */
package de.metas.async.model.validator;

import de.metas.async.AsyncBatchId;
import de.metas.async.QueueWorkPackageId;
import de.metas.async.api.IWorkpackageLogsRepository;
import de.metas.async.api.IWorkpackageParamDAO;
import de.metas.async.asyncbatchmilestone.AsyncBatchMilestone;
import de.metas.async.asyncbatchmilestone.AsyncBatchMilestoneQuery;
import de.metas.async.asyncbatchmilestone.AsyncBathMilestoneService;
import de.metas.async.model.I_C_Queue_Element;
import de.metas.async.model.I_C_Queue_WorkPackage;
import de.metas.logging.LogManager;
import de.metas.util.Services;
import lombok.NonNull;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.SpringContextHolder;
import org.compiere.model.ModelValidator;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author cg
 *
 */

@Interceptor(I_C_Queue_WorkPackage.class)
public class C_Queue_WorkPackage
{
	private static final Logger logger = LogManager.getLogger(C_Queue_WorkPackage.class);

	public static final C_Queue_WorkPackage INSTANCE = new C_Queue_WorkPackage();

	private C_Queue_WorkPackage()
	{
	}

	@ModelChange(timings = ModelValidator.TYPE_BEFORE_DELETE)
	public void deleteElements(final I_C_Queue_WorkPackage wp)
	{
		final int deleteCount = Services.get(IQueryBL.class).createQueryBuilder(I_C_Queue_Element.class, wp)
				.addEqualsFilter(I_C_Queue_Element.COLUMN_C_Queue_WorkPackage_ID, wp.getC_Queue_WorkPackage_ID())
				.create()
				.delete();
		logger.info("Deleted {} {} that referenced the to-be-deleted {} ", deleteCount, I_C_Queue_Element.Table_Name, wp);
	}

	@ModelChange(timings = ModelValidator.TYPE_BEFORE_DELETE)
	public void deleteParams(final I_C_Queue_WorkPackage wp)
	{
		final QueueWorkPackageId workpackageId = QueueWorkPackageId.ofRepoId(wp.getC_Queue_WorkPackage_ID());
		Services.get(IWorkpackageParamDAO.class).deleteWorkpackageParams(workpackageId);
	}

	@ModelChange(timings = ModelValidator.TYPE_BEFORE_DELETE)
	public void deleteLogs(final I_C_Queue_WorkPackage wp)
	{
		final IWorkpackageLogsRepository logsRepository = SpringContextHolder.instance.getBean(IWorkpackageLogsRepository.class);

		final QueueWorkPackageId workpackageId = QueueWorkPackageId.ofRepoId(wp.getC_Queue_WorkPackage_ID());
		logsRepository.deleteLogsInTrx(workpackageId);
	}

	@ModelChange(timings = ModelValidator.TYPE_AFTER_CHANGE, ifColumnsChanged = {
			I_C_Queue_WorkPackage.COLUMNNAME_Processed,
			I_C_Queue_WorkPackage.COLUMNNAME_IsError
	})
	public void processMilestoneFromWP(@NonNull final I_C_Queue_WorkPackage wp)
	{
		final AsyncBathMilestoneService asyncBathMilestoneService = SpringContextHolder.instance.getBean(AsyncBathMilestoneService.class);

		if (wp.getC_Async_Batch_ID() <= 0)
		{
			return;
		}

		final AsyncBatchId asyncBatchId = AsyncBatchId.ofRepoId(wp.getC_Async_Batch_ID());
		final String trxName = InterfaceWrapperHelper.getTrxName(wp);

		final AsyncBatchMilestoneQuery query = AsyncBatchMilestoneQuery.builder()
				.asyncBatchId(AsyncBatchId.ofRepoId(wp.getC_Async_Batch_ID()))
				.processed(false)
				.build();

		final List<AsyncBatchMilestone> milestones = asyncBathMilestoneService.getByQuery(query);

		if (milestones.size() > 0)
		{
			asyncBathMilestoneService.processAsyncBatchMilestone(asyncBatchId, milestones, trxName);
		}
	}
}
