
package SemanticAnalyzer;

import entities.Node;
import entities.SymbolTable;
import entities.Token;
import entities.SemanticErrorType;
import entities.Symbol;

import java.util.*;

public class SemanticAnalyzerDEBUG {
    private final Node root;
    private final SymbolTable symbolTableBona = new SymbolTable();
    private final Deque<Integer> scopeStack = new ArrayDeque<>();
    private int nextScopeId = 1;
    private boolean insideFunction = false;
    private boolean mainDeclared = false;
    private String currentFunctionReturnType = null;

    public SemanticAnalyzerDEBUG(Node root) {
        this.root = root;
    }

    public void analyze() {
        scopeStack.push(0);
        traverse(root);
        if (!mainDeclared) {
            throw new RuntimeException(SemanticErrorType.MISSING_MAIN.toString());
        }
        System.out.println("\nDEBUG:");
        symbolTableBona.printTable();
    }

    private int currentScope() { return scopeStack.peek(); }
    private void enterScope() { scopeStack.push(nextScopeId++); }
    private void exitScope() { scopeStack.pop(); }

    private Symbol getSymbol(String name) {
        return symbolTableBona.getSymbol(name, currentScope());
    }

    private boolean existsInCurrentScope(String name) {
        return symbolTableBona.getScopeSymbols(currentScope()).containsKey(name);
    }

    private void traverse(Node node) {
        if (node == null) return;
        String sym = node.getSymbol();
        if (sym.startsWith("<") && sym.endsWith(">")) {
            sym = sym.substring(1, sym.length() - 1);
        }

        switch (sym) {
            case "UNIT" -> handleUnit(node);
            case "DECLARACIO" -> handleDeclaration(node);
            case "CREA_FUNCIO" -> handleFunction(node);
            case "CREA_MAIN" -> handleMain(node);
            case "CONTENT" -> handleContent(node);
            case "ASSIGNACIO" -> handleAssignment(node);
            case "XINPUM" -> handleReturn(node);
            case "CALL_FUNCIO" -> handleFunctionCall(node);
            default -> node.getChildren().forEach(this::traverse);
        }
    }

    private void handleContent(Node node) {
        Node first = node.getChildren().get(0);
        Token tok = first.getToken();

        if (tok == null) {
            String sym = first.getSymbol().replaceAll("[<>]", "");
            if ("TIPUS".equals(sym)) {
                handleDeclaration(node);
                return;
            }
            traverseChildren(node);
            return;
        }

        switch (tok.getType()) {
            case "ID" -> handleIDContent(node, first);
            case "POS" -> handleArrayAssignment(node);
            case "RETURN" -> handleReturnContent(node, first);
            default -> traverseChildren(node);
        }
    }

    private void handleIDContent(Node node, Node first) {
        Node tail = node.getChildren().get(1);
        Token tailTok = tail.getChildren().get(0).getToken();
        String name = first.getToken().getLexeme();
        Symbol sym = getSymbol(name);

        if (tailTok != null && "EQUAL_ASSIGNATION".equals(tailTok.getType())) {
            if (sym == null) error(first, SemanticErrorType.VARIABLE_NOT_DECLARED, name);
            String expected = sym.getType();
            String actual = getExpressionType(tail.getChildren().get(1));
            if (!expected.equals(actual)) error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN, actual, expected);
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
        Symbol sym = getSymbol(arrName);

        if (sym == null) error(arrId, SemanticErrorType.VARIABLE_NOT_DECLARED, arrName);

        String t = sym.getType();
        if (!t.startsWith("ARRAY")) error(arrId, SemanticErrorType.NOT_AN_ARRAY, arrName);

        String idxType = getExpressionType(idx);
        if (!"INT".equals(idxType)) error(idx, SemanticErrorType.ARRAY_INDEX_TYPE, idxType);

        String base = t.substring(t.indexOf("]") + 1).replace("_VALUE", "");
        String valType = getExpressionType(node.getChildren().get(5));
        if (!base.equals(valType)) error(node, SemanticErrorType.ARRAY_ASSIGN_TYPE, valType, base);
    }

    private void handleReturnContent(Node node, Node first) {
        if (!insideFunction) error(first, SemanticErrorType.RETURN_OUTSIDE_FUNCTION);

        String rt = getExpressionType(node.getChildren().get(1));
        if (currentFunctionReturnType != null && !rt.equals(currentFunctionReturnType))
            error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, rt);
    }

    private void traverseChildren(Node node) {
        node.getChildren().forEach(this::traverse);
    }

    private void handleUnit(Node unitNode) {
        Node tipusNode = unitNode.getChildren().get(0);
        Node tail = unitNode.getChildren().get(1);
        Node first = tail.getChildren().get(0);

        if (first.getToken() != null && "MAIN".equals(first.getToken().getType())) {
            handleMainUnit(tipusNode, tail);
        } else {
            Node declTail = tail.getChildren().get(1);
            Node declFirst = declTail.getChildren().get(0);
            if ("EQUAL_ASSIGNATION".equals(declFirst.getToken().getType()))
                handleDeclarationUnit(unitNode, tipusNode, first, declTail);
            else
                handleFunctionUnit(tipusNode, first, declTail);
        }
    }

    private void handleDeclarationUnit(Node unitNode, Node tipusNode, Node idNode, Node declTail) {
        String type = getTypeFromTipus(tipusNode);
        String name = idNode.getToken().getLexeme();
        symbolTableBona.addSymbol(name, type, currentScope(),
                idNode.getToken().getLine(), idNode.getToken().getColumn());
        String valueType = getExpressionType(declTail.getChildren().get(1));
        if (!type.equals(valueType))
            error(unitNode, SemanticErrorType.TYPE_MISMATCH_ASSIGN, valueType, type);

    }

    private void handleFunctionUnit(Node tipusNode, Node idNode, Node declTail) {
        String returnType = getTypeFromTipus(tipusNode);
        String name = idNode.getToken().getLexeme();
        currentFunctionReturnType = returnType;
        insideFunction = true;
        enterScope();
        symbolTableBona.addSymbol(name, returnType, currentScope(),
                idNode.getToken().getLine(), idNode.getToken().getColumn());
        traverse(declTail.getChildren().get(1).getChildren().get(0));
        exitScope();
        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleMainUnit(Node tipusNode, Node tail) {
        currentFunctionReturnType = getTypeFromTipus(tipusNode);
        insideFunction = true;
        mainDeclared = true;
        enterScope();
        traverse(tail.getChildren().get(2));
        exitScope();
        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleFunction(Node node) {
        String returnType = node.getChildren().get(0).getSymbol();
        String name = node.getChildren().get(1).getToken().getLexeme();
        currentFunctionReturnType = returnType;
        insideFunction = true;
        enterScope();
        symbolTableBona.addSymbol(name, returnType, currentScope(),
                node.getChildren().get(1).getToken().getLine(), node.getChildren().get(1).getToken().getColumn());
        node.getChildren().forEach(this::traverse);
        exitScope();
        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleMain(Node node) {
        currentFunctionReturnType = node.getChildren().get(0).getSymbol();
        insideFunction = true;
        mainDeclared = true;
        enterScope();
        node.getChildren().forEach(this::traverse);
        exitScope();
        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleDeclaration(Node node) {
        String type = node.getChildren().get(0).getSymbol();
        String name = node.getChildren().get(1).getToken().getLexeme();
        Token idTok = node.getChildren().get(1).getToken();
        symbolTableBona.addSymbol(name, type, currentScope(), idTok.getLine(), idTok.getColumn());
    }

    private void handleAssignment(Node node) {
        String name = node.getChildren().get(0).getChildren().get(0).getToken().getLexeme();
        Symbol sym = getSymbol(name);
        if (sym == null) error(node, SemanticErrorType.VARIABLE_NOT_DECLARED, name);
        String expected = sym.getType();
        String actual = getExpressionType(node.getChildren().get(1));
        if (!expected.equals(actual)) error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN, actual, expected);
    }

    private void handleReturn(Node node) {
        if (!insideFunction) error(node, SemanticErrorType.RETURN_OUTSIDE_FUNCTION);
        if (node.getChildren().size() > 1) {
            String retType = getExpressionType(node.getChildren().get(1));
            if (!retType.equals(currentFunctionReturnType))
                error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, retType);
        }
    }

    private void handleFunctionCall(Node node) {
        String funcName = node.getChildren().get(0).getToken().getLexeme();
        Symbol sym = getSymbol(funcName);
        if (sym == null) error(node, SemanticErrorType.FUNCTION_NOT_DECLARED, funcName);
    }

    private String getTypeFromTipus(Node tipusNode) {
        Node first = tipusNode.getChildren().get(0);
        if ("ARRAY".equals(first.getSymbol())) {
            String size = tipusNode.getChildren().get(2).getToken().getLexeme();
            String baseType = tipusNode.getChildren().get(3).getChildren().get(0).getSymbol();
            return "ARRAY[" + size + "]" + baseType;
        } else {
            return first.getChildren().get(0).getSymbol();
        }
    }

    private String getExpressionType(Node node) {
        if (node.getToken() != null) {
            return switch (node.getToken().getType()) {
                case "INT_VALUE" -> "INT";
                case "FLOAT_VALUE" -> "FLOAT";
                case "CHAR_VALUE" -> "CHAR";
                case "ID" -> {
                    String name = node.getToken().getLexeme();
                    Symbol s = getSymbol(name);
                    if (s != null) yield s.getType();
                    error(node, SemanticErrorType.UNKNOWN_SYMBOL, name);
                    yield "UNKNOWN";
                }
                default -> "UNKNOWN";
            };
        }
        if (!node.getChildren().isEmpty()) {
            if (node.getChildren().size() == 3 && isOperator(node.getChildren().get(1))) {
                String left = getExpressionType(node.getChildren().get(0));
                String right = getExpressionType(node.getChildren().get(2));
                if (!left.equals(right))
                    error(node, SemanticErrorType.EXPRESSION_TYPE_MISMATCH, left, right);
                return left;
            } else {
                for (Node child : node.getChildren()) {
                    String type = getExpressionType(child);
                    if (!"UNKNOWN".equals(type)) return type;
                }
            }
        }
        return "UNKNOWN";
    }

    private boolean isOperator(Node node) {
        return switch (node.getSymbol()) {
            case "SUM", "MINUS", "MULTIPLY", "DIVISION" -> true;
            default -> false;
        };
    }

    private void error(Node node, SemanticErrorType type, Object... args) {
        int line = -1;
        if (node.getToken() != null) line = node.getToken().getLine();
        else for (Node child : node.getChildren()) {
            if (child.getToken() != null) {
                line = child.getToken().getLine();
                break;
            }
        }
        throw new RuntimeException("[Line " + line + "] " + type.format(args));
    }
}
