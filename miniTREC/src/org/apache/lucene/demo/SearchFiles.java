package org.apache.lucene.demo;

import java.io.File;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class SearchFiles {

  private SearchFiles() {}

  public static void main(String[] args) throws Exception {
    String usage = "Uso:\tjava SearchFiles "
                 + "-index <indexPath> "
                 + "-infoNeeds <infoNeedsFile> "
                 + "-output <resultsFile>";

    String index = null, infoNeeds = null, output = null;
    for(int i=0; i<args.length; i++) {
      if ("-index".equals(args[i])) {
        index = args[++i];
      } else if ("-infoNeeds".equals(args[i])) {
        infoNeeds = args[++i];
      } else if ("-output".equals(args[i])) {
        output = args[++i];
      }
    }

    if(index == null || infoNeeds == null || output == null || 
        "-h".equals(args[0]) || "-help".equals(args[0]) ||
        args.length != 6){

      System.out.println(usage);
      System.exit(0);
    }

    // Check index's path
    final File file = new File(index);
    if (!file.exists() || !file.canRead()) {
      System.out.println("El directorio '" +file.getAbsolutePath()+ "' no existe o no se puede leer.");
      System.exit(1);
    }

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new SpanishAnalyzer2();
    LanguageParser languageParser = new LanguageParser(infoNeeds, output);

    while(languageParser.nextNeed()){
      System.out.println("Buscando necesidad '" + languageParser.getIdNeed() + "'': " 
                          + languageParser.getStringQuery());
      Date start = new Date();
      TopDocs results = searcher.search(languageParser.getBooleanQuery(), Math.max(1, reader.maxDoc()));
      Date end = new Date();
      System.out.println("Tiempo final: " + (end.getTime() - start.getTime())/1000 + "seg");        

      ScoreDoc[] hits = results.scoreDocs;
      int numTotalHits = Math.toIntExact(results.totalHits.value);
      System.out.println(numTotalHits + " ficheros encontrados.");

      languageParser.writeResults(searcher, hits);
    }

    reader.close();
  }

}
