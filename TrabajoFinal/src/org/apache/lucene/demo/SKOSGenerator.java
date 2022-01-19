/*
 *    Author:         Patricia Briones Yus, 735576
 *    Creation Date:  Wednesday, January 19th 2022
 *    File: SKOSGenerator.java
 */
package org.apache.lucene.demo;

public class SKOSGenerator {
   
   private SKOSGenerator() {}
	
	public static void main(String[] args) {
      String usage = "Uso:\tjava SKOSGenerator "
						 + "-skos <skosPath>\n";
	                 
	   String skosPath = null;
      if(args[0] == "-skos") skosPath = args[1];

		if(skosPath == null || "-h".equals(args[0]) || "-help".equals(args[0]) || args.length != 2){
			System.out.println(usage);
			System.exit(0);
		}

      // Modelo SKOS

   }

}
