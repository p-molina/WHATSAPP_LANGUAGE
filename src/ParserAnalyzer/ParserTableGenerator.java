package ParserAnalyzer;

import java.util.*;
import entities.*;
import entities.Dictionary;

public class ParserTableGenerator {
    private Dictionary dictionary;
    private Grammar grammar;
    private Map<String, Map<String, List<String>>> parsingTable;

    public ParserTableGenerator(Dictionary dictionary, Grammar grammar) {
        this.dictionary = dictionary;
        this.grammar = grammar;
        this.parsingTable = new HashMap<>();
    }

    /**
     * Construye la tabla de parsing LL(1) e imprime FIRST, FOLLOW y la tabla.
     */
    public void buildParsingTable() {
        Map<String, List<List<String>>> grammarRules = grammar.getGrammarRules();
        FirstFollow calculator = new FirstFollow(grammarRules);

        // 1. Calcular FIRST y FOLLOW
        Map<String, Set<String>> firstSets = calculator.computeFirstSets();
        Map<String, Set<String>> followSets = calculator.computeFollowSets("<AXIOMA>");

        // 2. Imprimir FIRST sets
        System.out.println("=== FIRST sets ===");
        List<String> nts = new ArrayList<>(firstSets.keySet());
        Collections.sort(nts);
        for (String nt : nts) {
            List<String> elems = new ArrayList<>(firstSets.get(nt));
            Collections.sort(elems);
            System.out.println( nt + " : " + elems);
        }

        // 3. Imprimir FOLLOW sets
        System.out.println("=== FOLLOW sets ===");
        nts = new ArrayList<>(followSets.keySet());
        Collections.sort(nts);
        for (String nt : nts) {
            List<String> elems = new ArrayList<>(followSets.get(nt));
            Collections.sort(elems);
            System.out.println(nt + " : " + elems);
        }

        // 4. Construir la tabla LL(1)
        ParserTable parserTable = new ParserTable(dictionary, grammarRules, firstSets, followSets);
        parserTable.initParsingTable();
        parserTable.fillParsingTable();
        this.parsingTable = parserTable.getTable();

        // 5. Imprimir Parsing Table
        System.out.println("=== Parsing Table ===");
        System.out.println(this.parsingTable);
    }

    /**
     * Devuelve la tabla de parsing LL(1) generada.
     */
    public Map<String, Map<String, List<String>>> getParsingTable() {
        return parsingTable;
    }
}