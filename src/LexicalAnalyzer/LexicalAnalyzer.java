package LexicalAnalyzer;

import entities.Dictionary;
import entities.Token;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class LexicalAnalyzer {
    private List<Map.Entry<String, Pattern>> patterns;
    private List<Token> tokens;
    private int currentIndex = 0;

    /**
     * Constructor for LexicalAnalyzer.
     *
     * @param dictionary The dictionary containing token patterns.
     */
    public LexicalAnalyzer(Dictionary dictionary) {
        patterns = new ArrayList<>();
        tokens = new ArrayList<>();

        List<Map.Entry<String, String>> entries = new ArrayList<>(dictionary.getTokenPatterns().entrySet());
        entries.sort((a, b) -> {
            if (a.getKey().equals("ID")) return 1;
            if (b.getKey().equals("ID")) return -1;
            return 0;
        });

        for (Map.Entry<String, String> entry : entries) {
            String tokenType = entry.getKey();
            String regex = entry.getValue();

            Pattern pattern = Pattern.compile(regex);
            patterns.add(Map.entry(tokenType, pattern));
        }
    }

    /**
     * Tokenizes the input file.
     *
     * @param filePath The path to the file to be tokenized.
     * @throws IOException If an I/O error occurs.
     */
    public void tokenize(String filePath) throws IOException {
        int line = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                currentLine = currentLine.strip();
                String[] lexemes = currentLine.split("\\s+");
                int column = 1;

                if (currentLine.isBlank()) {
                    line++;
                    continue;
                }

                for (String word : lexemes) {
                    boolean matched = false;

                    for (Map.Entry<String, Pattern> entry : patterns) {
                        Matcher matcher = entry.getValue().matcher(word);
                        if (matcher.matches()) {
                            tokens.add(new Token(entry.getKey(), word, line, column));
                            matched = true;
                            break;
                        }
                    }

                    if (!matched) {
                        throw new RuntimeException("Unknown token at line " + line + ", column " + column + ": " + word);
                    }

                    column += word.length() + 1;
                }

                line++;
            }
        }
    }

    /**
     * Returns the next token from the list of tokens.
     *
     * @return The next token, or null if there are no more tokens.
     */
    public Token getNextToken() {
        if (currentIndex < tokens.size()) {
            return tokens.get(currentIndex++);
        } else {
            return null;
        }
    }

    /**
     * Returns the list of tokens.
     *
     * @return The list of tokens.
     */
    public List<Token> getTokens() {
        return tokens;
    }
}
