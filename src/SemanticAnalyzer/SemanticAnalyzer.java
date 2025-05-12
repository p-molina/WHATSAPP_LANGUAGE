package SemanticAnalyzer;

import entities.Node;
import entities.SymbolTable;
import entities.Token;
import entities.SemanticErrorType;
import entities.Symbol;

import java.util.*;

public class SemanticAnalyzer {
    private final Node root;
    private final Map<String, String> symbolTable = new HashMap<>();
    private final SymbolTable symbolTableBona = new SymbolTable();
    private final Deque<Integer> scopeStack = new ArrayDeque<>();
    private int nextScopeId = 1;

    private static class FunctionSignature {
        String returnType;
        List<String> paramTypes;
        FunctionSignature(String returnType, List<String> paramTypes) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }
    }

    private final Map<String, FunctionSignature> functionsDeclared = new HashMap<>();

    private boolean insideFunction = false;
    private boolean mainDeclared = false;
    private boolean enteredAnyFunction = false;
    private String currentFunctionReturnType = null;

    public SemanticAnalyzer(Node root) {
        this.root = root;
    }

    public void analyze() {
        // Inicialitzem l’àmbit global (0)
        scopeStack.push(0);
        traverse(root);
        if (!mainDeclared) {
            throw new RuntimeException(SemanticErrorType.MISSING_MAIN.toString());
        }
        symbolTableBona.printTable();
    }

    private int currentScope() {
        return scopeStack.peek();
    }

    private void enterScope() {
        int newScope = nextScopeId++;
        scopeStack.push(newScope);
    }

    private void exitScope() {
        scopeStack.pop();
    }

    private void traverse(Node node) {
        if (node == null) return;

        String sym = node.getSymbol();
        if (sym.startsWith("<") && sym.endsWith(">")) {
            sym = sym.substring(1, sym.length() - 1);
        }

        boolean recurse = true;
        switch (sym) {
            case "UNIT": handleUnit(node); return;
            case "DECLARACIO": handleDeclaration(node); break;
            case "CREA_FUNCIO": handleFunction(node); recurse = false; break;
            case "CREA_MAIN": handleMain(node); recurse = false; break;
            case "CONTENT": handleContent(node); return;
            case "ASSIGNACIO": handleAssignment(node); break;
            case "XINPUM": handleReturn(node); break;
            case "CALL_FUNCIO": handleFunctionCall(node); break;
        }
        if (recurse) {
            for (Node child : node.getChildren()) {
                traverse(child);
            }
        }
    }

    private void handleContent(Node node) {
        Node first = node.getChildren().get(0);
        Token tok = first.getToken();

        // 1) Si no té token i el node és <TIPUS>, tractem-ho com a declaració local
        if (tok == null) {
            String sym = first.getSymbol();
            if (sym.startsWith("<") && sym.endsWith(">")) {
                sym = sym.substring(1, sym.length() - 1);
            }
            if ("TIPUS".equals(sym)) {
                handleDeclaration(node);
                return;
            }
        }

        // 2) Si no té token però no és TIPUS, simplement descent:
        if (tok == null) {
            traverseChildren(node);
            return;
        }

        // 3) Ara tractem els casos que sí tenen token
        switch (tok.getType()) {
            case "ID": {
                Node tail = node.getChildren().get(1);
                Token tailTok = tail.getChildren().get(0).getToken();
                String name = first.getToken().getLexeme();

                if (tailTok != null && "EQUAL_ASSIGNATION".equals(tailTok.getType())) {
                    // Assignació a variable existent
                    if (!symbolTable.containsKey(name)) {
                        error(first, SemanticErrorType.VARIABLE_NOT_DECLARED, name);
                    }
                    String expected = symbolTable.get(name);
                    Node expr = tail.getChildren().get(1);
                    String actual = getExpressionType(expr);
                    if (!expected.equals(actual)) {
                        error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN, actual, expected);
                    }

                } else if (tailTok != null && "OPEN_PARENTESIS".equals(tailTok.getType())) {
                    // Crida a funció
                    handleFunctionCall(node);

                } else {
                    traverseChildren(node);
                }
                break;
            }
            case "POS": {
                // Assignació a array per índex
                Node idx = node.getChildren().get(1);
                Node arrId = node.getChildren().get(3);
                String arrName = arrId.getToken().getLexeme();
                if (!symbolTable.containsKey(arrName)) {
                    error(arrId, SemanticErrorType.VARIABLE_NOT_DECLARED, arrName);
                }
                String t = symbolTable.get(arrName);
                if (!t.startsWith("ARRAY")) {
                    error(arrId, SemanticErrorType.NOT_AN_ARRAY, arrName);
                }
                String idxType = getExpressionType(idx);
                if (!"INT".equals(idxType)) {
                    error(idx, SemanticErrorType.ARRAY_INDEX_TYPE, idxType);
                }
                String base = t.substring(t.indexOf("]") + 1);
                if (base.endsWith("VALUE")) base = base.replace("_VALUE","");
                Node valExpr = node.getChildren().get(5);
                String valType = getExpressionType(valExpr);
                if (!base.equals(valType)) {
                    error(node, SemanticErrorType.ARRAY_ASSIGN_TYPE, valType, base);
                }
                break;
            }
            case "RETURN": {
                // RETURN (xinpum)
                Node expr = node.getChildren().get(1);
                if (!insideFunction) {
                    error(first, SemanticErrorType.RETURN_OUTSIDE_FUNCTION);
                }
                String rt = getExpressionType(expr);
                if (currentFunctionReturnType != null && !rt.equals(currentFunctionReturnType)) {
                    error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, rt);
                }
                break;
            }
            default:
                traverseChildren(node);
        }
    }


    private void traverseChildren(Node node) {
        for (Node c: node.getChildren()) traverse(c);
    }

    private void handleUnit(Node unitNode) {
        Node tipusNode = unitNode.getChildren().get(0);
        Node tail = unitNode.getChildren().get(1);
        Node first = tail.getChildren().get(0);

        if (first.getToken() != null && "MAIN".equals(first.getToken().getType())) {
            processOriginalMain(unitNode, tipusNode, tail);
        } else if (first.getToken() != null && "ID".equals(first.getToken().getType())) {
            Node idNode = first;
            Node declTail = tail.getChildren().get(1);
            Node declFirst = declTail.getChildren().get(0);
            if (declFirst.getToken() != null && "EQUAL_ASSIGNATION".equals(declFirst.getToken().getType())) {
                processOriginalDeclaration(unitNode, tipusNode, idNode, declTail);
            } else if (declFirst.getToken() != null && "OPEN_CLAUDATOR".equals(declFirst.getToken().getType())) {
                processOriginalFunction(unitNode, tipusNode, idNode, declTail);
            }
        }
    }

    private String getTypeFromTipus(Node tipusNode) {
        Node first = tipusNode.getChildren().get(0);
        if ("ARRAY".equals(first.getSymbol())) {
            String size = tipusNode.getChildren().get(2).getToken().getLexeme();
            Node baseNode = tipusNode.getChildren().get(3);
            String baseType = baseNode.getChildren().get(0).getSymbol();
            return "ARRAY[" + size + "]" + baseType;
        } else {
            Node baseNode = first.getChildren().get(0);
            return baseNode.getSymbol();
        }
    }

    private void processOriginalDeclaration(Node unitNode, Node tipusNode, Node idNode, Node declTail) {
        if (enteredAnyFunction && !insideFunction) {
            error(unitNode, SemanticErrorType.FUNCTION_AFTER_MAIN);
        }
        String type = getTypeFromTipus(tipusNode);
        String name = idNode.getToken().getLexeme();
        if (symbolTable.containsKey(name)) {
            error(idNode, SemanticErrorType.VARIABLE_ALREADY_DECLARED, name);
        }
        if (functionsDeclared.containsKey(name)) {
            error(idNode, SemanticErrorType.VARIABLE_NAME_CONFLICT, name);
        }
        symbolTable.put(name, type);
        symbolTableBona.addSymbol(name, type, currentScope(), idNode.getToken().getLine(), idNode.getToken().getColumn());

        Node expr = declTail.getChildren().get(1);
        String valueType = getExpressionType(expr);
        if (!type.equals(valueType)) {
            error(unitNode, SemanticErrorType.TYPE_MISMATCH_ASSIGN, valueType, type);
        }
    }

    private void processOriginalFunction(Node unitNode, Node tipusNode, Node idNode, Node declTail) {
        if (mainDeclared) {
            error(unitNode, SemanticErrorType.FUNCTION_AFTER_MAIN);
        }
        enteredAnyFunction = true;
        String returnType = getTypeFromTipus(tipusNode);
        String name = idNode.getToken().getLexeme();
        if (symbolTable.containsKey(name)) {
            error(idNode, SemanticErrorType.FUNCTION_NAME_CONFLICT, name);
        }
        functionsDeclared.put(name, new FunctionSignature(returnType, new ArrayList<>()));

        currentFunctionReturnType = returnType;
        insideFunction = true;
        // Entrar a nou àmbit
        enterScope();
        symbolTableBona.addSymbol(name, returnType, currentScope(), idNode.getToken().getLine(), idNode.getToken().getColumn());

        Node rest = declTail.getChildren().get(1);
        Node bodyNode = rest.getChildren().get(0);
        traverse(bodyNode);
        // Sortir de l’àmbit
        exitScope();

        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void processOriginalMain(Node unitNode, Node tipusNode, Node tail) {
        if (mainDeclared) {
            error(unitNode, SemanticErrorType.MAIN_ALREADY_DEFINED);
        }
        mainDeclared = true;
        enteredAnyFunction = true;
        currentFunctionReturnType = getTypeFromTipus(tipusNode);
        insideFunction = true;

        enterScope();
        Node bodyNode = tail.getChildren().get(2);
        traverse(bodyNode);
        exitScope();

        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleFunction(Node node) {
        if (mainDeclared) {
            error(node, SemanticErrorType.FUNCTION_AFTER_MAIN);
        }
        enteredAnyFunction = true;
        String returnType = node.getChildren().get(0).getSymbol();
        String name = node.getChildren().get(1).getToken().getLexeme();
        if (symbolTable.containsKey(name)) {
            error(node, SemanticErrorType.FUNCTION_NAME_CONFLICT, name);
        }
        functionsDeclared.put(name, new FunctionSignature(returnType, new ArrayList<>()));

        currentFunctionReturnType = returnType;
        insideFunction = true;
        enterScope();
        symbolTableBona.addSymbol(name, returnType, currentScope(), node.getChildren().get(1).getToken().getLine(), node.getChildren().get(1).getToken().getColumn());

        for (Node child : node.getChildren()) traverse(child);
        exitScope();

        insideFunction = false;
        currentFunctionReturnType = null;
    }

    private void handleDeclaration(Node node) {
        if (enteredAnyFunction && !insideFunction) {
            error(node, SemanticErrorType.FUNCTION_AFTER_MAIN);
        }
        // Determinar tipus
        String type;
        if ("ARRAY".equals(node.getChildren().get(0).getSymbol())) {
            String size = node.getChildren().get(2).getToken().getLexeme();
            String baseType = node.getChildren().get(3).getSymbol();
            type = "ARRAY[" + size + "]" + baseType;
        } else {
            type = node.getChildren().get(0).getSymbol();
        }
        String name = node.getChildren().get(1).getToken().getLexeme();

        if (symbolTable.containsKey(name)) {
            error(node, SemanticErrorType.VARIABLE_ALREADY_DECLARED, name);
        }
        if (functionsDeclared.containsKey(name)) {
            error(node, SemanticErrorType.VARIABLE_NAME_CONFLICT, name);
        }

        // Afegir al taula de símbols interna
        symbolTable.put(name, type);

        // Afegir al SymbolTable "bona" amb la línia/columna de l'ID
        Token idTok = node.getChildren().get(1).getToken();
        symbolTableBona.addSymbol(
                name,
                type,
                currentScope(),
                idTok.getLine(),
                idTok.getColumn()
        );
    }

    private void handleMain(Node node) {
        if (mainDeclared) {
            error(node, SemanticErrorType.MAIN_ALREADY_DEFINED);
        }
        mainDeclared = true;
        enteredAnyFunction = true;
        // Agafem el tipus de retorn del MAIN
        currentFunctionReturnType = node.getChildren().get(0).getSymbol();
        insideFunction = true;

        // Entrar a un nou àmbit per al MAIN
        enterScope();
        // Recórrer tot el cos del main
        for (Node child : node.getChildren()) {
            traverse(child);
        }
        // Sortir de l’àmbit del MAIN
        exitScope();

        insideFunction = false;
        currentFunctionReturnType = null;
    }


    private void handleAssignment(Node node) {
        Node assignPrim = node.getChildren().get(0);
        if (assignPrim.getChildren().get(0).getSymbol().equals("POS")) {
            Node indexNode = assignPrim.getChildren().get(1);
            Node arrayIdNode = assignPrim.getChildren().get(3);
            String arrayName = arrayIdNode.getToken().getLexeme();
            if (!symbolTable.containsKey(arrayName)) {
                error(arrayIdNode, SemanticErrorType.VARIABLE_NOT_DECLARED, arrayName);
            }
            String type = symbolTable.get(arrayName);
            if (!type.startsWith("ARRAY")) {
                error(arrayIdNode, SemanticErrorType.NOT_AN_ARRAY, arrayName);
            }
            String indexType = getExpressionType(indexNode);
            if (!indexType.equals("INT")) {
                error(indexNode, SemanticErrorType.ARRAY_INDEX_TYPE, indexType);
            }
            String baseTypeRaw = type.substring(type.indexOf("]") + 1);
            String baseType = switch (baseTypeRaw) {
                case "INT_VALUE" -> "INT";
                case "FLOAT_VALUE" -> "FLOAT";
                case "CHAR_VALUE" -> "CHAR";
                default -> baseTypeRaw;
            };
            Node expr = node.getChildren().get(1);
            String valueType = getExpressionType(expr);
            if (!baseType.equals(valueType)) {
                error(node, SemanticErrorType.ARRAY_ASSIGN_TYPE, valueType, baseType);
            }
            return;
        }

        Node idNode = assignPrim.getChildren().get(0);
        String name = idNode.getToken().getLexeme();
        if (!symbolTable.containsKey(name)) {
            error(idNode, SemanticErrorType.VARIABLE_NOT_DECLARED, name);
        }
        String expectedType = symbolTable.get(name);
        Node expr = node.getChildren().get(1);
        String actualType = getExpressionType(expr);
        if (!expectedType.equals(actualType)) {
            error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN, actualType, expectedType);
        }
    }

    private void handleReturn(Node node) {
        if (!insideFunction) {
            error(node, SemanticErrorType.RETURN_OUTSIDE_FUNCTION);
        }
        if (node.getChildren().size() > 1) {
            String retType = getExpressionType(node.getChildren().get(1));
            if (currentFunctionReturnType != null && !retType.equals(currentFunctionReturnType)) {
                error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, retType);
            }
        }
    }

    private void handleFunctionCall(Node node) {
        Token funcToken = node.getChildren().get(0).getToken();
        String funcName = funcToken.getLexeme();
        FunctionSignature sig = functionsDeclared.get(funcName);
        if (sig == null) {
            error(node, SemanticErrorType.FUNCTION_NOT_DECLARED, funcName);
        }
        List<String> argTypes = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            if (child.getSymbol().equals("OPEN_PARENTESIS") || child.getSymbol().equals("CLOSE_PARENTESIS") || child.getSymbol().equals("LINE_DELIMITER"))
                continue;
            argTypes.add(getExpressionType(child));
        }
        if (argTypes.size() != sig.paramTypes.size()) {
            throw new RuntimeException("[Line " + funcToken.getLine() + "] Function '" + funcName + "' expects " + sig.paramTypes.size() + " arguments but got " + argTypes.size());
        }
        for (int i = 0; i < argTypes.size(); i++) {
            if (!argTypes.get(i).equals(sig.paramTypes.get(i))) {
                error(node, SemanticErrorType.FUNCTION_PARAMETER_TYPE, i + 1, funcName, sig.paramTypes.get(i), argTypes.get(i));
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
                    if (symbolTable.containsKey(lexeme)) return symbolTable.get(lexeme);
                    if (functionsDeclared.containsKey(lexeme)) return functionsDeclared.get(lexeme).returnType;
                    error(node, SemanticErrorType.UNKNOWN_SYMBOL, lexeme);
                }
            }
        } else if (!node.getChildren().isEmpty()) {
            if (node.getChildren().size() == 3 && isOperator(node.getChildren().get(1))) {
                String left = getExpressionType(node.getChildren().get(0));
                String right = getExpressionType(node.getChildren().get(2));
                if (!left.equals(right)) {
                    error(node, SemanticErrorType.EXPRESSION_TYPE_MISMATCH, left, right);
                }
                return left;
            } else {
                for (Node child : node.getChildren()) {
                    String type = getExpressionType(child);
                    if (!type.equals("UNKNOWN")) return type;
                }
            }
        }
        return "UNKNOWN";
    }

    private boolean isOperator(Node node) {
        String type = node.getSymbol();
        return type.equals("SUM") || type.equals("MINUS") || type.equals("MULTIPLY") || type.equals("DIVISION");
    }

    private void error(Node node, SemanticErrorType type, Object... args) {
        int line = -1;
        if (node.getToken() != null) {
            line = node.getToken().getLine();
        } else {
            for (Node child : node.getChildren()) {
                if (child.getToken() != null) {
                    line = child.getToken().getLine();
                    break;
                }
            }
        }
        throw new RuntimeException("[Line " + line + "] " + type.format(args));
    }
}

