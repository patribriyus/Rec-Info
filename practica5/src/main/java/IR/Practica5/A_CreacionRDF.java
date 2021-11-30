package IR.Practica5;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.VCARD;

/**
 * Ejemplo de como construir un modelo de Jena y añadir nuevos recursos 
 * mediante la clase Model
 */
public class A_CreacionRDF {
	
	/**
	 * muestra un modelo de jena de ejemplo por pantalla
	 */
	public static void main (String args[]) {
        Model model = A_CreacionRDF.generarEjemplo();
        // write the model in the standar output
        model.write(System.out); 
    }
	
	/**
	 * Genera un modelo de jena de ejemplo
	 */
	public static Model generarEjemplo(){
		// definiciones
        String personURI    = "http://somewhere/JohnSmith";
        String givenName    = "John";
        String familyName   = "Smith";
        String fullName     = givenName + " " + familyName;
        String uriType		= "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        String valueType	= "http://xmlns.com/foaf/0.1/person";

        // crea un modelo vacio
        Model model = ModelFactory.createDefaultModel();

        // le a�ade las propiedades
        Resource johnSmith  = model.createResource(personURI)
             .addProperty(VCARD.FN, fullName)
             .addProperty(VCARD.N, 
                      model.createResource()
                           .addProperty(VCARD.Given, givenName)
                           .addProperty(VCARD.Family, familyName))
             .addProperty(model.createProperty(uriType), valueType);
        
        Resource diegoCaballe = model.createResource("http://somewhere/DiegoCaballe")
                .addProperty(VCARD.FN, "Diego Caballe")
                .addProperty(VCARD.N, 
                         model.createResource()
                              .addProperty(VCARD.Given, "Diego")
                              .addProperty(VCARD.Family, "Caballe"))
                .addProperty(model.createProperty(uriType), valueType);
                
        Resource patriciaBriones = model.createResource("http://somewhere/PatriciaBriones")
                .addProperty(VCARD.FN, "Patricia Briones")
                .addProperty(VCARD.N, 
                         model.createResource()
                              .addProperty(VCARD.Given, "Patricia")
                              .addProperty(VCARD.Family, "Briones"))
                .addProperty(model.createProperty(uriType), valueType)
                .addProperty(model.createProperty("http://xmlns.com/foaf/0.1/knows"), diegoCaballe)
                .addProperty(model.createProperty("http://xmlns.com/foaf/0.1/knows"), johnSmith);
        
        diegoCaballe.addProperty(model.createProperty("http://xmlns.com/foaf/0.1/knows"), patriciaBriones);
        
        // Tripleta --> John conoce a Diego
        model.add(johnSmith, model.createProperty("http://xmlns.com/foaf/0.1/knows"), diegoCaballe);
        
        return model;
	}
	
	
}
