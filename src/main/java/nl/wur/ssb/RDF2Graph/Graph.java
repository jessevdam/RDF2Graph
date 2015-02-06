package nl.wur.ssb.RDF2Graph;

import com.hp.hpl.jena.rdf.model.Model;

public class Graph
{
	public Model model;
	public Graph(Model model)
	{
		this.model = model;
	}
	public String expand(String in)
	{
		return model.expandPrefix(in);
	}
	public void add(String subj,String pred,String obj)
	{
		synchronized(this)
		{
		  this.model.add(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)),this.model.createResource(expand(obj)));
		}
	}
	public void addLit(String subj,String pred,String obj)
	{
		synchronized(this)
		{
  		this.model.add(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)),this.model.createTypedLiteral(obj));
		}
	}
	public void add(String subj,String pred,int val)
	{
		synchronized(this)
		{
		  this.model.add(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)),this.model.createTypedLiteral(val));
	  }
	}
	public void add(String subj,String pred,boolean val)
	{
		synchronized(this)
		{
		  this.model.add(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)),this.model.createTypedLiteral(val));
	  }	
	}
	public void add(String subj,String pred,float val)
	{
		synchronized(this)
		{
		  this.model.add(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)),this.model.createTypedLiteral(val));
 	  }
	}
	public void add(String subj,String pred,double val)
	{
		synchronized(this)
		{
		  this.model.add(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)),this.model.createTypedLiteral(val));
  	}	
	}
	public boolean contains(String subj,String pred)
	{
		synchronized(this)
		{
		  return this.model.contains(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)));
	  }
	}
	public boolean containsLit(String subj,String pred,String lit)
	{
		synchronized(this)
		{
		  return this.model.contains(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)),this.model.createTypedLiteral(lit));
	  }
	}
	public boolean containsLit(String subj,String pred,int lit)
	{
		synchronized(this)
		{
		  return this.model.contains(this.model.createResource(expand(subj)),this.model.createProperty(expand(pred)),this.model.createTypedLiteral(lit));
		}
	}
	public void close()
	{
		this.model.close();
	}
}
