package TAC;

import entities.Node;
import entities.Symbol;
import entities.SymbolTable;
import entities.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * Generador de codi TAC (Three-Address Code) a partir de l'arbre sintàctic (AST) d'un programa.
 * Utilitza la taula de símbols per resoldre noms, tipus i generar codi intermediar.
 *
 * Aquesta classe recorre l'AST de manera recursiva, identificant blocs de control,
 * declaracions, assignacions, operacions i comparacions per construir instruccions TAC.
 */
public class TACGenerator {
    /**
     * Llista d'instruccions TAC
     */
    private final List<String> code = new ArrayList<>();
    /**
     * Pila per a temporals intermedis
     */
    private final Deque<String> stack = new ArrayDeque<>();
    /**
     * Map de literals a temporals (reutilització)
     */
    private final Map<String, String> literalToTemp = new HashMap<>();
    /**
     * Map de variables a temporals TAC
     */
    private final Map<String, String> varToTemp = new HashMap<>();
    /**
     * Comptador per a etiquetes
     */
    private int labelCounter = 0;
    /**
     * Comptador per a variables temporals
     */
    private int tempCounter = 0;
    /**
     * Identificador actual en assignacions
     */
    private String currentId = null;
    /**
     * Taula de símbols
     */
    private SymbolTable symbolTable;
    /**
     * Conjunt de noms de funcions
     */
    private final Set<String> functions = new HashSet<>();

    public TACGenerator() {}

    /**
     * Genera codi TAC per a l'AST especificat i l'escriu en un fitxer.
     *
     * @param root        Node arrel de l'AST
     * @param symbolTable Taula de símbols amb variables i funcions
     * @param filename    Ruta del fitxer de sortida per al codi TAC
     */
    public void generateFile(Node root, SymbolTable symbolTable, String filename) {
        this.symbolTable = symbolTable;
        labelCounter = 0;
        tempCounter = 0;

        start(root);

        try (FileWriter writer = new FileWriter(filename)) {
            for (String line : code) {
                writer.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Error escrivint codi TAC: " + e.getMessage());
        }
    }

    /**
     * Inicia el processament d'un node de l'AST, despatxant
     * al handler corresponent segons el tipus de node.
     *
     * @param node Node a processar
     */
    private void start(Node node) {
        if (node == null) return;

        // Detecció d'assignació en <CONTENT>
        if (node.getSymbol().equals("<CONTENT>") && node.getChildren().size() >= 2) {
            Node first = node.getChildren().get(0);
            Node second = node.getChildren().get(1);
            if (first.getToken() != null && "ID".equals(first.getToken().getType())
                    && "<ID_CONTENT>".equals(second.getSymbol())
                    && !second.getChildren().isEmpty()
                    && "EQUAL_ASSIGNATION".equals(second.getChildren().get(0).getToken().getType())) {
                currentId = first.getToken().getLexeme();
            }
        }

        switch (getNodeKind(node)) {
            case MAIN -> handleMain(node);
            case FUNCTION -> handleFunction(node);
            case WHILE -> handleWhile(node);
            case IF -> handleIf(node);
            case RETURN -> handleReturn(node);
            case ASSIGNATION -> handleAssignation(node);
            case OPERATION -> handleOperation(node);
            case COMPARATION -> handleComparation(node);
            case DECLARATION -> handleDeclaration(node);
            case GLOBAL_DECLARATION -> handleGlobalDeclaration(node);
            case OTHER -> handleOthers(node);
        }
    }

    /**
     * Gestiona la funció main, afegint etiqueta i processant el cos.
     *
     * @param node Node <UNIT> corresponent al main
     */
    private void handleMain(Node node) {
        Node unitTail = node.getChildren().get(1);
        String funcName = unitTail.getChildren().get(0).getToken().getLexeme();
        code.add("\n" + funcName + ":");
        start(unitTail.getChildren().get(2));
    }

    /**
     * Gestiona la definició d'una funció (no main), afegint etiqueta,
     * emmagatzemant el nom i processant el cos.
     *
     * @param node Node <UNIT> de funció
     */
    private void handleFunction(Node node) {
        Node unitTail = node.getChildren().get(1);
        String funcName = unitTail.getChildren().get(0).getToken().getLexeme();
        functions.add(funcName);
        code.add("\n" + funcName + ":");
        start(unitTail.getChildren().get(1).getChildren().get(1).getChildren().get(0));
    }

    /**
     * Gestiona una assignació de variable o crida a funció amb assignació.
     *
     * @param node Node <ID_CONTENT>
     */
    private void handleAssignation(Node node) {
        Node expr = node.getChildren().get(1);

        String funcName = expr
                .getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(0)
                .getToken().getLexeme();

        String destTemp = getOrCreateTempForVariable(currentId);

        if (functions.contains(funcName)) {
            code.add(destTemp + " = call " + funcName);
            stack.push(destTemp);
            return;
        }

        start(expr);
        String val = getLastTemp();
        code.add(destTemp + " = " + val);
        stack.push(destTemp);
    }

    /**
     * Gestiona operacions aritmètiques binàries (+, -, *, /).
     *
     * @param node Node <EXPRESSIO> o <TERME>
     */
    private void handleOperation(Node node) {
        if (node.getChildren().isEmpty()) return;

        Node leftNode = node.getChildren().get(0);
        start(leftNode);
        String left = extractOperand(leftNode);

        if (node.getChildren().size() == 2) {
            Node tail = node.getChildren().get(1);

            while (tail.getChildren().size() >= 2) {
                String op = tail.getChildren().get(0).getToken().getType();
                Node rightNode = tail.getChildren().get(1);

                start(rightNode);
                String right = extractOperand(rightNode);

                String tmp = newTemp();
                code.add(tmp + " = " + left + " " + mapOperator(op) + " " + right);
                stack.push(tmp);

                if (tail.getChildren().size() == 3) {
                    left = tmp;
                    tail = tail.getChildren().get(2);
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Gestiona expressions de comparació (<, >, <=, >=, ==, !=).
     *
     * @param node Node <COMPARACIO>
     */
    private void handleComparation(Node node) {
        if (node.getChildren().size() == 2) {
            Node left = node.getChildren().get(0);
            Node compTail = node.getChildren().get(1);

            if (compTail.getChildren().size() == 2) {
                Token opToken = compTail.getChildren().get(0).getChildren().get(0).getToken();
                Node right = compTail.getChildren().get(1);

                if (opToken != null) {
                    start(left);
                    start(right);

                    String rightVal = getLastTemp();
                    String leftVal = getLastTemp();
                    String tmp = newTemp();
                    String op = mapOperator(opToken.getType());

                    code.add(tmp + " = " + leftVal + " " + op + " " + rightVal);
                    stack.push(tmp);
                }
            } else {
                start(left);
            }
        }
    }

    /**
     * Gestiona l'instrucció return amb possible expressió.
     *
     * @param node Node <XINPUM>
     */
    private void handleReturn(Node node) {
        Token token = findFirstToken(node.getChildren().get(1));
        if (token == null) return;

        if ("ID".equals(token.getType())) {
            String temp = getOrCreateTempForVariable(token.getLexeme());
            code.add("return " + temp);
        } else {
            code.add("return " + token.getLexeme());
        }
    }

    /**
     * Gestiona la declaració local de variables amb possible assignació.
     *
     * @param node Node <CONTENT> amb <DECLARACIO>
     */
    private void handleDeclaration(Node node) {
        String id = node.getChildren().get(1).getToken().getLexeme();
        Node suffix = node.getChildren().get(2);

        if (suffix.getChildren().size() >= 2 &&
                "EQUAL_ASSIGNATION".equals(suffix.getChildren().get(0).getToken().getType())) {

            Node expr = suffix.getChildren().get(1);
            start(expr);

            String val = getLastTemp();
            String temp = getOrCreateTempForVariable(id);
            code.add(temp + " = " + val);
        }
    }


    /**
     * Gestiona la declaració global de variables amb assignació inicial.
     *
     * @param node Node <UNIT> amb declaració global
     */
    private void handleGlobalDeclaration(Node node) {
        Node unitTail = node.getChildren().get(1);
        String id = unitTail.getChildren().get(0).getToken().getLexeme();

        Node exprNode = unitTail.getChildren().get(1).getChildren().get(1);
        String val = exprNode.getChildren()
                .get(0).getChildren()
                .get(0).getChildren()
                .get(0).getChildren()
                .get(0).getToken().getLexeme();

        String temp = getOrCreateTempForVariable(id);
        code.add(temp + " = " + val);
    }

    /**
     * Gestiona un bucle while, generant etiquetes i salts.
     *
     * @param node Node <CONTENT> amb <BUCLE>
     */
    private void handleWhile(Node node) {
        String Lstart = newLabel(), Lbody = newLabel(), Lend = newLabel();
        code.add(Lstart + ":");

        String cond = handleCondition(node.getChildren().get(2));
        code.add("if " + cond + " goto " + Lbody);
        code.add("goto " + Lend);

        code.add("\n" + Lbody + ":");
        start(node.getChildren().get(5));
        code.add("goto " + Lstart);
        code.add("\n" + Lend + ":");
    }

    /**
     * Gestiona una condicional if/else, generant etiquetes i salts segons condició.
     *
     * @param node Node <CONTENT> amb <IF>
     */
    private void handleIf(Node node) {
        String Lthen = newLabel(), Lend = newLabel();
        String cond = handleCondition(node.getChildren().get(2));
        code.add("if " + cond + " goto " + Lthen);

        boolean hasElse = node.getChildren().size() == 8;
        if (hasElse) {
            start(node.getChildren().get(7));   // ELSE
            code.add("goto " + Lend);
            code.add("\n" + Lthen + ":");
            start(node.getChildren().get(5));   // THEN
        } else {
            code.add("goto " + Lend);
            code.add("\n" + Lthen + ":");
            start(node.getChildren().get(5));
        }

        code.add("\n" + Lend + ":");
    }

    /**
     * Processa nodes genèrics o desconeguts recursivament,
     * gestionant literals i identificadors.
     *
     * @param node Node a processar
     */
    private void handleOthers(Node node) {
        for (Node child : node.getChildren()) {
            start(child);
        }

        Token tok = node.getToken();
        if (tok != null) {
            switch (tok.getType()) {
                case "INT_VALUE", "FLOAT_VALUE", "CHAR_VALUE" -> {
                    String value = tok.getLexeme();
                    String tmp = literalToTemp.computeIfAbsent(value, v -> {
                        String t = newTemp();
                        code.add(t + " = " + v);
                        return t;
                    });
                    stack.push(tmp);
                }
                case "ID" -> {
                    String lex = tok.getLexeme();
                    String tmp = getOrCreateTempForVariable(lex);
                    stack.push(tmp);
                }
            }
        }
    }

    /**
     * Obté o crea un temporal TAC per a una variable,
     * llançant excepció si no està declarada.
     *
     * @param id Nom de la variable
     * @return Nom del temporal TAC assignat
     */
    private String getOrCreateTempForVariable(String id) {
        if (!varToTemp.containsKey(id)) {
            Symbol symbol = symbolTable.lookup(id);
            if (symbol == null) {
                throw new RuntimeException("Undeclared variable: " + id);
            }

            String temp = newTemp();
            varToTemp.put(id, temp);
        }
        return varToTemp.get(id);
    }

    /**
     * Gestiona la condició retornant el temporal resultant de la comparació.
     *
     * @param node Node <COMPARACIO>
     * @return Temporal TAC amb el valor de la condició
     */
    private String handleCondition(Node node) {
        start(node.getChildren().get(0));
        return getLastTemp();
    }

    /**
     * Troba el primer token en un subarbre (in-order).
     *
     * @param node Node arrel del subarbre
     * @return Token trobat o null si no existeix
     */
    private Token findFirstToken(Node node) {
        if (node == null) return null;
        if (node.getToken() != null) return node.getToken();
        for (Node child : node.getChildren()) {
            Token t = findFirstToken(child);
            if (t != null) return t;
        }
        return null;
    }

    /**
     * Detecta el tipus de node (NodeKind) segons la seva estructura.
     *
     * @param node Node de l'AST
     * @return NodeKind corresponent
     */
    private NodeKind getNodeKind(Node node) {
        List<Node> children = node.getChildren();
        if (children.isEmpty()) return NodeKind.OTHER;

        if (node.getSymbol().equals("<UNIT>") && children.size() >= 2) {
            Node unitTail = children.get(1);
            if (unitTail.getSymbol().equals("<UNIT_TAIL>")) {
                if (!unitTail.getChildren().isEmpty() &&
                        unitTail.getChildren().get(0).getToken() != null &&
                        "MAIN".equals(unitTail.getChildren().get(0).getToken().getType())) {
                    return NodeKind.MAIN;
                } else if (unitTail.getChildren().size() >= 2) {
                    Node tailNode = unitTail.getChildren().get(1);
                    if (tailNode.getSymbol().equals("<DECL_OR_FUNC_TAIL>") &&
                            tailNode.getChildren().size() >= 2 &&
                            tailNode.getChildren().get(0).getToken() != null &&
                            "OPEN_CLAUDATOR".equals(tailNode.getChildren().get(0).getToken().getType())) {
                        return NodeKind.FUNCTION;
                    }
                }
            }
        }

        if (node.getSymbol().equals("<UNIT>") && node.getChildren().size() == 2) {
            Node unitTail = node.getChildren().get(1);
            if (unitTail.getSymbol().equals("<UNIT_TAIL>") &&
                    unitTail.getChildren().size() == 2 &&
                    unitTail.getChildren().get(0).getToken() != null &&
                    "ID".equals(unitTail.getChildren().get(0).getToken().getType())) {
                Node declTail = unitTail.getChildren().get(1);
                if (declTail.getSymbol().equals("<DECL_OR_FUNC_TAIL>") &&
                        !declTail.getChildren().isEmpty() &&
                        declTail.getChildren().get(0).getToken() != null &&
                        "EQUAL_ASSIGNATION".equals(declTail.getChildren().get(0).getToken().getType())) {
                    return NodeKind.GLOBAL_DECLARATION;
                }
            }
        }

        if (node.getSymbol().equals("<CONTENT>") && node.getChildren().size() >= 3) {
            Node tipus = node.getChildren().get(0);
            Node idNode = node.getChildren().get(1);
            Node suffix = node.getChildren().get(2);

            if (idNode.getToken() != null &&
                    "ID".equals(idNode.getToken().getType()) &&
                    suffix.getSymbol().equals("<LOCAL_DECL_SUFFIX>")) {
                return NodeKind.DECLARATION;
            }
        }

        if (node.getSymbol().equals("<ID_CONTENT>") &&
                !node.getChildren().isEmpty() &&
                node.getChildren().get(0).getToken() != null &&
                "EQUAL_ASSIGNATION".equals(node.getChildren().get(0).getToken().getType())) {
            return NodeKind.ASSIGNATION;
        }

        if (Set.of("<EXPRESSIO>", "<TERME>").contains(node.getSymbol())) {
            return NodeKind.OPERATION;
        }

        if (node.getSymbol().equals("<COMPARACIO>")) {
            return NodeKind.COMPARATION;
        }

        if (node.getSymbol().equals("<CONTENT>") && !node.getChildren().isEmpty()) {
            Token first = node.getChildren().get(0).getToken();

            if (first != null) {
                return switch (first.getType()) {
                    case "IF" -> NodeKind.IF;
                    case "BUCLE" -> NodeKind.WHILE;
                    case "RETURN" -> NodeKind.RETURN;
                    default -> NodeKind.OTHER;
                };
            }
        }

        return NodeKind.OTHER;
    }


    /**
     * Genera una nova etiqueta única (L0, L1, ...).
     *
     * @return Nom de l'etiqueta generada
     */
    private String newLabel() {
        return "L" + labelCounter++;
    }

    /**
     * Genera un nou temporal TAC únic (t0, t1, ...).
     *
     * @return Nom del temporal generat
     */
    private String newTemp() {
        return "t" + tempCounter++;
    }

    /**
     * Retorna l'últim temporal generat (top de pila).
     *
     * @return Nom del temporal de la part superior de la pila, o "??" si la pila està buida
     */
    private String getLastTemp() {
        return stack.isEmpty() ? "??" : stack.pop();
    }

    /**
     * Extreu l'operant d'un node, retornant un temporal o literal.
     *
     * @param node Node del qual extraure l'operant
     * @return String amb el nom del temporal o la representació literal
     */
    private String extractOperand(Node node) {
        Token token = findFirstToken(node);
        if (token == null) return "??";

        String lex = token.getLexeme();
        if ("ID".equals(token.getType())) {
            return getOrCreateTempForVariable(lex);
        }
        return literalToTemp.getOrDefault(lex, lex);
    }

    /**
     * Mapeja el tipus de token d'operador a símbol infix (+, -, *, /, <, >, ==, !=, <=, >=).
     *
     * @param type Tipus de token de l'operador
     * @return Caràcter o cadena corresponent a l'operador infix
     */
    private String mapOperator(String type) {
        return switch (type) {
            case "SUM" -> "+";
            case "MINUS" -> "-";
            case "MULTIPLY" -> "*";
            case "DIVISION" -> "/";
            case "LOWER" -> "<";
            case "BIGGER" -> ">";
            case "EQUAL_COMPARATION" -> "==";
            case "DIFFERENT" -> "!=";
            case "LOWER_EQUAL" -> "<=";
            case "BIGGER_EQUAL" -> ">=";
            default -> "?";
        };
    }

    /**
     * Enumeració interna dels tipus de node suportats pel generador TAC.
     */
    private enum NodeKind {
        MAIN, FUNCTION, WHILE, IF, RETURN,
        ASSIGNATION, OPERATION, COMPARATION,
        DECLARATION, GLOBAL_DECLARATION, OTHER
    }
}
