/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Thursday, October 28th 2021
 *    File: LanguajeParser.java
 */
package org.apache.lucene.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

public class LanguageParser {
    private FileReader fileReader = null;

    private Matcher needMatcher = null;

    private BooleanQuery query = null;
    
    private String idNeed = null;
    private String allNeeds = null;

    LanguageParser(String needsPath, String resultsPath){
        
        try {
            // Abrir fichero
            File file = new File (needsPath);
            fileReader = new FileReader (file);
            BufferedReader bufferReader = new BufferedReader(fileReader);
   
            // Lectura del fichero
            String linea;
            while((linea=bufferReader.readLine())!=null)
                allNeeds += linea;

            // TODO: crear fichero de escritura fileWriter
         }
         catch(Exception e){
            e.printStackTrace();
         }

         // Expresion regular para detectar las necesidades
         Pattern pat = Pattern.compile("\\d+\\-\\d+[^(\\d+\\-\\d+)]*");
         needMatcher = pat.matcher(allNeeds); 
    }

    public Boolean nextNeed(){
        if(needMatcher.find()){
            String need = needMatcher.group(0);

            // Se guarda el identificador de la necesidad
            Pattern pat = Pattern.compile("\\d+\\-\\d+[^(\\d+\\-\\d+)]*");
            Matcher mat = pat.matcher(need);
            mat.find(); idNeed = mat.group(0);

            parsear(need);

            return true;
        }
        else {
            // Cerrar fichero de entrada y de salida
            try{                    
                if(fileReader != null){   
                    fileReader.close();
                    fileWriter.close();
                }                  
             }catch (Exception e2){ 
                e2.printStackTrace();
             }

            return false;
        }
    }

    private void parsear(String line){
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
        }
        
        //resto de queries

        this.query = queryFinal.build();
    }

    public void writeResults(IndexSearcher searcher, ScoreDoc[] hits){
        // TODO: escribe en el fichero txt
        Document doc = searcher.doc(hits[i].doc);
        String path = doc.get("path");
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
