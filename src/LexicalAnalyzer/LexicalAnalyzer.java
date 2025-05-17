package LexicalAnalyzer;

import ParserAnalyzer.GramaticalErrorType;
import entities.Dictionary;
import entities.Token;

import java.io.*;
import java.util.*;
import java.util.regex.*;


/**
 * Analitzador lèxic (scanner) que llegeix un fitxer font i genera una seqüència de tokens per al parser.
 *
 * Basat en les definicions de tokens del diccionari, aplica expressions regulars per reconèixer cada lexema.
 */
public class LexicalAnalyzer {
    // Llista de parells (nomToken, patroRegex) que defineixen els tipus de token.
    private List<Map.Entry<String, Pattern>> patterns;


    // Llista de tokens produïts després del procés de tokenització.
    private List<Token> tokens;

    /**
     * Construeix l'analitzador lèxic inicialitzant els patrons a partir del diccionari de definicions.
     *
     * @param dictionary Diccionari amb map de tipus de token a regex
     */
    public LexicalAnalyzer(Dictionary dictionary) {
        patterns = new ArrayList<>();
        tokens = new ArrayList<>();

        // Carrega i ordena les entrades del diccionari
        List<Map.Entry<String, String>> entries = new ArrayList<>(dictionary.getTokenPatterns().entrySet());
        entries.sort((a, b) -> {
            if (a.getKey().equals("ID")) return 1;
            if (b.getKey().equals("ID")) return -1;
            return 0;
        });

        // Compila cada regex i l'afegeix a la llista de patterns
        for (Map.Entry<String, String> entry : entries) {
            String tokenType = entry.getKey();
            String regex = entry.getValue();

            Pattern pattern = Pattern.compile(regex);
            patterns.add(Map.entry(tokenType, pattern));
        }
    }

    /**
     * Llegeix el fitxer especificat línia a línia i genera tokens.
     * Ignora línies en blanc.
     *
     * @param filePath Ruta al fitxer font (.wsp) a tokenitzar
     */
    public void tokenize(String filePath) {
        int line = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            // Recorre cada línia del fitxer
            while ((currentLine = reader.readLine()) != null) {
                currentLine = currentLine.strip();
                String[] lexemes = currentLine.split("\\s+");
                int column = 1;
                // Si la línia està buida, passa a la següent
                if (currentLine.isBlank()) {
                    line++;
                    continue;
                }
                // Divideix la línia en lexemes segons espais
                for (String word : lexemes) {
                    boolean matched = false;
                    // Prova cada patró per veure si encaixa
                    for (Map.Entry<String, Pattern> entry : patterns) {
                        Matcher matcher = entry.getValue().matcher(word);
                        if (matcher.matches()) {
                            // Crea el token i l'afegeix a la llista
                            tokens.add(new Token(entry.getKey(), word, line, column));
                            matched = true;
                            break;
                        }
                    }
                    // Si no s'ha trobat cap patró, error lèxic
                    if (!matched) {
                        throw new RuntimeException(
                            String.format( String.valueOf(GramaticalErrorType.GRAMATICAL_ERROR_TYPE), line, word)
                        );
                    }
                    column += word.length() + 1;
                }
                line++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error llegint el fitxer: " + filePath, e);
        }
    }

    /**
     * Retorna la llista completa de tokens generats.
     *
     * @return Llista de Token
     */
    public List<Token> getTokens() {
        return tokens;
    }

    /**
     * Neteja l'estat intern, eliminant els tokens emmagatzemats i reiniciant l'índex.
     */
    public void clear() {
        tokens.clear();
    }
}
