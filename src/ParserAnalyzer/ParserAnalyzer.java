// src/ParserAnalyzer/ParserAnalyzer.java
package ParserAnalyzer;

import entities.Grammar;
import entities.Node;
import entities.Token;
import entities.ParserTableBuilder;
import LexicalAnalyzer.LexicalAnalyzer;

import java.util.*;

public class ParserAnalyzer {
    private static final String END_MARKER = "$";
    private static final String EPSILON    = "ε";

    private final Grammar grammar;
    private final Map<String, Map<String, List<String>>> table;

    public ParserAnalyzer(Grammar grammar, ParserTableBuilder builder) {
        this.grammar = grammar;
        this.table   = builder.getParsingTable();
    }

    /**
     * Arranca el parseo LL(1) con el lexer ya tokenizado.
     * @return la raíz del árbol de parseo
     */
    public Node parse(LexicalAnalyzer lexer) {
        // 1) Obtener lista de tokens + marcador de fin
        List<Token> tokens = new ArrayList<>(lexer.getTokens());
        tokens.add(new Token(END_MARKER, END_MARKER, -1, -1));

        // 2) Pilas: símbolos y nodos
        Deque<String> symbolStack = new ArrayDeque<>();
        Deque<Node>   nodeStack   = new ArrayDeque<>();

        // 3) Inicializar
        symbolStack.push(END_MARKER);
        symbolStack.push("<AXIOMA>");
        Node root = new Node("<AXIOMA>");
        nodeStack.push(root);

        int index = 0;
        while (!symbolStack.isEmpty()) {
            String topSym = symbolStack.pop();
            Node   cur    = nodeStack.pop();
            Token  look   = tokens.get(index);

            // 4) Si es ε, lo ignoramos
            if (EPSILON.equals(topSym)) {
                continue;
            }

            // 5) Si es terminal, hacemos match
            if (isTerminal(topSym)) {
                if (topSym.equals(look.getType())) {
                    // rellenamos el nodo hoja con el token
                    cur.setToken(look);
                    index++;
                } else {
                    throw new RuntimeException(
                            String.format("Error sintáctico: esperaba %s pero llegó %s en línea %d,col %d",
                                    topSym, look.getType(), look.getLine(), look.getColumn())
                    );
                }
            } else {
                // 6) No terminal: mirar la tabla [NT][lookahead]
                Map<String, List<String>> row = table.get(topSym);
                if (row == null) {
                    throw new RuntimeException("No existe fila para no terminal " + topSym);
                }
                List<String> production = row.get(look.getType());
                if (production == null) {
                    throw new RuntimeException(
                            String.format("Error sintáctico: no hay producción para %s con '%s'",
                                    topSym, look.getType())
                    );
                }

                // 7) Crear nodos hijos y anexarlos
                List<Node> children = new ArrayList<>();
                for (String sym : production) {
                    Node child = new Node(sym);
                    children.add(child);
                    cur.addChild(child);
                }

                // 8) Apilar en orden inverso
                for (int i = production.size() - 1; i >= 0; i--) {
                    symbolStack.push(production.get(i));
                    nodeStack.push(children.get(i));
                }
            }
        }

        return root;
    }

    private boolean isTerminal(String sym) {
        if (EPSILON.equals(sym) || END_MARKER.equals(sym)) return true;
        // un terminal es cualquier cosa que no esté en la gramática
        return !grammar.getGrammarRules().containsKey(sym);
    }
}
