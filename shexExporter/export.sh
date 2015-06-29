echo "Creating SHEX file"
project=$1 
outfile=$2
if [[ "$1" = "-h" ||  "$1" = "--help" || "$#" = "0" || "$outfile" = "" ]] ; then
  echo "usage"
  echo "export.sh <project> <outfile>"
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

echo "exporting"
rm -rf ./temp
mkdir temp
tdbquery --loc $project --query $DIR/queries/all.txt --results N3 > temp/temp1.n3
tdbquery --loc $project --query $DIR/queries/createRoot.txt --results N3 > temp/temp2.n3
tdbloader -loc ./temp/tempdb --graph=http://ssb.wur.nl/shexExporter temp/temp1.n3
tdbloader -loc ./temp/tempdb --graph=http://ssb.wur.nl/shexExporter temp/temp2.n3
tdbupdate -loc ./temp/tempdb --update $DIR/queries/removeSubClassOfThing.txt
#hack not to use to much memory
while [[ $(tdbquery -loc ./temp/tempdb/ --query $DIR/queries/removeSubClassOfMeshDone.txt | grep '[01]' |sed 's/.*\([01]\).*/\1/') -eq 1 ]] ; do
  tdbupdate -loc ./temp/tempdb --update $DIR/queries/removeSubClassOfMesh.txt
  echo "."
done
tdbdump --loc ./temp/tempdb > temp/temp.n4
$DIR/jsonconvert.js convert ./temp/temp.n4  > temp/out.json
$DIR/jsonconvert.js compact temp/out.json -c $DIR/context.json > temp/compact.json
$DIR/jsonconvert.js frame temp/compact.json -f $DIR/frame.json > temp/result.json

$DIR/shexbuilder.js
html2text -width 1000 temp/out.html > $outfile