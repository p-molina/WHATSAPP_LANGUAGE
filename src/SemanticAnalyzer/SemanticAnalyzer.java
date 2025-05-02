package SemanticAnalyzer;

import entities.Node;
import entities.Token;
import java.util.*;

public class SemanticAnalyzer {
    private final Node root;
    private final Map<String, String> globalSymbols = new HashMap<>();
    private final Map<String, Map<String, String>> functionScopes = new HashMap<>();
    private final Set<String> functionsDeclared = new HashSet<>();
    private boolean inFunction = false;
    private boolean mainDeclared = false;
    private boolean passedMain = false;

    public SemanticAnalyzer(Node root) {
        this.root = root;
    }

    public void analyze() {
        Node unitList = root.getChildren().get(0);
        processUnitList(unitList);

        if (!mainDeclared) {
            error(root, "Missing main function 'xat'");
        }
    }

    private void processUnitList(Node node) {
        if (node.getSymbol().equals("<UNIT_LIST>")) {
            for (Node child : node.getChildren()) {
                if (child.getSymbol().equals("<UNIT>")) {
                    checkUnit(child);
                } else {
                    processUnitList(child);
                }
            }
        }
    }

    private void checkUnit(Node node) {
        if (!node.getSymbol().equals("<UNIT>")) return;

        Node tipus = node.getChildren().get(0);
        Node unitTail = node.getChildren().get(1);

        if (unitTail.getChildren().get(0).getSymbol().equals("MAIN")) {
            if (mainDeclared) error(node, "Multiple main 'xat' definitions");
            mainDeclared = true;
            passedMain = true;
            checkMain(unitTail);
        } else {
            if (passedMain)
                error(node, "Declarations or functions cannot appear after main 'xat'");
            String name = unitTail.getChildren().get(0).getToken().getLexeme();
            Node declOrFuncTail = unitTail.getChildren().get(1);

            if (declOrFuncTail.getChildren().get(0).getSymbol().equals("EQUAL_ASSIGNATION")) {
                declareGlobal(tipus, name);
            } else {
                declareFunction(name);
                inFunction = true;
                traverse(declOrFuncTail, functionScopes.computeIfAbsent(name, k -> new HashMap<>()));
                inFunction = false;
            }
        }
    }

    private void checkMain(Node node) {
        Node body = node.getChildren().get(2);
        traverse(body, new HashMap<>());
    }

    private void declareGlobal(Node tipus, String name) {
        String type = extractType(tipus);
        if (globalSymbols.containsKey(name))
            error(tipus, "Variable '" + name + "' already declared");
        globalSymbols.put(name, type);
    }

    private void declareFunction(String name) {
        if (functionsDeclared.contains(name))
            throw new RuntimeException("Function '" + name + "' already declared.");
        functionsDeclared.add(name);
    }

    private void traverse(Node node, Map<String, String> localSymbols) {
        for (Node child : node.getChildren()) {
            switch (child.getSymbol()) {
                case "<CONTENT>":
                    handleContent(child, localSymbols);
                    break;
                default:
                    traverse(child, localSymbols);
                    break;
            }
        }
    }

    private void handleContent(Node node, Map<String, String> localSymbols) {
        Node first = node.getChildren().get(0);
        if (first.getSymbol().equals("ID")) {
            String name = first.getToken().getLexeme();
            Node second = node.getChildren().get(1);
            if (second.getSymbol().equals("EQUAL_ASSIGNATION")) {
                String expectedType = resolveType(name, localSymbols);
                String actualType = getExpressionType(node.getChildren().get(2), localSymbols);
                if (!expectedType.equals(actualType))
                    error(node, "Cannot assign '" + actualType + "' to '" + expectedType + "'");
            } else if (second.getSymbol().equals("OPEN_PARENTHESIS")) {
                if (!functionsDeclared.contains(name))
                    error(node, "Function '" + name + "' not declared");
            }
        } else if (first.getSymbol().equals("RETURN")) {
            if (node.getChildren().size() > 1) {
                getExpressionType(node.getChildren().get(1), localSymbols); // Type checking for return
            }
        }
    }

    private String getExpressionType(Node node, Map<String, String> localSymbols) {
        if (node.getToken() != null) {
            switch (node.getToken().getType()) {
                case "INT_VALUE": return "INT";
                case "FLOAT_VALUE": return "FLOAT";
                case "CHAR_VALUE": return "CHAR";
                case "ID": return resolveType(node.getToken().getLexeme(), localSymbols);
            }
        } else if (!node.getChildren().isEmpty()) {
            if (node.getChildren().size() == 3 && isOperator(node.getChildren().get(1))) {
                String left = getExpressionType(node.getChildren().get(0), localSymbols);
                String right = getExpressionType(node.getChildren().get(2), localSymbols);
                if (!left.equals(right))
                    error(node, "Mismatched expression types: '" + left + "' vs '" + right + "'");
                return left;
            } else {
                return getExpressionType(node.getChildren().get(0), localSymbols);
            }
        }
        return "UNKNOWN";
    }

    private boolean isOperator(Node node) {
        String type = node.getSymbol();
        return Set.of("SUM", "MINUS", "MULTIPLY", "DIVISION").contains(type);
    }

    private String resolveType(String name, Map<String, String> localSymbols) {
        if (localSymbols.containsKey(name)) return localSymbols.get(name);
        if (globalSymbols.containsKey(name)) return globalSymbols.get(name);
        throw new RuntimeException("Undeclared variable: " + name);
    }

    private String extractType(Node tipus) {
        if (tipus.getChildren().size() == 1) {
            return tipus.getChildren().get(0).getChildren().get(0).getSymbol();
        } else {
            String size = tipus.getChildren().get(2).getToken().getLexeme();
            String base = tipus.getChildren().get(3).getChildren().get(0).getSymbol();
            return "ARRAY[" + size + "]" + base;
        }
    }

    private void error(Node node, String msg) {
        int line = node.getToken() != null ? node.getToken().getLine() : -1;
        throw new RuntimeException("[Line " + line + "] Semantic Error: " + msg);
    }
}