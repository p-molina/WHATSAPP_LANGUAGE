package LexicalAnalyzer;

import java.util.HashMap;
import java.util.Map;

public class Dictionary {
    private final Map<String, TokenType> reservedWords;

    public Dictionary() {
        reservedWords = new HashMap<>();
    }

    public Boolean contains(String word) {
        return reservedWords.containsKey(word);
    }

    public TokenType get(String word) {
        return reservedWords.get(word);
    }

    public enum TokenType {
        INT,

    }


}
