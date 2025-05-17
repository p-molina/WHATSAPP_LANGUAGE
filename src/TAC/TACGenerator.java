package TAC;

import entities.Node;
import entities.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TACGenerator {
    private final List<String> code = new ArrayList<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private final Map<String, String> varToTemp = new HashMap<>();
    private final Map<String, String> literalToTemp = new HashMap<>();
    private ArrayList<String> functions = new ArrayList<>();
    private int labelCounter = 0;
    private int tempCounter = 0;
    private String currentId = null;

    public TACGenerator() {}

    public void generateFile(Node root, String filename) {
        labelCounter = 0;
        tempCounter = 0;

        start(root);

        try (FileWriter writer = new FileWriter(filename)) {
            for (String line : code) {
                writer.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Error escrivint el TAC: " + e.getMessage());
        }
    }

    private void start(Node node) {
        // Detectem assignacions del tipus: ID -> EXPRESSIO;
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

    private void handleMain(Node node) {
        Node unitTail = node.getChildren().get(1); // <UNIT_TAIL>
        Node idNode = unitTail.getChildren().get(0); // ID
        String funcName = idNode.getToken().getLexeme();
        code.add("\n" + funcName + ":");

        Node body = unitTail.getChildren().get(2); // <DECL_OR_FUNC_TAIL>
        start(body);
    }

    private String handleCondition(Node node) {
        // node ::= <COMPARACIO> <CONDICIO'>
        start(node.getChildren().get(0)); // <COMPARACIO>
        return getLastTemp();
    }

    private void handleFunction(Node node) {
        Node unitTail = node.getChildren().get(1); // <UNIT_TAIL>
        Node idNode = unitTail.getChildren().get(0); // ID
        String funcName = idNode.getToken().getLexeme();
        functions.add(funcName);
        code.add("\n" + funcName + ":");

        Node declOrFuncTail = unitTail.getChildren().get(1); // <DECL_OR_FUNC_TAIL>
        Node declOrFuncTailRest = declOrFuncTail.getChildren().get(1); // <DECL_OR_FUNC_TAIL_REST>
        Node body = declOrFuncTailRest.getChildren().get(0); // <BODY>
        start(body);
    }

    private void handleWhile(Node node) {
        String Lstart = newLabel();
        String Lend = newLabel();

        code.add(Lstart + ":");

        Node condNode = node.getChildren().get(2); // <CONDICIO>
        String condTmp = handleCondition(condNode);

        code.add("if " + condTmp + " goto " + Lend);

        Node bodyNode = node.getChildren().get(5); // <BODY>
        start(bodyNode);

        code.add("goto " + Lstart);
        code.add(Lend + ":");
    }

    private void handleIf(Node node) {
        String Lthen = newLabel();
        String Lend = newLabel();

        Node condNode = node.getChildren().get(2); // <CONDICIO>
        String condTmp = handleCondition(condNode);

        code.add("if " + condTmp + " goto " + Lthen);

        boolean hasElse = node.getChildren().size() == 8;
        if (hasElse) {
            start(node.getChildren().get(7)); // ELSE
            code.add("goto " + Lend);
            code.add("\n" + Lthen + ":");
            start(node.getChildren().get(5)); // IF
        } else {
            code.add("goto " + Lend);
            code.add("\n" + Lthen + ":");
            start(node.getChildren().get(5));
        }

        code.add("\n" + Lend + ":");
    }

    private void handleReturn(Node node) {
        start(node.getChildren().get(1)); // <EXPRESSIO>

        // Busquem el primer token no nul dins de l'expressió
        Token token = findFirstToken(node.getChildren().get(1));
        if (token == null) {
            System.err.println("Error: no s'ha pogut trobar cap token dins del return.");
            return;
        }

        String val = token.getLexeme();
        String tmp = varToTemp.get(val);
        code.add("return " + tmp);
    }

    private Token findFirstToken(Node node) {
        if (node == null) return null;
        if (node.getToken() != null) return node.getToken();
        for (Node child : node.getChildren()) {
            Token t = findFirstToken(child);
            if (t != null) return t;
        }
        return null;
    }


    private void handleAssignation(Node node) {
        Node expr = node.getChildren().get(1); // <EXPRESSIO>
        String funcName =   expr
                            .getChildren().get(0)
                            .getChildren().get(0)
                            .getChildren().get(0)
                            .getToken().getLexeme();

        if (functions.contains(funcName)) {
            String tmp;
            if (varToTemp.containsKey(currentId)) {
                tmp = varToTemp.get(currentId);
            } else {
                tmp = newTemp();
                varToTemp.put(currentId, tmp);
            }

            code.add(tmp + " = call " + funcName);
            stack.push(tmp);
            return;
        }

        // Comportament normal per a assignacions
        start(expr);
        String val = getLastTemp();

        String tmp;
        if (varToTemp.containsKey(currentId)) {
            tmp = varToTemp.get(currentId);
        } else {
            tmp = newTemp();
            varToTemp.put(currentId, tmp);
        }

        code.add(tmp + " = " + val);
        stack.push(tmp);
    }

    private void handleOperation(Node node) {
        if (node.getChildren().isEmpty()) return;

        // Avaluar operand esquerre
        Node leftNode = node.getChildren().get(0);
        start(leftNode);
        String left = extractOperand(leftNode);

        if (node.getChildren().size() == 2) {
            Node tail = node.getChildren().get(1);

            while (tail.getChildren().size() >= 2) {
                String op = tail.getChildren().get(0).getToken().getType();
                Node rightNode = tail.getChildren().get(1);

                // Avaluar operand dret
                start(rightNode);
                String right = extractOperand(rightNode);

                String tmp = newTemp();
                code.add(tmp + " = " + left + " " + map(op) + " " + right);
                stack.push(tmp);

                // Si hi ha més operacions en cadena, continua
                if (tail.getChildren().size() == 3) {
                    left = tmp;  // la nova esquerra és el resultat parcial
                    tail = tail.getChildren().get(2);
                } else {
                    break;
                }
            }
        }
    }

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
                    String op = map(opToken.getType());

                    code.add(tmp + " = " + leftVal + " " + op + " " + rightVal);
                    stack.push(tmp);
                }
            } else {
                start(left);
            }
        }
    }

    private void handleDeclaration(Node node) {
        String id = node.getChildren().get(1).getToken().getLexeme();
        Node suffix = node.getChildren().get(2); // <LOCAL_DECL_SUFFIX>

        // Declaració amb assignació
        if (suffix.getChildren().size() >= 2 &&
                "EQUAL_ASSIGNATION".equals(suffix.getChildren().get(0).getToken().getType())) {

            Node expressio = suffix.getChildren().get(1);

            start(expressio);
            String val = getLastTemp();

            // Si el valor és un literal conegut, reutilitzem la temp
            if (literalToTemp.containsValue(val) && !varToTemp.containsKey(id)) {
                varToTemp.put(id, val);
            } else {
                String tmp = newTemp();
                varToTemp.put(id, tmp);
                code.add(tmp + " = " + val);
            }
        }
    }

    private void handleGlobalDeclaration(Node node) {
        Node unitTail = node.getChildren().get(1); // <UNIT_TAIL>
        Token idToken = unitTail.getChildren().get(0).getToken();
        String id = idToken.getLexeme();

        Node declTail = unitTail.getChildren().get(1); // <DECL_OR_FUNC_TAIL>
        Node exprNode = declTail.getChildren().get(1); // <EXPRESSIO>

        String val =    exprNode.getChildren()
                        .get(0).getChildren()
                        .get(0).getChildren()
                        .get(0).getChildren()
                        .get(0).getToken().getLexeme();

        String tmp = newTemp();
        varToTemp.put(id, tmp);
        code.add(tmp + " = " + val);
    }

    private void handleOthers(Node node) {
        for (Node child : node.getChildren()) {
            start(child);
        }

        Token tok = node.getToken();
        if (tok != null) {
            switch (tok.getType()) {
                case "INT_VALUE", "FLOAT_VALUE", "CHAR_VALUE" -> {
                    String value = tok.getLexeme();
                    String tmp;

                    if (literalToTemp.containsKey(value)) {
                        tmp = literalToTemp.get(value);
                    } else {
                        tmp = newTemp();
                        literalToTemp.put(value, tmp);
                        code.add(tmp + " = " + value);
                    }

                    stack.push(tmp);
                }

                case "ID" -> {
                    String lex = tok.getLexeme();
                    String tmp;

                    if (!varToTemp.containsKey(lex)) {
                        tmp = newTemp();
                        varToTemp.put(lex, tmp);
                    } else {
                        tmp = varToTemp.get(lex);
                    }
                    stack.push(tmp);
                }

            }
        }
    }

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
                    if (tailNode.getSymbol().equals("<DECL_OR_FUNC_TAIL>")
                            && tailNode.getChildren().size() >= 2
                            && tailNode.getChildren().get(0).getToken() != null
                            && "OPEN_CLAUDATOR".equals(tailNode.getChildren().get(0).getToken().getType())) {
                        return NodeKind.FUNCTION;
                    }
                }
            }
        }

        // Declaració global: <UNIT> ::= <TIPUS> ID -> EXPRESSIO
        if (node.getSymbol().equals("<UNIT>") && node.getChildren().size() == 2) {
            Node unitTail = node.getChildren().get(1);
            if (unitTail.getSymbol().equals("<UNIT_TAIL>") &&
                    unitTail.getChildren().size() == 2 &&
                    unitTail.getChildren().get(0).getToken() != null &&  // ID
                    "ID".equals(unitTail.getChildren().get(0).getToken().getType())) {

                Node declTail = unitTail.getChildren().get(1);
                if (declTail.getSymbol().equals("<DECL_OR_FUNC_TAIL>") &&
                        declTail.getChildren().size() == 3 &&
                        "EQUAL_ASSIGNATION".equals(declTail.getChildren().get(0).getToken().getType())) {
                    return NodeKind.GLOBAL_DECLARATION;
                }
            }
        }


        // Declaració local: <CONTENT> ::= <TIPUS> ID <LOCAL_DECL_SUFFIX> LINE_DELIMITER
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

        if (node.getSymbol().equals("<ID_CONTENT>")
                && !node.getChildren().isEmpty()
                && node.getChildren().get(0).getToken() != null
                && "EQUAL_ASSIGNATION".equals(node.getChildren().get(0).getToken().getType())) {
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

    private String extractOperand(Node node) {
        try {
            Token token =   node
                            .getChildren().get(0)
                            .getChildren().get(0)
                            .getChildren().get(0)
                            .getToken();

            if (token != null) {
                String lex = token.getLexeme();
                String type = token.getType();

                // Literal
                if (type.equals("INT_VALUE") || type.equals("FLOAT_VALUE") || type.equals("CHAR_VALUE")) {
                    if (!literalToTemp.containsKey(lex)) {
                        String tmp = newTemp();
                        code.add(tmp + " = " + lex);
                        literalToTemp.put(lex, tmp);
                    }
                    return literalToTemp.get(lex);
                }

                // Identificador (variable)
                if (type.equals("ID")) {
                    if (!varToTemp.containsKey(lex)) {
                        String tmp = newTemp();
                        varToTemp.put(lex, tmp);
                    }
                    return varToTemp.get(lex);
                }
            }
        } catch (Exception e) {
            // Fallback per seguretat
        }

        // Si no es pot identificar, fem servir l'última temp generada
        return getLastTemp();
    }


    private String getLastTemp() {
        return stack.isEmpty() ? "??" : stack.pop();
    }

    private String newLabel() {
        return "L" + (labelCounter++);
    }

    private String newTemp() {
        return "t" + (tempCounter++);
    }

    private static String map(String t) {
        return switch (t) {
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

    private enum NodeKind {
        MAIN,
        FUNCTION,
        WHILE,
        IF,
        RETURN,
        ASSIGNATION,
        OPERATION,
        COMPARATION,
        DECLARATION,
        GLOBAL_DECLARATION,
        OTHER
    }
}
