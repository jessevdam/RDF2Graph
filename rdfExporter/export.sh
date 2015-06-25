echo "Creating rdf turtle file"
project=$1 
outfile=$2
if [[ "$1" = "-h" ||  "$1" = "--help"  ||  "$outfile" = "" ]] ; then
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
cd -P $DIR
mkdir -p ./temp

#Get links from db
tdbquery --loc $project --query queries/all.txt --results N3 > $outfile
echo "export completed"