package org.apache.lucene.demo;

import org.apache.lucene.document.*;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.SKOS; 
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

// Clase que contiene el programa de transformación de la colección XML en RDF
public class SemanticGenerator {
  
	private SemanticGenerator() {}
	
	public static void main(String[] args) throws IOException {
		String usage = "Uso:\tjava SemanticGenerator "
						 + "-rdf <rdfPath> " // rdfPath --> debería ser RDF.ttl
						 + "-skos <skosPath> "
						 + "-owl <owlPath> "
						 + "-docs <docsPath>\n";
	                 
	   String rdfPath = null, skosPath = null, owlPath = null, docsPath = null;
		for(int i=0; i<args.length; i++) {
			switch(args[i]){
				case "-rdf":
					rdfPath = args[++i];
					break;
				case "-skos":
					skosPath = args[++i];
					break;
				case "-owl":
					owlPath = args[++i];
					break;
				case "-docs":
					docsPath = args[++i];
					break;
			}
		}

		if(rdfPath == null || skosPath == null || owlPath == null || docsPath == null ||
				"-h".equals(args[0]) || "-help".equals(args[0]) || args.length != 8){

			System.out.println(usage);
			System.exit(0);
		}

		String[] aux = {skosPath, owlPath, docsPath};
		for(String item : aux){
			// Check paths
			final File file = new File(item);
			if (!file.exists() || !file.canRead()) {
				System.out.println("El directorio o fichero '" +file.getAbsolutePath()+ "' no existe o no se puede leer.");
				System.exit(1);
			}
		}		
		
		Date start = new Date();
		
		// Generar RDF de la coleccion
		Model collectionModel = generateCollectionModel(docsPath);
		
		Date end = new Date();
		System.out.println((end.getTime() - start.getTime())/1000.0 + " seg");
		
		start = new Date();
		
		// Inferencia RDFS
		Model owlModel = RDFDataMgr.loadDataset(RDFDataMgr.open(owlPath).getBaseURI()).getDefaultModel();
		Model skosModel = RDFDataMgr.loadDataset(RDFDataMgr.open(skosPath).getBaseURI()).getDefaultModel();

		// Estas funciones estan deprecated, no utilizar
		// Model owlModel =  FileManager.get().loadModel(owlPath,"RDF/XML-ABBREV");
		// Model skosModel = FileManager.get().loadModel(skosPath,"RDF/XML-ABBREV");

		Model resultModel = ModelFactory.createUnion(ModelFactory.createUnion(collectionModel, owlModel), skosModel);
		InfModel inf = ModelFactory.createRDFSModel(resultModel);
		
		end = new Date();
		System.out.println((end.getTime() - start.getTime())/1000.0 + " seg");

		FileWriter rdf_file = new FileWriter(rdfPath);
		inf.write(rdf_file, "TURTLE"); // Creación del RDF.ttl
	}

	// Adaptación del fichero IndexFiles
	static Model generateCollectionModel(String docsPath) throws IOException{

		Model collectionModel = ModelFactory.createDefaultModel();
		final File docs_input = new File(docsPath);

		if (docs_input.canRead()) {
			if (docs_input.isDirectory()) {
				String[] files = docs_input.list();
				// an IO error could occur
				if (files != null) {
					for (int i=0; i<files.length; i++) {
						try {
							// make a new, empty document
							Document doc = new Document();

							// Add the path of the file as a field named "path".  Use a
							// field that is indexed (i.e. searchable), but don't tokenize 
							// the field into separate words and don't index term frequency
							// or positional information:
							Field pathField = new StringField("path", docs_input.getPath(), Field.Store.YES);
							doc.add(pathField);
			  
							// Add the last modified date of the file a field named "modified".
							// Use a StoredField to return later its value as a response to a query.
							// This indexes to milli-second resolution, which
							// is often too fine.  You could instead create a number based on
							// year/month/day/hour/minutes/seconds, down the resolution you require.
							// For example the long value 2011021714 would mean
							// February 17, 2011, 2-3 PM.
							doc.add(new StoredField("modified", docs_input.lastModified()));
			  
							//we get the DOM tree out of the file
							DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
							DocumentBuilder docBuild = documentBuilderFactory.newDocumentBuilder();
							org.w3c.dom.Document docTree = docBuild.parse(docs_input + "/" + files[i]);

							// Add the contents of the file to a field named "contents".  Specify a Reader,
							// so that the text of the file is tokenized and indexed, but not stored.
							// Note that FileReader expects the file to be in UTF-8 encoding.
							// If that's not the case searching for special characters will fail.
							AddAllFields(doc, docTree);
									
							String identifier = StringUtils.stripAccents(doc.get("dc:identifier"));
							String title = StringUtils.stripAccents(doc.get("dc:title"));
							String[] contributors = doc.getValues("dc:contributor");
							String[] subjects = doc.getValues("dc:subject");
							String type = StringUtils.stripAccents(doc.get("dc:type"));
							String description = StringUtils.stripAccents(doc.get("dc:description"));
							String creator = StringUtils.stripAccents(doc.get("dc:creator"));
							String publisher = StringUtils.stripAccents(doc.get("dc:publisher"));
							String language = StringUtils.stripAccents(doc.get("dc:language"));
							String date = StringUtils.stripAccents(doc.get("dc:date"));
							String relation = StringUtils.stripAccents(doc.get("dc:relation"));
							String rights = StringUtils.stripAccents(doc.get("dc:rights"));
							
							/*
							* Creación de propiedades
							*/ 
							
							String prefix_mv = "http://www.example.org/#";
							
							// Identifier
							Resource docResource = collectionModel.createResource(identifier)
									.addProperty(DCTerms.identifier, identifier);

							// Title
							Property titleProperty = collectionModel.createProperty(prefix_mv+"title");
							docResource.addProperty(titleProperty, title);

							// Contributor
							try{
							Property contributorProperty = collectionModel.createProperty(prefix_mv+"contributor");
							for (String contributor : contributors) {
								contributor = StringUtils.stripAccents(contributor);
								Resource contributorResource = collectionModel.createResource(
										StringUtils.stripAccents(contributor));								
								// A veces el campo del nombre no está separado por ','
								String firstName = "", familyName = "";
								if(contributor.contains(",")){
									firstName = contributor.split(",")[1].trim();
									familyName = contributor.split(",")[0].trim();
								}
								else{
									String[] nomCompleto = contributor.split("\\s");
									switch(nomCompleto.length){
										case 2:
											firstName = nomCompleto[0];
											familyName = nomCompleto[1];
											break;
										case 3:
											firstName = nomCompleto[0];
											familyName = nomCompleto[1] +""+ nomCompleto[2];
											break;
										case 4:
											firstName = nomCompleto[0] +""+ nomCompleto[1];
											familyName = nomCompleto[2] +""+ nomCompleto[3];
											break;
									}
								}
								contributorResource.addProperty(FOAF.firstName, firstName);
								contributorResource.addProperty(FOAF.familyName, familyName);
								docResource.addProperty(contributorProperty, contributorResource);
							}
							} catch(Exception e){
								System.err.println("hola");
							}

							// Subject
							Property subjectProperty = collectionModel.createProperty(prefix_mv+"subject");
							for (String subject : subjects) {
								subject = StringUtils.stripAccents(subject);
								Resource subjectResource = collectionModel.createResource(
										StringUtils.stripAccents(subject));

								subjectResource.addProperty(SKOS.prefLabel, subject);
								docResource.addProperty(subjectProperty, subjectResource);

								// insertSubject(docResource, NS, collectionModel, skosModel, subject);
							}
							
							String docType;
							if (type.contains("tesis")) docType = "tesis";
							else if (type.contains("tfm")) docType = "tfm";
							else if (type.contains("tfg")) docType = "tfg";
							else docType = "pfc";
							
							// TODO: un resource por cada tipo??
							Resource typeResource = collectionModel.createResource(prefix_mv+docType);
							docResource.addProperty(RDF.type, typeResource);
							
							// Description
							if(description != null){
								Property descriptionProperty = collectionModel.createProperty(prefix_mv+"description");
								docResource.addProperty(descriptionProperty, StringUtils.stripAccents(description));
							}

							// Creator
							Property creatorProperty = collectionModel.createProperty(prefix_mv+"creator");
							Resource creatorResource = collectionModel.createResource(creator);
							// A veces el campo del nombre no está separado por ','
							String firstName = "", familyName = "";
							if(creator.contains(",")){
								firstName = creator.split(",")[1].trim();
								familyName = creator.split(",")[0].trim();
							}
							else{
								String[] nomCompleto = creator.split("\\s");
								switch(nomCompleto.length){
									case 2:
										firstName = nomCompleto[0];
										familyName = nomCompleto[1];
										break;
									case 3:
										firstName = nomCompleto[0];
										familyName = nomCompleto[1] +""+ nomCompleto[2];
										break;
									case 4:
										firstName = nomCompleto[0] +""+ nomCompleto[1];
										familyName = nomCompleto[2] +""+ nomCompleto[3];
										break;
								}
							}
							creatorResource.addProperty(FOAF.firstName, firstName);
							creatorResource.addProperty(FOAF.familyName, familyName);
							docResource.addProperty(creatorProperty, creatorResource);

							// Publisher
							Property publisherProperty = collectionModel.createProperty(prefix_mv+"publisher");
							docResource.addProperty(publisherProperty, publisher);

							// Language
							Property languageProperty = collectionModel.createProperty(prefix_mv+"language");
							Resource languageResource = collectionModel.createResource()
									.addProperty(DCTerms.language, language);
							docResource.addProperty(languageProperty, languageResource);

							// Date
							if(date != null){
								Property dateProperty = collectionModel.createProperty(prefix_mv+"date");
								Resource dateResource = collectionModel.createResource()
										.addProperty(DCTerms.date, date);
								docResource.addProperty(dateProperty, dateResource);
							}

							// Relation
							Property relationProperty = collectionModel.createProperty(prefix_mv+"relation");
							Resource relationResource = collectionModel.createResource(relation);
							relationResource.addProperty(DCTerms.relation, relation);
							docResource.addProperty(relationProperty, relationResource);

							// Rights
							if(rights != null){
								Property rightsProperty = collectionModel.createProperty(prefix_mv+"rights");
								Resource rightsResource = collectionModel.createResource(rights);
								rightsResource.addProperty(DCTerms.rights, rights);
								docResource.addProperty(rightsProperty, rightsResource);
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return collectionModel;
	}

	private static void AddAllFields(Document doc, org.w3c.dom.Document docTree) {
		NodeList nodeList = docTree.getElementsByTagName("*");
		for (int i=0; i<nodeList.getLength(); i++) {
			org.w3c.dom.Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				doc.add(new TextField(node.getNodeName(), node.getTextContent(), Field.Store.YES));
			}
		}
	}

// 	private void insertSubject(Resource document, String NS, Model model, Model thesaurus, String s) {
// 		s= s.replace("'", "");
// 		s= s.replace("\n", "").toLowerCase(Locale.ROOT);
// 		String queryString =
// 				  "prefix skos: <http://www.w3.org/2004/02/skos/core#>" +
// 							 "prefix s: <http://www.grupo202.com/skos/subjects#>" +
// 							 "select ?parentConcept ?parentLabel WHERE {" +
// 								  "{ ?childConcept skos:prefLabel '" + s + "' } . " +
// 								  "{ ?childConcept skos:broader ?parentConcept } . " +
// 								  "{?parentConcept skos:prefLabel ?parentLabel} " +
// 							 "}";

// 		Query q1 = QueryFactory.create(queryString) ;

// 		try (QueryExecution qexec = QueryExecutionFactory.create(q1, thesaurus)) {
// 			 ResultSet results = qexec.execSelect();
// 			 while (results.hasNext()) {
// 				  QuerySolution soln = results.nextSolution();
// 				  Resource x = soln.getResource("parentConcept");
// 				  RDFNode z = soln.get("parentLabel");

// 				  Resource newResource = model.createResource(SKOS.Concept);
// 				  Resource resourceToInsert = model.createResource(NS + x.getURI());
// 				  resourceToInsert.addProperty(RDF.type, newResource);

// 				  resourceToInsert.addProperty(SKOS.prefLabel, model.createLiteral(z.toString().toLowerCase(Locale.ROOT)));
// 				  document.addProperty(model.getProperty(NS + SUBJECT_ID), resourceToInsert);


// 			 }
// 		}
//   }
}