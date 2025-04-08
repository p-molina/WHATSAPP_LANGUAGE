package SemanticAnalyzer;

import entities.Node;
import entities.Token;
import java.util.*;

public class SemanticAnalyzer {
    private final Node root;
    private final Map<String, String> symbolTable = new HashMap<>();
    private final Set<String> functionsDeclared = new HashSet<>();
    private boolean insideFunction = false;
    private boolean mainDeclared = false;

    public SemanticAnalyzer(Node root) {
        this.root = root;
    }

    public void analyze() {
        traverse(root);

        if (!mainDeclared) {
            throw new RuntimeException("Semantic Error: Missing main function 'xat'");
        }
    }

    private void traverse(Node node) {
        if (node == null) return;

        System.out.println("Visiting node: " + node);

        switch (node.getSymbol()) {
            case "DECLARACIO":
                handleDeclaration(node);
                break;
            case "CREA_FUNCIO":
                handleFunction(node);
                break;
            case "CREA_MAIN":
                handleMain(node);
                break;
            case "ASSIGNACIO":
                handleAssignment(node);
                break;
            case "XINPUM":
                handleReturn(node);
                break;
            case "CALL_FUNCIO":
                handleFunctionCall(node);
                break;
            default:
                break;
        }

        for (Node child : node.getChildren()) {
            traverse(child);
        }
    }

    private void handleDeclaration(Node node) {
        if (insideFunction) {
            error(node, "Variable declarations inside functions are not allowed.");
        }

        String type;
        if (node.getChildren().get(0).getSymbol().equals("ARRAY")) {
            // Ex: ARRAY de 3 INT
            String size = node.getChildren().get(2).getToken().getLexeme();
            String baseType = node.getChildren().get(3).getSymbol();
            type = "ARRAY[" + size + "]" + baseType;
        } else {
            type = node.getChildren().get(0).getSymbol();
        }

        String name = node.getChildren().get(1).getToken().getLexeme();

        System.out.println("Declaring variable: " + name + " of type " + type);

        if (symbolTable.containsKey(name)) {
            error(node, "Variable '" + name + "' already declared.");
        }

        symbolTable.put(name, type);
    }

    private void handleFunction(Node node) {
        if (mainDeclared) {
            error(node, "No functions are allowed after the main function 'xat'.");
        }

        String name = node.getChildren().get(1).getToken().getLexeme();
        functionsDeclared.add(name);

        System.out.println("Entering function '" + name + "'...");
        insideFunction = true;
        for (Node child : node.getChildren()) {
            traverse(child);
        }
        insideFunction = false;
        System.out.println("Exiting function '" + name + "'.");
    }

    private void handleMain(Node node) {
        System.out.println("Processing main function 'xat'...");

        if (mainDeclared) {
            error(node, "Main function 'xat' already defined.");
        }

        mainDeclared = true;
        insideFunction = true;
        for (Node child : node.getChildren()) {
            traverse(child);
        }
        insideFunction = false;

        System.out.println("Finished processing 'xat'.");
    }

    private void handleAssignment(Node node) {
        Node assignPrim = node.getChildren().get(0); // ASSIGNACIO_PRIM
        Node idNode = assignPrim.getChildren().get(0); // ID

        String name = idNode.getToken().getLexeme();
        System.out.println("Analyzing assignment to variable: " + name);

        if (!symbolTable.containsKey(name)) {
            error(idNode, "Variable '" + name + "' not declared.");
        }

        String expectedType = symbolTable.get(name);
        Node expr = node.getChildren().get(1); // EXPRESSIO
        String actualType = getExpressionType(expr);

        System.out.println("Expected type: " + expectedType + ", Actual type: " + actualType);

        if (!expectedType.equals(actualType)) {
            error(node, "Type mismatch: cannot assign '" + actualType + "' to '" + expectedType + "'.");
        }
    }

    private void handleReturn(Node node) {
        System.out.println("Checking return statement...");
        if (node.getChildren().size() > 1) {
            String retType = getExpressionType(node.getChildren().get(1));
            System.out.println("Returned value type: " + retType);
            // TODO: check against expected return type of function
        }
    }

    private void handleFunctionCall(Node node) {
        Token funcToken = node.getChildren().get(0).getToken();
        String funcName = funcToken.getLexeme();

        System.out.println("Calling function: " + funcName);

        if (!functionsDeclared.contains(funcName)) {
            error(node, "Function '" + funcName + "' not declared.");
        }
    }

    private String getExpressionType(Node node) {
        if (node.getToken() != null) {
            String lexeme = node.getToken().getLexeme();
            String tokenType = node.getToken().getType();
            System.out.println("Determining type of token: " + lexeme + " (" + tokenType + ")");

            switch (tokenType) {
                case "INT_VALUE": return "INT";
                case "FLOAT_VALUE": return "FLOAT";
                case "CHAR_VALUE": return "CHAR";
                case "ID":
                    String name = lexeme;
                    if (!symbolTable.containsKey(name)) {
                        error(node, "Variable '" + name + "' not declared.");
                    }
                    return symbolTable.get(name);
            }
        } else if (!node.getChildren().isEmpty()) {
            if (node.getChildren().size() == 3 && isOperator(node.getChildren().get(1))) {
                String left = getExpressionType(node.getChildren().get(0));
                String right = getExpressionType(node.getChildren().get(2));
                if (!left.equals(right)) {
                    error(node, "Expression types mismatch: '" + left + "' and '" + right + "'.");
                }
                return left;
            } else {
                return getExpressionType(node.getChildren().get(0));
            }
        }
        return "UNKNOWN";
    }

    private boolean isOperator(Node node) {
        String type = node.getSymbol();
        return type.equals("SUM") || type.equals("MINUS") ||
                type.equals("MULTIPLY") || type.equals("DIVISION");
    }

    private void error(Node node, String message) {
        int line = node.getToken() != null ? node.getToken().getLine() : -1;
        throw new RuntimeException("[Line " + line + "] Semantic Error: " + message);
    }
}