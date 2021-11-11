/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Monday, November 8th 2021
 *    File: Evaluation.java
 */

import java.io.*;
import java.util.*;

public class Evaluation {

    // <idNeed, <docId, relevancy>>
    private static Map<Integer, HashMap<Integer, Integer>> judgments = null;
    // <idNeed, docId[]>
    private static Map<Integer, List<Integer>> results = null;

    private static FileWriter output = null;

    private static int tp[] = null;

    private static int fp[] = null;

    private static int fn[] = null;

    private static double precision10[] = null;

    private static LinkedList<Double> precisionk = null;


    private Evaluation() {}

    public static void main(String[] args) throws IOException {
        checkInput(args);
        
        /*Double[] precision = new Double[judgments.size()],
                  recall = new Double[judgments.size()],
                  f1Balanceada = new Double[judgments.size()],
                  precision10 = new Double[judgments.size()],
                  average_precision = new Double[judgments.size()],
                  recall_precision = new Double[judgments.size()],
                  interpolated_recall_precision = new Double[judgments.size()];*/
        tp = new int[judgments.size()];
        fp = new int[judgments.size()];
        fn = new int[judgments.size()];
        precision10 = new double[judgments.size()];

        for(var entry : judgments.entrySet()){
            output.write("INFORMATION_NEED\t" + entry.getKey() + "\n");
            /*precision[entry.getKey()-1] = */ precision(entry.getKey());
            /*recall[entry.getKey()-1] = */ recall(entry.getKey());
            /*f1Balanceada[entry.getKey()-1] =*/ f1Balanceada();
            output.write("prec@10\t" + precision10[entry.getKey()-1] + "\n");
            /*average_precision[entry.getKey()-1] =*/ average_precision(entry.getKey());
            output.write("recall_precision \n");
            /*recall_precision[entry.getKey()-1] =*/ recall_precision(entry.getKey());
            /*interpolated_recall_precision[entry.getKey()-1] =*/ interpolated_recall_precision();
        }

        // Medidas globales

        output.write("TOTAL\n");
        precisionG();
        recallG();
        f1G();
        precision10G();
        MAPG();
        interpolated_recall_precision();

        output.close();
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
        HashMap<Integer, Integer> relevances = null;

        BufferedReader reader = new BufferedReader(new FileReader(qrelsPath));
        try {
            while((line = reader.readLine().split("\\t")) != null){
                if(!judgments.containsKey(Integer.parseInt(line[0]))){
                    relevances = new HashMap<>();
                }
                
                // añadir a sublista hashmap docId y relevancia
                relevances.put(Integer.parseInt(line[1]), Integer.parseInt(line[2]));
                // añadir sublista hashmap al map de la necesidad idNeed
                judgments.put(Integer.parseInt(line[0]), relevances);
            }
        } catch (NullPointerException e) {}
        reader.close();
    }

    private static void processResults(File resultsPath) throws IOException{
        String[] line = new String[2];
        results = new HashMap<>();
        List<Integer> docId = null;

        BufferedReader reader = new BufferedReader(new FileReader(resultsPath));
        try {
            while((line = reader.readLine().split("\\t")) != null){
                if(!results.containsKey(Integer.parseInt(line[0]))){
                    docId = new ArrayList<>();
                }

                // añadir a sublista List el docId
                docId.add(Integer.parseInt(line[1]));
                // añadir sublista List al map de la necesidad idNeed
                results.put(Integer.parseInt(line[0]), docId);
            }
            
        } catch (NullPointerException e) {}
        reader.close();
    }

    // Cálculos medidas de evaluación por necesidad

    private static void precision(int idNeed) throws IOException {
        tp[idNeed - 1] = 0; fp[idNeed - 1] = 0;
        int tp10 = 0; int fp10 = 0;
        int i = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<Integer> result = results.get(idNeed);
        HashMap<Integer, Integer> qrels = judgments.get(idNeed);
        for(var docId : result){
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

        output.write("precision\t" + (double) tp[idNeed - 1] / (tp[idNeed - 1] + fp[idNeed - 1]) + "\n");

        if(result.size() < 10) precision10[idNeed-1] = (double)tp10 / 10;
        else  precision10[idNeed-1] =  (double)tp10 / (tp10 + fp10);
    }

    private static double average_precision(int idNeed){
        int tp = 0, fp = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<Integer> sublist1 = results.get(idNeed);
        HashMap<Integer, Integer> sublist2 = judgments.get(idNeed);
        double total_precision = 0;
        for(var docId : sublist1){
            if(sublist2.containsKey(docId)){
                if(sublist2.get(docId)==1) {
                    tp++;
                    total_precision += (double)tp / (tp + fp);
                }
                else fp++;
            }
        }

        return(total_precision/tp);
    }

    private static void recall(int idNeed) throws IOException {
        //int tp = 0, fn = 0;
        fn[idNeed - 1] = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<Integer> sublist1 = results.get(idNeed);
        HashMap<Integer, Integer> sublist2 = judgments.get(idNeed);
        for(var entry : sublist2.entrySet()){
            if(entry.getValue()==1 && !sublist1.contains(entry.getKey())) fn[idNeed - 1]++;
        }
        output.write("recall\t" + (double)tp[idNeed - 1] / (tp[idNeed - 1] + fn[idNeed - 1]) + "\n");
    }

    private static void recall_precision(int idNeed) throws IOException {
        int tp = 0, fp = 0;
        //recorremos qrels hasta que encontremos el total de documentos relevantes
        List<Integer> result = results.get(idNeed);
        HashMap<Integer, Integer> qrels = judgments.get(idNeed);
        int totalDocRelevantes = 0;

        for(var entry : qrels.entrySet()) {
            if (entry.getValue() == 1) {
                totalDocRelevantes++;
            }
        }
        //iteramos como en preccision y en cada documento relevante calculamos tp/totalDocRelevantes y prec@k
        for(var documentoDevuelto : result){
            if(qrels.containsKey(documentoDevuelto)){
                if(qrels.get(documentoDevuelto)==1){
                    tp++;
                    output.write((double)tp/totalDocRelevantes + "\t" + (double)tp/(tp+fp) + "\n");
                }else{
                    fp++;
                }
            }

        }

    }

    private static double interpolated_recall_precision(){
        return 0.0;
    }

    private static double f1Balanceada(){
        return 0.0;
    }

    // Medidas globales

    private static double precisionG(){
        return 0.0;
    }

    private static double recallG(){
        return 0.0;
    }

    private static double f1G(){
        return 0.0;
    }

    private static double precision10G(){
        return 0.0;
    }

    private static double MAPG(){
        return 0.0;
    }

}