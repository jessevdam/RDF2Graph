package nl.wur.ssb.RDF2Graph;

public class StatusSystem
{
	private Graph graph;
	public StatusSystem(Graph graph)
	{
		this.graph = graph;
	}
	public void init()
	{
		this.graph.add("RDF2Graph:status","rdf:type","RDF2Graph:Status");
	}
	
	public boolean stepDone(String step)
	{
		return this.graph.containsLit("RDF2Graph:status","RDF2Graph:stepDone",step);
	}
	public void setStepDone(String step)
	{
		this.graph.addLit("RDF2Graph:status","RDF2Graph:stepDone",step);
	}
	public boolean stepDone(String clazz,String step)
	{
		return this.graph.containsLit(clazz,"RDF2Graph:stepDone",step);
	}
	public void setStepDone(String clazz,String step)
	{
		this.graph.addLit(clazz,"RDF2Graph:stepDone",step);
	}
	
	public void setPredAsClassDetected()
	{
		this.graph.add("RDF2Graph:status","RDF2Graph:predicateAsClassDetected",true);
	}
	
	public boolean hasPredAsClassDetected()
	{
		return this.graph.contains("RDF2Graph:status","RDF2Graph:predicateAsClassDetected");
	}
}
