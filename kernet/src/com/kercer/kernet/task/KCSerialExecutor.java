package com.kercer.kernet.task;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class KCSerialExecutor implements Executor
{

	private final Queue<Runnable> mQueue = new LinkedBlockingQueue<Runnable>();
	private Runnable mActive;

	public synchronized void execute(final Runnable r)
	{
		mQueue.offer(new Runnable()
		{
			public void run()
			{
				try
				{
					r.run();
				}
				finally
				{
					scheduleNext();
				}
			}
		});
		if (mActive == null)
		{
			scheduleNext();
		}
	}

	protected synchronized void scheduleNext()
	{
		if ((mActive = mQueue.poll()) != null)
		{
			KCTaskExecutor.executeTask(mActive);
		}
	}

}
