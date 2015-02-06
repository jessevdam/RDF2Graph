package nl.wur.ssb.RDF2Graph.simplify;

public class ShapeProperty
{
	public String typeName;
	public int count;
  public int forwardMinMultiplicity;
  public int forwardMaxMultiplicity;
  public int reverseMinMultiplicity;
  public int reverseMaxMultiplicity;
	
	public ShapeProperty(String destName,int count,int forwardMinMultiplicity,int forwardMaxMultiplicity,int reverseMinMultiplicity,int reverseMaxMultiplicity)
	{
		this.typeName = destName;
	  this.count = count;
	  this.forwardMinMultiplicity = forwardMinMultiplicity;
	  this.forwardMaxMultiplicity = forwardMaxMultiplicity;
	  this.reverseMinMultiplicity = reverseMinMultiplicity;
	  this.reverseMaxMultiplicity = reverseMaxMultiplicity;
	}
	
	public void combineWith(ShapeProperty other,String newDestName)
	{
		this.typeName = newDestName;
		this.count += other.count;
		assert(this.forwardMinMultiplicity >= 0 && this.forwardMaxMultiplicity >= 0);
		assert(other.forwardMinMultiplicity >= 0 && other.forwardMaxMultiplicity >= 0);
		this.forwardMinMultiplicity = Math.min(this.forwardMinMultiplicity,other.forwardMinMultiplicity);
		this.forwardMaxMultiplicity = Math.max(this.forwardMaxMultiplicity,other.forwardMaxMultiplicity);
		assert((this.reverseMaxMultiplicity >= 0 && other.reverseMaxMultiplicity >= 0) || (other.reverseMaxMultiplicity == -1 && this.reverseMaxMultiplicity == -1));
		assert((this.reverseMinMultiplicity >= 0 && other.reverseMinMultiplicity >= 0) || (other.reverseMinMultiplicity == -1 && this.reverseMinMultiplicity == -1));
		this.reverseMinMultiplicity = Math.min(this.reverseMinMultiplicity,other.reverseMinMultiplicity);
		this.reverseMaxMultiplicity = Math.max(this.reverseMaxMultiplicity,other.reverseMaxMultiplicity);
	}
}
