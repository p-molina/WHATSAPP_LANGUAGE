package LexicalAnalyzer;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Dictionary {
    private Map<String, String> tokenPatterns;

    /**
     * Constructor that initializes the dictionary with token patterns from a JSON file.
     *
     * @param filePath The path to the JSON file containing token patterns.
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
     * Returns the token patterns.
     *
     * @return A map of token types to their corresponding regex patterns.
     */
    public Map<String, String> getTokenPatterns() {
        return tokenPatterns;
    }
}
