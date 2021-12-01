/*
*    Author:         Patricia Briones Yus, 735576
*    Creation Date:  Monday, November 15th 2021
*    File: Graphic
*/

import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

public class Graphic {

    private Map<String, Integer> needsId = null;
    private int numNeeds;

    Graphic(Map<String, Integer> needsId){
        this.needsId = needsId;
        this.numNeeds = needsId.size();
    }

    public void precision_recall(double[][] interpolated, double[] interpolatedGlobal) throws Exception {
        double[] puntos = new double[11];
        for(int i=0; i<=10; i++){
            puntos[i] = i/10.0;
        }

        DefaultXYDataset dataset = new DefaultXYDataset();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        for(int i=0; i<numNeeds; i++){
            double[] aux = new double[11];
            for(int j=0; j<=10; j++){
                aux[j] = interpolated[i][j];
            }
            dataset.addSeries("Need_"+ getSingleKeyFromValue(i+1), new double[][] {puntos, aux});
            renderer.setSeriesStroke(i, new BasicStroke(2));
        }
        dataset.addSeries("Total", new double[][] {puntos, interpolatedGlobal});
        renderer.setSeriesStroke(numNeeds, new BasicStroke(2));

        JFreeChart chart = ChartFactory.createXYLineChart("Gráfica precisión-exhaustividad", "Recall", "Precision", dataset);
        chart.getXYPlot().getRangeAxis().setRange(0, 1.1);
        chart.getXYPlot().getDomainAxis().setRange(0, 1.1);

        chart.getXYPlot().setRenderer(renderer);

        BufferedImage image = chart.createBufferedImage(600, 400);
        ImageIO.write(image, "png", new File("precision-exhaustividad.png"));
    }

    public void graficoBarras(double[] precisionData, double[] recallData, 
                double[] f1Data) throws IOException{
        final String precision = "Precisión";
        final String recall = "Exhaustividad";
        final String f1 = "F1";
    
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    
        for(int i=0; i<numNeeds; i++){
            dataset.addValue(precisionData[i] , "Need_" + getSingleKeyFromValue(i+1) , precision);
            dataset.addValue(recallData[i] , "Need_" + getSingleKeyFromValue(i+1) , recall);
            dataset.addValue(f1Data[i] , "Need_" + getSingleKeyFromValue(i+1) , f1);
        }
    
        JFreeChart chart = ChartFactory.createBarChart("Medidas de evaluación", 
            "", "", dataset,PlotOrientation.VERTICAL, true, true, false);

        BufferedImage image = chart.createBufferedImage(600, 400);
        ImageIO.write(image, "png", new File("barras-medidas.png"));
    }

    public String getSingleKeyFromValue(int idNeed) {
        for(Map.Entry<String, Integer> entry : needsId.entrySet()){
            if (Objects.equals(idNeed, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}