package org.apache.lucene.demo;

import java.io.File;
import java.io.IOException;

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
      

   }
}
