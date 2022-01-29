package org.apache.lucene.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

public class SemanticSearcher {
   
   private SemanticSearcher() {}
	
	public static void main(String[] args) throws IOException {
      String usage = "Uso:\tjava SemanticSearcher "
						 + "-rdf <rdfPath> "
						 + "-infoNeeds <infoNeedsFile> "
						 + "-output <resultsFile>\n";
	                 
	   String rdfPath = null, infoNeedsFile = null, resultsFile = null;
		for(int i=0; i<args.length; i++) {
			switch(args[i]){
				case "-rdf":
					rdfPath = args[++i];
					break;
				case "-infoNeeds":
               infoNeedsFile = args[++i];
					break;
				case "-output":
               resultsFile = args[++i];
					break;
			}
		}

		if(rdfPath == null || infoNeedsFile == null || resultsFile == null ||
				"-h".equals(args[0]) || "-help".equals(args[0]) || args.length != 6){

			System.out.println(usage);
			System.exit(0);
		}

		String[] aux = {rdfPath, infoNeedsFile};
		for(String item : aux){
			// Check paths
			final File file = new File(item);
			if (!file.exists() || !file.canRead()) {
				System.out.println("El directorio o fichero '" +file.getAbsolutePath()+ "' no existe o no se puede leer.");
				System.exit(1);
			}
		}

		Model collectionModel = RDFDataMgr.loadDataset(RDFDataMgr.open(rdfPath).getBaseURI()).getDefaultModel();

		try (
		// Si el fichero de resultados ya existe, lo reescribe
		FileWriter fileWriter = new FileWriter(resultsFile)) {
			Scanner scanner = new Scanner(new File(infoNeedsFile));
			while(scanner.hasNextLine()){
				String need = scanner.nextLine();
				String idNeed = need.split("\\t")[0];
				String infoNeed = need.split("\\t")[1];

				Query query = QueryFactory.create(infoNeed);
				QueryExecution queryExecution = QueryExecutionFactory.create(query, collectionModel);
				try{
					ResultSet results = queryExecution.execSelect();
					while (results.hasNext()) {
						QuerySolution result = results.nextSolution();
						String path = result.getResource("x").getURI();
						String idFile = path.substring(path.lastIndexOf("/")+1);
							
						fileWriter.write(idNeed + "\toai_zaguan.unizar.es_" + idFile + ".xml\n");
					}
				} finally { queryExecution.close(); }			
			}
		}
   }
}
