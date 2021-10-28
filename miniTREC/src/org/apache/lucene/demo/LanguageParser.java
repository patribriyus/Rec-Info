/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Thursday, October 28th 2021
 *    File: LanguajeParser.java
 */
package org.apache.lucene.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

public class LanguageParser {
    QueryParser parser = null;
    private Scanner scanner = null;
    private FileWriter fileWriter = null;

    private BooleanQuery query = null;
    
    private String idNeed = null;

    LanguageParser(String needsPath, String resultsPath) throws IOException{

        // Expresion regular para detectar las necesidades
        scanner = new Scanner(new File(needsPath)).useDelimiter("\\d+\\-\\d+[^(\\d+\\-\\d+)]*");
        scanner.next(); // saltamos el primer match

        // Si el fichero ya existe lo reescribe
        fileWriter = new FileWriter(resultsPath);

        Analyzer analyzer = new SpanishAnalyzer2();
        parser = new QueryParser("contents", analyzer);
    }

    public Boolean nextNeed() throws ParseException, IOException{
        if(scanner.hasNext()){
            idNeed = scanner.findInLine("\\d+\\-\\d+");
            String need = scanner.next();

            parsear(need);

            return true;
        }
        else {
            // Cerrar fichero de entrada y de salida
            scanner.close();
            fileWriter.close();

            return false;
        }
    }

    private void parsear(String line) throws ParseException{
        // TODO: Modifica el this.query

        BooleanQuery.Builder queryFinal = new BooleanQuery.Builder(); // consulta final

        // spatial:<west>,<east>,<south>,<north>
        Pattern pat = Pattern.compile("spatial\\:(\\s*\\-?\\s*\\d{1,3}(\\.\\d+)?\\s*,){3}" +
                                        "\\s*\\-?\\s*\\d{1,3}(\\.\\d+)?");
        Matcher mat = pat.matcher(line.toLowerCase());
        if(mat.find()){

            // Se coge solo la restriccion de spatial
            String spatialLine = mat.group(0).replaceAll("\\s+", "");

            Double west = Double.valueOf(spatialLine.split(":")[1].split(",")[0]),
                east = Double.valueOf(spatialLine.split(":")[1].split(",")[1]),
                south = Double.valueOf(spatialLine.split(":")[1].split(",")[2]),
                north = Double.valueOf(spatialLine.split(":")[1].split(",")[3]);

            // Xmin <= east
            Query westRangeQuery = DoublePoint.newRangeQuery("west",
                    Double.NEGATIVE_INFINITY, east);
            // Xmax ≥ West
            Query eastRangeQuery = DoublePoint.newRangeQuery("east", 
                    west, Double.POSITIVE_INFINITY );
            // Ymin ≤ North
            Query southRangeQuery = DoublePoint.newRangeQuery("south", 
                    Double.NEGATIVE_INFINITY, north);
            // Ymax ≥ South
            Query northRangeQuery = DoublePoint.newRangeQuery("north", 
                    south, Double.POSITIVE_INFINITY);
            
            BooleanQuery query = new BooleanQuery.Builder()
                .add(westRangeQuery, BooleanClause.Occur.MUST)
                .add(eastRangeQuery, BooleanClause.Occur.MUST)
                .add(northRangeQuery, BooleanClause.Occur.MUST)
                .add(southRangeQuery, BooleanClause.Occur.MUST).build();

            // Disyuncion
            line = line.replace(spatialLine, "");

            // Se añade la restricción de spatial
            queryFinal.add(query, BooleanClause.Occur.SHOULD);
        }

        // date:[<fecha inicio> TO <fecha fin>]
        pat = Pattern.compile("date\\:\\s*\\[\\s*\\d{4,8}\\s*to\\s*\\d{4,8}\\s*\\]");
        mat = pat.matcher(line.toLowerCase());
        if (mat.find()) {
            // Se coge solo la restriccion de date
            String datelLine = mat.group(0);
            
            String[] date = new String[2];
            pat = Pattern.compile("\\d{4,8}");
            mat = pat.matcher(datelLine);
            mat.find(); date[0] = mat.group(0);
            mat.find(); date[1] = mat.group(0);

            // En caso de faltar el mes o el día se añade predeterminadamente
            // YYYY/01/01 para begin y YYYY/12/31 para end
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

            // begin <= fecha fin
            Query beginRangeQuery = LongPoint.newRangeQuery("begin", 
                    Long.parseLong(date[0]), Long.MAX_VALUE);
            // end ≥ fecha inicio
            Query endRangeQuery = LongPoint.newRangeQuery("end", 
                    Long.MIN_VALUE, Long.parseLong(date[1]));
            
            BooleanQuery query = new BooleanQuery.Builder()
                .add(beginRangeQuery, BooleanClause.Occur.MUST)
                .add(endRangeQuery, BooleanClause.Occur.MUST).build();

            // Disyuncion
            line = line.replace(datelLine, "");

            // Se añade la restricción de date
            queryFinal.add(query, BooleanClause.Occur.SHOULD);

            // TODO: importante usar el parser
            Query q = parser.parse(line);
        }
        
        //resto de queries

        this.query = queryFinal.build();
    }

    public void writeResults(IndexSearcher searcher, ScoreDoc[] hits) throws IOException{
        for(ScoreDoc hit : hits){
            Document doc = searcher.doc(hit.doc);
            String path = doc.get("path");
    
            fileWriter.write(idNeed + "\t" + path + "\n");
        }
    }

    public BooleanQuery getBooleanQuery() {
        return query;
    }

    public void setQuery(BooleanQuery query) {
        this.query = query;
    }

    public String getStringQuery() {
        return query.toString();
    }

    public String getIdNeed() {
        return idNeed;
    }

    public void setIdNeed(String idNeed) {
        this.idNeed = idNeed;
    }

}
