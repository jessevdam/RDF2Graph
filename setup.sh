git clone https://github.com/jessevdam/RDFSimpleCon
cd RDFSimpleCon
mvn install
cd ..
mvn install
cp ./target/RDF2Graph-0.1-jar-with-dependencies.jar ./RDF2Graph.jar
cd shexExporter
npm install async commander jade lodash