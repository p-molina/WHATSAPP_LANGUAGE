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


    private NodeKind getNodeKind(Node n) {
        List<Node> children = n.getChildren();
        if (children.isEmpty()) return NodeKind.OTHER;

        Token firstTok = children.get(0).getToken();
        if (firstTok == null) return NodeKind.OTHER;

        String type = firstTok.getType();

        // Funció: <TIPUS> ID JAJAJ <BODY> JEJEJ
        if (type.matches("NUM|FLOAT|CHAR") &&
                children.size() > 2 &&
                children.get(1).getToken() != null &&
                children.get(2).getToken() != null &&
                "JAJAJ".equals(children.get(2).getToken().getType())) {
            return NodeKind.FUNCTION;
        }

        if ("BUCLE".equals(type)) return NodeKind.WHILE;
        if ("IF".equals(type) || "BRO".equals(type)) return NodeKind.IF;
        if ("RETURN".equals(type) || "XINPUM".equals(type)) return NodeKind.RETURN;
        if ("EQUAL_ASSIGNATION".equals(type)) return NodeKind.ASSIGNATION;
        if (Set.of("SUM", "MINUS", "MULTIPLY", "DIVISION").contains(type)) return NodeKind.OPERATION;

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
