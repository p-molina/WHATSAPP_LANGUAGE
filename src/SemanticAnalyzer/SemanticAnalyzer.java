package SemanticAnalyzer;

import entities.*;
import java.util.*;
import entities.SemanticErrorType;

public class SemanticAnalyzer {
    private final Node root;
    private final SymbolTable symbolTable = new SymbolTable();
    private int currentScope = 0;
    private int scopeCounter = 1; // scope 0 for globals, functions start from 1

    private boolean insideFunction = false;
    private boolean mainDeclared = false;
    private boolean enteredAnyFunction = false;
    private String currentFunctionReturnType = null;

    public SemanticAnalyzer(Node root) {
        this.root = root;
    }

    public void analyze() {
        traverse(root);
        if (!mainDeclared) {
            throw new RuntimeException(SemanticErrorType.MISSING_MAIN.format());
        }
        symbolTable.printTable();
    }

    private void traverse(Node node) {
        if (node == null) return;
        String sym = normalizeSymbol(node.getSymbol());

        switch (sym) {
            case "UNIT" -> handleUnit(node);
            case "DECLARACIO" -> handleDeclaration(node);
            case "CREA_FUNCIO", "CREA_MAIN" -> { return; }
            case "ASSIGNACIO" -> handleAssignment(node);
            case "XINPUM" -> handleReturn(node);
            case "CALL_FUNCIO" -> handleFunctionCall(node);
            case "CONTENT" -> handleContent(node);
            default -> traverseChildren(node);
        }
    }

    private void traverseChildren(Node node) {
        for (Node child : node.getChildren()) traverse(child);
    }

    private String normalizeSymbol(String sym) {
        if (sym.startsWith("<") && sym.endsWith(">")) {
            return sym.substring(1, sym.length() - 1);
        }
        return sym;
    }

    private void handleContent(Node node) {
        Node first = node.getChildren().get(0);
        Token tok = first.getToken();
        if (tok == null) { traverseChildren(node); return; }
        switch (tok.getType()) {
            case "ID" -> handleIdContent(node, first);
            case "POS" -> handleArrayAssignment(node);
            case "RETURN" -> handleReturnStatement(node);
            default -> traverseChildren(node);
        }
    }

    private void handleIdContent(Node node, Node idNode) {
        Node tail = node.getChildren().get(1);
        Token tailTok = tail.getChildren().get(0).getToken();
        String name = idNode.getToken().getLexeme();

        if (tailTok != null && "EQUAL_ASSIGNATION".equals(tailTok.getType())) {
            String expected = lookupType(name);
            if (expected == null) error(idNode, SemanticErrorType.VARIABLE_NOT_DECLARED.format(name));
            String actual = getExpressionType(tail.getChildren().get(1));
            if (!expected.equals(actual)) error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN.format(actual, expected));
        } else if (tailTok != null && "OPEN_PARENTESIS".equals(tailTok.getType())) {
            handleFunctionCall(node);
        } else {
            traverseChildren(node);
        }
    }

    private void handleArrayAssignment(Node node) {
        Node idx = node.getChildren().get(1);
        Node arrId = node.getChildren().get(3);
        String arrName = arrId.getToken().getLexeme();

        String t = lookupType(arrName);
        if (t == null) error(arrId, SemanticErrorType.VARIABLE_NOT_DECLARED.format(arrName));
        if (!t.startsWith("ARRAY")) error(arrId, SemanticErrorType.NOT_AN_ARRAY.format(arrName));

        String idxType = getExpressionType(idx);
        if (!"INT".equals(idxType)) error(idx, SemanticErrorType.ARRAY_INDEX_TYPE.format(idxType));

        String base = t.substring(t.indexOf("]") + 1).replace("_VALUE", "");
        String valType = getExpressionType(node.getChildren().get(5));
        if (!base.equals(valType)) error(node, SemanticErrorType.ARRAY_ASSIGN_TYPE.format(valType, base));
    }

    private void handleReturnStatement(Node node) {
        if (!insideFunction) error(node.getChildren().get(0), SemanticErrorType.RETURN_OUTSIDE_FUNCTION.format());
        String rt = getExpressionType(node.getChildren().get(1));
        if (currentFunctionReturnType != null && !rt.equals(currentFunctionReturnType)) {
            error(node, SemanticErrorType.RETURN_TYPE_MISMATCH.format(currentFunctionReturnType, rt));
        }
    }

    private void handleUnit(Node unitNode) {
        Node tipusNode = unitNode.getChildren().get(0);
        Node tail = unitNode.getChildren().get(1);
        Node first = tail.getChildren().get(0);

        if ("MAIN".equals(first.getToken().getType())) {
            processOriginalMain(unitNode, tipusNode, tail);
        } else {
            Node idNode = first;
            Node declTail = tail.getChildren().get(1);
            Node declFirst = declTail.getChildren().get(0);

            if ("EQUAL_ASSIGNATION".equals(declFirst.getToken().getType())) {
                processOriginalDeclaration(unitNode, tipusNode, idNode, declTail);
            } else {
                processOriginalFunction(unitNode, tipusNode, idNode, declTail);
            }
        }
    }

    private void processOriginalMain(Node unitNode, Node tipusNode, Node tail) {
        if (mainDeclared) error(unitNode, SemanticErrorType.MAIN_ALREADY_DEFINED.format());

        mainDeclared = true;
        enteredAnyFunction = true;
        currentFunctionReturnType = getTypeFromTipus(tipusNode);
        insideFunction = true;

        Node bodyNode = tail.getChildren().get(2);
        traverse(bodyNode);

        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void processOriginalDeclaration(Node unitNode, Node tipusNode, Node idNode, Node declTail) {
        String type = getTypeFromTipus(tipusNode);
        String name = idNode.getToken().getLexeme();

        if (lookupType(name) != null) error(idNode, SemanticErrorType.VARIABLE_ALREADY_DECLARED.format(name));

        Token token = idNode.getToken();
        symbolTable.addSymbol(name, type, currentScope, token.getLine(), token.getColumn());

        String valueType = getExpressionType(declTail.getChildren().get(1));
        if (!type.equals(valueType)) error(unitNode, SemanticErrorType.TYPE_MISMATCH_ASSIGN.format(valueType, type));
    }

    private void processOriginalFunction(Node unitNode, Node tipusNode, Node idNode, Node declTail) {
        if (mainDeclared) error(unitNode, SemanticErrorType.FUNCTION_AFTER_MAIN.format());

        String name = idNode.getToken().getLexeme();
        if (lookupType(name) != null) error(idNode, SemanticErrorType.FUNCTION_NAME_CONFLICT.format(name));

        String returnType = getTypeFromTipus(tipusNode);
        Token token = idNode.getToken();

        int functionScope = scopeCounter++;
        symbolTable.addSymbol(name, "FUNC", functionScope, token.getLine(), token.getColumn());

        currentFunctionReturnType = returnType;
        insideFunction = true;
        enteredAnyFunction = true;
        currentScope = functionScope;

        Node bodyNode = declTail.getChildren().get(1).getChildren().get(0);
        traverse(bodyNode);

        currentScope = 0;
        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleDeclaration(Node node) {
        String type = getTypeFromTipus(node);
        String name = node.getChildren().get(1).getToken().getLexeme();

        if (lookupType(name) != null) error(node, SemanticErrorType.VARIABLE_ALREADY_DECLARED.format(name));

        Token token = node.getChildren().get(1).getToken();
        symbolTable.addSymbol(name, type, currentScope, token.getLine(), token.getColumn());
    }

    private void handleAssignment(Node node) {
        Node assignPrim = node.getChildren().get(0);

        if (assignPrim.getChildren().get(0).getSymbol().equals("POS")) {
            handleArrayAssignment(node);
            return;
        }

        String name = assignPrim.getChildren().get(0).getToken().getLexeme();
        String expectedType = lookupType(name);
        if (expectedType == null) error(assignPrim, SemanticErrorType.VARIABLE_NOT_DECLARED.format(name));

        String actualType = getExpressionType(node.getChildren().get(1));
        if (!expectedType.equals(actualType)) {
            error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN.format(actualType, expectedType));
        }
    }

    private void handleReturn(Node node) {
        if (!insideFunction) error(node, SemanticErrorType.RETURN_OUTSIDE_FUNCTION.format());

        if (node.getChildren().size() > 1) {
            String retType = getExpressionType(node.getChildren().get(1));
            if (currentFunctionReturnType != null && !retType.equals(currentFunctionReturnType)) {
                error(node, SemanticErrorType.RETURN_TYPE_MISMATCH.format(currentFunctionReturnType, retType));
            }
        }
    }

    private void handleFunctionCall(Node node) {
        String funcName = node.getChildren().get(0).getToken().getLexeme();
        Symbol s = symbolTable.getSymbol(funcName, 0);
        if (s == null || !s.getType().equals("FUNC")) {
            error(node, SemanticErrorType.FUNCTION_NOT_DECLARED.format(funcName));
        }

        List<String> argTypes = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            String sym = child.getSymbol();
            if (!List.of("OPEN_PARENTESIS", "CLOSE_PARENTESIS", "LINE_DELIMITER").contains(sym)) {
                argTypes.add(getExpressionType(child));
            }
        }

        if (!argTypes.isEmpty()) {
            error(node, SemanticErrorType.FUNCTION_ARGUMENTS_MISMATCH.format(funcName, 0, argTypes.size()));
        }
    }

    private void handleLocalDeclaration(Node node) {
        String type = getTypeFromTipus(node.getChildren().get(0));
        Token idToken = node.getChildren().get(1).getToken();
        String name = idToken.getLexeme();

        if (lookupType(name) != null) {
            error(node, SemanticErrorType.VARIABLE_ALREADY_DECLARED.format(name));
        }

        symbolTable.addSymbol(name, type, currentScope, idToken.getLine(), idToken.getColumn());

        Node suffixNode = node.getChildren().get(2);
        if (suffixNode.getChildren().isEmpty()) return;

        Node maybeAssign = suffixNode.getChildren().get(0);
        if ("EQUAL_ASSIGNATION".equals(maybeAssign.getToken().getType())) {
            Node expr = suffixNode.getChildren().get(1);
            String valueType = getExpressionType(expr);
            if (!type.equals(valueType)) {
                error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN.format(valueType, type));
            }
        }
    }

    private String getExpressionType(Node node) {
        if (node.getToken() != null) {
            String lexeme = node.getToken().getLexeme();
            String tokenType = node.getToken().getType();
            switch (tokenType) {
                case "INT_VALUE": return "INT";
                case "FLOAT_VALUE": return "FLOAT";
                case "CHAR_VALUE": return "CHAR";
                case "ID": {
                    String type = lookupType(lexeme);
                    if (type != null) return type;
                    Symbol s = symbolTable.getSymbol(lexeme, 0);
                    if (s != null && s.getType().startsWith("FUNC->"))
                        return s.getType().substring("FUNC->".length());
                    error(node, SemanticErrorType.UNKNOWN_SYMBOL.format(lexeme));
                }
            }
        } else if (!node.getChildren().isEmpty()) {
            if (node.getChildren().size() == 3 && isOperator(node.getChildren().get(1))) {
                String left = getExpressionType(node.getChildren().get(0));
                String right = getExpressionType(node.getChildren().get(2));
                if (!left.equals(right)) error(node, SemanticErrorType.EXPRESSION_TYPE_MISMATCH.format(left, right));
                return left;
            }
            for (Node child : node.getChildren()) {
                String type = getExpressionType(child);
                if (!type.equals("UNKNOWN")) return type;
            }
        }
        return "UNKNOWN";
    }

    private boolean isOperator(Node node) {
        String type = node.getSymbol();
        return type.equals("SUM") || type.equals("MINUS") || type.equals("MULTIPLY") || type.equals("DIVISION");
    }

    private String getTypeFromTipus(Node tipusNode) {
        Node first = tipusNode.getChildren().get(0);
        if ("ARRAY".equals(first.getSymbol())) {
            String size = tipusNode.getChildren().get(2).getToken().getLexeme();
            String baseType = tipusNode.getChildren().get(3).getChildren().get(0).getSymbol();
            return "ARRAY[" + size + "]" + baseType;
        }
        return first.getChildren().get(0).getSymbol();
    }

    private String lookupType(String name) {
        Symbol s = symbolTable.getSymbol(name, currentScope);
        if (s == null) s = symbolTable.getSymbol(name, 0);
        return s != null ? s.getType() : null;
    }

    private void error(Node node, String message) {
        int line = -1;
        if (node.getToken() != null) line = node.getToken().getLine();
        else for (Node child : node.getChildren()) {
            if (child.getToken() != null) { line = child.getToken().getLine(); break; }
        }
        throw new RuntimeException("[Line " + line + "] Semantic Error: " + message);
    }

}
