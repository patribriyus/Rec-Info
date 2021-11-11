/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Monday, November 8th 2021
 *    File: Evaluation.java
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Evaluation {

    private Evaluation() {}

    public static void main(String[] args) throws IOException {
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
        if (!qrelsFile.exists() || !qrelsFile.canRead()) {
            System.out.println("El directorio '" + qrelsFile.getAbsolutePath() + "' no existe o no se puede leer.");
            System.exit(1);
        }
        if (!resultsFile.exists() || !resultsFile.canRead()) {
            System.out.println("El directorio '" + resultsFile.getAbsolutePath() + "' no existe o no se puede leer.");
            System.exit(1);
        }

        // <idNeed, <docId, relevancy>>
        Map<Integer, HashMap<Integer, Integer>> judgments = processJudgments(qrelsFile);
        // <idNeed, docId[]>
        Map<Integer, List<Integer>> results = processResults(resultsFile);

        Double[] precision = new Double[judgments.size()],
                  recall = new Double[judgments.size()],
                  f1Balanceada = new Double[judgments.size()],
                  precision10 = new Double[judgments.size()],
                  average_precision = new Double[judgments.size()],
                  recall_precision = new Double[judgments.size()],
                  interpolated_recall_precision = new Double[judgments.size()];

        for(var entry : judgments.entrySet()){
            System.out.println("INFORMATION_NEED\t" + entry.getKey());
            precision[entry.getKey()-1] = precision(entry.getKey(), judgments, results);
            recall[entry.getKey()-1] = recall(entry.getKey(), judgments, results);
            f1Balanceada[entry.getKey()-1] = f1Balanceada();
            precision10[entry.getKey()-1] = precision10(entry.getKey(), judgments, results);
            average_precision[entry.getKey()-1] = average_precision();
            recall_precision[entry.getKey()-1] = recall_precision();
            interpolated_recall_precision[entry.getKey()-1] = interpolated_recall_precision();
            
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }

        // Medidas globales

        System.out.println("TOTAL");
        precisionG();
        recallG();
        f1G();
        precision10G();
        MAPG();
        interpolated_recall_precision();
    }

    private static Map<Integer, HashMap<Integer, Integer>> processJudgments(File qrelsPath) throws IOException{
        String[] line = new String[3];
        Map<Integer, HashMap<Integer, Integer>> judgments = new HashMap<>();
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
        return judgments;
    }

    private static Map<Integer, List<Integer>> processResults(File resultsPath) throws IOException{
        String[] line = new String[2];
        Map<Integer, List<Integer>> results = new HashMap<>();
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
        return results;
    }

    // Cálculos medidas de evaluación por necesidad

    private static double precision(int idNeed, Map<Integer, HashMap<Integer, Integer>> judgments, Map<Integer, List<Integer>> results){
        int tp = 0, fp = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<Integer> sublist1 = results.get(idNeed);
        HashMap<Integer, Integer> sublist2 = judgments.get(idNeed);
        for(var docId : sublist1){
            if(sublist2.containsKey(docId)){
                if(sublist2.get(docId)==1) tp ++;
                else fp++;
            }
        }
        
        // fp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 0
        // si no aparece en 'judgments' su relevancy es 0

        return (double)tp / (tp + fp);
    }

    private static double precision10(int idNeed, Map<Integer, HashMap<Integer, Integer>> judgments, Map<Integer, List<Integer>> results){
        int tp = 0, fp = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<Integer> sublist1 = results.get(idNeed);
        HashMap<Integer, Integer> sublist2 = judgments.get(idNeed);
        int i = 0;
        for(var docId : sublist1){
            if(i == 10) break;
            if(sublist2.containsKey(docId)){
                if(sublist2.get(docId)==1) tp ++;
                else fp++;
            }
            i++;
        }
        
        // fp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 0
        // si no aparece en 'judgments' su relevancy es 0
        if(sublist2.size() < 10) return (double)tp / 10;
        else return (double)tp / (tp + fp);
    }

    private static double average_precision(){
        return 0.0;
    }

    private static double recall(int idNeed, Map<Integer, HashMap<Integer, Integer>> judgments, Map<Integer, List<Integer>> results){
        int tp = 0, fn = 0;
        // tp --> todos los documentos de 'results' cuya relevancy en 'judgments' es 1
        List<Integer> sublist1 = results.get(idNeed);
        HashMap<Integer, Integer> sublist2 = judgments.get(idNeed);
        for(var entry : sublist2.entrySet()){
            if(entry.getValue()==1 && !sublist1.contains(entry.getKey())) fn++;
        }
                
        fn = sublist1.size() - tp;// TODO: revisar porque diría que no es así

        return (double)tp / (tp + fn);
    }

    private static double recall_precision(){
        return 0.0;
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