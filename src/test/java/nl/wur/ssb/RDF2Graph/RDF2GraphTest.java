package nl.wur.ssb.RDF2Graph;

import junit.framework.TestCase;
import nl.wur.ssb.RDFSimpleCon.RDFSimpleCon;
import nl.wur.ssb.RDFSimpleCon.Util;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

public class RDF2GraphTest extends TestCase
{
  public RDF2GraphTest( String testName )
  {
      super( testName );
      BasicConfigurator.configure();
  }
  
  @Test
  public void testParsing() throws Exception
  {
  	//TODO busy adding testcases
  	//RDFSimpleCon con = new RDFSimpleCon(RDF2GraphTest.class.getResource("labels.tsv").toString());
  	//con.save("test.ttl");
  	//Util.
    //System.out.println(Util.readFile("test.tsv"));
  }
}
