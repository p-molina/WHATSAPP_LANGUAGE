package entities;

import java.util.*;

public class ParserTableBuilder {
    private Dictionary dictionary;
    private Grammar grammar;
    private Map<String, List<List<String>>> grammarRules;
    private Map<String, Map<String, List<String>>> parsingTable;

    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;

    private static final String END_MARKER = "$";
    private static final String EPSILON = "ε";

    public ParserTableBuilder(Dictionary dictionary, Grammar grammar) {
        this.dictionary = dictionary;
        this.grammar = grammar;
        this.parsingTable = new HashMap<>();
        this.grammarRules = grammar.getGrammarRules();
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    /**
     * Construye la tabla de parsing LL(1).
     */
    public void buildParsingTable() {
        this.firstSets = computeFirstSets(this.grammarRules);
        this.followSets = computeFollowSets(this.grammarRules, this.firstSets, "<AXIOMA>");

        initParsingTable();
        fillParsingTable();

        // Opcional: imprimir tabla para debug
        printingTable();
    }

    /**
     * Devuelve la tabla de parsing LL(1) generada.
     */
    public Map<String, Map<String, List<String>>> getParsingTable() {
        return parsingTable;
    }

    // ------------------------
    // 1. Cálculo de FIRST
    // ------------------------
    public static Map<String, Set<String>> computeFirstSets(Map<String, List<List<String>>> grammarRules) {
        Map<String, Set<String>> first = new HashMap<>();
        Set<String> nonTerminals = grammarRules.keySet();

        for (String nt : nonTerminals) {
            first.put(nt, new HashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (String A : nonTerminals) {
                for (List<String> production : grammarRules.get(A)) {
                    Set<String> firstA = first.get(A);
                    int beforeSize = firstA.size();

                    boolean derivesEpsilonAll = true;
                    for (String symbol : production) {
                        if (nonTerminals.contains(symbol)) {
                            Set<String> firstSym = new HashSet<>(first.get(symbol));
                            firstSym.remove(EPSILON);
                            firstA.addAll(firstSym);

                            if (!first.get(symbol).contains(EPSILON)) {
                                derivesEpsilonAll = false;
                                break;
                            }
                        } else {
                            firstA.add(symbol);
                            derivesEpsilonAll = false;
                            break;
                        }
                    }

                    if (derivesEpsilonAll) {
                        firstA.add(EPSILON);
                    }

                    if (firstA.size() > beforeSize) {
                        changed = true;
                    }
                }
            }
        } while (changed);

        System.out.println("\n<< FIRST >>\n");
        first.forEach((nt, fset) -> System.out.println("FIRST(" + nt + ") = " + fset));
        return first;
    }

    // -------------------------
    // 2. Cálculo de FOLLOW
    // -------------------------
    public static Map<String, Set<String>> computeFollowSets(
            Map<String, List<List<String>>> grammarRules,
            Map<String, Set<String>> firstSets,
            String startSymbol) {

        Map<String, Set<String>> follow = new HashMap<>();
        for (String nt : grammarRules.keySet()) {
            follow.put(nt, new HashSet<>());
        }
        follow.get(startSymbol).add(END_MARKER);

        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<List<String>>> entry : grammarRules.entrySet()) {
                String A = entry.getKey();
                for (List<String> production : entry.getValue()) {
                    Set<String> trailer = new HashSet<>(follow.get(A));
                    for (int i = production.size() - 1; i >= 0; i--) {
                        String symbol = production.get(i);
                        if (grammarRules.containsKey(symbol)) {
                            Set<String> followSym = follow.get(symbol);
                            int before = followSym.size();
                            followSym.addAll(trailer);
                            if (followSym.size() > before) {
                                changed = true;
                            }

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
                            trailer.clear();
                            trailer.add(symbol);
                        }
                    }
                }
            }
        } while (changed);

        System.out.println("\n<< FOLLOW >>\n");
        follow.forEach((nt, f) -> System.out.printf("FOLLOW(%s) = %s%n", nt, f));
        return follow;
    }

    // -------------------------
    // 3. Construcción Tabla
    // -------------------------
    private void initParsingTable() {
        for (String nonTerminal : grammarRules.keySet()) {
            parsingTable.put(nonTerminal, new HashMap<>());
        }
    }

    private void fillParsingTable() {
        // Conjunto de terminales (tokens) + marcador final
        Set<String> terminals = new HashSet<>(dictionary.getTokenPatterns().keySet());
        terminals.add(END_MARKER);

        for (String A : grammarRules.keySet()) {
            Map<String, List<String>> row = parsingTable.get(A);
            for (List<String> prod : grammarRules.get(A)) {
                Set<String> firstAlpha = computeFirstOfSequence(prod);
                // 1) FIRST(α) \ {ε}
                for (String t : firstAlpha) {
                    if (EPSILON.equals(t) || !terminals.contains(t)) continue;
                    if (row.containsKey(t)) {
                        throw new RuntimeException("Tabla LL(1) conflictiva: " + A + " / " + t);
                    }
                    row.put(t, prod);
                }
                // 2) Si ε ∈ FIRST(α), usar FOLLOW(A)
                if (firstAlpha.contains(EPSILON)) {
                    for (String b : followSets.get(A)) {
                        if (!terminals.contains(b) || row.containsKey(b)) continue;
                        row.put(b, prod);
                    }
                }
            }
        }
    }

    private Set<String> computeFirstOfSequence(List<String> symbols) {
        Set<String> result = new HashSet<>();
        boolean allEpsilon = true;
        for (String sym : symbols) {
            Set<String> symFirst;
            if (firstSets.containsKey(sym)) {
                symFirst = new HashSet<>(firstSets.get(sym));
            } else {
                symFirst = new HashSet<>();
                symFirst.add(sym);
            }
            if (symFirst.contains(EPSILON)) {
                symFirst.remove(EPSILON);
                result.addAll(symFirst);
            } else {
                result.addAll(symFirst);
                allEpsilon = false;
                break;
            }
        }
        if (allEpsilon) result.add(EPSILON);
        return result;
    }

    // Método opcional para debug
    private void printingTable() {
        System.out.println("\n<< LL(1) TABLE >>");
        parsingTable.forEach((nt, row) -> {
            System.out.println("NonTerminal: " + nt);
            row.forEach((t, prod) -> System.out.println("  [" + t + "] -> " + prod));
        });
    }
}
