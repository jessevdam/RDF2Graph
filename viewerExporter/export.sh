rm -r ./temp
mkdir temp
tdbquery --loc ../tdb --query queries/getnetquery1.txt --results N3 > temp/temp1.n3
tdbquery --loc ../tdb --query queries/getnetquery2.txt --results N3 > temp/temp2.n3
tdbquery --loc ../tdb --query queries/getnetquery3.txt --results N3 > temp/temp3.n3
tdbloader -loc ./temp/tempdb --graph=http://ssb.wur.nl/destructviewer temp/temp1.n3
tdbloader -loc ./temp/tempdb --graph=http://ssb.wur.nl/destructviewer temp/temp2.n3
tdbloader -loc ./temp/tempdb --graph=http://ssb.wur.nl/destructviewer temp/temp3.n3
tdbupdate -loc ./temp/tempdb --update queries/getnetquery4.txt
tdbupdate -loc ./temp/tempdb --update queries/getnetquery5.txt
tdbdump --loc ./temp/tempdb > temp/temp.n4
./jsonconvert.js convert ./temp/temp.n4  > temp/out.json
./jsonconvert.js compact temp/out.json -c context.json > temp/compact.json
./jsonconvert.js frame temp/compact.json -f frame.json > temp/result.json
cp temp/result.json /home/jesse/code/ruby/destructviewer/data/framed.json