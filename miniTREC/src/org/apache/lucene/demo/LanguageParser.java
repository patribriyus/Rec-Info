/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Thursday, October 28th 2021
 *    File: LanguageParser.java
 */
package org.apache.lucene.demo;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.xml.sax.SAXException;
import opennlp.tools.*;

public class LanguageParser {
    private QueryParser parser = null;
    private FileWriter fileWriter = null;
    private Analyzer analyzer = null;
    NameFinderME nameFinder = null;

    private org.w3c.dom.Document docTree = null;
    private BooleanQuery query = null;    
    private String idNeed = null;
    private int iterator = 0;

    private final int TYPE_WEIGHT = 10;
    private final int LANGUAGE_WEIGHT = 10;
    private final int DATE_WEIGHT = 10;
    private final int DESCRIPTION_WEIGHT = 4;
    private final int TITLE_WEIGHT = 4;
    private final int SUBJECT_WEIGHT = 4;
    private final int LOCATION_WEIGHT = 10;
    private final int CURRENT_YEAR = 2021;

    LanguageParser(String needsPath, String resultsPath) throws IOException, SAXException, ParserConfigurationException{

        try (InputStream modelIn = new FileInputStream("/home/diego/Desktop/info/4-1/RecuInfo/git-practicas/miniTREC/es-ner-person.bin")){
            TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
            this.nameFinder = new NameFinderME(model);
        } catch (IOException e) {
            e.printStackTrace();
        }

        analyzer = new SpanishAnalyzer2();
        parser = new QueryParser("contents", analyzer);

        // Se extraen todas las necesidades de información
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuild = documentBuilderFactory.newDocumentBuilder();
        docTree = docBuild.parse(new File(needsPath));

        // Si el fichero de resultados ya existe, lo reescribe
        fileWriter = new FileWriter(resultsPath);
    }

    public Boolean nextNeed() throws ParseException, IOException{
        if(docTree.getElementsByTagName("informationNeed").item(iterator) != null){
            String need = docTree.getElementsByTagName("text").item(iterator).getTextContent();
            parsear(need);

            setIdNeed(docTree.getElementsByTagName("identifier").item(iterator).getTextContent());
            iterator++;

            return true;
        }
        else {
            // Cerrar fichero de salida
            fileWriter.close();

            return false;
        }
    }

    private void parsear(String line) throws ParseException, FileNotFoundException {

        BooleanQuery.Builder queryFinal = new BooleanQuery.Builder(); // consulta final

        BoostQuery description = new BoostQuery(new QueryParser("description", analyzer).parse(line),DESCRIPTION_WEIGHT);
        BoostQuery title = new BoostQuery(new QueryParser("title", analyzer).parse(line),TITLE_WEIGHT);

        BooleanQuery type = queryType(line);
        if(type != null)
            queryFinal.add(type, BooleanClause.Occur.SHOULD);

        BooleanQuery language = queryLanguage(line);
        if(language != null)
            queryFinal.add(language, BooleanClause.Occur.SHOULD);

        BooleanQuery date = queryDate(line);
        if(date != null)
            queryFinal.add(date, BooleanClause.Occur.SHOULD);

        BooleanQuery Publisher = queryPublisher(line);
        if(Publisher != null)
            queryFinal.add(Publisher, BooleanClause.Occur.SHOULD);

        BooleanQuery contributorsCreator = queryContributorsCreator(line);
        if(contributorsCreator != null)
            queryFinal.add(contributorsCreator, BooleanClause.Occur.SHOULD);

        BoostQuery subject = new BoostQuery(new QueryParser("subject", analyzer).parse(line),SUBJECT_WEIGHT);

        queryFinal.add(description, BooleanClause.Occur.SHOULD);
        queryFinal.add(title, BooleanClause.Occur.SHOULD);
        queryFinal.add(subject, BooleanClause.Occur.SHOULD);

        query = queryFinal.build();

    }
    private BooleanQuery queryContributorsCreator(String line) {
        String[] lineArray = line.split("");

        nameFinder.find(lineArray);



        return null;
    }
    /*
     *   Query for Publisher field
     */
    private BooleanQuery queryPublisher(String line) throws ParseException {

        Pattern pat = Pattern.compile("departamento?\\s*(de)?\\s*(.*?)(\\?|,|\\.|!|;)");

        Matcher mat = pat.matcher(line.toLowerCase());

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        if (mat.find()) {
            BoostQuery queryLocation = new BoostQuery(new QueryParser("publisher", analyzer).parse(mat.group(2)), LOCATION_WEIGHT);

            builder.add(queryLocation, BooleanClause.Occur.SHOULD);

            line.replace("departamento " + mat.group(1) + mat.group(2), "");

        }

        return builder.build();
    }
    /*
     *   Query for date field
     */
    private BooleanQuery queryDate(String line) throws ParseException {

        Pattern pat = Pattern.compile("[ú|u]ltimos (\\d*) años");

        Pattern pat2 = Pattern.compile("entre (\\d*) y (\\d*)");

        Matcher mat = pat.matcher(line.toLowerCase());

        Matcher mat2 = pat2.matcher(line.toLowerCase());
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        if (mat.find()) {
            Query rangeQuery = LongPoint.newRangeQuery("begin",
                    CURRENT_YEAR-Long.parseLong(mat.group(1)), CURRENT_YEAR);


            builder.add(rangeQuery, BooleanClause.Occur.SHOULD);

            line.replace("últimos " + mat.group(1) + " años", "");

        }

        if (mat2.find()) {
            Query beginRangeQuery = LongPoint.newRangeQuery("begin",
                    Long.parseLong(mat2.group(1)), Long.MAX_VALUE);
            // end ≥ fecha inicio
            Query endRangeQuery = LongPoint.newRangeQuery("end",
                    Long.MIN_VALUE, Long.parseLong(mat2.group(2)));

            builder.add(beginRangeQuery, BooleanClause.Occur.SHOULD);
            builder.add(endRangeQuery, BooleanClause.Occur.SHOULD);

            line.replace("entre " + mat2.group(1) + " y " + mat2.group(2), "");

        }

        return builder.build();
    }
    /*
     *   Query for type field
     */
    private BooleanQuery queryType(String line) throws ParseException {

        Pattern pat = Pattern.compile("trabajos (de)?\\s*fin (de)?\\s*grado|trabajos (de)?\\s*fin (de)?\\s* m[a|á]ster" +
                "\\s*([o,y]\\s*((trabajos (de)?\\s*fin (de)?\\s*)?grado|\\s*(trabajos (de)?\\s*fin (de)?\\s*)?m[a|á]ster))*");
        Matcher mat = pat.matcher(line.toLowerCase());

        if (mat.find()) {
            BoostQuery queryTypeTFG = new BoostQuery(new QueryParser("type", analyzer).parse("TFG"), TYPE_WEIGHT);
            BoostQuery queryTypeTFM = new BoostQuery(new QueryParser("type", analyzer).parse("TFM"), TYPE_WEIGHT);

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(queryTypeTFG, BooleanClause.Occur.SHOULD);
            builder.add(queryTypeTFM, BooleanClause.Occur.SHOULD);

            return builder.build();

        }else{
            return null;
        }

    }
    /*
     *   Query for language field
     */
    private BooleanQuery queryLanguage(String line) throws ParseException {
        Pattern pat = Pattern.compile("lenguaje ([a-z]*)");
        Matcher mat = pat.matcher(line.toLowerCase());

        if (mat.find()) {
            BoostQuery queryLanguage = new BoostQuery(new QueryParser("language", analyzer)
                    .parse(mat.group(1).equals("espa") ? "spa":"eng"), LANGUAGE_WEIGHT);

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(queryLanguage, BooleanClause.Occur.SHOULD);

            line.replace("lenguaje " + mat.group(1), "");

            return builder.build();

        }else{
            return null;
        }
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

    public void setIdNeed(String idNeed){
        this.idNeed = idNeed;
    }

}
