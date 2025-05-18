package entities;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Classe Grammar que parseja una gramàtica des d'un fitxer JSON.
 * Emmagatzema les regles en un mapa de no-terminals a llistes de produccions.
 */
public class Grammar {
    private Map<String, List<List<String>>> grammarRules;

    /**
     * Construeix una gramàtica llegint les regles des d'un fitxer JSON.
     *
     * @param filePath Ruta al fitxer JSON amb la gramàtica.
     */
    public Grammar(String filePath) {
        grammarRules = new HashMap<>();

        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(filePath));

            for (Object key : jsonObject.keySet()) {
                String nonTerminal = (String) key;

                // Obtenim array de produccions
                JSONArray productionsArray = (JSONArray) jsonObject.get(nonTerminal);
                List<List<String>> listOfProductions = new ArrayList<>();

                // Recorrem cada producció
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
     * Retorna les regles de la gramàtica carregades.
     *
     * @return Mapa de no-terminals a llistes de produccions.
     */
    public Map<String, List<List<String>>> getGrammarRules() {
        return grammarRules;
    }
}
