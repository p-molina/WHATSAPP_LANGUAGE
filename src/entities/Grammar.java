package entities;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class Grammar {
    private Map<String, Map<String, String[]>> grammarRules;

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

            // Se recorre cada no terminal de la gramática
            for (Object key : jsonObject.keySet()) {
                String nonTerminal = (String) key;
                JSONObject rules = (JSONObject) jsonObject.get(nonTerminal);
                Map<String, String[]> ruleMap = new HashMap<>();

                // Para cada clave del no terminal se obtiene la producción (un arreglo de símbolos)
                for (Object innerKey : rules.keySet()) {
                    String productionKey = (String) innerKey;
                    JSONArray productionArray = (JSONArray) rules.get(productionKey);
                    String[] production = new String[productionArray.size()];

                    for (int i = 0; i < productionArray.size(); i++) {
                        production[i] = (String) productionArray.get(i);
                    }
                    ruleMap.put(productionKey, production);
                }
                grammarRules.put(nonTerminal, ruleMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retorna las reglas de la gramática.
     *
     * @return Un mapa con las reglas de la gramática.
     */
    public Map<String, Map<String, String[]>> getGrammarRules() {
        return grammarRules;
    }
}