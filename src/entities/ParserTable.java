package entities;

import java.util.*;

/**
 * Classe responsable de la construcció de la taula de parsing LL(1)
 * per a una determinada gramàtica i conjunts FIRST/FOLLOW.
 */
public class ParserTable {
    /** Taula de parsing: mapeja cada no-terminal a un mapa de terminal a producció. */
    private Map<String, Map<String, List<String>>> parsingTable;
    /** Diccionari de tokens que defineix els terminals reconeguts. */
    private Dictionary dictionary;
    /** Regles de la gramàtica: per a cada no-terminal, llista de produccions. */
    private Map<String, List<List<String>>> grammarRules;
    /** Conjunt FIRST per a cada no-terminal. */
    private Map<String, Set<String>> firstSets;
    /** Conjunt FOLLOW per a cada no-terminal. */
    private Map<String, Set<String>> followSets;
    /** Marcador de final de cadena en la taula (símbol $). */
    private static final String END_MARKER = "$";
    /** Símbol d’epsilon (ε) per a produccions buides. */
    private static final String EPSILON = "ε";

    /**
     * Crea una instància de ParserTable amb els recursos necessaris.
     *
     * @param dictionary
     *   Diccionari de tokens amb els terminals reconeguts.
     * @param grammarRules
     *   Map que associa cada no-terminal amb la seva llista de produccions.
     * @param firstSets
     *   Conjunt FIRST per a cada no-terminal previament calculat.
     * @param followSets
     *   Conjunt FOLLOW per a cada no-terminal previament calculat.
     */
    public ParserTable(
            Dictionary dictionary,
            Map<String, List<List<String>>> grammarRules,
            Map<String, Set<String>> firstSets,
            Map<String, Set<String>> followSets
    ) {
        this.dictionary = dictionary;
        this.grammarRules = grammarRules;
        this.firstSets = firstSets;
        this.followSets = followSets;
        this.parsingTable = new HashMap<>();
    }

    /**
     * Inicialitza la taula de parsing creant una fila buida
     * per a cada no-terminal de la gramàtica.
     */
    public void initParsingTable() {
        for (String nonTerminal : grammarRules.keySet()) {
            parsingTable.put(nonTerminal, new HashMap<>());
        }
    }

    /**
     * Omple la taula de parsing LL(1) seguint les regles:
     * <ol>
     *   <li>Per a cada producció A → α, assigna α a M[A, t] per a
     *       tots els terminals t ∈ FIRST(α) \ {ε}.</li>
     *   <li>Si ε ∈ FIRST(α), assigna α també a M[A, b] per a
     *       tots els terminals b ∈ FOLLOW(A).</li>
     * </ol>
     *
     * @throws RuntimeException
     *   Si es detecta un conflicte en la taula (més d’una producció
     *   per a la mateixa cel·la M[A, t]).
     */
    public void fillParsingTable() {
        // Conjunt de terminals (claus del diccionari) + marcador final
        Set<String> terminals = new HashSet<>(dictionary.getTokenPatterns().keySet());
        terminals.add(END_MARKER);

        // Per a cada no-terminal i cada producció
        for (String A : grammarRules.keySet()) {
            Map<String, List<String>> row = parsingTable.get(A);
            for (List<String> prod : grammarRules.get(A)) {
                // FIRST(α) de la seqüència prod
                Set<String> firstAlpha = computeFirstOfSequence(prod);
                // FIRST(α) \ {ε}
                for (String t : firstAlpha) {
                    if (EPSILON.equals(t) || !terminals.contains(t)) continue;
                    if (row.containsKey(t)) {
                        throw new RuntimeException("Taula LL(1) conflictiva: no-terminal " + A + " / terminal " + t);
                    }
                    row.put(t, prod);
                }
                // Si ε ∈ FIRST(α), utilitzar FOLLOW(A)
                if (firstAlpha.contains(EPSILON)) {
                    for (String b : followSets.get(A)) {
                        if (!terminals.contains(b) || row.containsKey(b)) continue;
                        row.put(b, prod);
                    }
                }
            }
        }
    }

    /**
     * Calcula el conjunt FIRST d’una seqüència de símbols α.
     *
     * @param symbols
     *   Llista de símbols (terminals o no-terminals) que formen α.
     * @return
     *   Conjunt de terminals que poden aparèixer inicialment en α,
     *   inclòs ε si tots els símbols poden derivar ε.
     */
    private Set<String> computeFirstOfSequence(List<String> symbols) {
        Set<String> result = new HashSet<>();
        boolean allEpsilon = true;
        for (String sym : symbols) {
            Set<String> symFirst;
            if (firstSets.containsKey(sym)) {
                symFirst = new HashSet<>(firstSets.get(sym));
            } else {
                // És un terminal, així que FIRST(sym) = {sym}
                symFirst = new HashSet<>();
                symFirst.add(sym);
            }

            // Si conté ε, afegeix la resta i continua
            if (symFirst.contains(EPSILON)) {
                symFirst.remove(EPSILON);
                result.addAll(symFirst);
            } else {
                // Si no pot derivar ε, afegeix FIRST(sym) i atura
                result.addAll(symFirst);
                allEpsilon = false;
                break;
            }
        }
        // Si tots poden derivar ε, inclou ε en el resultat
        if (allEpsilon) {
            result.add(EPSILON);
        }
        return result;
    }

    /**
     * Retorna la taula de parsing LL(1) construïda.
     *
     * @return
     *   Map de no-terminal a (terminal → producció).
     */
    public Map<String, Map<String, List<String>>> getTable() {
        return parsingTable;
    }
}