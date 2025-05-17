package TAC;

import entities.Node;
import entities.Symbol;
import entities.SymbolTable;
import entities.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * TACGenerator is responsible for generating Three-Address Code (TAC)
 * from the abstract syntax tree (AST) of a program using a symbol table.
 */
public class TACGenerator {
    private final List<String> code = new ArrayList<>();                // TAC code
    private final Deque<String> stack = new ArrayDeque<>();             // Stack for temporary variables
    private final Map<String, String> literalToTemp = new HashMap<>();  // Map for literals to temporary variables
    private final Map<String, String> varToTemp = new HashMap<>();      // Map for variables to temporary variables
    private int labelCounter = 0;                                       // Label counter
    private int tempCounter = 0;                                        // Temporary variable counter
    private String currentId = null;                                    // Current ID for assignment
    private SymbolTable symbolTable;                                    // Symbol table
    private final Set<String> functions = new HashSet<>();              // Set of function names

    public TACGenerator() {}

    /**
     * Generates TAC code for the given AST and writes it to a file.
     *
     * @param root        the root node of the AST
     * @param symbolTable the symbol table for variables
     * @param filename    the file where the TAC will be written
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
            System.err.println("Error writing TAC: " + e.getMessage());
        }
    }

    /**
     * Dispatches processing to the appropriate handler based on node type.
     */
    private void start(Node node) {
        if (node == null) return;

        // Assignation detection
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
     * Handles the main function node.
     */
    private void handleMain(Node node) {
        Node unitTail = node.getChildren().get(1);
        String funcName = unitTail.getChildren().get(0).getToken().getLexeme();
        code.add("\n" + funcName + ":");
        start(unitTail.getChildren().get(2));
    }

    /**
     * Handles a function declaration.
     */
    private void handleFunction(Node node) {
        Node unitTail = node.getChildren().get(1);
        String funcName = unitTail.getChildren().get(0).getToken().getLexeme();
        functions.add(funcName);
        code.add("\n" + funcName + ":");
        start(unitTail.getChildren().get(1).getChildren().get(1).getChildren().get(0));
    }

    /**
     * Handles a variable assignment, including function call assignments.
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
     * Handles arithmetic operations.
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
     * Handles comparison expressions.
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
     * Handles a return statement.
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
     * Handles a local variable declaration.
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
     * Handles a global variable declaration.
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
     * Handles a while loop.
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
     * Handles an if/else conditional.
     */
    private void handleIf(Node node) {
        String Lthen = newLabel(), Lend = newLabel();
        String cond = handleCondition(node.getChildren().get(2));
        code.add("if " + cond + " goto " + Lthen);

        boolean hasElse = node.getChildren().size() == 8;
        if (hasElse) {
            start(node.getChildren().get(7)); // ELSE
            code.add("goto " + Lend);
            code.add("\n" + Lthen + ":");
            start(node.getChildren().get(5)); // THEN
        } else {
            code.add("goto " + Lend);
            code.add("\n" + Lthen + ":");
            start(node.getChildren().get(5));
        }

        code.add("\n" + Lend + ":");
    }

    /**
     * Recursively processes unknown or generic nodes.
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
     * Returns the temporary variable assigned to a variable,
     * creating one if it doesn't exist.
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
     * Handles a condition by processing its comparison node.
     */
    private String handleCondition(Node node) {
        start(node.getChildren().get(0)); // comparation
        return getLastTemp();
    }

    /**
     * Returns the first token found in a subtree.
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
     * Detects the kind of the given node.
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


    // --- Utilities ---
    private String newLabel() {
        return "L" + labelCounter++;
    }

    private String newTemp() {
        return "t" + tempCounter++;
    }

    private String getLastTemp() {
        return stack.isEmpty() ? "??" : stack.pop();
    }

    private String extractOperand(Node node) {
        Token token = findFirstToken(node);
        if (token == null) return "??";

        String lex = token.getLexeme();
        if ("ID".equals(token.getType())) {
            return getOrCreateTempForVariable(lex);
        }
        return literalToTemp.getOrDefault(lex, lex);
    }

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

    private enum NodeKind {
        MAIN, FUNCTION, WHILE, IF, RETURN, ASSIGNATION, OPERATION, COMPARATION,
        DECLARATION, GLOBAL_DECLARATION, OTHER
    }
}
