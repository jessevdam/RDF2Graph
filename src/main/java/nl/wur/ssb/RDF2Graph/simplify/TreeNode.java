package nl.wur.ssb.RDF2Graph.simplify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;


public class TreeNode
{
	/* The parent tree */
	private final Tree tree;
	/* The iri of the class node */
	public final String name;
	/* List of all parent classes */
	private LinkedList<TreeNode> parents = new LinkedList<TreeNode>();
	/* List of all child classes  */
	private LinkedList<TreeNode> childs = new LinkedList<TreeNode>();
	/*identify root node */
	private boolean isRoot;
	/*The temporary field used in the algoritm*/
	private HashMap<String,ShapeProperty> prepDestTypes = new HashMap<String,ShapeProperty>();
	private HashSet<ShapeProperty> destTypes = new HashSet<ShapeProperty>();
	private int markParent;
	private boolean projectStep1Done = false;
	private boolean projectStep2Done = false;
	private int subClassOffInstanceCount = -2;
	private int classInstanceCount = -2; 

  TreeNode(String name,Tree tree,int classInstanceCount)
  {
  	this.isRoot = false;
  	this.name = name;
  	this.tree = tree;
  	this.classInstanceCount = classInstanceCount;
  }
  
  TreeNode(Tree tree)
  {
    this.isRoot = true;
    this.name = "{{root}}";
  	this.tree = tree;
  }
  public int getSubClassOffInstanceCount()
  {
    return this.subClassOffInstanceCount;
  }
  
  public boolean isRoot()
  {
  	return this.isRoot;
  }
  
  void behaveAsRoot()
  {
  	this.isRoot = true;
  }
  boolean setRootIfNoParents(TreeNode root)
  {
  	if(this.parents.size() == 0)
  	{
  		this.setParent(root);
  		return true;
  	}
  	return false;
  }
  void resetMarkParent()
  {
  	this.markParent = 0;
  }
  
  void setParent(TreeNode parent)
  {
  	this.parents.add(parent);
  	parent.childs.add(this);
  }
  public void addType(String name,int count,int forwardMinMultiplicity,int forwardMaxMultiplicity,int reverseMinMultiplicity,int reverseMaxMultiplicity)
  {
  	if(this.isRoot)
  		return;
  	if(!this.destTypes.contains(name))
  	{
  	  this.prepDestTypes.put(name,new ShapeProperty(name,count,forwardMinMultiplicity,forwardMaxMultiplicity,reverseMinMultiplicity,reverseMaxMultiplicity));
  	}
  }
  void calculateSubClassOfIntanceOfCount(HashMap<String,Integer> countSet)
  {
  	if(this.subClassOffInstanceCount == -2)
  	{
  	  HashMap<String,Integer> myCountSet = new HashMap<String,Integer>();
  	  for(TreeNode child : this.childs)
  	  {
        child.calculateSubClassOfIntanceOfCount(myCountSet);
  	  }
  	  int totalCount = -1;
  	  for(int val : myCountSet.values())
  	  {
  	   	if(val != -1)
  		  {
  			  if(totalCount == -1)
  				  totalCount = 0;
  			  totalCount += val;
  		  }
  	  }
  	  countSet.putAll(myCountSet);
  	  this.subClassOffInstanceCount = totalCount;
  	}
	  if(this.classInstanceCount != -2)
	  {
	  	countSet.put(this.name,this.classInstanceCount);
	  }
  }
  
  public void prepareDestType()
  {
  	this.destTypes.addAll(this.prepDestTypes.values());
  }
  
  public ShapeProperty[] getResDestTypes()
  {
  	return (ShapeProperty[])this.destTypes.toArray(new ShapeProperty[0]);
  }
  
  void clean()
  {
  	this.prepDestTypes.clear();
  	destTypes.clear();
  	projectStep1Done = false;
  	projectStep2Done = false;
  	markParent = 0;
  }
  //Combine from upper level to lower level
  void projectDownStep1(HashSet<TreeNode> visited) throws Exception
  {
  	if(projectStep1Done)
  		return;
  	projectStep1Done = true;
  	if(visited.contains(this))
  		throw new Exception("cycle in rdfs:subClassOf hiearchy found");
  	visited.add(this);
  	for(TreeNode child : this.childs)
  	{
  		child.projectDownStep1(visited);
  		if(this.isRoot)
  			continue;
  		this.destTypes.addAll(child.destTypes);
   	}
 		boolean changed = true;
		outer:while(changed)
		{
			changed = false;
		  for(ShapeProperty dest1 : destTypes)
		  {
		  	for(ShapeProperty dest2 : destTypes)
			  {
		  		if(dest1 == dest2)
		  			continue;
				  String common = tree.findCommonParent(dest1.typeName,dest2.typeName);
				  if(common != null)
				  {
				  	//destTypes.remove(dest1);
				  	destTypes.remove(dest2);  				 
				  	dest1.combineWith(dest2,common);
				  	//destTypes.add(dest1);
				  	changed = true;
				  	continue outer;
				  }
			  }
		  }
		}
  	visited.remove(this);
  }
  //Remove 'none splitting' elements
  LinkedList<TreeNode> projectDownStep2() 
  {
  	//If one of the parents is not processed yet then re-add it to the end of the list
  	for(TreeNode parent : this.parents)
  	{
  		if(parent.projectStep2Done == false)
  		{
  			LinkedList<TreeNode> toRet = new LinkedList<TreeNode>();
  			toRet.add(this);
  			return toRet;
  		}
  	}
  	LinkedList<ShapeProperty> toRemove = new LinkedList<ShapeProperty>();
    for(ShapeProperty dest : destTypes)
    {
    	int count = 0;
      for(TreeNode child : this.childs)
      {
      	for(ShapeProperty destChild : child.destTypes)
      	{
      		if(this.tree.isParentOf(dest.typeName,destChild.typeName))
      			count++;
      	}
      }
      if(count == 1) //count != 0 || count < 2
      {
      	toRemove.add(dest);
      }
    }
    this.destTypes.removeAll(toRemove);
    this.projectStep2Done = true;
    return this.childs;
  }
  //Remove element that are already by parent node
  void projectDownStep3(HashSet<ShapeProperty> parentsContents) 
  {
  	LinkedList<ShapeProperty> toRemove = new LinkedList<ShapeProperty>();
    for(ShapeProperty dest : destTypes)
    {
    	for(ShapeProperty parentContent : parentsContents)
    	{
    		if(tree.isParentOf(parentContent.typeName,dest.typeName))
    		{
    			toRemove.add(dest);
    			break;
    		}
    	}
    } 	
    this.destTypes.removeAll(toRemove);
    HashSet<ShapeProperty> copy = (HashSet<ShapeProperty>)parentsContents.clone();
    copy.addAll(this.destTypes);
  	for(TreeNode child : this.childs)
  	{
  	  child.projectDownStep3(copy);
  	}  
  }
  

  void markParentList1(int count)
  {
  	this.markParent = count++;
  	for(TreeNode parent : this.parents)
  	{
  		parent.markParentList1(count);
  	}
  }
  //Search for the hihgest possible common ancestor
  TreeNode findCommon()
  {
  	int max = Integer.MAX_VALUE;
  	TreeNode best = null;
  	LinkedList<TreeNode> temp = new LinkedList<TreeNode>();
  	temp.addAll(this.parents);
  	temp.add(this);
  	for(TreeNode parent : temp)
  	{
  		//exlude the root item we do not want to simplify down to root item
  		if(parent.isRoot)
  			continue;
  		int checkMax = Integer.MAX_VALUE;
  		TreeNode candidate = null;
  		if(parent.markParent != 0)
  		{
  			checkMax = parent.markParent;
  			candidate = parent;
  		}
  		else if(parent != this)
  		{
  			candidate = parent.findCommon();
  			if(candidate != null)
  		    checkMax = candidate.markParent;
  		}
  		if(candidate != null && checkMax < max)
  		{
  			best = candidate;
  			max = checkMax;
  		}
  	}
  	return best;
  }
  
  boolean hasParent(TreeNode parent)
  {
  	if(this == parent)
  		return true;
  	for(TreeNode checkParent : this.parents)
  	{
  		if(checkParent.hasParent(parent))
  			return true;
  	}
  	return false;
  }
  
  public String toString()
  {
  	return this.name;// + " | " + this.childs.toString();
  }
  
  public int hashCode()
  {
  	return this.name.hashCode();
  }
  
  public boolean equals(Object other)
  {
  	return this.name.equals(((TreeNode)other).name);
  }  
}

