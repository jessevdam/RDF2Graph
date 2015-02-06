package nl.wur.ssb.RDF2Graph;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.atlas.web.auth.SimpleAuthenticator;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;

public class GraphConfig
{
	private String server;
	private int port;
	private String finalLocation;
	private String graph;
	private SimpleAuthenticator authen;
	private boolean eachThreadSeperate = false;
	private int counter = 0;
	private HashMap<Long,Integer> threadMap = new HashMap<Long,Integer>();
	private int maxThreadCount = 1;
	public GraphConfig(String server,String graph)
	{
		Matcher matcher = Pattern.compile("http://(.+):([\\d]+)/(.*)").matcher(server);
		if(!matcher.matches())
		{
			matcher = Pattern.compile("http://(.+)/(.*)").matcher(server);
			matcher.matches();
			this.server = matcher.group(1);
			this.port = 80;
			this.finalLocation = matcher.group(2);
		}
		else
		{
			this.server = matcher.group(1);
			this.port = Integer.parseInt(matcher.group(2));
			this.finalLocation = matcher.group(3);
		}
		this.graph = graph;
	}
	
	public void enableEachThreadSeperatePort(int threadCount)
	{
		this.eachThreadSeperate = true;
		this.maxThreadCount = threadCount;
	}
	
  public void setAuthen(String user,String pass)
  {
  	authen = new SimpleAuthenticator(user,pass.toCharArray());
  }
	
	public QueryExecution createQuery(Main main,String queryFile,Object ... args)  throws Exception
	{
	  Object toPass[] = new Object[args.length + 1];
	  System.arraycopy(args,0,toPass,1,args.length);
	  toPass[0] = this.graph;
	  int port = this.port;
	  if(this.eachThreadSeperate)
	  {
	  	port = this.getThreadPortNum();
	  }
	  String server = "http://" + this.server + ":" + port + "/" + this.finalLocation;

		QueryExecution qe = QueryExecutionFactory.sparqlService(server,main.getQuery(queryFile,toPass),authen);
		qe.setTimeout(7,TimeUnit.DAYS);
		return qe;
	}
	
	private int getThreadPortNum() throws Exception
	{
		long threadId = Thread.currentThread().getId();
		if(this.threadMap.containsKey(threadId))
			return this.threadMap.get(threadId) + this.port;
		int newCount = this.counter++;
		if(newCount > this.maxThreadCount)
			throw new Exception("max thread count reached");
		this.threadMap.put(threadId,newCount);
		return newCount + this.port;
	}
	
	
}
