package entities;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Classe Dictionary que carrega patrons de tokens des d'un fitxer JSON.
 * Cada entrada associa un tipus de token amb el seu patró regex.
 */
public class Dictionary {
    private Map<String, String> tokenPatterns;

    /**
     * Construeix un diccionari amb els patrons de tokens definits al fitxer JSON.
     *
     * @param filePath Ruta al fitxer JSON que conté les definicions de tokens.
     */
    public Dictionary(String filePath) {
        tokenPatterns = new HashMap<>();

        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(filePath));

            for (Object key : jsonObject.keySet()) {
                String tokenType = (String) key;
                String pattern = (String) jsonObject.get(key);
                tokenPatterns.put(tokenType, pattern);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retorna els patrons de tokens carregats.
     *
     * @return Mapa on la clau és el tipus de token i el valor és el patró regex.
     */
    public Map<String, String> getTokenPatterns() {
        return tokenPatterns;
    }
}
