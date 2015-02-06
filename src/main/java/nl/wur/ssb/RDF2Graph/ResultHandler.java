package nl.wur.ssb.RDF2Graph;

import java.util.HashMap;

import com.hp.hpl.jena.rdf.model.RDFNode;

public interface ResultHandler
{
	public void handleResult(HashMap<String, RDFNode> item,int count) throws Exception;
}
