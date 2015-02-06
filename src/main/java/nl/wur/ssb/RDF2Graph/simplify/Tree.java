package nl.wur.ssb.RDF2Graph.simplify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;


public class Tree
{
	private HashMap<String,TreeNode> nodes = new HashMap<String,TreeNode>();
	private LinkedList<TreeNode> allNodes = new LinkedList<TreeNode>();
	private TreeNode root;
	
  public Tree()
  {

  }
  
  public void prepareDestType()
  {
  	for(TreeNode node : nodes.values())
  	{
  		node.prepareDestType();
  	}
  }
  
  public void projectDownStep1() throws Exception
  {
  	this.root.projectDownStep1(new HashSet<TreeNode>());
  }
  
  public void projectDownStep2()
  {
  	LinkedList<TreeNode> todo = new LinkedList<TreeNode>();
  	todo.addAll(this.root.projectDownStep2());
  	while(!todo.isEmpty())
  	{
  		todo.addAll(todo.removeFirst().projectDownStep2()); 	
  	}
  }
  
  public void projectDownStep3()
  {
		this.root.projectDownStep3(new HashSet<ShapeProperty>());
  }
  
  public TreeNode getNode(String node)
  {
  	return this.nodes.get(node);
  }
  
  public TreeNode createMissingNode(String node,Tree tree)
  {
  	TreeNode toRet = this.getCreateNode(node,-2);
  	toRet.setParent(tree.root);
  	return toRet;
  }
  
  private TreeNode getCreateNode(String node,int count)
  {
  	if(nodes.containsKey(node))
  		return nodes.get(node);
  	TreeNode toRet = new TreeNode(node,this,count);
  	nodes.put(node,toRet);
  	allNodes.add(toRet);
  	return toRet;
  }
  
  public void buildLink(String parent,int parentCount,String child,int childCount)
  {
  	TreeNode parentNode = getCreateNode(parent,parentCount);
  	TreeNode childNode = getCreateNode(child,childCount);
  	childNode.setParent(parentNode);
  }
  
  public void finish() 
  {
  	root = new TreeNode(this);
  	for(TreeNode node : nodes.values())
  	{
  		node.setRootIfNoParents(root);
  	}
  	this.allNodes.add(this.root);
  	TreeNode classNode = this.nodes.get("http://www.w3.org/2002/07/owl#Thing");           
   	if(classNode != null) //TODO not for rdfs schema properties
  		classNode.behaveAsRoot();
  }
  
  public void calculateSubClassOfIntanceOfCount()
  {
  	root.calculateSubClassOfIntanceOfCount(new HashMap<String,Integer>());
  }
  
  public String findCommonParent(String node1,String node2)
  {
  	if(node1.equals(node2))
  		return node1;
  	TreeNode treeNode1 = this.getNode(node1);
  	TreeNode treeNode2 = this.getNode(node2);
  	if(treeNode1 == null || treeNode2 == null)
  		return null;
  	//reset the state
  	for(TreeNode node : this.allNodes)
  	{
  		node.resetMarkParent();
  	}
  	treeNode1.markParentList1(1);
  	TreeNode toRet = treeNode2.findCommon();
  	if(toRet != null)
  		return toRet.name;
  	return null;  	
  }
  
  //Check if child is child of parent
  boolean isParentOf(String parent,String child)
  {
  	if(parent.equals(child))
  		return true;
  	TreeNode treeNodeParent = this.getNode(parent);
  	TreeNode treeNodeChild = this.getNode(child);
  	if(treeNodeParent == null || treeNodeChild == null)
  		return false;
  	return treeNodeChild.hasParent(treeNodeParent);
  }
  
  public void clean()
  {
  	for(TreeNode node : this.allNodes)
  	{
  		node.clean();
  	}
  	this.root.clean();
  }
  
  public LinkedList<TreeNode> getAllNodes()
  {
  	return this.allNodes;
  }
}

