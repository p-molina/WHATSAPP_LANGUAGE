package TAC;

import entities.Node;
import entities.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TACGeneratorNEW {
    private final List<String> code = new ArrayList<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private int labelCounter = 0;
    private int tempCounter = 0;
    private String currentId = null;

    public TACGeneratorNEW() {}

    public void generateFile(Node root, String filename) {
        start(root);

        try (FileWriter writer = new FileWriter(filename)) {
            for (String line : code) {
                System.out.println(line);
                writer.write(line + System.lineSeparator());
            }
            System.out.println("TAC escrit a: " + filename);
        } catch (IOException e) {
            System.err.println("Error escrivint el TAC: " + e.getMessage());
        }
    }

    private void start(Node node) {
        if (node.getSymbol().equals("<CONTENT>") && node.getChildren().size() >= 2) {
            Node first = node.getChildren().get(0);
            Node second = node.getChildren().get(1);
            if (first.getToken() != null && "ID".equals(first.getToken().getType()) &&
                                            "<ID_CONTENT>".equals(second.getSymbol())) {
                currentId = first.getToken().getLexeme();
            }
        }

        switch (getNodeKind(node)) {
            case FUNCTION -> handleFunction(node);
            case WHILE -> handleWhile(node);
            case IF -> handleIf(node);
            case RETURN -> handleReturn(node);
            case ASSIGNATION -> handleAssignation(node);
            case OPERATION -> handleOperation(node);
            case OTHER -> {
                for (Node child : node.getChildren()) {
                    start(child);
                }
                Token tok = node.getToken();
                if (tok != null) {
                    switch (tok.getType()) {
                        case "INT_VALUE", "FLOAT_VALUE", "CHAR_VALUE" -> {
                            String tmp = newTemp();
                            code.add(tmp + " = " + tok.getLexeme());
                            stack.push(tmp);
                        }
                        case "ID" -> stack.push(tok.getLexeme());
                    }
                }
            }
        }
    }

    private void handleFunction(Node node) {
        Node unitTail = node.getChildren().get(1); // <UNIT_TAIL>
        Node idNode = unitTail.getChildren().get(0); // ID
        String funcName = idNode.getToken().getLexeme();
        code.add(funcName + ":");

        tempCounter = 0; // reiniciem temporals

        Node declOrFuncTail = unitTail.getChildren().get(1); // <DECL_OR_FUNC_TAIL>
        Node declOrFuncTailRest = declOrFuncTail.getChildren().get(1); // <DECL_OR_FUNC_TAIL_REST>
        Node body = declOrFuncTailRest.getChildren().get(0); // <BODY>
        start(body); // recórrer cos
    }

    private void handleWhile(Node node) {
        String Lstart = newLabel();
        String Lend = newLabel();

        code.add(Lstart + ":");

        Node condNode = node.getChildren().get(2); // <CONDICIO>
        start(condNode);
        String condTmp = getLastTemp();

        code.add("if_false " + condTmp + " goto " + Lend);

        Node bodyNode = node.getChildren().get(5); // <BODY>
        start(bodyNode);

        code.add("goto " + Lstart);
        code.add(Lend + ":");
    }

    private void handleIf(Node node) {
        String Lthen = newLabel();
        String Lend = newLabel();

        Node condNode = node.getChildren().get(2); // <CONDICIO>
        start(condNode);
        String condTmp = getLastTemp();

        code.add("if " + condTmp + " goto " + Lthen);

        boolean hasElse = node.getChildren().size() == 8;
        if (hasElse) {
            start(node.getChildren().get(7)); // ELSE
            code.add("goto " + Lend);
            code.add(Lthen + ":");
            start(node.getChildren().get(5)); // IF
        } else {
            code.add("goto " + Lend);
            code.add(Lthen + ":");
            start(node.getChildren().get(5));
        }

        code.add(Lend + ":");
    }

    private void handleReturn(Node node) {
        if (node.getChildren().size() > 1) {
            start(node.getChildren().get(1)); // <EXPRESSIO>
            String val = getLastTemp();
            code.add("return " + val);
        } else {
            code.add("return");
        }
    }

    private void handleAssignation(Node node) {
        start(node.getChildren().get(1)); // EXPRESSIO
        String val = getLastTemp();
        code.add(currentId + " = " + val);
    }

    private void handleOperation(Node node) {
        if (node.getChildren().size() == 0) return;

        start(node.getChildren().get(0));

        if (node.getChildren().size() == 2) {
            Node tail = node.getChildren().get(1);
            while (tail.getChildren().size() >= 2) {
                String op = tail.getChildren().get(0).getToken().getType();
                start(tail.getChildren().get(1));

                String right = getLastTemp();
                String left = getLastTemp();
                String tmp = newTemp();
                code.add(tmp + " = " + left + " " + map(op) + " " + right);
                stack.push(tmp);

                if (tail.getChildren().size() == 3) {
                    tail = tail.getChildren().get(2);
                } else {
                    break;
                }
            }
        }
    }

    private NodeKind getNodeKind(Node node) {
        List<Node> children = node.getChildren();
        if (children.isEmpty()) return NodeKind.OTHER;

        if (node.getSymbol().equals("<UNIT>") && children.size() >= 2) {
            Node unitTail = children.get(1);
            if (unitTail.getSymbol().equals("<UNIT_TAIL>") && unitTail.getChildren().size() >= 2) {
                Node tailNode = unitTail.getChildren().get(1);
                if (tailNode.getSymbol().equals("<DECL_OR_FUNC_TAIL>")
                        && tailNode.getChildren().size() >= 2
                        && tailNode.getChildren().get(0).getToken() != null
                        && "OPEN_CLAUDATOR".equals(tailNode.getChildren().get(0).getToken().getType())) {
                    return NodeKind.FUNCTION;
                }
            }
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

        if (node.getSymbol().equals("<ID_CONTENT>")
                && !node.getChildren().isEmpty()
                && node.getChildren().get(0).getToken() != null
                && "EQUAL_ASSIGNATION".equals(node.getChildren().get(0).getToken().getType())) {
            return NodeKind.ASSIGNATION;
        }

        if (Set.of("<EXPRESSIO>", "<TERME>").contains(node.getSymbol())) {
            return NodeKind.OPERATION;
        }

        return NodeKind.OTHER;
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
            default -> "?";
        };
    }

    private enum NodeKind {
        FUNCTION,
        WHILE,
        IF,
        RETURN,
        ASSIGNATION,
        OPERATION,
        OTHER
    }
}
