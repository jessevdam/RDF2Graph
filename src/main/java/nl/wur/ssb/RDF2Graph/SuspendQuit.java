package nl.wur.ssb.RDF2Graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

//Not used anymore
public class SuspendQuit implements Runnable
{
	private boolean isQuited = false;
	private Thread thread;
	SuspendQuit()
	{
		thread = new Thread(this);
		thread.start();
	}
	public void run()
	{
		try
		{
		  BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		  while(!this.isQuited)
		  {
		  	while(!br.ready())
		  	{
		  		if(this.isQuited)
		  			return;
		  		Thread.sleep(200);
		  	}
		    if(br.readLine().equals("q"))
		    {
		  	  this.isQuited = true;
		  	  System.out.println("will stop at next checkpoint");	
		    }
		    if(thread.isInterrupted())
		    {
		  	  this.isQuited = true;
		    }
		  }
		}
		catch(IOException ieo)
		{
			ieo.printStackTrace();
		}
		catch(InterruptedException e)
		{
		  this.isQuited = true;
		}
	}
	public void checkquit() throws QuitException
	{
		if(this.isQuited)
			throw new QuitException();
	}
	public void quit()
	{
	  this.isQuited = true;
	  this.thread.interrupt();
	  try
		{
			System.in.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
