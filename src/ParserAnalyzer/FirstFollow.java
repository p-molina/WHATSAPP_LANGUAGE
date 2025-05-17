package ParserAnalyzer;

import java.util.*;

/**
 * Classe encarregada de calcular els conjunts FIRST i FOLLOW
 * per a una gramàtica donada, útils per construir una taula LL(1).
 */
public class FirstFollow {
    /** Regles de la gramàtica: per cada no-terminal, llista de produccions. */
    private Map<String, List<List<String>>> grammarRules;

    /** Conjunt FIRST per a cada no-terminal. */
    private Map<String, Set<String>> firstSets;

    /** Conjunt FOLLOW per a cada no-terminal. */
    private Map<String, Set<String>> followSets;

    /** Marcador de final de cadena en FOLLOW. */
    private static final String END_MARKER = "$";

    /** Símbol que representa ε (epsilon). */
    private static final String EPSILON = "ε";

    /**
     * Crea un calculador de FIRST/FOLLOW per a la gramàtica.
     *
     * @param grammarRules
     *   {@code Map} que associa cada no-terminal amb la seva llista de produccions,
     *   on cada producció és una llista de símbols (terminals o no-terminals).
     */
    public FirstFollow(Map<String, List<List<String>>> grammarRules) {
        this.grammarRules = grammarRules;
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    /**
     * Calcula els conjunts FIRST per a tots els no-terminals de la gramàtica.
     *
     * <p>L’algorisme és iteratiu i fa un “fixpoint” sobre les regles:</p>
     * <ol>
     *   <li>Inicialitza FIRST(A) = ∅ per a cada no-terminal A.</li>
     *   <li>Per a cada producció A → α,
     *     afegeix a FIRST(A) els terminals o ε de FIRST(α).</li>
     *   <li>Repeteix fins que no hi hagi més canvis.</li>
     * </ol>
     *
     * @return
     *   Un {@code Map<String, Set<String>>} amb el conjunt FIRST per a cada no-terminal.
     */
    public Map<String, Set<String>> computeFirstSets() {
        // Inicialitza els conjunts FIRST buits
        Set<String> nonTerminals = grammarRules.keySet();
        for (String nt : nonTerminals) {
            firstSets.put(nt, new HashSet<>());
        }

        boolean changed;
        // Itera fins arribar a fixpoint
        do {
            changed = false;
            for (String A : nonTerminals) {
                for (List<String> production : grammarRules.get(A)) {
                    Set<String> firstA = firstSets.get(A);
                    int beforeSize = firstA.size();
                    boolean derivesEpsilonAll = true;

                    // Recorre cada símbol de la producció
                    for (String symbol : production) {
                        if (nonTerminals.contains(symbol)) {
                            // Si és no-terminal, afegeix FIRST(sym) \ {ε}
                            Set<String> firstSym = new HashSet<>(firstSets.get(symbol));
                            firstSym.remove(EPSILON);
                            firstA.addAll(firstSym);

                            // Si sym NO deriva ε, atura
                            if (!firstSets.get(symbol).contains(EPSILON)) {
                                derivesEpsilonAll = false;
                                break;
                            }
                        } else {
                            // Si és terminal, s’afegeix directament i atura
                            firstA.add(symbol);
                            derivesEpsilonAll = false;
                            break;
                        }
                    }

                    // Si tot deriva ε, afegeix ε
                    if (derivesEpsilonAll) {
                        firstA.add(EPSILON);
                    }

                    // Comprova si s’ha afegit algun element nou
                    if (firstA.size() > beforeSize) {
                        changed = true;
                    }
                }
            }
        } while (changed);

        return firstSets;
    }

    /**
     * Calcula els conjunts FOLLOW per a tots els no-terminals de la gramàtica.
     *
     * <p>L’algorisme:</p>
     * <ol>
     *   <li>Inicialitza FOLLOW(A) = ∅ per a cada no-terminal A.</li>
     *   <li>Afegeix END_MARKER a FOLLOW(startSymbol).</li>
     *   <li>Per a cada producció A → α, recorre α cap enrere i propaga
     *       terminals/FIRST/símbol de seguinça (trailer).</li>
     *   <li>Repeteix fins a fixpoint.</li>
     * </ol>
     *
     * @param startSymbol
     *   No-terminal inicial (axioma) a partir del qual començar el FOLLOW.
     * @return
     *   Un {@code Map<String, Set<String>>} amb el conjunt FOLLOW per a cada no-terminal.
     */
    public Map<String, Set<String>> computeFollowSets(String startSymbol) {
        // Inicialitza els conjunts FOLLOW buits
        Set<String> nonTerminals = grammarRules.keySet();
        for (String nt : nonTerminals) {
            followSets.put(nt, new HashSet<>());
        }
        // Afegeix el símbol del final a l’axioma
        followSets.get(startSymbol).add(END_MARKER);

        boolean changed;
        // Itera fins a fixpoint
        do {
            changed = false;
            for (Map.Entry<String, List<List<String>>> entry : grammarRules.entrySet()) {
                String A = entry.getKey();
                for (List<String> production : entry.getValue()) {
                    // Trailer comença amb FOLLOW(A)
                    Set<String> trailer = new HashSet<>(followSets.get(A));
                    List<String> symbols = production;

                    // Recorre la producció de dreta a esquerra
                    for (int i = symbols.size() - 1; i >= 0; i--) {
                        String symbol = symbols.get(i);
                        if (nonTerminals.contains(symbol)) {
                            Set<String> followSym = followSets.get(symbol);
                            int before = followSym.size();
                            // Afegeix el trailer actual
                            followSym.addAll(trailer);
                            if (followSym.size() > before) {
                                changed = true;
                            }
                            // Actualitza trailer segons FIRST(sym)
                            Set<String> firstSym = firstSets.get(symbol);
                            if (firstSym.contains(EPSILON)) {
                                Set<String> minusEps = new HashSet<>(firstSym);
                                minusEps.remove(EPSILON);
                                trailer.addAll(minusEps);
                            } else {
                                trailer = new HashSet<>(firstSym);
                                trailer.remove(EPSILON);
                            }
                        } else {
                            // Si és terminal, el nou trailer és només aquest símbol
                            trailer.clear();
                            trailer.add(symbol);
                        }
                    }
                }
            }
        } while (changed);

        return followSets;
    }

    /**
     * Obté el conjunt FIRST calculat, pot servir per a consultes externes.
     *
     * @return Map de no-terminal a conjunt FIRST.
     */
    public Map<String, Set<String>> getFirstSets() {
        return firstSets;
    }

    /**
     * Obté el conjunt FOLLOW calculat, pot servir per a consultes externes.
     *
     * @return Map de no-terminal a conjunt FOLLOW.
     */
    public Map<String, Set<String>> getFollowSets() {
        return followSets;
    }
}