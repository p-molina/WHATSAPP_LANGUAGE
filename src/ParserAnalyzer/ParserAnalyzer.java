package ParserAnalyzer;

import entities.Dictionary;
import entities.Grammar;
import entities.ParserTableBuilder;
import entities.Node;
import entities.Token;

import java.util.*;

public class ParserAnalyzer {
    private Dictionary dictionary;
    private Grammar grammar;

    private Map<String, Map<String, List<String>>> parsingTable;

    private static final String END_MARKER = "$";
    private static final String EPSILON = "ε";

    public ParserAnalyzer(Dictionary dictionary, Grammar grammar) {
        this.dictionary = dictionary;
        this.grammar = grammar;

        ParserTableBuilder builder = new ParserTableBuilder(dictionary, grammar);
        builder.buildParsingTable();
        this.parsingTable = builder.getParsingTable();
    }

    /**
     * Realiza el parse de la lista de tokens y retorna la raíz del AST.
     *
     * @param tokens Lista de tokens generada por LexicalAnalyzer.
     * @return Nodo raíz del AST.
     */
    public Node parse(List<Token> tokens) {
        tokens.add(new Token(END_MARKER, END_MARKER, -1, -1));
        LinkedList<Node> stack = new LinkedList<>();

        String startSymbol = "<AXIOMA>";

        Node root = new Node(startSymbol);
        Node dollarNode = new Node(END_MARKER);

        stack.push(dollarNode);
        stack.push(root);

        int index = 0;
        Token lookahead = tokens.get(index);

        while (!stack.isEmpty()) {
            Node topNode = stack.peek();
            String topSymbol = topNode.getSymbol();

            // Si la cima es un terminal (o $)
            if (isTerminal(topSymbol)) {
                // Comprobamos si coincide con el lookahead
                if (topSymbol.equals(lookahead.getType()) || topSymbol.equals(lookahead.getLexeme())) {
                    // Asignamos el Token real a este node (si no es '$')
                    if (!topSymbol.equals(END_MARKER)) {
                        topNode.setToken(lookahead);
                    }
                    stack.pop();
                    index++;
                    if (index < tokens.size()) {
                        lookahead = tokens.get(index);
                    }
                } else {
                    throw new RuntimeException(
                            "Error: se esperaba el terminal '" + topSymbol + "' pero llegó '" + lookahead + "'"
                    );
                }
            }
            // Si la cima es un no terminal
            else {
                // Buscamos la producción en la tabla [noTerminal, lookahead]
                List<String> production = getProduction(topSymbol, lookahead);

                if (production == null) {
                    // No existe entrada en la tabla => Error
                    throw new RuntimeException(
                            "Error sintáctico: No hay producción para <" + topSymbol + ", " + lookahead + ">"
                    );
                } else {
                    // Expandir la producción
                    stack.pop();

                    // Si la producción no es simplemente [ε]
                    if (!(production.size() == 1 && production.get(0).equals(EPSILON))) {
                        // Apilamos los símbolos de la producción en orden inverso
                        for (int i = production.size() - 1; i >= 0; i--) {
                            String symbol = production.get(i);
                            if (!symbol.equals(EPSILON)) {
                                Node child = new Node(symbol);
                                stack.push(child);
                            }
                        }

                        // Creamos hijos en orden natural para el árbol
                        List<Node> childrenInNaturalOrder = new ArrayList<>();
                        for (String symbol : production) {
                            if (!symbol.equals(EPSILON)) {
                                childrenInNaturalOrder.add(new Node(symbol));
                            }
                        }

                        // Añadimos como hijos del topNode
                        for (Node c : childrenInNaturalOrder) {
                            topNode.addChild(c);
                        }
                    } else {
                        // Producción -> ε
                        topNode.addChild(new Node(EPSILON));
                    }
                }
            }

            if (!stack.isEmpty() &&
                    stack.peek().getSymbol().equals(END_MARKER) &&
                    lookahead.getType().equals(END_MARKER)) {
                stack.pop(); // consumimos $
                break;
            }
        }

        return root;
    }

    /**
     * Devuelve la producción table[nonTerminal][lookahead], o null si no existe.
     */
    private List<String> getProduction(String nonTerminal, Token lookahead) {
        Map<String, List<String>> row = parsingTable.get(nonTerminal);
        if (row == null) {
            return null;
        }

        List<String> production = row.get(lookahead.getType());
        if (production != null) {
            return production;
        }

        production = row.get(lookahead.getLexeme());
        if (production != null) {
            return production;
        }

        return null;
    }

    /**
     * Distingue si un símbolo es terminal o no.
     * Asumimos que un símbolo es terminal si:
     *  - Es "$" o "ε"
     *  - No está en grammarRules (o sea, no es un noTerminal)
     */
    private boolean isTerminal(String symbol) {
        if (symbol.equals(END_MARKER) || symbol.equals(EPSILON)) {
            return true;
        }
        return !grammar.getGrammarRules().containsKey(symbol);
    }

    public Map<String, Map<String, List<String>>> getParsingTable() {
        return parsingTable;
    }

}
