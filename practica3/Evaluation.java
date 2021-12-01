/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Monday, November 8th 2021
 *    File: Evaluation.java
 */

import java.io.*;
import java.util.*;

public class Evaluation {

    // <idNeed, <docId, relevancy>>
    private static Map<String, HashMap<String, Integer>> judgments = null;
    // <idNeed, docId[]>
    private static Map<String, List<String>> results = null;

    //empieza en el 1
    private static Map<String, Integer> needsId = null;

    private static FileWriter output = null;

    private static int tp[] = null;

    private static int fp[] = null;

    private static int fn[] = null;

    private static double precision[] = null;

    private static double recall[] = null;

    private static double precision10[] = null;

    private static double f1[] = null;

    private static double interpolated[][] = null;

    private static double interpolatedGlobal[] = null;


    private Evaluation() {}

    public static void main(String[] args) throws Exception {
        checkInput(args);

        tp = new int[needsId.size()];
        fp = new int[needsId.size()];
        fn = new int[needsId.size()];
        precision10 = new double[needsId.size()];
        precision = new double[needsId.size()];
        recall = new double[needsId.size()];
        f1 = new double[needsId.size()];
        interpolated = new double[needsId.size()][11];
        interpolatedGlobal = new double[11];

        for(Map.Entry<String, HashMap<String, Integer>> entry : judgments.entrySet()){
            output.write("INFORMATION_NEED\t" + getNumKey(entry.getKey()) + "\n");
            precision(entry.getKey());
            recall(entry.getKey());
            f1Balanceada(entry.getKey());
            output.write("prec@10\t" + precision10[getNumKey(entry.getKey())-1] + "\n");
            average_precision(entry.getKey());            
            recall_precision(entry.getKey());
            interpolated_recall_precision(entry.getKey());
            output.write("\n");
        }

        // Medidas globales

        output.write("TOTAL\n");
        precisionG();
        recallG();
        f1G();
        precision10G();
        MAPG();
        interpolated_recall_precisionG();

        output.close();

        Graphic graphic = new Graphic(needsId);
        graphic.precision_recall(interpolated, interpolatedGlobal);
        graphic.graficoBarras(precision, recall, f1);
    }

    private static Integer getNumKey(String key){
        return needsId.get(key);
    }
    
    private static void checkInput(String[] args) throws IOException {
        String usage = "Uso:\tjava Evaluation "
                 + "-qrels <qrelsFileName> "
                 + "-results <resultsFileName> "
                 + "-output <outputFileName>";

        String qrelsPath = null, resultsPath = null, outputPath = null;
        for(int i=0; i<args.length; i++) {
            if ("-qrels".equals(args[i])) {
                qrelsPath = args[++i];
            } else if ("-results".equals(args[i])) {
                resultsPath = args[++i];
            } else if ("-output".equals(args[i])) {
                outputPath = args[++i];
            }
        }

        if(qrelsPath == null || resultsPath == null || outputPath == null || 
            "-h".equals(args[0]) || "-help".equals(args[0]) ||
            args.length != 6){

            System.out.println(usage);
            System.exit(0);
        }

        // Check index's path
        final File qrelsFile = new File(qrelsPath);
        final File resultsFile = new File(resultsPath);
        output = new FileWriter(outputPath);
        if (!qrelsFile.exists() || !qrelsFile.canRead()) {
            System.out.println("El directorio '" + qrelsFile.getAbsolutePath() + "' no existe o no se puede leer.");
            System.exit(1);
        }
        if (!resultsFile.exists() || !resultsFile.canRead()) {
            System.out.println("El directorio '" + resultsFile.getAbsolutePath() + "' no existe o no se puede leer.");
            System.exit(1);
        }

        processJudgments(qrelsFile);
        processResults(resultsFile);
    }

    private static void processJudgments(File qrelsPath) throws IOException{
        String[] line = new String[3];
        judgments = new HashMap<>();
        HashMap<String, Integer> relevances = null;

        BufferedReader reader = new BufferedReader(new FileReader(qrelsPath));
        try {
            while((line = reader.readLine().split("\\t")) != null){
                if(!judgments.containsKey(line[0])){
                    relevances = new HashMap<>();
                }
                
                // añadir a sublista hashmap docId y relevancia
                relevances.put(line[1], Integer.parseInt(line[2]));
                // añadir sublista hashmap al map de la necesidad idNeed
                judgments.put(line[0], relevances);
            }
        } catch (NullPointerException e) {}
        reader.close();
    }

    private static void processResults(File resultsPath) throws IOException{
        String[] line = new String[2];
        results = new HashMap<>();
        needsId = new HashMap<>();
        List<String> docId = null;

        BufferedReader reader = new BufferedReader(new FileReader(resultsPath));
        try {
            int numResultados = 1,
                idNeed = 1;
            while((line = reader.readLine().split("\\t")) != null){
                if(!results.containsKey(line[0])){
                    docId = new ArrayList<>();
                    numResultados = 1;
                    needsId.put(line[0], idNeed++);
                }

                if(numResultados <= 50){
                    // añadir a sublista List el docId
                    docId.add(line[1]);
                    // añadir sublista List al map de la necesidad idNeed
                    results.put(line[0], docId);
                }
                numResultados++;
            }
            
        } catch (NullPointerException e) {}
        reader.close();
    }

    // Cálculos medidas de evaluación por necesidad

    private static void precision(String need) throws IOException {
        int idNeed = getNumKey(need);
        tp[idNeed - 1] = 0; fp[idNeed - 1] = 0;
        int tp10 = 0; int fp10 = 0;
        int i = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<String> result = results.get(need);
        HashMap<String, Integer> qrels = judgments.get(need);
        for(String docId : result){
            if(qrels.containsKey(docId)){
                if(qrels.get(docId)==1){
                    tp[idNeed - 1] ++;
                    if(i < 10){
                        tp10++;
                    }
                    // precisionk.add((double) tp[idNeed - 1]/(fp[idNeed - 1]+tp[idNeed-1]));
                }
                else {
                    fp[idNeed - 1]++;
                    if(i < 10){
                        fp10++;
                    }
                }
            }
            i++;
        }
        
        // fp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 0
        // si no aparece en 'judgments' su relevancy es 0        

        precision[idNeed - 1] = (double) tp[idNeed - 1] / (tp[idNeed - 1] + fp[idNeed - 1]);
        output.write("precision\t" + round(precision[idNeed - 1]) + "\n");

        if(result.size() < 10) precision10[idNeed-1] = (double)tp10 / 10;
        else  precision10[idNeed-1] =  (double)tp10 / (tp10 + fp10);
    }

    private static double average_precision(String need){
        int tp = 0, fp = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<String> result = results.get(need);
        HashMap<String, Integer> qrels = judgments.get(need);
        double total_precision = 0;
        for(String docId : result){
            if(qrels.containsKey(docId)){
                if(qrels.get(docId)==1) {
                    tp++;
                    total_precision += (double)tp / (tp + fp);
                }
                else fp++;
            }
        }

        return(total_precision/tp);
    }

    private static void recall(String need) throws IOException {
        int idNeed = getNumKey(need);

        fn[idNeed - 1] = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<String> result = results.get(need);
        HashMap<String, Integer> qrels = judgments.get(need);
        for(Map.Entry<String, Integer> entry : qrels.entrySet()){
            if(entry.getValue()==1 && !result.contains(entry.getKey())) fn[idNeed - 1]++;
        }

        recall[idNeed - 1] = (double)tp[idNeed - 1] / (tp[idNeed - 1] + fn[idNeed - 1]);
        output.write("recall\t" + round(recall[idNeed - 1]) + "\n");
    }

    private static void recall_precision(String need) throws IOException {
        output.write("recall_precision \n");
        int tp = 0, fp = 0;
        //recorremos qrels hasta que encontremos el total de documentos relevantes
        List<String> result = results.get(need);
        HashMap<String, Integer> qrels = judgments.get(need);
        int totalDocRelevantes = 0;

        for(Map.Entry<String, Integer> entry : qrels.entrySet()) {
            if (entry.getValue() == 1) {
                totalDocRelevantes++;
            }
        }
        //iteramos como en preccision y en cada documento relevante calculamos tp/totalDocRelevantes y prec@k
        for(String documentoDevuelto : result){
            if(qrels.containsKey(documentoDevuelto)){
                if(qrels.get(documentoDevuelto)==1){
                    tp++;
                    output.write(round((double)tp/totalDocRelevantes) + "\t" 
                                + round((double)tp/(tp+fp)) + "\n");
                }else{
                    fp++;
                }
            }

        }
    }

    private static void interpolated_recall_precision(String need) throws IOException{
        int idNeed = getNumKey(need);
        int tp = 0, fp = 0;
        //recorremos qrels hasta que encontremos el total de documentos relevantes
        List<String> result = results.get(need);
        HashMap<String, Integer> qrels = judgments.get(need);
        int totalDocRelevantes = 0;

        for(Map.Entry<String, Integer> entry : qrels.entrySet()) {
            if (entry.getValue() == 1) {
                totalDocRelevantes++;
            }
        }

        List<Double> precisionIRP = new ArrayList<>();
        List<Double> recallIRP = new ArrayList<>();
        //iteramos como en preccision y en cada documento relevante calculamos tp/totalDocRelevantes y prec@k
        for(String documentoDevuelto : result){
            if(qrels.containsKey(documentoDevuelto)){
                if(qrels.get(documentoDevuelto)==1){
                    tp++;
                    precisionIRP.add((double)tp/(tp+fp));
                    recallIRP.add((double)tp/totalDocRelevantes);
                }else{
                    fp++;
                }
            }
        }

        output.write("interpolated_recall_precision\n");

        int x = 0;
        for(double recall = 0.0; recall <= 1.0; recall+=0.1){
            int index = -1; // índice del valor de recall más cercano a rec
            for(Double valor : recallIRP){
                if(valor >= recall) {
                    index = recallIRP.indexOf(valor);
                    break;
                }
            }

            double interpolated_precision = 0.0;
            if(index == -1) interpolated_precision = 0.0;
            else interpolated_precision = Collections.max(precisionIRP.subList(index, precisionIRP.size()), null);
            output.write(round(recall) + "\t" + round(interpolated_precision) + "\n");
            interpolated[idNeed-1][x] = interpolated_precision;
            x++;
        }
    }

    private static void f1Balanceada(String need) throws IOException{
        int idNeed = getNumKey(need);
        f1[idNeed - 1] = (2*precision[idNeed - 1]*recall[idNeed - 1]) / (precision[idNeed - 1]+recall[idNeed - 1]);
        output.write("F1\t" + round(f1[idNeed - 1]) + "\n");
    }

    // Medidas globales

    private static void precisionG() throws IOException{
        double precisionTotal = 0.0;
        for(int i=0; i<judgments.size(); i++){
            precisionTotal += precision[i];
        }
        output.write("precision\t" + round(precisionTotal/judgments.size()) + "\n");
    }

    private static void recallG() throws IOException{
        double recallTotal = 0.0;
        for(int i=0; i<judgments.size(); i++){
            recallTotal += recall[i];
        }
        output.write("recall\t" + round(recallTotal/judgments.size()) + "\n");
    }

    private static void f1G() throws IOException{
        double f1Total = 0.0;
        for(int i=0; i<judgments.size(); i++){
            f1Total += f1[i];
        }
        output.write("F1\t" + round(f1Total/judgments.size()) + "\n");
    }

    private static void precision10G() throws IOException{
        double precisionTotal = 0.0;
        for(int i=0; i<judgments.size(); i++){
            precisionTotal += precision10[i];
        }
        output.write("prec@10\t" + round(precisionTotal/judgments.size()) + "\n");
    }

    private static void MAPG() throws IOException{
        double precisionk = 0.0;

        for(int i=1; i<=judgments.size(); i++){
            precisionk += average_precision(getSingleKeyFromValue(i));
        }

        double MAP = precisionk / 2.0;
        output.write("MAP\t" + round(MAP) + "\n");
    }

    private static void interpolated_recall_precisionG() throws IOException {
        output.write("interpolated_recall_precision\n");
        int x = 0;
        for(double recall=0.0; recall<=1.0; recall+=0.1){
            double interpolatedTotal = 0.0;
            for(int i=0; i<judgments.size(); i++){
                interpolatedTotal += interpolated[i][x];
            }
            interpolatedGlobal[x] = round(interpolatedTotal/judgments.size());
            output.write(round(recall) + "\t" + interpolatedGlobal[x] + "\n");
            x++;
        }
    }

    private static double round(double value){
        return (double)Math.round(value * 1000d) / 1000d;
    }

    public static String getSingleKeyFromValue(int idNeed) {
        for(Map.Entry<String, Integer> entry : needsId.entrySet()){
            if (Objects.equals(idNeed, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}