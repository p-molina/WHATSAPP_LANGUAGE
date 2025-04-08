package entities;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

public class ParserTableBuilder {
    private Dictionary dictionary;
    private Grammar grammar;

    private Map<String, Map<String, List<String>>> parsingTable;

    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;

    private static final String END_MARKER = "$";
    private static final String EPSILON = "ε";

    public ParserTableBuilder(Dictionary dictionary, Grammar grammar) {
        this.dictionary = dictionary;
        this.grammar = grammar;
        this.parsingTable = new HashMap<>();
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    /**
     * Construye la tabla de parsing LL(1).
     */
    public void buildParsingTable() {
        // Calcular FIRST para todos los no terminales y sus producciones
        computeFirstSets();

        // Calcular FOLLOW para todos los no terminales
        computeFollowSets();

        // Inicializar la tabla (fila por cada noTerminal, columna por cada terminal + $)
        initParsingTable();

        // Rellenar la tabla usando FIRST y FOLLOW
        fillParsingTable();
    }

    /**
     * Devuelve la tabla de parsing LL(1) generada.
     * Estructura: parsingTable[NoTerminal][Terminal] = List<String> (la producción)
     */
    public Map<String, Map<String, List<String>>> getParsingTable() {
        return parsingTable;
    }

    // ------------------------
    // | 1. Cálculo de FIRST  |
    // ------------------------
    private void computeFirstSets() {
        // Inicializar conjuntos FIRST vacíos para cada símbolo no terminal
        for (String nonTerminal : grammar.getGrammarRules().keySet()) {
            firstSets.put(nonTerminal, new HashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            // Recorremos cada regla A
            for (String A : grammar.getGrammarRules().keySet()) {
                List<List<String>> productions = grammar.getGrammarRules().get(A);

                for (List<String> alpha : productions) {
                    // Hallar FIRST(α)
                    Set<String> alphaFirst = computeFirstOfSequence(alpha);

                    // Añadir FIRST(α) a FIRST(A)
                    Set<String> currentFirstA = firstSets.get(A);
                    int oldSize = currentFirstA.size();
                    currentFirstA.addAll(alphaFirst);
                    if (currentFirstA.size() > oldSize) {
                        changed = true;
                    }
                }
            }
        }
    }

    /**
     * FIRST de una secuencia de símbolos (puede ser varios o uno) en forma de lista.
     * - Recorremos símbolo por símbolo.
     * - Si es terminal, se añade y paramos.
     * - Si es no terminal, agregamos FIRST(noTerminal) y si FIRST(noTerminal) contiene ε, seguimos.
     * - Si todos pueden producir ε, agregamos ε.
     */
    private Set<String> computeFirstOfSequence(List<String> symbols) {
        Set<String> result = new HashSet<>();
        // Secuencia vacía -> FIRST = {ε}
        if (symbols.isEmpty()) {
            result.add(EPSILON);
            return result;
        }

        for (int i = 0; i < symbols.size(); i++) {
            String s = symbols.get(i);
            // Si es terminal (lo reconocemos porque está en dictionary o es algo como +, -),
            // lo añadimos y paramos
            if (isTerminal(s) && !s.equals(EPSILON)) {
                result.add(s);
                break;
            }
            // Si es ε, la añadimos y continuamos
            if (s.equals(EPSILON)) {
                result.add(EPSILON);
                break;
            }
            // Si es un no terminal, añadimos FIRST(noTerminal) excepto ε
            if (grammar.getGrammarRules().containsKey(s)) {
                Set<String> firstOfNonTerminal = firstSets.get(s);
                // Añadimos todo excepto ε
                boolean hasEpsilon = false;
                for (String t : firstOfNonTerminal) {
                    if (!t.equals(EPSILON)) {
                        result.add(t);
                    } else {
                        hasEpsilon = true;
                    }
                }
                if (hasEpsilon) {
                    // Continuamos al siguiente símbolo
                    // Pero si es el último y también tiene ε, lo añadimos
                    if (i == symbols.size() - 1) {
                        result.add(EPSILON);
                    }
                } else {
                    // Si no hay ε, paramos
                    break;
                }
            } else {
                // Si no es noTerminal ni ε, puede ser un terminal no contemplado
                // Lo añadimos y paramos
                result.add(s);
                break;
            }
        }
        return result;
    }

    // ------------------------
    // | 2. Cálculo de FOLLOW |
    // ------------------------
    private void computeFollowSets() {
        // Inicializar FOLLOW(A) vacío para cada no terminal A
        for (String A : grammar.getGrammarRules().keySet()) {
            followSets.put(A, new HashSet<>());
        }
        // Agregar el símbolo $ a FOLLOW del axioma
        String firstNonTerminal = grammar.getGrammarRules().keySet().iterator().next();
        followSets.get(firstNonTerminal).add(END_MARKER);

        boolean changed = true;
        while (changed) {
            changed = false;
            // Para cada regla A -> α
            for (String A : grammar.getGrammarRules().keySet()) {
                List<List<String>> productions = grammar.getGrammarRules().get(A);

                for (List<String> alpha : productions) {
                    // Recorremos la producción símbolo a símbolo
                    for (int i = 0; i < alpha.size(); i++) {
                        String B = alpha.get(i);
                        if (grammar.getGrammarRules().containsKey(B)) {
                            // B es no terminal
                            Set<String> followB = followSets.get(B);
                            int oldSize = followB.size();

                            // Todo lo que esté en FIRST(α(i+1)) excepto ε, está en FOLLOW(B)
                            boolean allNullable = true;
                            for (int j = i + 1; j < alpha.size(); j++) {
                                String nextSymbol = alpha.get(j);
                                Set<String> firstNext = computeFirstOfSequence(
                                        new ArrayList<String>(){{ add(nextSymbol); }});

                                // Agregamos todo excepto ε a FOLLOW(B)
                                for (String t : firstNext) {
                                    if (!t.equals(EPSILON)) {
                                        followB.add(t);
                                    }
                                }

                                // Si no hay ε en FIRST(nextSymbol), paramos
                                if (!firstNext.contains(EPSILON)) {
                                    allNullable = false;
                                    break;
                                }
                            }

                            // Si todos los símbolos siguientes pueden derivar ε
                            // o si B es el último símbolo, entonces FOLLOW(A) subset de FOLLOW(B)
                            if (allNullable || i == alpha.size() - 1) {
                                followB.addAll(followSets.get(A));
                            }

                            if (followB.size() > oldSize) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private void initParsingTable() {
        // Para cada no terminal, creamos un map interno
        for (String nonTerminal : grammar.getGrammarRules().keySet()) {
            parsingTable.put(nonTerminal, new HashMap<>());
        }
    }

    private void fillParsingTable() {
        // Obtenemos la lista de terminales desde Dictionary + el símbolo $
        Set<String> terminals = new HashSet<>(dictionary.getTokenPatterns().keySet());
        terminals.add(END_MARKER); // añadir $ como terminal

        // Recorremos cada producción A -> α
        for (String A : grammar.getGrammarRules().keySet()) {
            List<List<String>> productions = grammar.getGrammarRules().get(A);
            for (List<String> alpha : productions) {
                // Computamos FIRST(α)
                Set<String> firstAlpha = computeFirstOfSequence(alpha);

                // Para cada terminal 't' ∈ FIRST(α), t != ε, añadir:
                // table[A][t] = α
                for (String t : firstAlpha) {
                    if (!t.equals(EPSILON)) {
                        parsingTable.get(A).put(t, alpha);
                    }
                }
                // Si FIRST(α) contiene ε -> para cada 'b' ∈ FOLLOW(A), table[A][b] = α
                if (firstAlpha.contains(EPSILON)) {
                    for (String b : followSets.get(A)) {
                        parsingTable.get(A).put(b, alpha);
                    }
                }
            }
        }
    }

    private boolean isTerminal(String symbol) {
        if (symbol.equals(EPSILON) || symbol.equals(END_MARKER)) {
            return true;
        }
        // Está en la lista de terminales
        if (dictionary.getTokenPatterns().containsKey(symbol)) {
            return true;
        }
        // No es uno de los no terminales
        return !grammar.getGrammarRules().containsKey(symbol);
    }
}
