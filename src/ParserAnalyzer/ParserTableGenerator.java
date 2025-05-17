package ParserAnalyzer;

import java.util.*;
import entities.Dictionary;
import entities.Grammar;
import entities.ParserTable;

/**
 * Classe responsable de generar la taula de parsing LL(1) a partir
 * d’un diccionari de tokens i una gramàtica.
 */
public class ParserTableGenerator {
    private Dictionary dictionary;
    private Grammar grammar;
    private Map<String, Map<String, List<String>>> parsingTable;

    /**
     * Crea una instància de ParserTableGenerator.
     *
     * @param dictionary Diccionari de tokens amb els terminals reconeguts.
     * @param grammar    Gramàtica que defineix els no-terminis i les seves regles.
     */
    public ParserTableGenerator(Dictionary dictionary, Grammar grammar) {
        this.dictionary = dictionary;
        this.grammar = grammar;
        this.parsingTable = new HashMap<>();
    }

    /**
     * Genera la taula de parsing LL(1).
     *
     * Aquest mètode realitza els passos següents:
     * <ol>
     *   <li>Obtenir les regles de la gramàtica.</li>
     *   <li>Calcular els conjunts FIRST i FOLLOW.</li>
     *   <li>Inicialitzar i omplir la taula de parsing amb la classe ParserTable.</li>
     *   <li>Emmagatzemar el resultat a l’atribut {@code parsingTable}.</li>
     * </ol>
     */
    public void buildParsingTable() {
        // Obtenir les regles de la gramàtica
        Map<String, List<List<String>>> grammarRules = grammar.getGrammarRules();
        FirstFollow calculator = new FirstFollow(grammarRules);

        // Calcular FIRST y FOLLOW
        Map<String, Set<String>> firstSets = calculator.computeFirstSets();
        Map<String, Set<String>> followSets = calculator.computeFollowSets("<AXIOMA>");

        // Crear i omplir la taula de parsing
        ParserTable parserTable = new ParserTable(dictionary, grammarRules, firstSets, followSets);
        parserTable.initParsingTable();
        parserTable.fillParsingTable();

        // Desa la taula final
        this.parsingTable = parserTable.getTable();
    }

    /**
     * Retorna la taula de parsing LL(1) construïda.
     *
     * @return
     *   Un {@code Map} on cada clau és un no-terminal i cada valor és
     *   un altre {@code Map} que associa cada terminal a la llista
     *   de símbols de la producció corresponent.
     */
    public Map<String, Map<String, List<String>>> getParsingTable() {
        return parsingTable;
    }
}