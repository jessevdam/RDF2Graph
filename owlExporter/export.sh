echo "Creating owl file"
project=$1 
includerev=$2
outfile=$3
if [[ "$1" = "-h" ||  "$1" = "--help"  ||  "$outfile" = "" || ( "$includerev" != "true"  &&  "$includerev" != "false")]] ; then
  echo "usage"
  echo "export.sh <project> <include reverse cardinality(true,false)> <outfile>"
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
cd -P $DIR
mkdir -p ./temp

#Get links from db
tdbquery --loc $project --query queries/step1_predicates_domain_1.txt --results N3  > ./temp/owl.n3
tdbquery --loc $project --query queries/step1_predicates_domain_2plus.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step1_predicates_range_ObjectProperty_1.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step1_predicates_range_ObjectProperty_2plus.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step1_predicates_range_DataTypeProperty_1.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step1_predicates_range_DataTypeProperty_2plus.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step2_classes.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step3_subclasses.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step4_classproperty_1.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step4_classproperty_2plus.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step5_linkref.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step6_forwardmultiplicityexact.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step6_forwardmultiplicitymax.txt --results N3  >> ./temp/owl.n3
tdbquery --loc $project --query queries/step6_forwardmultiplicitymin.txt --results N3  >> ./temp/owl.n3
if [ "$includerev" = "true" ] ; then
  tdbquery --loc $project --query queries/step7_reversemultiplicityexact.txt --results N3  >> ./temp/owl.n3
  tdbquery --loc $project --query queries/step7_reversemultiplicitymax.txt --results N3  >> ./temp/owl.n3
  tdbquery --loc $project --query queries/step7_reversemultiplicitymin.txt --results N3  >> ./temp/owl.n3
fi
rm -r ./temp/tempdb
tdbloader -loc ./temp/tempdb --graph=http://ssb.wur.nl/owlExporter ./temp/owl.n3
tdbupdate -loc ./temp/tempdb --update queries/removeSubClassOfThing.txt
java -cp ../target/RDF2Graph-0.1-jar-with-dependencies.jar nl.wur.ssb.RDF2Graph.owlExporter.Main ./temp/tempdb
tdbquery --loc ./temp/tempdb --query queries/selectAll.txt --results N3  > $outfile
echo "export completed, you can use protoge to read file"