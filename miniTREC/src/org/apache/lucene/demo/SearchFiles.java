package org.apache.lucene.demo;

import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class SearchFiles {

  private SearchFiles() {}

  public static void main(String[] args) throws Exception {
    String usage = "Usage:\tjava SearchFiles -index <indexPath> " + 
                   "-infoNeeds <infoNeedsFile> -output <resultsFile>";
    if (args.length != 6 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

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

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);

    LanguageParser languageParser = new LanguageParser(infoNeeds, output);

    while(languageParser.nextNeed()){
      System.out.println("Buscando necesidad " + languageParser.getIdNeed() + ": " 
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
