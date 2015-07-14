echo "Creating network based view"
if ! which Cytoscape >/dev/null ; then
    echo "Please make sure Cystoscape is available in the PATH enviroment"
    exit
fi
echo "If cytoscape opens whithout any other events occuring, please make sure to install at least one app (known cytoscape bug)"
echo "Export script is tested on cytoscape version 3.2"
echo "TIP: use the scale function in Cytoscape to improve the readability of the view of the network"
echo "TIP: enable graphical details, so edge names are always shown"
project=$1 
includeConcepts=$2
output=$3
if [[ "$1" = "-h" ||  "$1" = "--help"  || ( "$includeConcepts" != "true"  &&  "$includeConcepts" != "false") || ("$output" != "view" && "$output" != *".cys" && "$output" != *".xgmml") ]] ; then
  echo "usage"
  echo "export.sh <project> <include concept classes> <output>"
  echo "output -> 'view' opens cytoscape view"
  echo "output -> '*.cys' saves cytoscape session file to file"
  echo "output -> '*.xgmml' saves network to file and save error report to *_error.xgmml"
  echo "include concept classes -> also include all concept classes in the view (true or false)" 
  exit
fi

if [ ! -d "$project" ]; then
  echo "$project does not exists"
  exit
fi

#directory change source from http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

echo "exporting data to cytoscape files"
mkdir -p ./temp
#Get links from db
#sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' work around for some bug in cytoscape it does something special to biopax ontology, which is buggy
tdbquery --loc $project --query $DIR/queries/primarylinks.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 > ./temp/RDF2Graph.txt
tdbquery --loc $project --query $DIR/queries/secondarylinks.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/RDF2Graph.txt
tdbquery --loc $project --query $DIR/queries/subClassOffByDefLinks1.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/RDF2Graph.txt
tdbquery --loc $project --query $DIR/queries/subClassOffByDefLinks2.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/RDF2Graph.txt
if [ "$includeConcepts" = "true" ] ; then
  tdbquery --loc $project --query $DIR/queries/subClassOfLinksIncludeConceptsClasses.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/RDF2Graph.txt
else
  tdbquery --loc $project --query $DIR/queries/subClassOfLinks.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/RDF2Graph.txt
fi
PWD=$(pwd)

echo -e "name\thide" > ./temp/nodeprops.txt 
echo -e "ErrorReportHidden\thide" >> ./temp/nodeprops.txt
tdbquery --loc $project --query $DIR/queries/hideSecondaryLinks.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/nodeprops.txt 
echo -e "shared name\tname\tcount\tchild instance count\tfull iri" > ./temp/nodeprops2.txt 
tdbquery --loc $project --query $DIR/queries/getnodeproperties.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/nodeprops2.txt 
echo -e "shared name\tsource\tpredicate name\tdestination type\tforward multiplicity\treverse multiplicity\treference count\tis_simple\tfull predicate" > ./temp/edgeprops.txt 
tdbquery --loc $project --query $DIR/queries/getedgeproperties.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/edgeprops.txt 
#error report generation
tdbquery --loc $project --query $DIR/queries/errorReport1.txt --results TSV |  sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 > ./temp/errorreport.txt
echo -e "shared name\tname\thide" > ./temp/errornodeprops.txt 
echo -e "ErrorReportHidden\tError-report\thide" >> ./temp/errornodeprops.txt 
echo -e "ErrorReport\tError report\t" >> ./temp/errornodeprops.txt 
echo -e "shared name\ttype\tpredicate name\treference count\tfull predicate" > ./temp/erroredgeprops.txt 
tdbquery --loc $project --query $DIR/queries/errorReport2.txt --results TSV | sed 's/"//g' | sed 's/http:\/\/www.biopax.org\/release\/biopax-level3.owl#/http:\/\/www.biopax.org\/release\/bp-level3.owl#/g' | tail -n +2 >> ./temp/erroredgeprops.txt 


##tdbquery --loc ../tdb --query $DIR/queries/getresult3.txt  > props.txt 
##tdbquery --loc ../tdb --query $DIR/queries/getresult4.txt  >> props.txt 
##tdbquery --loc ../tdb --query $DIR/queries/getresult5.txt  >> props.txt 
echo "Creating cytoscape session file"
echo -e "network import file file="$PWD"/temp/RDF2Graph.txt indexColumnSourceInteraction=1 indexColumnTargetInteraction=3 indexColumnTypeInteraction=2" > ./temp/cytoscapebuildrun.txt
echo -e "vizmap load file file="$DIR"/RDF2Graphstyle.xml" >> ./temp/cytoscapebuildrun.txt
echo -e "vizmap apply styles=RDF2Graphstyle" >> ./temp/cytoscapebuildrun.txt
echo -e "table import file file="$PWD'/temp/nodeprops.txt DataTypeTargetForNetworkCollection="Node Table Columns" KeyColumnForMapping="name" TargetNetworkCollection="RDF2Graph.txt" TargetNetworkList="RDF2Graph.txt" WhereImportTable="To selected networks only" dataTypeTargetForNetworkList="Node Table Columns" firstRowAsColumnNames=true keyColumnIndex=1 delimiters="\\t" startLoadRow=1' >> ./temp/cytoscapebuildrun.txt
echo -e "table import file file="$PWD'/temp/nodeprops2.txt DataTypeTargetForNetworkCollection="Node Table Columns" KeyColumnForMapping="name" TargetNetworkCollection="RDF2Graph.txt" TargetNetworkList="RDF2Graph.txt" WhereImportTable="To selected networks only" dataTypeTargetForNetworkList="Node Table Columns" firstRowAsColumnNames=true keyColumnIndex=1 delimiters="\\t" startLoadRow=1' >> ./temp/cytoscapebuildrun.txt
echo -e "table import file file="$PWD'/temp/edgeprops.txt DataTypeTargetForNetworkCollection="Edge Table Columns" KeyColumnForMapping="shared name" TargetNetworkCollection="RDF2Graph.txt" TargetNetworkList="RDF2Graph.txt" WhereImportTable="To selected networks only" dataTypeTargetForNetworkList="Edge Table Columns" firstRowAsColumnNames=true keyColumnIndex=1 delimiters="\\t" startLoadRow=1' >> ./temp/cytoscapebuildrun.txt
echo -e "layout force-directed" >> ./temp/cytoscapebuildrun.txt
echo -e "network import file file="$PWD"/temp/errorreport.txt indexColumnSourceInteraction=1 indexColumnTargetInteraction=3 indexColumnTypeInteraction=2 RootNetworkList=RDF2Graph.txt" >> ./temp/cytoscapebuildrun.txt
echo -e "table import file file="$PWD'/temp/errornodeprops.txt DataTypeTargetForNetworkCollection="Node Table Columns" KeyColumnForMapping="name" TargetNetworkCollection="RDF2Graph.txt" TargetNetworkList="errorreport.txt" WhereImportTable="To selected networks only" dataTypeTargetForNetworkList="Node Table Columns" firstRowAsColumnNames=true keyColumnIndex=1 delimiters="\\t" startLoadRow=1' >> ./temp/cytoscapebuildrun.txt
echo -e "table import file file="$PWD'/temp/erroredgeprops.txt DataTypeTargetForNetworkCollection="Edge Table Columns" KeyColumnForMapping="shared name" TargetNetworkCollection="RDF2Graph.txt" TargetNetworkList="errorreport.txt" WhereImportTable="To selected networks only" dataTypeTargetForNetworkList="Edge Table Columns" firstRowAsColumnNames=true keyColumnIndex=1 delimiters="\\t" startLoadRow=1' >> ./temp/cytoscapebuildrun.txt
echo -e "vizmap apply styles=RDF2Graphstyle" >> ./temp/cytoscapebuildrun.txt
echo -e "view set current network=RDF2Graph.txt" >> ./temp/cytoscapebuildrun.txt
if [[ "$output" != "/"* ]] ; then
  output="$PWD/$output"
fi
if [[ "$output" = *".cys" ]] ; then
  echo -e "session save file=$output" >> ./temp/cytoscapebuildrun.txt
  echo -e "command quit" >> ./temp/cytoscapebuildrun.txt
fi
if [[ "$output" = *".xgmml" ]] ; then
  output2=${output/.xgmml/}"_error.xgmml"
  echo -e 'network export OutputFile="'$output'"' >> ./temp/cytoscapebuildrun.txt
  echo -e 'view set current network="errorreport.txt"' >> ./temp/cytoscapebuildrun.txt
  echo -e 'network export OutputFile="'$output2'"' >> ./temp/cytoscapebuildrun.txt
  echo -e "command quit" >> ./temp/cytoscapebuildrun.txt
fi
Cytoscape -S $(pwd)"/temp/cytoscapebuildrun.txt" 
