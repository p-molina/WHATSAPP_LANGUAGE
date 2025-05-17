package ParserAnalyzer;

import java.util.*;

public class FirstFollow {
    private Map<String, List<List<String>>> grammarRules;
    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;
    private static final String END_MARKER = "$";
    private static final String EPSILON = "ε";

    public FirstFollow(Map<String, List<List<String>>> grammarRules) {
        this.grammarRules = grammarRules;
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    public Map<String, Set<String>> computeFirstSets() {
        Set<String> nonTerminals = grammarRules.keySet();
        for (String nt : nonTerminals) {
            firstSets.put(nt, new HashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (String A : nonTerminals) {
                for (List<String> production : grammarRules.get(A)) {
                    Set<String> firstA = firstSets.get(A);
                    int beforeSize = firstA.size();

                    boolean derivesEpsilonAll = true;
                    for (String symbol : production) {
                        if (nonTerminals.contains(symbol)) {
                            Set<String> firstSym = new HashSet<>(firstSets.get(symbol));
                            firstSym.remove(EPSILON);
                            firstA.addAll(firstSym);

                            if (!firstSets.get(symbol).contains(EPSILON)) {
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

        return firstSets;
    }

    public Map<String, Set<String>> computeFollowSets(String startSymbol) {
        Set<String> nonTerminals = grammarRules.keySet();
        for (String nt : nonTerminals) {
            followSets.put(nt, new HashSet<>());
        }
        followSets.get(startSymbol).add(END_MARKER);

        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<List<String>>> entry : grammarRules.entrySet()) {
                String A = entry.getKey();
                for (List<String> production : entry.getValue()) {
                    Set<String> trailer = new HashSet<>(followSets.get(A));
                    List<String> symbols = production;
                    for (int i = symbols.size() - 1; i >= 0; i--) {
                        String symbol = symbols.get(i);
                        if (nonTerminals.contains(symbol)) {
                            Set<String> followSym = followSets.get(symbol);
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

        return followSets;
    }

    public Map<String, Set<String>> getFirstSets() {
        return firstSets;
    }

    public Map<String, Set<String>> getFollowSets() {
        return followSets;
    }
}