
package SemanticAnalyzer;

import entities.Node;
import entities.SymbolTable;
import entities.Token;
import entities.Symbol;

import java.util.*;

public class SemanticAnalyzer {
    private final Node root;
    private final SymbolTable symbolTable;
    private final Deque<Integer> scopeStack = new ArrayDeque<>();
    private int nextScopeId = 1;
    private boolean insideFunction = false;
    private boolean mainDeclared = false;
    private String currentFunctionReturnType = null;

    public SemanticAnalyzer(Node root, SymbolTable symbolTable) {
        this.root = root;
        this.symbolTable = symbolTable;
    }


    public void analyze() {
        scopeStack.push(0);
        traverse(root);
        if (!mainDeclared) throw new RuntimeException(SemanticErrorType.MISSING_MAIN.toString());

        //symbolTable.printTable();
    }

    private int currentScope() { return scopeStack.peek(); }
    private void enterScope() { scopeStack.push(nextScopeId++); }
    private void exitScope() { scopeStack.pop(); }

    private Symbol getSymbol(String name) { return symbolTable.getSymbol(name, currentScope()); }

    private void traverse(Node node) {
        if (node == null) return;
        String sym = node.getSymbol();
        if (sym.startsWith("<") && sym.endsWith(">")) sym = sym.substring(1, sym.length() - 1);

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

    private void handleLocalDeclaration(Node node) {
        // 1) extraer tipo y nombre
        Node tipusNode = node.getChildren().get(0);
        Node idNode    = node.getChildren().get(1);
        String name    = idNode.getToken().getLexeme();
        String type    = getTypeFromTipus(tipusNode);

        // 2) chequear redeclaración
        if (getSymbol(name) != null) {
            error(node, SemanticErrorType.VARIABLE_REDECLARED, name);
        }

        // 3) añadir con el tipo correcto
        symbolTable.addSymbol(
                name,
                type,
                currentScope(),
                idNode.getToken().getLine(),
                idNode.getToken().getColumn()
        );

        // 4) si hay asignación (LOCAL_DECL_SUFFIX), comprobar tipos
        if (node.getChildren().size() > 2) {
            Node suffix = node.getChildren().get(2);  // <LOCAL_DECL_SUFFIX>
            if (!suffix.getChildren().isEmpty()) {
                Node firstSuffixChild = suffix.getChildren().get(0);
                Token tok = firstSuffixChild.getToken();
                if (tok != null && "EQUAL_ASSIGNATION".equals(tok.getType())) {
                    // ya tenemos confirmada la asignación
                    // ojo: el expr estará en suffix.getChildren().get(1)
                    Node expr = suffix.getChildren().get(1);
                    String actual = getExpressionType(expr);
                    if (!type.equals(actual)) {
                        error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN, actual, type);
                    }
                }
            }
        }
    }

    private void handleContent(Node node) {
        Node first = node.getChildren().get(0);
        Token tok = first.getToken();

        if (tok == null) {
            String sym = first.getSymbol().replaceAll("[<>]", "");
            if ("TIPUS".equals(sym)) {
                handleLocalDeclaration(node);
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
        if (!insideFunction)
            error(first, SemanticErrorType.RETURN_OUTSIDE_FUNCTION);

        Node expr = node.getChildren().get(1);

        if (expr.getToken() != null && "ID".equals(expr.getToken().getType())) {
            String varName = expr.getToken().getLexeme();
            Symbol sym = getSymbol(varName);
            if (sym == null) {
                error(expr, SemanticErrorType.UNKNOWN_SYMBOL, varName);
            }
            String rt = sym.getType();
            if (!rt.equals(currentFunctionReturnType)) {
                error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, rt);
            }
        }
        else {
            String rt = getExpressionType(expr);
            if (!rt.equals(currentFunctionReturnType)) {
                error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, rt);
            }
        }
    }

    private void traverseChildren(Node node) { node.getChildren().forEach(this::traverse); }

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
            else handleFunctionUnit(tipusNode, first, declTail);
        }
    }

    private void handleDeclarationUnit(Node unitNode, Node tipusNode, Node idNode, Node declTail) {
        String name      = idNode.getToken().getLexeme();
        String type      = getTypeFromTipus(tipusNode);
        String valueType = getExpressionType(declTail.getChildren().get(1));

        if (symbolTable.getScopeSymbols(currentScope()).containsKey(name)) {
            error(idNode, SemanticErrorType.VARIABLE_REDECLARED, name);
        }

        symbolTable.addSymbol(
                name,
                type,
                currentScope(),
                idNode.getToken().getLine(),
                idNode.getToken().getColumn()
        );

        if (!type.equals(valueType)) {
            error(idNode, SemanticErrorType.TYPE_MISMATCH_ASSIGN, valueType, type);
        }
    }


    private void handleFunctionUnit(Node tipusNode, Node idNode, Node declTail) {
        String name = idNode.getToken().getLexeme();
        String returnType = getTypeFromTipus(tipusNode);

        currentFunctionReturnType = returnType;
        insideFunction = true;

        enterScope();

        if (getSymbol(name) != null) error(idNode, SemanticErrorType.FUNCTION_REDECLARED, name);

        symbolTable.addSymbol(name, returnType, currentScope(),
                                    idNode.getToken().getLine(), idNode.getToken().getColumn());

        traverse(declTail.getChildren().get(1).getChildren().get(0));

        exitScope();

        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleMainUnit(Node tipusNode, Node tail) {
        String name = tail.getChildren().get(0).getToken().getLexeme();
        currentFunctionReturnType = getTypeFromTipus(tipusNode);
        insideFunction = true;
        mainDeclared = true;

        enterScope();

        if (getSymbol(name) != null) error(tail, SemanticErrorType.FUNCTION_REDECLARED, name);

        symbolTable.addSymbol(name, currentFunctionReturnType, currentScope(),
                tail.getChildren().get(0).getToken().getLine(), tail.getChildren().get(0).getToken().getColumn());

        traverse(tail.getChildren().get(2));

        exitScope();

        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleFunction(Node node) {
        String name = node.getChildren().get(1).getToken().getLexeme();
        String returnType = node.getChildren().get(0).getSymbol();
        currentFunctionReturnType = returnType;
        insideFunction = true;

        enterScope();

        symbolTable.addSymbol(name, returnType, currentScope(),
                node.getChildren().get(1).getToken().getLine(), node.getChildren().get(1).getToken().getColumn());

        node.getChildren().forEach(this::traverse);

        exitScope();

        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleMain(Node node) {
        String name = node.getChildren().get(1).getToken().getLexeme();
        String returnType = node.getChildren().get(0).getSymbol();
        currentFunctionReturnType = node.getChildren().get(0).getSymbol();
        insideFunction = true;
        mainDeclared = true;

        enterScope();

        symbolTable.addSymbol(name, returnType, currentScope(),
                node.getChildren().get(1).getToken().getLine(), node.getChildren().get(1).getToken().getColumn());

        node.getChildren().forEach(this::traverse);

        exitScope();

        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleDeclaration(Node node) {
        Node tipusNode = node.getChildren().get(0);
        Node idNode = node.getChildren().get(1);
        String name = idNode.getToken().getLexeme();
        String type = getTypeFromTipus(tipusNode);

        if (symbolTable.getScopeSymbols(currentScope()).containsKey(name)) {
            error(idNode, SemanticErrorType.VARIABLE_REDECLARED, name);
        }


        symbolTable.addSymbol(name, type, currentScope(), idNode.getToken().getLine(), idNode.getToken().getColumn());
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
        if (!insideFunction)
            error(node, SemanticErrorType.RETURN_OUTSIDE_FUNCTION);

        if (node.getChildren().size() > 1) {
            Node expr = node.getChildren().get(1);

            if (expr.getToken() != null && "ID".equals(expr.getToken().getType())) {
                String varName = expr.getToken().getLexeme();
                Symbol sym = getSymbol(varName);
                if (sym == null) {
                    error(expr, SemanticErrorType.UNKNOWN_SYMBOL, varName);
                }
                String retType = sym.getType();

                if (!retType.equals(currentFunctionReturnType)) {
                    error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, retType);
                }
            }
            else {
                // En cualquier otro caso (literal, expresión compuesta…), seguimos usando getExpressionType
                String retType = getExpressionType(expr);
                if (!retType.equals(currentFunctionReturnType)) {
                    error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, retType);
                }
            }
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
        String sym = node.getSymbol();

        if (sym.startsWith("<") && sym.endsWith(">")) {
            sym = sym.substring(1, sym.length() - 1);
        }

        if ("TIPUS".equals(sym)) {return getTypeFromTipus(node);}

        if (node.getToken() != null) {
            switch (node.getToken().getType()) {
                case "INT_VALUE"   ->   { return "INT"; }
                case "FLOAT_VALUE" ->   { return "FLOAT"; }
                case "CHAR_VALUE"  ->   { return "CHAR"; }
                case "ID" -> {
                    String name = node.getToken().getLexeme();
                    Symbol s = getSymbol(name);
                    if (s != null) return s.getType();
                    error(node, SemanticErrorType.UNKNOWN_SYMBOL, name);
                    return "UNKNOWN";
                }
                case "TIPUS" -> {
                    return node.getToken().getLexeme();
                }
                default -> {}
            }
        }

        if (node.getChildren().size() == 3 && isOperator(node.getChildren().get(1))) {
            String left  = getExpressionType(node.getChildren().get(0));
            String right = getExpressionType(node.getChildren().get(2));
            if (!left.equals(right)) {
                error(node, SemanticErrorType.EXPRESSION_TYPE_MISMATCH, left, right);
            }
            return left;
        }

        for (Node child : node.getChildren()) {
            String t = getExpressionType(child);
            if (!"UNKNOWN".equals(t)) {
                return t;
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
