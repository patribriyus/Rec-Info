package org.apache.lucene.demo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Paths;
import java.util.Date;

/* Index all text files under a directory. */
public class IndexFiles {

  private IndexFiles() {}

  public static void main(String[] args) {
    String usage = "Uso:\tjava IndexFiles "
                 + "-index <indexPath>"
                 + "-docs <docsPath>";

    String indexPath = null, docsPath = null;
    for(int i=0; i<args.length; i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[++i];
      } else if ("-docs".equals(args[i])) {
        docsPath = args[++i];
      }
    }

    if(indexPath == null || docsPath == null || "-h".equals(args[0]) || 
        "-help".equals(args[0]) || args.length != 4){

      System.out.println(usage);
      System.exit(0);
    }

    String[] aux = {indexPath, docsPath};
    for(String item : aux){
      // Check paths
      final File file = new File(item);
      if (!file.exists() || !file.canRead()) {
        System.out.println("El directorio '" +file.getAbsolutePath()+ "' no existe o no se puede leer.");
        System.exit(1);
      }
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexando al directorio '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new SpanishAnalyzer2();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      // Create a new index/add documents to an existing index
      iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      // iwc.setRAMBufferSizeMB(256.0);


      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, new File(docsPath));

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println((end.getTime() - start.getTime())/1000.0 + " seg");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(IndexWriter writer, File file)
    throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {  //file is an XML file

        FileInputStream fis;
        try {
          fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
          // at least on windows, some temporary files raise this exception with an "access denied" message
          // checking if the file can be read doesn't help
          return;
        }

        try {

          // make a new, empty document
          Document doc = new Document();

          // Add the path of the file as a field named "path".  Use a
          // field that is indexed (i.e. searchable), but don't tokenize 
          // the field into separate words and don't index term frequency
          // or positional information:
          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
          doc.add(pathField);

          // Add the last modified date of the file a field named "modified".
          // Use a StoredField to return later its value as a response to a query.
          // This indexes to milli-second resolution, which
          // is often too fine.  You could instead create a number based on
          // year/month/day/hour/minutes/seconds, down the resolution you require.
          // For example the long value 2011021714 would mean
          // February 17, 2011, 2-3 PM.
          doc.add(new StoredField("modified", file.lastModified()));

          //we get the DOM tree out of the file
          DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder docBuild = documentBuilderFactory.newDocumentBuilder();
          org.w3c.dom.Document docTree = docBuild.parse(file);

          /*// Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in UTF-8 encoding.
          // If that's not the case searching for special characters will fail.
          doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, "UTF-8"))));
          */
          AddTextField(doc, docTree, "dc:title", "title");
          AddTextField(doc, docTree, "dc:contributor", "contributor");
          AddStringField(doc, docTree, "dc:identifier", "identifier");
          AddTextField(doc, docTree, "dc:subject", "subject");
          AddStringField(doc, docTree, "dc:type", "type");
          AddTextField(doc, docTree, "dc:description", "description");
          AddTextField(doc, docTree, "dc:creator", "creator");
          AddTextField(doc, docTree, "dc:publisher", "publisher");
          AddStringField(doc, docTree, "dc:language", "language");
          AddDateField(doc, docTree, "dc:date");


          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("adding " + file);
            writer.addDocument(doc);
          } else {
            // Existing index (an old copy of this document may have been indexed) so 
            // we use updateDocument instead to replace the old one matching the exact 
            // path, if present:
            System.out.println("updating " + file);
            writer.updateDocument(new Term("path", file.getPath()), doc);
          }
          
        } catch (ParserConfigurationException e) {
          e.printStackTrace();
        } catch (SAXException e) {
          e.printStackTrace();
        } finally {
          fis.close();
        }
      }
    }
  }

  private static void AddTextField(Document doc, org.w3c.dom.Document docTree, String s, String creator) {
    NodeList nodeList;
    nodeList = docTree.getElementsByTagName(s);
    if (nodeList.item(0) != null)
      doc.add(new TextField(creator, new BufferedReader(new StringReader(docTree.getElementsByTagName(s).item(0).getTextContent()
              .replaceAll("-", "")
              .replaceAll(":", "")
              .replaceAll("/", "")
              .toLowerCase()))));
  }
  private static void AddStringField(Document doc, org.w3c.dom.Document docTree, String s, String creator) {
    NodeList nodeList;
    nodeList = docTree.getElementsByTagName(s);
    if (nodeList.item(0) != null){
      doc.add(new StringField(creator, docTree.getElementsByTagName(s).item(0).getTextContent()
              .replaceAll("-", "")
              .replaceAll(":", "")
              .replaceAll("/", "")
              .toLowerCase(), Field.Store.YES));
    }
  }

  private static void AddDoublePointField(Document doc, org.w3c.dom.Document docTree, String s){
    NodeList nodeList;
    nodeList = docTree.getElementsByTagName(s);
    if (nodeList.item(0) != null) {
      if (s.equals("ows:LowerCorner")) {
        DoublePoint westField = new DoublePoint("west",
                Double.parseDouble(docTree.getElementsByTagName(s).item(0).getTextContent().split(" ")[0]));

        DoublePoint southField = new DoublePoint("south",
                Double.parseDouble(docTree.getElementsByTagName(s).item(0).getTextContent().split(" ")[1]));

        doc.add(westField);
        doc.add(southField);

      }else if(s.equals("ows:UpperCorner")){
        DoublePoint eastField = new DoublePoint("east",
                Double.parseDouble(docTree.getElementsByTagName(s).item(0).getTextContent().split(" ")[0]));

        DoublePoint northField = new DoublePoint("north",
                Double.parseDouble(docTree.getElementsByTagName(s).item(0).getTextContent().split(" ")[1]));

        doc.add(eastField);
        doc.add(northField);

      }else{
        //COMPORTAMIENTO NO ESPERADO
        System.out.println("COMPORTAMIENTO NO ESPERADO, LINEA DE INDEXFILESMULTIPLEINDEXES FUNC AddDoublePointField");
      }

    }
  }

  private static void AddDateField(Document doc, org.w3c.dom.Document docTree, String s) {
    NodeList nodeList;
    nodeList = docTree.getElementsByTagName(s);
    
    if (nodeList.item(0) != null) {
      // En caso de faltar el mes o el día se añade predeterminadamente
      // YYYY/01/01 para begin y YYYY/12/31 para end
      String[] date = new String[2];

      String temporal = docTree.getElementsByTagName(s).item(0).getTextContent();
      if(temporal.contains("begin") && temporal.contains("end")){ // buen formato
        date[0] = temporal.split("=")[1].split(";")[0].replaceAll("-", "");
        date[1] = temporal.split("=")[2].split(";")[0].replaceAll("-", "");
      }
      else {
        try{
          Long.parseLong(temporal); // comprobamos si es un número
          date[0] = date[1] = temporal;
        } catch (NumberFormatException excepcion) {
          System.err.println("Err: El formato de " + s + " del documento '"+ doc.get("path") +"' no sigue nuestras reglas.");
          return;
        }
      }      
      
      for(int i=0; i<2; i++){
        if(date[i].length() < 6){
          // no tiene mes
          if(i==0) date[i] += "01"; else date[i] += "12";
          if(date[i].length() < 8){
            // no tiene día
            if(i==0) date[i] += "01"; else date[i] += "31";
          }
        }
      }

      doc.add(new LongPoint("begin", Long.parseLong(date[0])));
      doc.add(new LongPoint("end", Long.parseLong(date[1])));
    }
  }
}
