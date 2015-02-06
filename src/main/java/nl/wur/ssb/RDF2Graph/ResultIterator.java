package nl.wur.ssb.RDF2Graph;

import java.util.HashMap;
import java.util.Iterator;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class ResultIterator implements Iterator<HashMap<String,RDFNode>>
{
	private ResultSet resultSet;

	public ResultIterator(ResultSet resultSet)
	{
		this.resultSet = resultSet;
	}

	@Override
	public boolean hasNext()
	{
		return resultSet.hasNext();
	}

	@Override
	public HashMap<String,RDFNode> next()
	{
		QuerySolution sol = resultSet.next();
		HashMap<String,RDFNode> map = new HashMap<String,RDFNode>();
		for(String name : new Iteration<String>(sol.varNames()))
		{
			map.put(name,sol.get(name));
		}
		return map;
	}

	@Override
	public void remove()
	{

	}

}
