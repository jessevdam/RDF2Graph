package nl.wur.ssb.RDF2Graph;

import java.util.concurrent.Callable;

public class TaskWorker implements Callable<Integer>
{
	private TaskExecuter executer;
	private Task task;
	private Object[] args;
	private int taskNumber;
	public TaskWorker(Task task,Object[] args,TaskExecuter executer,int taskNumber)
	{
		this.task = task;
		this.args = args;
		this.executer = executer;
		this.taskNumber = taskNumber;
	}
	public Integer call() 
	{
		try
		{
		  this.task.run(args);
		}
		catch(Throwable th)
		{
			th.printStackTrace();
			executer.reportError(th);
		}
		return this.taskNumber;
	}
}
