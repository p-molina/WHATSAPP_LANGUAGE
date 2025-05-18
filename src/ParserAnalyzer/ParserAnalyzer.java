package ParserAnalyzer;

import entities.Grammar;
import entities.Node;
import entities.Token;
import LexicalAnalyzer.LexicalAnalyzer;

import java.util.*;

/**
 * Classe encarregada de realitzar el parsing predictiu LL(1)
 * sobre una seqüència de tokens obtinguda d’un analitzador lèxic.
 *
 * <p>Utilitza una pila de símbols i una pila de nodes per construir
 * l’arbre sintàctic a partir d’una taula LL(1) prèviament generada.</p>
 */
public class ParserAnalyzer {
    /** Marcador que indica el final de la llista de tokens. */
    private static final String END_MARKER = "$";
    /** Símbol que representa ε (epsilon). */
    private static final String EPSILON    = "ε";
    /** Gramàtica amb les regles de no-terminis i produccions. */
    private final Grammar grammar;
    /** Taula LL(1): mapeja cada no-terminal i terminal a la producció corresponent. */
    private final Map<String, Map<String, List<String>>> table;

    /**
     * Construeix un {@code ParserAnalyzer} amb la gramàtica i el builder de taula.
     *
     * @param grammar
     *   Objecte {@link Grammar} que conté les regles de la gramàtica.
     * @param builder
     *   Instància de {@link ParserTableGenerator} amb la taula LL(1) ja construïda.
     */
    public ParserAnalyzer(Grammar grammar, ParserTableGenerator builder) {
        this.grammar = grammar;
        this.table   = builder.getParsingTable();
    }

    /**
     * Realitza el parsing LL(1) de la seqüència de tokens proporcionada pel lexer.
     *
     * @param lexer
     *   Instància de {@link LexicalAnalyzer} que conté els tokens a parsejar.
     * @return
     *   El {@link Node} arrel de l’arbre de parsing generat.
     * @throws RuntimeException
     *   Si es produeix un error sintàctic (token inesperat) o no es troba entrada a la taula.
     */
    public Node parse(LexicalAnalyzer lexer) {
        // Llista de tokens + marcador de final
        List<Token> tokens = new ArrayList<>(lexer.getTokens());
        tokens.add(new Token(END_MARKER, END_MARKER, -1, -1));

        // Piles per a símbols i nodes
        Deque<String> symbolStack = new ArrayDeque<>();
        Deque<Node>   nodeStack   = new ArrayDeque<>();

        // Inicialització: END_MARKER, després l'axioma
        symbolStack.push(END_MARKER);
        symbolStack.push("<AXIOMA>");
        Node root = new Node("<AXIOMA>");
        nodeStack.push(root);

        int index = 0;
        // Loop fins trobar END_MARKER
        while (!symbolStack.isEmpty()) {
            String topSym = symbolStack.pop();
            // Si trobem el marcador de final, sortim
            if (END_MARKER.equals(topSym)) {
                break;
            }

            Node cur = nodeStack.pop();
            Token look = tokens.get(index);

            // Si es ε, l'ignorem
            if (EPSILON.equals(topSym)) {
                continue;
            }

            // Si és terminal, comprovem coincidència
            if (isTerminal(topSym)) {
                if (topSym.equals(look.getType())) {
                    cur.setToken(look);
                    index++;
                } else {
                    throw new RuntimeException(
                            String.format("Error sintàctic: s’esperava %s però ha arribat %s en línia %d, columna %d",
                                    topSym, look.getType(), look.getLine(), look.getColumn())
                    );
                }

            } else {
                // No-terminal: buscar producció a la taula
                Map<String, List<String>> row = table.get(topSym);
                if (row == null) {
                    throw new RuntimeException("No hi ha fila per al no-terminal " + topSym);
                }
                List<String> production = row.get(look.getType());
                if (production == null) {
                    throw new RuntimeException(
                            String.format(String.valueOf(
                                    GramaticalErrorType.GRAMATICAL_ERROR_TYPE),
                                    look.getLine(), look.getLexeme()
                            )
                    );
                }

                // Crear i enllaçar nodes fills
                List<Node> children = new ArrayList<>();
                for (String sym : production) {
                    Node child = new Node(sym);
                    children.add(child);
                    cur.addChild(child);
                }

                // Apilar símbols i nodes en ordre invers
                for (int i = production.size() - 1; i >= 0; i--) {
                    symbolStack.push(production.get(i));
                    nodeStack.push(children.get(i));
                }
            }
        }
        return root;
    }

    /**
     * Determina si un símbol és terminal.
     *
     * <p>Considera terminals tots els símbols que no siguin no-terminis de la gramàtica,
     * així com ε i el marcador de final.</p>
     *
     * @param sym
     *   Cadena que representa el símbol a avaluar.
     * @return
     *   {@code true} si és terminal o marcador, {@code false} si és no-terminal.
     */
    private boolean isTerminal(String sym) {
        if (EPSILON.equals(sym) || END_MARKER.equals(sym)) return true;
        return !grammar.getGrammarRules().containsKey(sym);
    }
}