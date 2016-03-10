package nl.wur.ssb.RDF2Graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import nl.wur.ssb.RDF2Graph.simplify.Tree;
import nl.wur.ssb.RDF2Graph.simplify.TreeNode;
import nl.wur.ssb.RDF2Graph.simplify.UniqueTypeLink;
import nl.wur.ssb.RDFSimpleCon.RDFSimpleCon;
import nl.wur.ssb.RDFSimpleCon.ResultLine;
import nl.wur.ssb.RDFSimpleCon.Util;
import nl.wur.ssb.RDFSimpleCon.concurrent.ResultHandler;
import nl.wur.ssb.RDFSimpleCon.concurrent.TaskExecuter;

import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.riot.web.HttpOp;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/*
 * Please see doc manual.pdf for full description of the process
 */

public class RDF2Graph
{
	private HashSet<String> excludeProps = new HashSet<String>();
	private StatusSystem status;
  private	RDFSimpleCon remoteGraph1;
  private	RDFSimpleCon remoteGraph2;
	private RDFSimpleCon localStore;
	private TaskExecuter executer;
	private boolean collectPredicateStatistics = false;
	private boolean collectClassStatistics = false;
	private boolean collectShapePropertyStatistics = false;
	private boolean checkPredicateSourceClass = false;
	private boolean collectForwardMultiplicity = false;
	private boolean collectReverseMultiplicity = false;
	private boolean executeSimplify = false; 
	private boolean treatSubClassOfAsIntanceOf = false;
	private boolean removeOWLProperties = false;
	private boolean useClassPropertyRecoveryPerClass = false;
	private int numberOfThreads = 1;
	private HashMap<String,LinkedList<String>> keepUniq = new HashMap<String,LinkedList<String>>(); 

	public static void main(String args[]) throws Exception
	{
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
	
		boolean ok = true;
  		
		String outputDir = "";
		RDFSimpleCon remoteGraph1 = null;
		RDFSimpleCon remoteGraph2 = null;
		boolean collectPredicateStatistics = false;
		boolean collectClassStatistics = false;
		boolean collectShapePropertyStatistics = false;
		boolean checkPredicateSourceClass = false;
		boolean collectForwardMultiplicity = false;
		boolean collectReverseMultiplicity = false;
		boolean executeSimplify = false; 
		boolean treatSubClassOfAsIntanceOf = false;
		boolean removeOWLProperties = false;
		boolean eachThreadOnePort = false;
		boolean useClassPropertyRecoveryPerClass = false;
		int numberOfThreads = 1;
		
		int rest = 2;
		if(args.length >= 2)
		{
			outputDir = args[0];
			remoteGraph1 = new RDFSimpleCon(args[1]);
			if(args.length >= 3 && args[2].equals("+"))
			{
				remoteGraph2 = new RDFSimpleCon(args[3]);
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
    	  	collectPredicateStatistics = true;
    	  	collectClassStatistics = true;
    	  	checkPredicateSourceClass = true;
    	  	collectShapePropertyStatistics = true;
    	  	collectForwardMultiplicity = true;
    	  	collectReverseMultiplicity = true;
    	  }
    	  else if(enProp.equals("collectPredicateStatistics"))
      		collectPredicateStatistics = true;
      	else if(enProp.equals("collectClassStatistics"))
      		collectClassStatistics = true;
      	else if(enProp.equals("collectShapePropertyStatistics"))//TODO Rename
      		collectShapePropertyStatistics = true;
      	else if(enProp.equals("checkPredicateSourceClass"))
      		checkPredicateSourceClass = true;
      	else if(enProp.equals("collectForwardMultiplicity"))
      		collectForwardMultiplicity = true;
      	else if(enProp.equals("collectReverseMultiplicity"))
      		collectReverseMultiplicity = true;
      	else if(enProp.equals("executeSimplify"))
      		executeSimplify = true;
      	else if(enProp.equals("treatSubClassOfAsIntanceOf"))
      		treatSubClassOfAsIntanceOf = true;      
      	else if(enProp.equals("removeOWLProperties"))
      		removeOWLProperties = true;
      	else if(enProp.equals("eachThreadOnePort"))
      		eachThreadOnePort = true;    	  
      	else if(enProp.equals("useClassPropertyRecoveryPerClass"))
      		useClassPropertyRecoveryPerClass = true;
      	else if(enProp.equals("multiThread"))
      	{
      		try
      		{
      	  	numberOfThreads = Integer.parseInt(args[++i]);
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
			System.out.println("loading/creating local tdb database");
			RDFSimpleCon localStore = new RDFSimpleCon("[http://ssb.wur.nl/RDF2Graph/]",outputDir);	
			System.out.println("loading/creating local tdb database done"); 
			RDF2Graph main = new RDF2Graph(localStore, remoteGraph1, remoteGraph2, collectPredicateStatistics, collectClassStatistics,collectShapePropertyStatistics, 
					checkPredicateSourceClass, collectForwardMultiplicity, collectReverseMultiplicity, executeSimplify, treatSubClassOfAsIntanceOf,
					removeOWLProperties, eachThreadOnePort, useClassPropertyRecoveryPerClass, numberOfThreads);
	  	main.run();
		}
	  else
	  {	  
	  	System.out.println("usage: java -jar RDF2Graph.jar <output directory> (<user>:<pass>@)?<uri of sparql endpoint>([<uri of graph>])? --multiThread <numofthreads> --eachThreadOnePort --collectPredicateStatistics --collectClassStatistics --collectShapePropertyStatistics --checkPredicateSourceClass --collectForwardMultiplicity --collectReverseMultiplicity --executeSimplify --treatSubClassOfAsIntanceOf --removeOWLClasses --useClassPropertyRecoveryPerClass");
	  	System.out.println("example usage: java -jar RDF2Graph.jar <output directory> <admin:mypass@http://myserver:8080/db/query[http://example.com/basegraph] --multiThread 4 --collectPredicateStatistics --collectClassStatistics --collectForwardMultiplicity --executeSimplify");
	    System.out.println("Please see RDF2Graph manual for more details");
	  }
	}
	
	public RDF2Graph(RDFSimpleCon localStore, RDFSimpleCon graph,boolean executeSimplify,boolean removeOWLProperties)
	{
		this(localStore,graph,true,true,true,true,true,true,executeSimplify,false,removeOWLProperties);
	}
	
	public RDF2Graph(RDFSimpleCon localStore,RDFSimpleCon remoteGraph,boolean collectPredicateStatistics,boolean collectClassStatistics,
			boolean collectShapePropertyStatistics, boolean checkPredicateSourceClass,boolean collectForwardMultiplicity,boolean collectReverseMultiplicity,
			boolean executeSimplify, 	boolean treatSubClassOfAsIntanceOf,	boolean removeOWLProperties)
	{
		this(localStore,remoteGraph,null,collectPredicateStatistics,collectClassStatistics,collectShapePropertyStatistics,checkPredicateSourceClass,
				collectForwardMultiplicity,collectReverseMultiplicity, executeSimplify, treatSubClassOfAsIntanceOf,removeOWLProperties,false,false,1);
	}
	
	public RDF2Graph(RDFSimpleCon localStore,RDFSimpleCon graph1,RDFSimpleCon graph2,boolean collectPredicateStatistics,boolean collectClassStatistics,
			boolean collectShapePropertyStatistics, boolean checkPredicateSourceClass,boolean collectForwardMultiplicity,boolean collectReverseMultiplicity,
			boolean executeSimplify, 	boolean treatSubClassOfAsIntanceOf,	boolean removeOWLProperties,boolean eachThreadOnePort,
			boolean useClassPropertyRecoveryPerClass,	int numberOfThreads)
	{
		this.localStore = localStore;
		this.remoteGraph1 = graph1;
		this.remoteGraph2 = graph2;
		if(this.remoteGraph2 == null)
			this.remoteGraph2 = this.remoteGraph1;
		this.collectPredicateStatistics = collectPredicateStatistics;
		this.collectClassStatistics = collectClassStatistics;
		this.collectShapePropertyStatistics = collectShapePropertyStatistics;
		this.checkPredicateSourceClass = checkPredicateSourceClass;
		this.collectForwardMultiplicity = collectForwardMultiplicity;
		this.collectReverseMultiplicity = collectReverseMultiplicity;
		this.executeSimplify = executeSimplify; 
		this.treatSubClassOfAsIntanceOf = treatSubClassOfAsIntanceOf;
		this.removeOWLProperties = removeOWLProperties;
		this.useClassPropertyRecoveryPerClass = useClassPropertyRecoveryPerClass;
		this.numberOfThreads = numberOfThreads;
		if(eachThreadOnePort)
		{
			remoteGraph1.enableEachThreadSeperatePort(numberOfThreads);
		}
	}
	
	//Some predicates are not interesting for the structure recovery and should be excluded
	public boolean filterPredicate(String predicate)
	{
		return excludeProps.contains(predicate);//predicate.startsWith("http://www.w3.org/2002/07/owl#") && keepOWLClasses == false) || 
	}
	
	public void init() throws Exception
	{				
		localStore.setNsPrefix("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		localStore.setNsPrefix("rdfs","http://www.w3.org/2000/01/rdf-schema#");
		localStore.setNsPrefix("owl","http://www.w3.org/2002/07/owl#");	
		localStore.setNsPrefix("core","http://purl.uniprot.org/core/");		
		localStore.setNsPrefix("RDF2Graph","http://ssb.wur.nl/RDF2Graph/");	
		localStore.setNsPrefix("rs","http://open-services.net/ns/core#");		
		localStore.setNsPrefix("se","http://www.w3.org/2013/ShEx/Definition#");	
		
		excludeProps.add(localStore.expand("rdf:type"));
		excludeProps.add(localStore.expand("rdf:first"));
		excludeProps.add(localStore.expand("rdf:rest"));
		excludeProps.add(localStore.expand("rdfs:domain"));
		excludeProps.add(localStore.expand("rdfs:range"));
		excludeProps.add(localStore.expand("rdf:object"));
		excludeProps.add(localStore.expand("rdf:predicate"));
		excludeProps.add(localStore.expand("rdf:subject"));
		excludeProps.add(localStore.expand("rdf:value"));					
		
	  this.status = new StatusSystem(this.localStore);
	  this.status.init();
	  PoolingClientConnectionManager connectionPool = new PoolingClientConnectionManager();
	  connectionPool.setDefaultMaxPerRoute(this.numberOfThreads + 1);
	  connectionPool.setMaxTotal(this.numberOfThreads + 1);
	  HttpOp.setDefaultHttpClient(new DefaultHttpClient(connectionPool));
	  this.executer = new TaskExecuter(this.numberOfThreads);
	}
	
	public RDF2Graph()
	{

	}
	public void run()
	{		
		try
		{
			init();
		        
		  //1. First get all the predicates present in the database
			if (!status.stepDone("recoveryDone"))
			{
				System.out.println("Getting all predicates present in the database");
				for (ResultLine item : this.runRemoteQuery(remoteGraph2,"getAllPredicates.txt"))
				{
					String pred = item.getIRI("pred").toString();
					// exclude does properties that are there for describing the structure
					if (!filterPredicate(pred))
					{
						System.out.println("found predicate: " + pred);
						localStore.add(pred,"rdf:type","rdf:Property");
					}
				}	  
			  // 2. count for each predicate how often it occurs
				if(collectPredicateStatistics)
				{			  
				  for (ResultLine item : this.runLocalQuery(localStore,true,"getPredicates.txt"))
				  {
					  String pred = item.getIRI("pred");
					  System.out.println("counting number of triples with predicate: " + pred);
					  for (ResultLine item2 : this.runRemoteQuery(remoteGraph2,"countPredicate.txt",pred))
					  {
						  int count = item2.getLitInt("count");
						  System.out.println("count = " + count);
						  localStore.add(pred,"RDF2Graph:count",count);
				  	}
				  }
				}
			
			  //3. Source subject error checking: find for each predicate:
				//The number of source subjects that have no rdf:type defined and create an ErrorMsgObject
				//Those subjects are not reported in the reconstructed structure of the document
				if(checkPredicateSourceClass)
				{
					this.executer.runEachAsTask(this.runLocalQuery(localStore,true,"getPredicates.txt"),(ResultHandler)(ResultLine item,int index) -> {
					  String pred = item.getIRI("pred");
					  System.out.println("find and count number of subjects that have no rdf:type defined that are using the predicate: " + pred);
					  for (ResultLine item2 : this.runRemoteQuery(remoteGraph1,"testSourceTypesForPredicate.txt",pred))
					  {
						  int count = item2.getLitInt("count");
						  System.out.println("count = " + count);
						  if(count > 0)
						  {
						  	System.out.println("found predicate with source subject that has no type (RDF2Graph process may be incomplete)");
						    String errorObj = pred + "_error";
						    localStore.add(pred,"RDF2Graph:error",errorObj);
						    localStore.add(errorObj,"rdf:type","RDF2Graph:SourceNoTypeError");
						    localStore.add(errorObj,"RDF2Graph:count",count);
						  }
					  }
				  });
				}
				this.executer.finishAllFirst();
			
		  	//4. get all classes present in the database and create them as RDF2Graph:Class
				HashSet<String> classSet = new HashSet<String>(); 
				for (ResultLine item : this.runRemoteQuery(remoteGraph2,"getAllClasses.txt"))
				{
					String clazz = item.getIRI("class");
					//Skip owl:Class and rdfs:Class if we treat subClassOf as instanceOf
					if(this.treatSubClassOfAsIntanceOf && (clazz.equals("http://www.w3.org/2002/07/owl#Class") || clazz.equals("http://www.w3.org/2000/01/rdf-schema#Class")))
					  continue;
					if(clazz == null)
					{
						System.out.println("Skipping class which is a blank node");
						continue;
					}
				  System.out.println("class: " + clazz);
					localStore.add(clazz,"rdf:type","RDF2Graph:Class");
					classSet.add("<" + clazz + ">");
				}
		
			  //5. get additional data for all the classes found in the database
				this.executer.runEachAsTask(this.runLocalQuery(localStore,true,"getFoundClasses.txt"),(ResultHandler)(ResultLine item,int index) -> {
				  String clazz = item.getIRI("class");
				  reconstructClass(clazz);
				});
			
				if(!useClassPropertyRecoveryPerClass)
				{
				  // * 5.2 collect for the specific class used predicate
				  // Get for this class all the outward going predicates (aka classProperty)
				  System.out.println("get properties for all classes ");
				  this.executer.runEachAsTask (this.runRemoteQuery(remoteGraph1,"getClassAllShapeProperties.txt"),(ResultHandler)(ResultLine item,int index) -> {
				  	String clazz = item.getIRI("type");
				  	String pred = item.getIRI("pred");
				  	if (this.filterPredicate(pred))
				  	{
				  		return;
				  	}
				  	if(clazz == null)
				  	{
				  		System.out.println("Skipping proper of a class which is a blank node");
				  		return;
				  	}
					  reconstructClassProperty(clazz,pred);
				  });
				}

				this.executer.finishAllFirst();
				
			 	//6. Get all the subClass of references into our database
		  	System.out.println("Loading all subClass of relationships");
		  	for(ResultLine item : this.runRemoteQuery(remoteGraph1,"getAllSubClassOf.txt"))
		  	{
		  		String child = item.getIRI("child");			
		  		String parent = item.getIRI("parent");	
					//System.out.println("class structure:" + parent + " -> " + child);
				  localStore.add(child,"rdfs:subClassOf",parent);
				  classSet.add("<" + child + ">");
				  classSet.add("<" + parent + ">");
				}
		  	getRDFLabels(classSet);	
		  	
		    //Get property labels and descriptions if available
				LinkedList<String> propertySet = new LinkedList<String>();
				for (ResultLine item : this.runLocalQuery(localStore,true,"simplify2_getAllProps.txt"))
				{
					propertySet.add("<" + item.getIRI("property") + ">");
				}
				getRDFLabels(propertySet);
				
				loadDefaultSubClassOfRelationShips();
		  	
		  	System.out.println("Loaded: all subClass of relationships");
				status.setStepDone("recoveryDone");	
		 	}			
			
		  //Clean owl classes
			cleanOWL();
		  
		  this.includeStaticDefs();
		  
		  //These steps has always to be executed
	    //Step 7.1 Mark classes with some instances and classes that have subclasses with some instances
	  	runUpdateQueryOnce(localStore,"finalize1_addRDF2GraphClass.txt");
		  //Step 7.2 Mark all concept classes
		  runUpdateQueryOnce(localStore,"finalize2_insertConceptClasses1.txt");		
		  runUpdateQueryOnce(localStore,"finalize3_insertConceptClasses2.txt");
		  //7. The cleaning phase
		  if(executeSimplify && !(status.stepDone("executeSimplify")))
		  {
		  	this.simplify();
		  	cleanOWL();
			  runUpdateQueryOnce(localStore,"simplify6_addIsCoreClass1.txt");		
			  runUpdateQueryOnce(localStore,"simplify7_addIsCoreClass2.txt");
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
			this.executer.close();
			this.localStore.close();
		}
	}
	
	/*
	 * Load default subClass relation ships from the OWL ontology itself
	 */
	private void loadDefaultSubClassOfRelationShips() throws Exception
	{
		for (String line : Util.readFile("defaultOwlSubClassOfRelations.tsv").split("\\n"))
		{
			String tmp[] = line.split("\\t");
			this.localStore.add(tmp[0],"rdfs:subClassOf",tmp[1]);
		}
	}
	
	private void getRDFLabels(Collection<String> classSet) throws Exception
	{
		if(classSet.size() > 0)
		{
			for (ResultLine item : this.runRemoteQuery(remoteGraph2,"getAllRDFLabels.txt",String.join(" ",classSet)))
			{
				String clazz = item.getIRI("class");
				String label = item.asString("label");
				String comment = item.asString("comment");
				localStore.addLit(clazz,"rdfs:label",label);
				if(comment != null)
					localStore.addLit(clazz,"rdfs:comment",comment);
			}
		}
	}
	
	private void cleanOWL()
	{
	  if(this.removeOWLProperties)
	  {
	  	System.out.println("cleaning OWL classes");
	  	localStore.runUpdateQuery("local/cleanOWL1.txt");
	  	localStore.runUpdateQuery("local/cleanOWL2.txt");
	  	localStore.runUpdateQuery("local/cleanOWL3.txt");
	  	localStore.runUpdateQuery("local/cleanOWL4.txt");
	  	localStore.runUpdateQuery("local/cleanOWL5.txt");
	  }
	}
	
	private void includeStaticDefs()
	{
		this.localStore.add("RDF2Graph:Class","rdfs:subClassOf","RDF2Graph:Type");
		this.localStore.add("RDF2Graph:Class","rdf:type","owl:Class");
		this.localStore.add("RDF2Graph:ConceptClass","rdf:type","owl:Class");
		this.localStore.add("RDF2Graph:Class","rdfs:subClassOf","rdfs:Class");
		this.localStore.add("RDF2Graph:ConceptClass","rdfs:subClassOf","rdfs:Class");
		this.localStore.add("RDF2Graph:Type","rdf:type","owl:Class");
		this.localStore.add("RDF2Graph:invalid","rdf:type","RDF2Graph:Invalid");
		this.localStore.add("RDF2Graph:Invalid","rdfs:subClassOf","RDF2Graph:Type");
		this.localStore.add("RDF2Graph:Invalid","rdf:type","owl:Class");
		this.localStore.add("RDF2Graph:externalref","rdf:type","RDF2Graph:ExternalReference");
		this.localStore.add("RDF2Graph:ExternalReference","rdfs:subClassOf","RDF2Graph:Type");
		this.localStore.add("RDF2Graph:ExternalReference","rdf:type","owl:Class");
		this.localStore.add("RDF2Graph:DataType","rdfs:subClassOf","RDF2Graph:Type");
		this.localStore.add("RDF2Graph:DataType","rdf:type","owl:Class");
		
		this.localStore.add("RDF2Graph:SourceNoTypeError","rdf:type","owl:Class");
		this.localStore.add("RDF2Graph:SourceNoTypeError","rdfs:subClassOf","RDF2Graph:Error");
		this.localStore.add("RDF2Graph:ClassAsPredicateError","rdf:type","owl:Class");
		this.localStore.add("RDF2Graph:ClassAsPredicateError","rdfs:subClassOf","RDF2Graph:Error");
		this.localStore.add("RDF2Graph:DestNoTypeError","rdf:type","owl:Class");
		this.localStore.add("RDF2Graph:DestNoTypeError","rdfs:subClassOf","RDF2Graph:Error");
		this.localStore.add("RDF2Graph:Error","rdf:type","owl:Class");
	}
	
	private void reconstructClass(String clazz) throws Exception
	{
		// 5.1 count the amount of instances that a present in the database for this class type
		if(this.collectClassStatistics && !localStore.contains(clazz,"RDF2Graph:count"))
		{
		  System.out.println("counting number of instances of class: " + clazz);
		  for (ResultLine item : this.runRemoteQuery(remoteGraph2,"countNumberOfClassInstances.txt",clazz))
		  {
			  int count = item.getLitInt("count");
			  System.out.println("count = " + count);
			  localStore.add(clazz,"RDF2Graph:count",count);
		  }
  	}

		// Get for this class all the outward going predicates (aka classProperty)
		System.out.println("check if class in not used as predicate: " + clazz);
		for (ResultLine item : this.runRemoteQuery(remoteGraph1,"checkClassIsNotPredicate.txt",clazz))
		{
			// pred == clazz
			System.out.println("Found class that is also used as predicate (RDF2Graph process may be incorrect)");
			String errorObj = clazz + "_clazz_error";
			localStore.add(clazz,"RDF2Graph:error",errorObj);
			localStore.add(errorObj,"rdf:type","RDF2Graph:ClassAsPredicateError");
			this.status.setPredAsClassDetected();
		}

		
		if(useClassPropertyRecoveryPerClass)
		{
		  // * 5.2 collect for the specific class used predicate
		  // Get for this class all the outward going predicates (aka classProperty)
		  System.out.println("get properties for class " + clazz);
		  this.executer.runEachAsTask (this.runRemoteQuery(remoteGraph1,"getClassShapeProperties.txt",clazz),(ResultHandler)(ResultLine item,int index) -> {
		  	String pred = item.getIRI("pred");
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
		localStore.add(classProperty,"rdf:type","RDF2Graph:ClassProperty");
		//graph.add(classProperty,"rdf:type","owl:Class");
		localStore.add(classProperty,"RDF2Graph:rdfProperty",predicate);
		localStore.add(clazz,"RDF2Graph:property",classProperty);
		//graph.add(clazz,"rdfs:subClassOf",classProperty);
		return classProperty;
	}
	
	private String createTypeLink(String classProperty,String uniq,int count)
	{
		String typeLink = classProperty + "/" +uniq + count++;
		localStore.add(typeLink,"rdf:type","RDF2Graph:TypeLink");
		localStore.add(classProperty,"RDF2Graph:linkTo",typeLink);
		return typeLink;
	}
	
	private String createDestError(String typeLink,String predicate,String uniq)
	{
		String errorObj = typeLink + "_error" + uniq;
		localStore.add(typeLink,"RDF2Graph:error",errorObj);
		localStore.add(errorObj,"rdf:type","RDF2Graph:DestNoTypeError");
		localStore.add(predicate,"RDF2Graph:error",errorObj);
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
		int resCount = this.executer.runEachAsTask(this.runRemoteQuery(remoteGraph1,"getClassPropertyDetails.txt",clazz,predicate),(ResultHandler)(ResultLine item,int count) -> {
			System.out.println("Type link found" + predicate + " for shape: " + clazz + " #" + count);
			String typeLink = createTypeLink(classProperty,"o",count);
			String refType = item.getIRI("refType");
			String xsdType = item.getIRI("xsdType");

			//If we have no type associated it can be either an subject which has not class or be reference to external resource
			if (refType == null && xsdType == null)
			{
				System.out.println("No type found checking if its a external reference");
				if(this.runRemoteQuery(remoteGraph1,"checkClassPropertyExternalRef.txt",clazz,predicate).iterator().hasNext())
				{
					System.out.println("Referencing to a subject, which is not typed, report error");
					localStore.add(typeLink,"RDF2Graph:type","RDF2Graph:invalid");
					createDestError(typeLink,predicate,"");
				}
				else
				{
					System.out.println("It is a external reference");
					localStore.add(typeLink,"RDF2Graph:type","RDF2Graph:externalref");
					recoverTypelink(clazz,typeLink,predicate,"rdfs:Resource","exref");					
				}
			}
			else if(refType != null)
			{
				System.out.println("Referencing to shape: " + refType);
				localStore.add(typeLink,"RDF2Graph:type",refType); 
				recoverTypelink(clazz,typeLink,predicate,refType,"ref");				
			}
			else
			{
				System.out.println("DataType property found of type: " + xsdType);
				localStore.add(typeLink,"RDF2Graph:type",xsdType);
				localStore.add(xsdType,"rdf:type","RDF2Graph:DataType");
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
			for (ResultLine item : this.runRemoteQuery(remoteGraph1,"getShapePropertyCount_" + suffix + ".txt",clazz,predicate,toType))
			{
				int count = item.getLitInt("count");
				System.out.println("count = " + count); 
				localStore.add(typeLink,"RDF2Graph:count",count);
			}
		}
		// 5.3.1.2 collect forward multiplicity for the property
		if(this.collectForwardMultiplicity)
		{
		  System.out.println("Getting maximum forward multiplicity");
		  int max = 0;
		  int min = 1;
		  for (ResultLine item : this.runRemoteQuery(remoteGraph1,"getShapePropertyForwardMaxMultiplicity_" + suffix + ".txt",clazz,predicate,toType))
		  {
			  max = item.getLitInt("max");
	  	}
	  	System.out.println("Getting minimum forward multiplicity");
	  	for (ResultLine item : this.runRemoteQuery(remoteGraph1,"getShapePropertyForwardMinMultiplicity_" + suffix + ".txt",clazz,predicate,toType))
		  {
			  min = 0;
	  	}
	  	
	  	String multiplicy = getMultiplicity(min,max);
		  System.out.println("forward multiplicity: " + multiplicy);
		  localStore.add(typeLink,"RDF2Graph:forwardMultiplicity",multiplicy);
		}
		// 5.2.1.3 collect reverse multiplicity for the property
		if(this.collectReverseMultiplicity)
		{
		  System.out.println("Getting maximum reverse multiplicity");
		  int max = 0;
		  int min = 1;
		  if(suffix == "ref" || suffix == "exref" || (suffix == "simple" && toType.equals("http://www.w3.org/2001/XMLSchema#string")))
		  {
		    for (ResultLine item : this.runRemoteQuery(remoteGraph1,"getShapePropertyReverseMaxMultiplicity_" + suffix + ".txt",clazz,predicate,toType))
		    {
			    max = item.getLitInt("max");
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
	  	  for (ResultLine item : this.runRemoteQuery(remoteGraph1,"getShapePropertyReverseMinMultiplicity_ref.txt",toType,predicate,clazz))
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
		  localStore.add(typeLink,"RDF2Graph:reverseMultiplicity",multiplicy);
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
	  //Step 7.2
	  Tree tree = buildTree();
	  //Step 7.3
	  tree.calculateSubClassOfIntanceOfCount();
	  storeClassProps(tree);
	  //Step 7.4
	  for (ResultLine item : this.runLocalQuery(localStore,true,"simplify2_getAllProps.txt"))
  	{
		  String property = item.getIRI("property");
	    simplifyForProperty(tree,property);
	  }
	  //Step 7.5
	  runUpdateQueryOnce(localStore,"simplify4_removeDanglingClassProps1.txt");
	  runUpdateQueryOnce(localStore,"simplify5_removeDanglingClassProps2.txt");
	}
	
	private void simplifyForProperty(Tree tree,String property) throws Exception
	{
		//step 7.4.1: Step 1 of simplification process
		System.out.println("performing simplification for property: " + property);
		System.out.println("loading property information into the tree");
		for (ResultLine item : this.runLocalQuery(localStore,true,"simplify3_getAllClassProps.txt",property))
		{
			String source = item.getIRI("source");
			String type = item.getIRI("type");
			if(source == null || type == null)
				throw new RuntimeException("source or dest of shape property is null");
			int count = item.getLitInt("count",0);
			String forwardMultiplicity = item.getIRI("forwardMultiplicity");
			String reverseMultiplicity = item.getIRI("reverseMultiplicity");;
			TreeNode sourceNode = tree.getNode(source);
			if(sourceNode == null)
			{
				System.out.println("source node not found: " + source);
				sourceNode = tree.createMissingNode(source,tree);
			}
			sourceNode.addTemporaryLink(type,count,this.getMin(forwardMultiplicity),this.getMax(forwardMultiplicity),this.getMin(reverseMultiplicity),this.getMax(reverseMultiplicity));				
		}		
		tree.prepTemporaryLinks();
		printTemporaryLinks(tree);
		//step 7.4.2
		System.out.println("step 1");
		tree.simplifyStep2();
		printTemporaryLinks(tree);
	  //step 7.4.3
		System.out.println("step 2");
		tree.simplifyStep3();
		printTemporaryLinks(tree);
	  //step 7.4.4
		System.out.println("step 3");
		tree.simplifyStep4();
		printTemporaryLinks(tree);
		//write new properties to database
		//step 7.4.5
		storeNewUniqueTypeLinks(tree,property);
		System.out.println("clean tree");
		tree.clean();
	}
	
	private void printTemporaryLinks(Tree tree)
	{
		for(TreeNode node : tree.getAllNodes())
		{
			for(UniqueTypeLink dest : node.getTemporaryLinks())
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
			  localStore.add(clazz,"RDF2Graph:subClassOfInstanceCount",count);
		}
	}
	
	private void storeNewUniqueTypeLinks(Tree tree,String predicate) throws Exception
	{
		int count = 1;
		for(TreeNode node : tree.getAllNodes())
		{
			for(UniqueTypeLink dest : node.getTemporaryLinks())
			{
				String sourceType = node.name;
				String typeName = dest.typeName;
				String classProperty = createClassProperty(sourceType,predicate,"_n");
				String typeLink = createTypeLink(classProperty,"n",count++);
						
				localStore.add(typeLink,"RDF2Graph:type",typeName);
				localStore.add(typeLink,"RDF2Graph:count",dest.count);
			  localStore.add(typeLink,"RDF2Graph:forwardMultiplicity",this.getMultiplicity(dest.forwardMinMultiplicity,dest.forwardMaxMultiplicity));
			  localStore.add(typeLink,"RDF2Graph:reverseMultiplicity",this.getMultiplicity(dest.reverseMinMultiplicity,dest.reverseMaxMultiplicity));
			  //note them as new so they do not get deleted
			  localStore.addLit(classProperty,"RDF2Graph:noteAsNew","new");
			  
			  if(typeName.equals(localStore.expand("RDF2Graph:invalid")))
			  {
				  createDestError(typeLink,predicate,"_n");
			  }
			}
		}
	}
	
	private Tree buildTree() throws Exception
	{
		System.out.println("building the subclassOf tree");
		Tree tree = new Tree();
		for (ResultLine item : this.runLocalQuery(localStore,true,"simplify1_getTree.txt"))
		{
			String parent = item.getIRI("parent");
			String child = item.getIRI("child");
			int childCount = item.getLitInt("childCount",-2);
			int parentCount = item.getLitInt("parentCount",-2);
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
  private Iterable<ResultLine> runRemoteQuery(RDFSimpleCon graph,String queryFile,Object ... args) throws Exception
  {    
    long millis = System.currentTimeMillis();
    if(treatSubClassOfAsIntanceOf)
    	queryFile = "remoteSubClassOf/" + queryFile;
    else
    	queryFile = "remote/" + queryFile;
    Iterable<ResultLine> toRet = graph.runQuery(queryFile,true,args);
	  System.out.println("time: " + (System.currentTimeMillis() - millis) + " for query " + queryFile); 
	  return toRet;
  }
  
  private Iterable<ResultLine> runLocalQuery(RDFSimpleCon graph,boolean preload,String queryFile,Object ... args) throws Exception
  {
	  return graph.runQuery("local/" + queryFile,true,args);
  }

	private void runUpdateQueryOnce(RDFSimpleCon graph,String file,Object ...args)
	{
	  if(!graph.containsLit("RDF2Graph:status","RDF2Graph:updatePerformed",file))
	 	{
	  	System.out.println("running update query " + file);
	  	graph.runUpdateQuery("local/" + file,args);
	  	graph.addLit("RDF2Graph:status","RDF2Graph:updatePerformed",file);
	 	}
	}
	private void runUpdateQueryOnceA(RDFSimpleCon graph,String file,Object ...args)
	{
	  System.out.println("DEV: running update query " + file);
	  graph.runUpdateQuery(file,args);
	  graph.addLit("RDF2Graph:status","RDF2Graph:updatePerformed",file);
	}
}
