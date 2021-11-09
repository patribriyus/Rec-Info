/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Monday, November 8th 2021
 *    File: Evaluation.java
 */

import java.io.File;

public class Evaluation {

    private Evaluation() {}

    public static void main(String[] args) {
        String usage = "Uso:\tjava Evaluation "
                 + "-qrels <qrelsFileName> "
                 + "-results <resultsFileName> "
                 + "-output <outputFileName>";

        String qrels = null, results = null, output = null;
        for(int i=0; i<args.length; i++) {
            if ("-qrels".equals(args[i])) {
                qrels = args[++i];
            } else if ("-results".equals(args[i])) {
                results = args[++i];
            } else if ("-output".equals(args[i])) {
                output = args[++i];
            }
        }

        if(qrels == null || results == null || output == null || 
            "-h".equals(args[0]) || "-help".equals(args[0]) ||
            args.length != 6){

            System.out.println(usage);
            System.exit(0);
        }

        // Check index's path
        final File qrelsFile = new File(qrels);
        final File resultsFile = new File(qrels);
        if (!qrelsFile.exists() || !qrelsFile.canRead()) {
            System.out.println("El directorio '" + qrelsFile.getAbsolutePath() + "' no existe o no se puede leer.");
            System.exit(1);
        }
        if (!resultsFile.exists() || !resultsFile.canRead()) {
            System.out.println("El directorio '" + resultsFile.getAbsolutePath() + "' no existe o no se puede leer.");
            System.exit(1);
        }


    }

    // Cálculos medidas de evaluación

    private double precision(){

    }

    private double prec10(){
    }

    private double average_precision(){
        
    }

    private double recall(){
        
    }

    private double recall_precision(){

    }

    private double interpolated_recall_precision(){

    }

    private double f1Balanceada(){
        
    }

    private double MAP(){
        
    }

}