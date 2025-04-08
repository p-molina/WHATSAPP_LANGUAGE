package ParserAnalyzer;

import entities.Dictionary;
import entities.Grammar;
import entities.ParserTableBuilder;

import java.util.Map;
import java.util.List;

public class ParserAnalyzer {
    private Dictionary dictionary;
    private Grammar grammar;

    private Map<String, Map<String, List<String>>> parsingTable;

    public ParserAnalyzer(Dictionary dictionary, Grammar grammar) {
        this.dictionary = dictionary;
        this.grammar = grammar;

        // Generar la tabla de parsing
        ParserTableBuilder builder = new ParserTableBuilder(dictionary, grammar);
        builder.buildParsingTable();
        this.parsingTable = builder.getParsingTable();

    }

    // Resto de pasos (2,3,4,5) vendrían después:
    // 2. FIRST y FOLLOW ya calculados en la clase de builder
    // 3. Omplir la taula (ya hecho)
    // 4. Rellenar el stack, parsear tokens, etc.
    // 5. Construir el árbol
}
