package nl.wur.ssb.RDF2Graph.owlExporter;

import java.util.HashSet;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.tdb.TDBFactory;

public class Main
{
	private Dataset dataset;
	private Model model;
	public static void main(String args[])
	{
		new Main(args);
	}
	public Main(String args[])
	{
		String project = args[0];
		dataset = TDBFactory.createDataset(project);
		model = dataset.getNamedModel("http://ssb.wur.nl/owlExporter");
		Property makeAnonPred = model.createProperty("http://ssb.wur.nl/RDF2Graph/makeAnon");
		//Convert all nodes marked with makeAnon to anonymous nodes
		for(Statement makeAnon : model.listStatements(null,makeAnonPred,(RDFNode)null).toList())
		{
			Resource newAnonObj = model.createResource();
			for(Statement targets : model.listStatements(makeAnon.getSubject(),null,(RDFNode)null).toList())
			{
				model.remove(targets);
			  if(!targets.getPredicate().equals(makeAnonPred))
			  {
			  	model.add(newAnonObj,targets.getPredicate(),targets.getObject());
			  }
			}
			for(Statement sources : model.listStatements(null,null,(RDFNode)makeAnon.getSubject()).toList())
			{
				model.remove(sources);
		  	model.add(sources.getSubject(),sources.getPredicate(),newAnonObj);
			}
		}	
		makeRdfList("http://www.w3.org/2002/07/owl#unionOf");
		makeRdfList("http://www.w3.org/2002/07/owl#intersectionOf");
		dataset.close();		
	}
	private void makeRdfList(String predIRI)
	{
		Property pred = model.createProperty(predIRI);
		Property first = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
		Property rest = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");
		HashSet<Resource> subjects = new HashSet<Resource>();
	  for(Statement buildUnionList1 : model.listStatements(null,pred,(RDFNode)null).toList())
	  {
	  	subjects.add(buildUnionList1.getSubject());
	  }
	  for(Resource subject : subjects)
	  {
     	HashSet<RDFNode> items = new HashSet<RDFNode>();
		  for(Statement buildUnionList2 : model.listStatements(subject,pred,(RDFNode)null).toList())
		  {
			  items.add(buildUnionList2.getObject());
			  model.remove(buildUnionList2);
		  }
	    Resource lastNode = model.createResource();
	    model.add(subject,pred,lastNode);
	    int count = 0;
	    for(RDFNode item : items)
	    {
	    	if(count++ != 0)
	    	{
	    	  Resource temp = model.createResource();
	    	  model.add(lastNode,rest,temp);
	    	  lastNode = temp;
	    	}
	    	model.add(lastNode,first,item);
	    }
	    model.add(lastNode,rest,model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"));
		}
	}
}
