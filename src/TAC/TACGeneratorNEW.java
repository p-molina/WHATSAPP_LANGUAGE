package TAC;

import entities.Node;
import entities.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TACGeneratorNEW {
    private final List<String> code = new ArrayList<>();

    public TACGeneratorNEW() {
    }

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
            }
        }
    }

    private void handleFunction(Node node) {
    }

    private void handleWhile(Node node) {
    }

    private void handleIf(Node node) {
    }

    private void handleReturn(Node node) {
    }

    private void handleAssignation(Node node) {
    }

    private void handleOperation(Node node) {
    }


    private NodeKind getNodeKind(Node node) {
        List<Node> children = node.getChildren();
        if (children.isEmpty()) return NodeKind.OTHER;

        // FUNCIONS
        if (node.getSymbol().equals("<UNIT>")) {
            if (children.size() >= 2) {
                Node unitTail = children.get(1);
                if (unitTail.getSymbol().equals("<UNIT_TAIL>")
                        && unitTail.getChildren().size() >= 2) {
                    Node idNode = unitTail.getChildren().get(0);
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

        // IF
        if (node.getSymbol().equals("<CONTENT>")
                && !node.getChildren().isEmpty()
                && node.getChildren().get(0).getToken() != null
                && "IF".equals(node.getChildren().get(0).getToken().getType())) {
            return NodeKind.IF;
        }

        // WHILE
        if (node.getSymbol().equals("<CONTENT>")
                && !node.getChildren().isEmpty()
                && node.getChildren().get(0).getToken() != null
                && "BUCLE".equals(node.getChildren().get(0).getToken().getType())) {
            return NodeKind.WHILE;
        }

        // RETURN
        if (node.getSymbol().equals("<CONTENT>")
                && !node.getChildren().isEmpty()
                && node.getChildren().get(0).getToken() != null
                && "RETURN".equals(node.getChildren().get(0).getToken().getType())) {
            return NodeKind.RETURN;
        }

        // ASSIGNACIÓ (ID -> EXP)
        if (node.getSymbol().equals("<ID_CONTENT>")
                && !node.getChildren().isEmpty()
                && node.getChildren().get(0).getToken() != null
                && "EQUAL_ASSIGNATION".equals(node.getChildren().get(0).getToken().getType())) {
            return NodeKind.ASSIGNATION;
        }

        // OPERACIÓ
        if (Set.of("<EXPRESSIO>", "<TERME>").contains(node.getSymbol())) {
            return NodeKind.OPERATION;
        }

        return NodeKind.OTHER;
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
