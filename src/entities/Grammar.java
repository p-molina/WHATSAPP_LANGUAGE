package entities;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Grammar {
    // Estructura: NoTerminal -> Lista de producciones -> Producción (lista de símbolos)
    private Map<String, List<List<String>>> grammarRules;

    /**
     * Constructor que parsea la gramática a partir de un archivo JSON.
     *
     * @param filePath La ruta al archivo JSON que contiene la gramática.
     */
    public Grammar(String filePath) {
        grammarRules = new HashMap<>();

        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(filePath));

            for (Object key : jsonObject.keySet()) {
                String nonTerminal = (String) key;
                System.out.println("Cargando no terminal: " + nonTerminal);


                // Obtenemos el array de producciones (que a su vez es un array de arrays)
                JSONArray productionsArray = (JSONArray) jsonObject.get(nonTerminal);

                List<List<String>> listOfProductions = new ArrayList<>();

                // Recorremos cada producción
                for (Object productionObj : productionsArray) {
                    JSONArray productionArray = (JSONArray) productionObj;
                    List<String> productionSymbols = new ArrayList<>();

                    for (Object symbol : productionArray) {
                        productionSymbols.add((String) symbol);
                    }
                    listOfProductions.add(productionSymbols);
                }


                grammarRules.put(nonTerminal, listOfProductions);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retorna las reglas de la gramática.
     *
     * @return Un mapa con las reglas de la gramática, donde cada
     *         no terminal se asocia a una lista de producciones,
     *         y cada producción se representa como una lista de símbolos.
     */
    public Map<String, List<List<String>>> getGrammarRules() {
        return grammarRules;
    }
}
