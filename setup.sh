git clone https://github.com/jessevdam/RDFSimpleCon
git pull
git -C RDFSimpleCon/ pull
git branch | sed -n '/\* /s///p' | xargs git -C RDFSimpleCon/ checkout 
cd RDFSimpleCon
mvn install
cd ..
mvn install
cp ./target/RDF2Graph-0.1-jar-with-dependencies.jar ./RDF2Graph.jar
cd shexExporter
npm install async commander jade lodash