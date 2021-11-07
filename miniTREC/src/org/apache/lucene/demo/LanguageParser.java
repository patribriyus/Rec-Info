/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Thursday, October 28th 2021
 *    File: LanguageParser.java
 */
package org.apache.lucene.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.xml.sax.SAXException;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

public class LanguageParser {

    private static final int TYPE_WEIGHT = 10;
    private static final int LANGUAGE_WEIGHT = 10;
    private static final int DESCRIPTION_WEIGHT = 4;
    private static final int TITLE_WEIGHT = 4;
    private static final int SUBJECT_WEIGHT = 4;
    private static final int LOCATION_WEIGHT = 10;
    private static final int CONTRIBUTOR_CREATOR_WEIGHT = 10;
    private static final int CURRENT_YEAR = 2021;

    private FileWriter fileWriter = null;
    private Analyzer analyzer = null;
    private NameFinderME nameFinder = null;

    private org.w3c.dom.Document docTree = null;
    private BooleanQuery query = null;    
    private String idNeed = null;
    private int iterator = 0;
    private String needLeft = null;

    LanguageParser(String needsPath, String resultsPath) throws IOException, SAXException, ParserConfigurationException{

        try (InputStream modelIn = new FileInputStream("es-ner-person.bin")){
            TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
            this.nameFinder = new NameFinderME(model);
        } catch (IOException e) {
            e.printStackTrace();
        }

        analyzer = new SpanishAnalyzer2();

        // Se extraen todas las necesidades de información
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuild = documentBuilderFactory.newDocumentBuilder();
        docTree = docBuild.parse(new File(needsPath));

        // Si el fichero de resultados ya existe, lo reescribe
        fileWriter = new FileWriter(resultsPath);
    }

    public Boolean nextNeed() throws ParseException, IOException{
        if(docTree.getElementsByTagName("informationNeed").item(iterator) != null){
            needLeft = docTree.getElementsByTagName("text").item(iterator).getTextContent()
                        .replaceAll("[?|¿|*]","");
            parsear();

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

    private void parsear() throws ParseException, FileNotFoundException {

        BooleanQuery.Builder queryFinal = new BooleanQuery.Builder(); // consulta final

        BooleanQuery contributorsCreator = queryContributorsCreator();
        if(contributorsCreator != null)
            queryFinal.add(contributorsCreator, BooleanClause.Occur.SHOULD);

        needLeft = needLeft.toLowerCase();

        BooleanQuery type = queryType();
        if(type != null)
            queryFinal.add(type, BooleanClause.Occur.MUST);

        BooleanQuery language = queryLanguage();
        if(language != null)
            queryFinal.add(language, BooleanClause.Occur.MUST);

        BooleanQuery date = queryDate();
        if(date != null)
            queryFinal.add(date, BooleanClause.Occur.MUST);

        BooleanQuery Publisher = queryPublisher();
        if(Publisher != null)
            queryFinal.add(Publisher, BooleanClause.Occur.MUST);
        
        BoostQuery description = new BoostQuery(new QueryParser("description", analyzer).parse(needLeft), DESCRIPTION_WEIGHT);
        BoostQuery title = new BoostQuery(new QueryParser("title", analyzer).parse(needLeft), TITLE_WEIGHT);
        BoostQuery subject = new BoostQuery(new QueryParser("subject", analyzer).parse(needLeft), SUBJECT_WEIGHT);

        queryFinal.add(description, BooleanClause.Occur.MUST);
        queryFinal.add(title, BooleanClause.Occur.SHOULD);
        queryFinal.add(subject, BooleanClause.Occur.SHOULD);

        query = queryFinal.build();

    }

    /*
     *   Query for type field
     */
    private BooleanQuery queryType() throws ParseException {

        Pattern patTFG = Pattern.compile("(trabajos? (de)? (fin)? de grado)|(tfgs?)"),
                patTFM = Pattern.compile("(trabajos? (de)? (fin)? de m[a|á]ster)|(tfms?)"),
                patTESIS = Pattern.compile("tesis");
        Matcher matTFG = patTFG.matcher(needLeft),
                matTFM = patTFM.matcher(needLeft),
                matTESIS = patTESIS.matcher(needLeft);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        BoostQuery queryTypeTFG = null, 
                   queryTypeTFM = null,
                   queryTypeTESIS = null;

        if(!matTFG.find(0) && !matTFM.find(0) && !matTESIS.find(0)) return null;

        if(matTFG.find(0)){
            queryTypeTFG = new BoostQuery(new QueryParser("type", analyzer).parse("taztfg"), TYPE_WEIGHT);
            builder.add(queryTypeTFG, BooleanClause.Occur.SHOULD);
            needLeft = needLeft.replace(matTFG.group(0), "");
        }
        if(matTFM.find(0)){
            queryTypeTFM = new BoostQuery(new QueryParser("type", analyzer).parse("taztfm"), TYPE_WEIGHT);
            builder.add(queryTypeTFM, BooleanClause.Occur.SHOULD);
            needLeft = needLeft.replace(matTFM.group(0), "");
        }
        if(matTESIS.find(0)){
            queryTypeTESIS = new BoostQuery(new QueryParser("type", analyzer).parse("tesis"), TYPE_WEIGHT);
            builder.add(queryTypeTESIS, BooleanClause.Occur.SHOULD);
            needLeft = needLeft.replace(matTESIS.group(0), "");
        }
        
        return builder.build();
    }

    /*
     *   Query for language field
     */
    private BooleanQuery queryLanguage() throws ParseException {

        Pattern pat = Pattern.compile("en (lenguaje)? (español)|(ingles)");
        Matcher mat = pat.matcher(needLeft);

        if (!mat.find(0)) return null;

        BoostQuery queryLanguage = new BoostQuery(new QueryParser("language", analyzer)
                .parse(mat.group(0).contains("español") ? "spa" : "eng"), LANGUAGE_WEIGHT);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(queryLanguage, BooleanClause.Occur.SHOULD);

        needLeft = needLeft.replace(mat.group(0), "");

        return builder.build();
    }
    
    /*
     *   Query for date field
     */
    private BooleanQuery queryDate() throws ParseException {

        Pattern pat1 = Pattern.compile("[ú|u]ltimos? \\d+ años?"),
                pat2 = Pattern.compile("entre \\d{4} y \\d{4}"),
                patAnyo = Pattern.compile("\\d{1,4}");
        Matcher mat1 = pat1.matcher(needLeft),
                mat2 = pat2.matcher(needLeft);

        if(!mat1.find(0) && !mat2.find(0)) return null;

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        String dateneedLeft = null;
        String[] date = new String[2];
        if (mat1.find(0)) {
            dateneedLeft = mat1.group(0);
            Matcher mat = patAnyo.matcher(dateneedLeft);
            
            mat.find(); date[0] = Long.toString(CURRENT_YEAR - Long.parseLong(mat.group(0))); // begin
            date[1] = Long.toString(CURRENT_YEAR); // end
        }
        else if (mat2.find(0)) {
            dateneedLeft = mat2.group(0);
            Matcher mat = patAnyo.matcher(dateneedLeft);
            
            // Se asume que la fecha introducida inicial será menor que la final
            mat.find(); date[0] = mat.group(0);
            mat.find(); date[1] = mat.group(0);
        }

        defaultDate(date);

        // begin <= fecha fin
        Query beginRangeQuery = LongPoint.newRangeQuery("begin", 
                Long.parseLong(date[0]), Long.MAX_VALUE);
        // end ≥ fecha inicio
        Query endRangeQuery = LongPoint.newRangeQuery("end", 
                Long.MIN_VALUE, Long.parseLong(date[1]));

        BooleanQuery rangeQuery = new BooleanQuery.Builder()
                .add(beginRangeQuery, BooleanClause.Occur.MUST)
                .add(endRangeQuery, BooleanClause.Occur.MUST).build();

        builder.add(rangeQuery, BooleanClause.Occur.SHOULD);

        needLeft.replace(dateneedLeft, "");

        return builder.build();
    }

    /*
     *   Query for Publisher field
     */
    private BooleanQuery queryPublisher() throws ParseException {

        Pattern patDEP = Pattern.compile("departamento?\\s*(de)?\\s*(.*?)(\\?|,|\\.|!|;)"),
                patUNI = Pattern.compile("universidad\\sde\\s[^\\s]+");
        Matcher matDEP = patDEP.matcher(needLeft),
                matUNI = patUNI.matcher(needLeft);

        if(!matDEP.find(0) && !matUNI.find(0)) return null;

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        String publisher = null;
        if (matDEP.find(0)) publisher = matDEP.group(0);
        else if(matUNI.find(0)) publisher = matUNI.group(0);

        BoostQuery queryLocation = new BoostQuery(new QueryParser("publisher", analyzer).parse(publisher), LOCATION_WEIGHT);
        builder.add(queryLocation, BooleanClause.Occur.SHOULD);

        needLeft = needLeft.replace(publisher, "");

        return builder.build();
    }
    
    /*
     *   Query for contributor and creator field
     */
    private BooleanQuery queryContributorsCreator() throws ParseException {
        String[] lineArray = needLeft.split("\\s+");

        if(nameFinder.find(lineArray).length <= 0) return null;
        
        BoostQuery queryCreator = null,
                   queryContributor = null;
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
        for (int si = 0; si < lineArray.length; si++) {
            Span[] tokenSpans = tokenizer.tokenizePos(lineArray[si]);
            String[] tokens = Span.spansToStrings(tokenSpans, lineArray[si]);
            Span[] names = nameFinder.find(tokens);
            for (int ni = 0; ni < names.length; ni++) {
                Span startSpan = tokenSpans[names[ni].getStart()];
                int nameStart = startSpan.getStart();
                Span endSpan = tokenSpans[names[ni].getEnd() - 1];
                int nameEnd = endSpan.getEnd();
                String name = lineArray[si].substring(nameStart, nameEnd);

                queryCreator = new BoostQuery(new QueryParser("creator", analyzer).parse(String.valueOf(name)), CONTRIBUTOR_CREATOR_WEIGHT);
                queryContributor = new BoostQuery(new QueryParser("contributor", analyzer).parse(String.valueOf(name)), CONTRIBUTOR_CREATOR_WEIGHT);
                builder.add(queryCreator, BooleanClause.Occur.SHOULD);
                builder.add(queryContributor, BooleanClause.Occur.SHOULD);
            }
        }

        return builder.build();
    }

    public void writeResults(IndexSearcher searcher, ScoreDoc[] hits) throws IOException{
        for(ScoreDoc hit : hits){
            Document doc = searcher.doc(hit.doc);
            String path = doc.get("path");
    
            fileWriter.write(idNeed + "\t" + path + "\n");
        }
    }

    private void defaultDate(String[] date){
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
