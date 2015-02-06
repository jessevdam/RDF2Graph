package nl.wur.ssb.RDF2Graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.wur.ssb.RDF2Graph.simplify.ShapeProperty;
import nl.wur.ssb.RDF2Graph.simplify.Tree;
import nl.wur.ssb.RDF2Graph.simplify.TreeNode;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.jena.riot.web.HttpOp;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.update.UpdateAction;


/*
 * Please see doc manual.pdf for full description of the process
 */

public class Main
{
	private HashSet<String> excludeProps = new HashSet<String>();
	private StatusSystem status;
	private Dataset dataset;
  private	GraphConfig remoteGraph1;
  private	GraphConfig remoteGraph2;
	private Graph graph;
	private String project;
	private TaskExecuter executer;
	private boolean collectPredicateStatistics = false;
	private boolean collectClassStatistics = false;
	private boolean collectShapePropertyStatistics = false;
	private boolean checkPredicateSourceClass = false;
	private boolean collectForwardMultiplicity = false;
	private boolean collectReverseMultiplicity = false;
	private boolean executeSimplify = false; 
	private boolean treatSubClassOfAsIntanceOf = false;
	private boolean keepOWLClasses = false;
	private boolean eachThreadOnePort = false;
	private boolean useClassPredFindAtOnce = false;
	private int numberOfThreads = 1;
	private HashMap<String,LinkedList<String>> keepUniq = new HashMap<String,LinkedList<String>>(); 

	private static GraphConfig processGraphConfig(String arg)
	{
		try
		{
		  String username = null;
		  String pass = null;
		  String graph = "";
	    if(arg.indexOf("@") != -1)
	    {
	    	String temp[] = arg.split("@");
	    	String temp2[] = temp[0].split(":");
	  	  if(temp2.length != 2)
	  		  return null;
	  	  username = temp2[0];
	  	  pass = temp2[1];
	  	  arg = temp[1];
	    }
	    if(arg.indexOf("[") != -1)
	    {
	  	  Matcher temp = Pattern.compile("(.*)\\[(.*)\\]").matcher(arg);
	  	  if(temp.matches() == false)
	  	  	return null;
	  	  arg = temp.group(1);
	  	  graph = temp.group(2);
	    }
	    String server = arg;
	    GraphConfig toRet = new GraphConfig(server,graph);
	    if(username != null)
	    	toRet.setAuthen(username,pass);
	    return toRet;
		}
		catch(Throwable th)
		{
			th.printStackTrace();
			return null;
		}
	  
	}
	
	public static void main(String args[])
	{
		Main main = new Main();

		boolean ok = true;
  		
		int rest = 2;
		if(args.length >= 2)
		{
			main.project = args[0];
			main.remoteGraph1 = main.remoteGraph2 = processGraphConfig(args[1]);
			if(args.length >= 3 && args[2].equals("+"))
			{
				main.remoteGraph2 = processGraphConfig(args[3]);
				rest = 4;
			}
		}
		else
		{
			ok = false;
		}
    for(int i = rest;i < args.length;i++)
    { 
    	if(args[i].startsWith("--"))
    	{
    	  String enProp = args[i].substring(2);
    	  if(enProp.equals("all"))
    	  {
    	  	main.collectPredicateStatistics = true;
    	  	main.collectClassStatistics = true;
    	  	main.checkPredicateSourceClass = true;
    	  	main.collectShapePropertyStatistics = true;
    	  	main.collectForwardMultiplicity = true;
    	  	main.collectReverseMultiplicity = true;
    	  }
    	  else if(enProp.equals("collectPredicateStatistics"))
      		main.collectPredicateStatistics = true;
      	else if(enProp.equals("collectClassStatistics"))
      		main.collectClassStatistics = true;
      	else if(enProp.equals("collectShapePropertyStatistics"))//TODO Rename
      		main.collectShapePropertyStatistics = true;
      	else if(enProp.equals("checkPredicateSourceClass"))
      		main.checkPredicateSourceClass = true;
      	else if(enProp.equals("collectForwardMultiplicity"))
      		main.collectForwardMultiplicity = true;
      	else if(enProp.equals("collectReverseMultiplicity"))
      		main.collectReverseMultiplicity = true;
      	else if(enProp.equals("executeSimplify"))
      		main.executeSimplify = true;
      	else if(enProp.equals("treatSubClassOfAsIntanceOf"))
      		main.treatSubClassOfAsIntanceOf = true;      
      	else if(enProp.equals("keepOWLClasses"))
      		main.keepOWLClasses = true;
      	else if(enProp.equals("eachThreadOnePort"))
      		main.eachThreadOnePort = true;    	  
      	else if(enProp.equals("useClassPredFindAtOnce"))
      		main.useClassPredFindAtOnce = true;
      	else if(enProp.equals("multiThread"))
      	{
      		try
      		{
      	  	main.numberOfThreads = Integer.parseInt(args[++i]);
      		}
      		catch(Throwable th)
      		{
      			ok = false;
      		}
      	}
      	else
      	{
      		System.out.println("unknown option: --" + enProp);
      		ok = false;
      		break;
      	}
    	}
    	else
    	{
    		ok = false;
    		break;
    	}
    }
		if(ok) 
		{
			if(main.eachThreadOnePort)
			{
				main.remoteGraph1.enableEachThreadSeperatePort(main.numberOfThreads);
			}
	  	main.run();
		}
	  else
	  {	  
	  	System.out.println("usage: java -jar RDF2Graph.jar <output directory> (<user>:<pass>@)?<uri of sparql endpoint>([<uri of graph>])? --multiThread <numofthreads> --eachThreadOnePort --collectPredicateStatistics --collectClassStatistics --collectShapePropertyStatistics --checkPredicateSourceClass --collectForwardMultiplicity --collectReverseMultiplicity --executeSimplify --treatSubClassOfAsIntanceOf --keepOWLClasses");
	  	System.out.println("example usage: java -jar RDF2Graph.jar <output directory> <admin:mypass@http://myserver:8080/db/query[http://example.com/basegraph] --multiThread 4 --collectPredicateStatistics --collectClassStatistics --collectForwardMultiplicity --executeSimplify");
	    System.out.println("Please see RDF2Graph manual for more details");
	  }
	}
	
	//Some predicates are not interesting for the structure recovery and should be excluded
	public boolean filterPredicate(String predicate)
	{
		return excludeProps.contains(predicate);//predicate.startsWith("http://www.w3.org/2002/07/owl#") && keepOWLClasses == false) || 
	}
	
	public void init() throws Exception
	{				
		System.out.println("loading tdb database");
		dataset = TDBFactory.createDataset(this.project);
		System.out.println("loading tdb database done");
		Model localDb = dataset.getNamedModel("http://ssb.wur.nl/RDF2Graph/");
	  localDb.setNsPrefix("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		localDb.setNsPrefix("rdfs","http://www.w3.org/2000/01/rdf-schema#");
		localDb.setNsPrefix("owl","http://www.w3.org/2002/07/owl#");	
		localDb.setNsPrefix("core","http://purl.uniprot.org/core/");		
		localDb.setNsPrefix("RDF2Graph","http://ssb.wur.nl/RDF2Graph/");	
		localDb.setNsPrefix("rs","http://open-services.net/ns/core#");		
		localDb.setNsPrefix("se","http://www.w3.org/2013/ShEx/Definition#");	
		graph = new Graph(localDb);	
		
		excludeProps.add(localDb.expandPrefix("rdf:type"));
		excludeProps.add(localDb.expandPrefix("rdf:first"));
		excludeProps.add(localDb.expandPrefix("rdf:rest"));
		excludeProps.add(localDb.expandPrefix("rdfs:domain"));
		excludeProps.add(localDb.expandPrefix("rdfs:range"));
		excludeProps.add(localDb.expandPrefix("rdf:object"));
		excludeProps.add(localDb.expandPrefix("rdf:predicate"));
		excludeProps.add(localDb.expandPrefix("rdf:subject"));
		excludeProps.add(localDb.expandPrefix("rdf:value"));					
		
	  this.status = new StatusSystem(this.graph);
	  this.status.init();
	  PoolingClientConnectionManager connectionPool = new PoolingClientConnectionManager();
	  connectionPool.setDefaultMaxPerRoute(this.numberOfThreads + 1);
	  connectionPool.setMaxTotal(this.numberOfThreads + 1);
	  HttpOp.setDefaultHttpClient(new DefaultHttpClient(connectionPool));
	  this.executer = new TaskExecuter(this.numberOfThreads);
	}
	
	public Main()
	{

	}
	public void run()
	{

		
		try
		{
			init();
		        
		  //1. First get all the predicates present in the database
			if (!(status.stepDone("recoveryDone") || status.stepDone("1")))
			{
				System.out.println("Getting all predicates present in the database");
				for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph2,"getAllPredicates.txt"))
				{
					String pred = item.get("pred").toString();
					// exclude does properties that are there for describing the structure
					if (!filterPredicate(pred))
					{
						System.out.println("found predicate: " + pred);
						graph.add(pred,"rdf:type","rdf:Property");
					}
				}	  
				
			  // 2. count for each predicate how often it occurs
				if(collectPredicateStatistics)
				{			  
				  for (HashMap<String, RDFNode> item : this.runLocalQuery(graph,true,"getPredicates.txt"))
				  {
					  String pred = item.get("pred").toString();
					  System.out.println("counting number of triples with predicate: " + pred);
					  for (HashMap<String, RDFNode> item2 : this.runRemoteQuery(remoteGraph2,"countPredicate.txt",pred))
					  {
						  int count = Integer.parseInt(item2.get("count").asLiteral().getValue().toString());
						  System.out.println("count = " + count);
						  graph.add(pred,"RDF2Graph:count",count);
				  	}
				  }
				}
			
			  //3. Source subject error checking: find for each predicate:
				//The number of source subjects that have no rdf:type defined and create an ErrorMsgObject
				//Those subjects are not reported in the reconstructed structure of the document
				if(checkPredicateSourceClass)
				{
					runEachAsTask(this.runLocalQuery(graph,true,"getPredicates.txt"),(HashMap<String, RDFNode> item,int index) -> {
					  String pred = item.get("pred").toString();
					  System.out.println("find and count number of subjects that have no rdf:type defined that are using the predicate: " + pred);
					  for (HashMap<String, RDFNode> item2 : this.runRemoteQuery(remoteGraph1,"testSourceTypesForPredicate.txt",pred))
					  {
						  int count = Integer.parseInt(item2.get("count").asLiteral().getValue().toString());
						  System.out.println("count = " + count);
						  if(count > 0)
						  {
						  	System.out.println("found predicate with source subject that has no type (RDF2Graph process may be incomplete)");
						    String errorObj = pred + "_error";
						    graph.add(pred,"RDF2Graph:error",errorObj);
						    graph.add(errorObj,"rdf:type","RDF2Graph:SourceNoTypeError");
						    graph.add(errorObj,"RDF2Graph:count",count);
						  }
					  }
				  });
				}
				this.executer.finishAllFirst();
			
		 	//4. get all classes present in the database and create them as RDF2Graph:Class

				for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph2,"getAllClasses.txt"))
				{
					String clazz = item.get("class").toString();
					//Skip owl:Class and rdfs:Class if we treat subClassOf as instanceOf
					if(this.treatSubClassOfAsIntanceOf && (clazz.equals("http://www.w3.org/2002/07/owl#Class") || clazz.equals("http://www.w3.org/2000/01/rdf-schema#Class")))
					  continue;
				  System.out.println("class: " + clazz);
					graph.add(clazz,"rdf:type","RDF2Graph:Class");
				}
		
			  //5. get additional data for all the classes found in the database
				runEachAsTask(this.runLocalQuery(graph,true,"getFoundClasses.txt"),(HashMap<String, RDFNode> item,int index) -> {
				  String clazz = item.get("class").toString();
				  reconstructClass(clazz);
				});
			
				if(useClassPredFindAtOnce)
				{
				  // * 5.2 collect for the specific class used predicate
				  // Get for this class all the outward going predicates (aka classProperty)
				  System.out.println("get properties for all classes ");
				  runEachAsTask (this.runRemoteQuery(remoteGraph1,"getClassAllShapeProperties.txt"),(HashMap<String, RDFNode> item,int index) -> {
				  	String clazz = item.get("type").toString();
				  	String pred = item.get("pred").toString();
				  	if (this.filterPredicate(pred))
				  	{
				  		return;
				  	}
					  reconstructClassProperty(clazz,pred);
				  });
				}

				this.executer.finishAllFirst();
				
			 	//6. Get all the subClass of references into our database
		  	System.out.println("Loading all subClass of relationships");
		  	for(HashMap<String,RDFNode> item : this.runRemoteQuery(remoteGraph1,"getAllSubClassOf.txt"))
		  	{
		  		String child = item.get("child").toString();			
		  		String parent = item.get("parent").toString();	
					//System.out.println("class structure:" + parent + " -> " + child);
				  graph.add(child,"rdfs:subClassOf",parent);
				}		  	
		  	System.out.println("Loaded: all subClass of relationships");
				status.setStepDone("recoveryDone");	
		 	}

		  //Clean owl classes
			cleanOWL();
		  
		  this.includeStaticDefs();
		  //7. The cleaning phase
		  if(executeSimplify && !(status.stepDone("executeSimplify")))
		  {
		  	this.simplify();
		  	cleanOWL();
		  	status.setStepDone("executeSimplify");
		  }
		  


	    if(this.status.hasPredAsClassDetected())
	      System.out.println("Class as Predicate detected, note that the results might be invalid");
      System.out.println("recovery process completed");
		}
		catch(QuitException qe)
		{
			System.out.println("quited process will continue upon reactivation");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
			  this.executer.finishAllFirst();
			}
			catch(Throwable th)
			{
				th.printStackTrace();
			}
			this.dataset.close() ;
			this.executer.close();
		}
	}
	
	private void cleanOWL()
	{
	  if(!this.keepOWLClasses)
	  {
	  	System.out.println("cleaning OWL classes");
	  	runUpdateQuery(graph,"cleanOWL.txt");
	  	runUpdateQuery(graph,"cleanOWL2.txt");
	  }
	}
	
	private void includeStaticDefs()
	{
		this.graph.add("RDF2Graph:Class","rdfs:subClassOf","RDF2Graph:Type");
		this.graph.add("RDF2Graph:Class","rdf:type","owl:Class");
		this.graph.add("RDF2Graph:Type","rdf:type","owl:Class");
		this.graph.add("RDF2Graph:invalid","rdf:type","RDF2Graph:Invalid");
		this.graph.add("RDF2Graph:Invalid","rdfs:subClassOf","RDF2Graph:Type");
		this.graph.add("RDF2Graph:Invalid","rdf:type","owl:Class");
		this.graph.add("RDF2Graph:externalref","rdf:type","RDF2Graph:ExternalReference");
		this.graph.add("RDF2Graph:ExternalReference","rdfs:subClassOf","RDF2Graph:Type");
		this.graph.add("RDF2Graph:ExternalReference","rdf:type","owl:Class");
		this.graph.add("RDF2Graph:DataType","rdfs:subClassOf","RDF2Graph:Type");
		this.graph.add("RDF2Graph:DataType","rdf:type","owl:Class");
		
		this.graph.add("RDF2Graph:SourceNoTypeError","rdf:type","owl:Class");
		this.graph.add("RDF2Graph:SourceNoTypeError","rdfs:subClassOf","RDF2Graph:Error");
		this.graph.add("RDF2Graph:ClassAsPredicateError","rdf:type","owl:Class");
		this.graph.add("RDF2Graph:ClassAsPredicateError","rdfs:subClassOf","RDF2Graph:Error");
		this.graph.add("RDF2Graph:DestNoTypeError","rdf:type","owl:Class");
		this.graph.add("RDF2Graph:DestNoTypeError","rdfs:subClassOf","RDF2Graph:Error");
		this.graph.add("RDF2Graph:Error","rdf:type","owl:Class");
	}
	
	private void reconstructClass(String clazz) throws Exception
	{
		// 5.1 count the amount of instances that a present in the database for this class type
		if(this.collectClassStatistics && !graph.contains(clazz,"RDF2Graph:count"))
		{
		  System.out.println("counting number of instances of class: " + clazz);
		  for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph2,"countNumberOfClassInstances.txt",clazz))
		  {
			  int count = Integer.parseInt(item.get("count").asLiteral().getValue().toString());
			  System.out.println("count = " + count);
			  graph.add(clazz,"RDF2Graph:count",count);
		  }
  	}

		// Get for this class all the outward going predicates (aka classProperty)
		System.out.println("check if class in not used as predicate: " + clazz);
		for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph1,"checkClassIsNotPredicate.txt",clazz))
		{
			// pred == clazz
			System.out.println("Found class that is also used as predicate (RDF2Graph process may be incorrect)");
			String errorObj = clazz + "_clazz_error";
			graph.add(clazz,"RDF2Graph:error",errorObj);
			graph.add(errorObj,"rdf:type","RDF2Graph:ClassAsPredicateError");
			this.status.setPredAsClassDetected();
		}

		
		if(!useClassPredFindAtOnce)
		{
		  // * 5.2 collect for the specific class used predicate
		  // Get for this class all the outward going predicates (aka classProperty)
		  System.out.println("get properties for class " + clazz);
		  runEachAsTask (this.runRemoteQuery(remoteGraph1,"getClassShapeProperties.txt",clazz),(HashMap<String, RDFNode> item,int index) -> {
		  	String pred = item.get("pred").toString();
		  	if (this.filterPredicate(pred))
		  	{
		  		return;
		  	}
			  reconstructClassProperty(clazz,pred);
		  });
		}
	}
	
	//Make sure that we keep uniq iris if we build them from a parent en child iri
	private String buildUniqIri(String parentIri,String childIri)
	{
		String key = parentIri.replace('#','/') + childIri.substring(Math.max(childIri.replace('#','/').lastIndexOf('/'),0));
		String key2 = parentIri.replace('#','/') + childIri;
		LinkedList<String> list = keepUniq.get(key);
		if(list == null)
		{
			list = new LinkedList<String>();
		  list.add(key2);
			keepUniq.put(key,list);
			return key;
		}
		int index = list.indexOf(key2);
		if(index == -1)
		{
			index = list.size();
			list.add(key2);
		}
		if(index > 0)
			key += "/" + index;
		return key;
	}
	
	private String createClassProperty(String clazz,String predicate,String uniq)
	{
		String classProperty = buildUniqIri(clazz,predicate) + uniq;
		graph.add(classProperty,"rdf:type","RDF2Graph:ClassProperty");
		//graph.add(classProperty,"rdf:type","owl:Class");
		graph.add(classProperty,"RDF2Graph:rdfProperty",predicate);
		graph.add(clazz,"RDF2Graph:property",classProperty);
		//graph.add(clazz,"rdfs:subClassOf",classProperty);
		return classProperty;
	}
	
	private String createTypeLink(String classProperty,String uniq,int count)
	{
		String typeLink = classProperty + "/" +uniq + count++;
		graph.add(typeLink,"rdf:type","RDF2Graph:TypeLink");
		graph.add(classProperty,"RDF2Graph:linkTo",typeLink);
		return typeLink;
	}
	
	private String createDestError(String typeLink,String predicate,String uniq)
	{
		String errorObj = typeLink + "_error" + uniq;
		graph.add(typeLink,"RDF2Graph:error",errorObj);
		graph.add(errorObj,"rdf:type","RDF2Graph:DestNoTypeError");
		graph.add(predicate,"RDF2Graph:error",errorObj);
   	//extra ref from object to predicate object
		//graph.add(errorObj,"RDF2Graph:predicate",predicate);	
		
		return errorObj;
	}
	
	private void reconstructClassProperty(String clazz,String predicate) throws Exception
	{
		System.out.println("Found property: " + predicate + " for shape: " + clazz);
		//Create the classproperty
		String classProperty = createClassProperty(clazz,predicate,"");

		// * 5.3.1 get for each class/prop combi the types it references
		// Each type that is referenced generates one seperate shape property
		int resCount = runEachAsTask(this.runRemoteQuery(remoteGraph1,"getClassPropertyDetails.txt",clazz,predicate),(HashMap<String, RDFNode> item,int count) -> {
			System.out.println("Type link found" + predicate + " for shape: " + clazz + " #" + count);
			String typeLink = createTypeLink(classProperty,"o",count);
			String refType = null;
			if(item.get("refType") != null)
				refType = item.get("refType").toString();
			String xsdType = null;
			if(item.get("xsdType") != null)
				xsdType = item.get("xsdType").toString();
			//If we have no type associated it can be either an subject which has not class or be reference to external resource
			if (refType == null && xsdType == null)
			{
				System.out.println("No type found checking if its a external reference");
				if(this.runRemoteQuery(remoteGraph1,"checkClassPropertyExternalRef.txt",clazz,predicate).iterator().hasNext())
				{
					System.out.println("Referencing to a subject, which is not typed, report error");
					graph.add(typeLink,"RDF2Graph:type","RDF2Graph:invalid");
					createDestError(typeLink,predicate,"");
				}
				else
				{
					System.out.println("It is a external reference");
					graph.add(typeLink,"RDF2Graph:type","RDF2Graph:externalref");
					recoverTypelink(clazz,typeLink,predicate,"rdfs:Resource","exref");					
				}
			}
			else if(refType != null)
			{
				System.out.println("Referencing to shape: " + refType);
				graph.add(typeLink,"RDF2Graph:type",refType); 
				recoverTypelink(clazz,typeLink,predicate,refType,"ref");				
			}
			else
			{
				System.out.println("DataType property found of type: " + xsdType);
				graph.add(typeLink,"RDF2Graph:type",xsdType);
				graph.add(xsdType,"rdf:type","RDF2Graph:DataType");
				recoverTypelink(clazz,typeLink,predicate,xsdType,"simple");				
			}
		});
		if(resCount == 0)
			throw new Error("Class property that has no ref to types");
	}
	
	private void recoverTypelink(String clazz,String typeLink,String predicate,String toType,String suffix) throws Exception
	{ 
		// 5.3.1.1 collect statistics for the shape property
		if(this.collectShapePropertyStatistics)
		{
			System.out.println("counting number of occurrences for the shape property");
			for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph1,"getShapePropertyCount_" + suffix + ".txt",clazz,predicate,toType))
			{
				int count = Integer.parseInt(item.get("count").asLiteral().getValue().toString());
				System.out.println("count = " + count); 
				graph.add(typeLink,"RDF2Graph:count",count);
			}
		}
		// 5.3.1.2 collect forward multiplicity for the property
		if(this.collectForwardMultiplicity)
		{
		  System.out.println("Getting maximum forward multiplicity");
		  int max = 0;
		  int min = 1;
		  if(clazz.equals("http://vocabularies.wikipathways.org/gpml#Interaction") && predicate.equals("http://vocabularies.wikipathways.org/gpml#linethickness"))
		  	System.out.println("ok");
		  for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph1,"getShapePropertyForwardMaxMultiplicity_" + suffix + ".txt",clazz,predicate,toType))
		  {
			  max = Integer.parseInt(item.get("max").asLiteral().getValue().toString());
	  	}
	  	System.out.println("Getting minimum forward multiplicity");
	  	for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph1,"getShapePropertyForwardMinMultiplicity_" + suffix + ".txt",clazz,predicate,toType))
		  {
			  min = 0;
	  	}
	  	
	  	String multiplicy = getMultiplicity(min,max);
		  System.out.println("forward multiplicity: " + multiplicy);
		  graph.add(typeLink,"RDF2Graph:forwardMultiplicity",multiplicy);
		}
		// 5.2.1.3 collect reverse multiplicity for the property
		if(this.collectReverseMultiplicity)
		{
		  System.out.println("Getting maximum reverse multiplicity");
		  int max = 0;
		  int min = 1;
		  if(suffix == "ref" || suffix == "exref" || (suffix == "simple" && toType.equals("http://www.w3.org/2001/XMLSchema#string")))
		  {
		    for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph1,"getShapePropertyReverseMaxMultiplicity_" + suffix + ".txt",clazz,predicate,toType))
		    {
			    max = Integer.parseInt(item.get("max").asLiteral().getValue().toString());
	  	  }
		  }
		  else
		  {
		  	//Max reverse multiplicity makes no sense for simples such as date, integer, float etc
		  	max = -1;
		  }
	  	System.out.println("Getting minimum reverse multiplicity");
	    //not possible to check for simple types or external references so set in to invalid 
	  	//as there are always simples or external references that are not reference by our classProperty
	  	if(suffix == "ref") 
	  	{
	  	  for (HashMap<String, RDFNode> item : this.runRemoteQuery(remoteGraph1,"getShapePropertyReverseMinMultiplicity_ref.txt",toType,predicate,clazz))
		    {
			    min = 0;
	  	  }
	  	}
	  	else
	  	{
	  		min = -1;
	  	}
		  
	  	String multiplicy = getMultiplicity(min,max);
	  	System.out.println("reverse multiplicity: " + multiplicy);
		  graph.add(typeLink,"RDF2Graph:reverseMultiplicity",multiplicy);
		}
	}
	
	private String getMultiplicity(int min,int max) throws Exception
	{
	  if (max == 1 && min == 1)
		  return "rs:Exactly-one";
	  else if (max > 1 && min == 1)
	  	return "rs:One-or-many";
  	else if (max > 1 && min == 0)
  		return "rs:Zero-or-many";
	  else if (max == 1 && min == 0)
	  	return "rs:Zero-or-one";
	  else if (min == -1 && max == 1)
	  	return "RDF2Graph:x_1";
	  else if (min == -1 && max > 1)
	  	return "RDF2Graph:x_n";
	  else if (max == -1 && min == -1)
	  	return "RDF2Graph:none";
	  else
	  {
	  	System.out.println("ERROR: unknow multiplicy type: min = " + min + " max = " + max);
	  	return "RDF2Graph:none";
		//  throw new Exception("unknow multiplicy type: min = " + min + " max = " + max);
	  }
	}
	private int getMax(String multiplicity)
	{
		if(multiplicity == null)
			return -1;
		if(multiplicity.equals("http://open-services.net/ns/core#Exactly-one"))
			return 1;
		else if(multiplicity.equals("http://open-services.net/ns/core#One-or-many"))
			return 2;
		else if(multiplicity.equals("http://open-services.net/ns/core#Zero-or-many"))
			return 2;
		else if(multiplicity.equals("http://open-services.net/ns/core#Zero-or-one"))
			return 1;
		else if(multiplicity.equals("http://ssb.wur.nl/RDF2Graph/x_1"))
			return 1;	
		else if(multiplicity.equals("http://ssb.wur.nl/RDF2Graph/x_n"))
			return 2;	
		else if(multiplicity.equals("http://ssb.wur.nl/RDF2Graph/none"))
			return -1;	
		throw new RuntimeException("unknown multiplicity " + multiplicity);
	}
	private int getMin(String multiplicity)
	{
		if(multiplicity == null)
			return -1;
		else if(multiplicity.equals("http://open-services.net/ns/core#Exactly-one"))
			return 1;
		else if(multiplicity.equals("http://open-services.net/ns/core#One-or-many"))
			return 1;
		else if(multiplicity.equals("http://open-services.net/ns/core#Zero-or-many"))
			return 0;
		else if(multiplicity.equals("http://open-services.net/ns/core#Zero-or-one"))
			return 0;
		else if(multiplicity.equals("http://ssb.wur.nl/RDF2Graph/none") || multiplicity.equals("http://ssb.wur.nl/RDF2Graph/x_1") || multiplicity.equals("http://ssb.wur.nl/RDF2Graph/x_n"))
			return -1;		
		throw new RuntimeException("unknown multiplicity " + multiplicity);
	}
	
	private void simplify() throws Exception
	{
    //Step 7.1 Mark instantiated classes and in between classes
  	runUpdateQueryOnce(graph,"clean2_project1.txt");
  	runUpdateQueryOnce(graph,"clean2_project2.txt");
	  //Step 7.2
	  Tree tree = buildTree();
	  //Step 7.3
	  tree.calculateSubClassOfIntanceOfCount();
	  storeClassProps(tree);
	  //Step 7.4
	  for (HashMap<String, RDFNode> item : this.runLocalQuery(graph,true,"clean4_getAllProps.txt"))
  	{
		  String property = item.get("property").toString();
	    cleanForProperty(tree,property);
	  }
	  //Step 7.5
	  runUpdateQueryOnce(graph,"clean6_removeAllShapeProps1.txt");
	  runUpdateQueryOnce(graph,"clean7_removeAllShapeProps2.txt");
	  //Step 7.6
	  runUpdateQueryOnce(graph,"clean8_insertConceptClasses1.txt");		
	  runUpdateQueryOnce(graph,"clean8_insertConceptClasses2.txt");
	}
	
	private void cleanForProperty(Tree tree,String property) throws Exception
	{
		//step 7.4.1
		System.out.println("performing cleaning for property: " + property);
		System.out.println("loading property information into the tree");
		for (HashMap<String, RDFNode> item : this.runLocalQuery(graph,true,"clean5_getAllShapeProps.txt",property))
		{
			String source = item.get("source").toString();
			String type = item.get("type").toString();
			if(source == null || type == null)
				throw new RuntimeException("source or dest of shape property is null");
			int count = 0;
			if(item.get("count") != null)
				count = item.get("count").asLiteral().getInt();
			String forwardMultiplicity = null;
			if(item.get("forwardMultiplicity") != null)
				forwardMultiplicity =	item.get("forwardMultiplicity").toString();
			String reverseMultiplicity = null;
			if(item.get("reverseMultiplicity") != null)
		    reverseMultiplicity = item.get("reverseMultiplicity").toString();
			TreeNode sourceNode = tree.getNode(source);
			if(sourceNode == null)
			{
				System.out.println("source node not found: " + source);
				sourceNode = tree.createMissingNode(source,tree);
			}
			sourceNode.addType(type,count,this.getMin(forwardMultiplicity),this.getMax(forwardMultiplicity),this.getMin(reverseMultiplicity),this.getMax(reverseMultiplicity));				
		}		
		tree.prepareDestType();
		printShapeProps(tree);
		//step 7.4.2
		System.out.println("step 1");
		if(property.equals("http://ssb.wur.nl/RDF2Graph/rdfProperty"))
			System.out.println("debug");
		tree.projectDownStep1();
		printShapeProps(tree);
	  //step 7.4.3
		System.out.println("step 2");
		tree.projectDownStep2();
		printShapeProps(tree);
	  //step 7.4.4
		System.out.println("step 3");
		tree.projectDownStep3();
		printShapeProps(tree);
		//write new properties to database
		//step 7.4.5
		storeShapeProps(tree,property);
		System.out.println("clean tree");
		tree.clean();
	}
	
	private void printShapeProps(Tree tree)
	{
		for(TreeNode node : tree.getAllNodes())
		{
			for(ShapeProperty dest : node.getResDestTypes())
			{
				System.out.println(node.name + " ---> " + dest.typeName);
			}
		}
	}
	
	private void storeClassProps(Tree tree)
	{
		for(TreeNode node : tree.getAllNodes())
		{
			if(node.isRoot()) //do not export root object
				continue;
			String clazz = node.name;
			int count = node.getSubClassOffInstanceCount();
			assert(count != -2);
			if(count != -1)
			  graph.add(clazz,"RDF2Graph:subClassOfInstanceCount",count);
		}
	}
	
	private void storeShapeProps(Tree tree,String predicate) throws Exception
	{
		int count = 1;
		for(TreeNode node : tree.getAllNodes())
		{
			for(ShapeProperty dest : node.getResDestTypes())
			{
				String sourceType = node.name;
				String typeName = dest.typeName;
				String classProperty = createClassProperty(sourceType,predicate,"_n");
				String typeLink = createTypeLink(classProperty,"n",count++);
						
				graph.add(typeLink,"RDF2Graph:type",typeName);
				graph.add(typeLink,"RDF2Graph:count",dest.count);
			  graph.add(typeLink,"RDF2Graph:forwardMultiplicity",this.getMultiplicity(dest.forwardMinMultiplicity,dest.forwardMaxMultiplicity));
			  graph.add(typeLink,"RDF2Graph:reverseMultiplicity",this.getMultiplicity(dest.reverseMinMultiplicity,dest.reverseMaxMultiplicity));
			  //note them as new so they do not get deleted
			  graph.add(classProperty,"RDF2Graph:noteAsNew","new");
			  
			  if(typeName.equals(graph.expand("RDF2Graph:invalid")))
			  {
				  createDestError(typeLink,predicate,"_n");
			  }
			}
		}
	}
	
	private Tree buildTree()
	{
		System.out.println("building the subclassOf tree");
		Tree tree = new Tree();
		for (HashMap<String, RDFNode> item : this.runLocalQuery(graph,true,"clean3_getTree.txt"))
		{
			String parent = item.get("parent").toString();
			String child = item.get("child").toString();
			RDFNode temp = item.get("childCount");
			int childCount = -2;
			if(temp != null)
				childCount = temp.asLiteral().getInt();
			temp = item.get("parentCount");
			int parentCount = -2;
			if(temp != null)
				parentCount = temp.asLiteral().getInt();
			System.out.println("parent = " + parent + " child = " + child);
      tree.buildLink(parent,parentCount,child,childCount);
		}	
    tree.finish();
		System.out.println("building the subclassOf tree done");
		return tree;
	}
	
	private String readFile(String file) throws IOException
	{
		File inFile = new File(file);
		FileInputStream input = new FileInputStream(inFile);
		byte all[] = new byte[(int)inFile.length()];
		input.read(all);
		String string = new String(all);		
		input.close();
		return string;
	}
	
	public Query getQuery(String file,Object ... args)
	{
		try
		{
			String header = this.readFile("queries/header.txt");
			String content = this.readFile(file);
			String query = header + content;
			query = String.format(query,args);			
			return QueryFactory.create(query);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
  private Iterable<HashMap<String,RDFNode>> runRemoteQuery(GraphConfig graph,String queryFile,Object ... args) throws Exception
  {    
    long millis = System.currentTimeMillis();
    if(treatSubClassOfAsIntanceOf)
    	queryFile = "queries/remoteSubClassOf/" + queryFile;
    else
    	queryFile = "queries/remote/" + queryFile;
    Iterable<HashMap<String,RDFNode>> toRet = runQuery(graph.createQuery(this,queryFile,args),true);
	  System.out.println("time: " + (System.currentTimeMillis() - millis) + " for query " + queryFile); 
	  return toRet;
  }
  private int runEachAsTask(Iterable<HashMap<String,RDFNode>> totalSet,ResultHandler resultHandler) throws Exception
  {
  	int count = 1;
  	for (HashMap<String, RDFNode> item : totalSet)
  	{
  		this.executer.executeTask((Object[] data) -> {
  			resultHandler.handleResult((HashMap<String, RDFNode>)data[0],((Integer)data[1]).intValue());
  		},item,count++);
  	}
  	return count - 1;
  }
  
  private Iterable<HashMap<String,RDFNode>> runLocalQuery(Graph graph,boolean preload,String queryFile,Object ... args)
  {
	  return runQuery(QueryExecutionFactory.create(this.getQuery("queries/local/" + queryFile,args),graph.model,null),preload);
  }
	private Iterable<HashMap<String,RDFNode>> runQuery(QueryExecution qe,boolean preload)
	{
		ResultSet result = qe.execSelect();
		Iterable<HashMap<String,RDFNode>> walker = new Iteration<HashMap<String,RDFNode>>(new ResultIterator(result));
		if(preload == false)
		{
			return walker;
		}
		else
		{
			LinkedList<HashMap<String,RDFNode>> res = new LinkedList<HashMap<String,RDFNode>>();
			for(HashMap<String,RDFNode> item : walker)
			{
				res.add(item);
			}
			qe.close();
			return new Iteration<HashMap<String,RDFNode>>(res.iterator());
		}
	}
	private void runUpdateQueryOnce(Graph graph,String file,Object ...args)
	{
	  if(!graph.containsLit("RDF2Graph:status","RDF2Graph:updatePerformed",file))
	 	{
	  	System.out.println("running update query " + file);
	  	runUpdateQuery(graph,file,args);
	  	graph.addLit("RDF2Graph:status","RDF2Graph:updatePerformed",file);
	 	}
	}
	private void runUpdateQueryOnceA(Graph graph,String file,Object ...args)
	{
	  System.out.println("DEV: running update query " + file);
	  runUpdateQuery(graph,file,args);
	  graph.addLit("RDF2Graph:status","RDF2Graph:updatePerformed",file);
	}
	
  private void runUpdateQuery(Graph graph,String file,Object ...args)
  {
		try
		{
			String header = this.readFile("queries/header.txt");
			String content = this.readFile("queries/local/" + file);
			String query = header + content;
			query = String.format(query,args);		
			UpdateAction.parseExecute(query, graph.model);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}  	
  }
}

