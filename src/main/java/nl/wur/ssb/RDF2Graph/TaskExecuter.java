package nl.wur.ssb.RDF2Graph;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TaskExecuter
{
	private ExecutorService executor;
	private int maxThreadCount = 1;
	private Throwable error = null;
	private Object lock = new Object();
	private ArrayList<Future<Integer>> futures = new ArrayList<Future<Integer>>();
	private int taskCount = 0;
	public TaskExecuter(int maxThreadCount)
	{
		this.maxThreadCount = maxThreadCount;
		executor = Executors.newFixedThreadPool(this.maxThreadCount);
	}
	
	public void executeTask(Task task,Object ... params) throws Exception
	{
		if(this.maxThreadCount == 1)
		{
			task.run(params);
		  checkForErrors();
		}
		else
		{
		  checkForErrors();		  
		  synchronized(this.futures)
		  {
		  	TaskWorker worker = new TaskWorker(task,params,this,++taskCount);
		    futures.add(this.executor.submit(worker));
	  	}
		}
	}
	
	public void close()
	{
		this.executor.shutdown();
	}
	
	public void checkForErrors() throws Exception
	{
		synchronized(this.lock)
		{
			if(this.error != null)
			{
				System.out.println("SEVERE: error occured: " + this.error.getMessage());
			  try
			  {
				  throw new Exception("Exception occured",this.error);
			  }
			  catch(Exception e)
			  {
			  	e.printStackTrace(System.out);
			  }
			}
		}
	}
	
	public void reportError(Throwable error)
	{
		synchronized(this.lock)
		{
			if(this.error == null)
			{
			  this.error = error;
			}
		}
	}
	
	public void finishAllFirst() throws Exception
	{
		int lastTask = -1;
		boolean sleep = false;
		while(true)
		{
			if(sleep)
			{
				Thread.sleep(10000);
				sleep = false;
			}
			Future<Integer> future = null;
			synchronized(this.futures)
			{
				if(this.futures.size() == 0)
				{
					if(lastTask != -1 && lastTask != this.taskCount)
					{
						System.out.println("FAULT: features not waiting for tasks to finish");
            sleep = true;
						continue;
					}
					break;
				}
				future = this.futures.get(0);
				this.futures.remove(0);
			}
			try
			{
				int number = future.get();
				lastTask = Math.max(number,lastTask);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (ExecutionException e)
			{
				e.printStackTrace();
			}
		}
		checkForErrors();
	}
}
