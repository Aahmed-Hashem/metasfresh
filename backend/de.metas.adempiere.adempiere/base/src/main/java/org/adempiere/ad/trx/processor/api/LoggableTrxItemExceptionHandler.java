package org.adempiere.ad.trx.processor.api;

import org.adempiere.util.ILoggable;
import org.adempiere.util.Loggables;

/**
 * An {@link ITrxItemProcessorExecutor}'s exception handler which just logs the exception to {@link ILoggable} and does nothing else.
 *
 * @author tsa
 *
 */
public final class LoggableTrxItemExceptionHandler implements ITrxItemExceptionHandler
{
	public static final LoggableTrxItemExceptionHandler instance = new LoggableTrxItemExceptionHandler();

	private LoggableTrxItemExceptionHandler()
	{
		super();
	}

	@Override
	public void onNewChunkError(final Throwable e, final Object item)
	{
		Loggables.get().addLog("Error while trying to create a new chunk for item={}; exception={}", item, e);
	}

	@Override
	public void onItemError(final Throwable e, final Object item)
	{
		Loggables.get().addLog("Error while trying to process item={}; exception={}", item, e);
	}

	@Override
	public void onCompleteChunkError(final Throwable e)
	{
		Loggables.get().addLog("Error while completing current chunk; exception={}", e);
	}

	@Override
	public void onCommitChunkError(final Throwable e)
	{
		Loggables.get().addLog("Processor failed to commit current chunk => rollback transaction; exception={}", e);
	}

	@Override
	public void onCancelChunkError(final Throwable e)
	{
		Loggables.get().addLog("Error while cancelling current chunk. Ignored; exception={}", e);
	}
}
