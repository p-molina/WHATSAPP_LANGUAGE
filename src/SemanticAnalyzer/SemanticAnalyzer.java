package SemanticAnalyzer;

import entities.Node;
import entities.Token;
import java.util.*;

public class SemanticAnalyzer {
    private final Node root;
    private final Map<String, String> symbolTable = new HashMap<>();

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
        traverse(root);
        if (!mainDeclared) {
            throw new RuntimeException("Semantic Error: Missing main function 'xat'");
        }
    }

    private void traverse(Node node) {
        if (node == null) return;

        // Normalize symbol
        String sym = node.getSymbol();
        if (sym.startsWith("<") && sym.endsWith(">")) {
            sym = sym.substring(1, sym.length()-1);
        }
        System.out.println("Visiting node: " + sym);

        boolean traverseChildren = true;
        switch (sym) {
            case "UNIT":
                handleUnit(node);
                return;
            case "DECLARACIO":
                handleDeclaration(node);
                break;
            case "CREA_FUNCIO":
                handleFunction(node);
                traverseChildren = false;
                break;
            case "CREA_MAIN":
                handleMain(node);
                traverseChildren = false;
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
            case "CONTENT":
                handleContent(node);
                return;
            default:
                break;
        }
        if (traverseChildren) {
            for (Node c: node.getChildren()) traverse(c);
        }
    }

    private void handleContent(Node node) {
        Node first = node.getChildren().get(0);
        Token tok = first.getToken();
        if (tok == null) {
            // no terminal, dive in
            traverseChildren(node);
            return;
        }
        switch(tok.getType()) {
            case "ID":
                Node idNode = first;
                Node tail = node.getChildren().get(1);
                Token tailTok = tail.getChildren().get(0).getToken();
                if (tailTok != null && "EQUAL_ASSIGNATION".equals(tailTok.getType())) {
                    // simple assignment
                    String name = idNode.getToken().getLexeme();
                    if (!symbolTable.containsKey(name)) {
                        error(idNode, "Variable '" + name + "' not declared.");
                    }
                    String expected = symbolTable.get(name);
                    Node expr = tail.getChildren().get(1);
                    String actual = getExpressionType(expr);
                    if (!expected.equals(actual)) {
                        error(node, "Type mismatch: cannot assign '" + actual + "' to '" + expected + "'.");
                    }
                } else if (tailTok != null && "OPEN_PARENTESIS".equals(tailTok.getType())) {
                    // function call
                    handleFunctionCall(node);
                } else {
                    traverseChildren(node);
                }
                break;
            case "POS":
                // array assignment
                Node idx = node.getChildren().get(1);
                Node arrId = node.getChildren().get(3);
                String arrName = arrId.getToken().getLexeme();
                if (!symbolTable.containsKey(arrName)) {
                    error(arrId, "Array '" + arrName + "' not declared.");
                }
                String t = symbolTable.get(arrName);
                if (!t.startsWith("ARRAY")) {
                    error(arrId, "'" + arrName + "' is not an array.");
                }
                String idxType = getExpressionType(idx);
                if (!"INT".equals(idxType)) {
                    error(idx, "Array index must be of type 'INT', but got '" + idxType + "'");
                }
                String base = t.substring(t.indexOf("]")+1);
                if (base.endsWith("VALUE")) base = base.replace("_VALUE","");
                Node valExpr = node.getChildren().get(5);
                String valType = getExpressionType(valExpr);
                if (!base.equals(valType)) {
                    error(node, "Type mismatch: cannot assign '" + valType + "' to array of '" + base + "'");
                }
                break;
            case "RETURN":
                // return in original grammar
                Node expr = node.getChildren().get(1);
                if (!insideFunction) {
                    error(first, "'xinpum' (return) statement is only allowed inside a function.");
                }
                String rt = getExpressionType(expr);
                if (currentFunctionReturnType != null && !rt.equals(currentFunctionReturnType)) {
                    error(node, "Return type mismatch: expected '"+currentFunctionReturnType+"', got '"+rt+"'");
                }
                break;
            default:
                traverseChildren(node);
        }
    }
    private void traverseChildren(Node node) {
        for (Node c: node.getChildren()) traverse(c);
    }

    /**
     * Adapts to original grammar UNIT nodes: distinguishes decl, func or main
     */
    private void handleUnit(Node unitNode) {
        Node tipusNode = unitNode.getChildren().get(0);
        Node tail = unitNode.getChildren().get(1);
        Node first = tail.getChildren().get(0);

        // main: <TIPUS> MAIN OPEN_CLAUDATOR <BODY> CLOSE_CLAUDATOR
        if (first.getToken() != null && "MAIN".equals(first.getToken().getType())) {
            processOriginalMain(unitNode, tipusNode, tail);
        }
        // decl or func: <TIPUS> ID <DECL_OR_FUNC_TAIL>
        else if (first.getToken() != null && "ID".equals(first.getToken().getType())) {
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

    /**
     * Extracts a type string from a <TIPUS> node (handles arrays too)
     */
    private String getTypeFromTipus(Node tipusNode) {
        Node first = tipusNode.getChildren().get(0);
        if ("ARRAY".equals(first.getSymbol())) {
            String size = tipusNode.getChildren().get(2).getToken().getLexeme();
            Node baseNode = tipusNode.getChildren().get(3);
            // baseNode is <TIPUS_BASE>
            String baseType = baseNode.getChildren().get(0).getSymbol();
            return "ARRAY[" + size + "]" + baseType;
        } else {
            // single base type
            Node baseNode = first.getChildren().get(0);
            return baseNode.getSymbol();
        }
    }

    private void processOriginalDeclaration(Node unitNode, Node tipusNode, Node idNode, Node declTail) {
        if (enteredAnyFunction && !insideFunction) {
            error(unitNode, "Global declarations are not allowed after a function or main definition.");
        }
        String type = getTypeFromTipus(tipusNode);
        String name = idNode.getToken().getLexeme();
        System.out.println("Declaring variable: " + name + " of type " + type);
        if (symbolTable.containsKey(name)) {
            error(idNode, "Variable '" + name + "' already declared.");
        }
        if (functionsDeclared.containsKey(name)) {
            error(idNode, "Variable '" + name + "' cannot have the same name as a declared function.");
        }
        symbolTable.put(name, type);
        // check assigned expression
        Node expr = declTail.getChildren().get(1);
        String valueType = getExpressionType(expr);
        if (!type.equals(valueType)) {
            error(unitNode, "Type mismatch: cannot assign '" + valueType + "' to '" + type + "'.");
        }
    }

    private void processOriginalFunction(Node unitNode, Node tipusNode, Node idNode, Node declTail) {
        if (mainDeclared) {
            error(unitNode, "No functions are allowed after the main function 'xat'.");
        }
        enteredAnyFunction = true;
        String returnType = getTypeFromTipus(tipusNode);
        String name = idNode.getToken().getLexeme();
        if (symbolTable.containsKey(name)) {
            error(idNode, "Function '" + name + "' cannot have the same name as a declared variable.");
        }
        functionsDeclared.put(name, new FunctionSignature(returnType, new ArrayList<>()));
        currentFunctionReturnType = returnType;
        System.out.println("Entering function '" + name + "'...");
        insideFunction = true;
        // declTail: [OPEN_CLAUDATOR, <DECL_OR_FUNC_TAIL_REST>]
        Node rest = declTail.getChildren().get(1);
        // rest: [<BODY>, CLOSE_CLAUDATOR]
        Node bodyNode = rest.getChildren().get(0);
        traverse(bodyNode);
        insideFunction = false;
        currentFunctionReturnType = null;
        System.out.println("Exiting function '" + name + "'.");
    }

    private void processOriginalMain(Node unitNode, Node tipusNode, Node tail) {
        if (mainDeclared) {
            error(unitNode, "Main function 'xat' already defined.");
        }
        System.out.println("Processing main function 'xat'...");
        mainDeclared = true;
        enteredAnyFunction = true;
        currentFunctionReturnType = getTypeFromTipus(tipusNode);
        insideFunction = true;
        // tail: [MAIN, OPEN_CLAUDATOR, <BODY>, CLOSE_CLAUDATOR]
        Node bodyNode = tail.getChildren().get(2);
        traverse(bodyNode);
        insideFunction = false;
        currentFunctionReturnType = null;
        System.out.println("Finished processing 'xat'.");
    }

    // Resta de mètodes existents (handleDeclaration, handleFunction, handleMain, handleAssignment,
    // handleReturn, handleFunctionCall, getExpressionType, isOperator, error) es mantenen igual.



private void handleDeclaration(Node node) {
        if (enteredAnyFunction && !insideFunction) {
            error(node, "Global declarations are not allowed after a function or main definition.");
        }

        String type;
        if (node.getChildren().get(0).getSymbol().equals("ARRAY")) {
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

        if (functionsDeclared.containsKey(name)) {
            error(node, "Variable '" + name + "' cannot have the same name as a declared function.");
        }

        symbolTable.put(name, type);
    }

    private void handleFunction(Node node) {
        if (mainDeclared) {
            error(node, "No functions are allowed after the main function 'xat'.");
        }

        enteredAnyFunction = true;

        String name = node.getChildren().get(1).getToken().getLexeme();
        if (symbolTable.containsKey(name)) {
            error(node, "Function '" + name + "' cannot have the same name as a declared variable.");
        }

        String returnType = node.getChildren().get(0).getSymbol();
        List<String> paramTypes = new ArrayList<>(); // Si més endavant afegeixes paràmetres
        functionsDeclared.put(name, new FunctionSignature(returnType, paramTypes));

        currentFunctionReturnType = returnType;

        System.out.println("Entering function '" + name + "'...");
        insideFunction = true;
        for (Node child : node.getChildren()) {
            traverse(child);
        }
        insideFunction = false;
        currentFunctionReturnType = null;
        System.out.println("Exiting function '" + name + "'.");
    }

    private void handleMain(Node node) {
        System.out.println("Processing main function 'xat'...");

        if (mainDeclared) {
            error(node, "Main function 'xat' already defined.");
        }

        mainDeclared = true;
        enteredAnyFunction = true;
        currentFunctionReturnType = node.getChildren().get(0).getSymbol();

        insideFunction = true;
        for (Node child : node.getChildren()) {
            traverse(child);
        }
        insideFunction = false;
        currentFunctionReturnType = null;

        System.out.println("Finished processing 'xat'.");
    }

    private void handleAssignment(Node node) {
        Node assignPrim = node.getChildren().get(0);

        // 👇 Comprovació especial per a assignació a una posició d'array
        if (assignPrim.getChildren().get(0).getSymbol().equals("POS")) {
            Node indexNode = assignPrim.getChildren().get(1);     // INT_VALUE o ID
            Node arrayIdNode = assignPrim.getChildren().get(3);   // ID del nom de l'array

            String arrayName = arrayIdNode.getToken().getLexeme();
            if (!symbolTable.containsKey(arrayName)) {
                error(arrayIdNode, "Array '" + arrayName + "' not declared.");
            }

            String type = symbolTable.get(arrayName);
            if (!type.startsWith("ARRAY")) {
                error(arrayIdNode, "'" + arrayName + "' is not an array.");
            }

            // ✅ Nova comprovació: tipus de l’índex ha de ser INT
            String indexType = getExpressionType(indexNode);
            if (!indexType.equals("INT")) {
                error(indexNode, "Array index must be of type 'INT', but got '" + indexType + "'");
            }

            String baseTypeRaw = type.substring(type.indexOf("]") + 1);
            String baseType = switch (baseTypeRaw) {
                case "INT_VALUE" -> "INT";
                case "FLOAT_VALUE" -> "FLOAT";
                case "CHAR_VALUE" -> "CHAR";
                default -> baseTypeRaw;
            };

            Node expr = node.getChildren().get(1); // valor a assignar
            String valueType = getExpressionType(expr);

            System.out.println("Assigning to array '" + arrayName + "' of base type " + baseType + ", value type: " + valueType);

            if (!baseType.equals(valueType)) {
                error(node, "Type mismatch: cannot assign '" + valueType + "' to array of '" + baseType + "'");
            }

            return;
        }

        // 👇 Assignació normal a variable
        Node idNode = assignPrim.getChildren().get(0);
        String name = idNode.getToken().getLexeme();
        System.out.println("Analyzing assignment to variable: " + name);

        if (!symbolTable.containsKey(name)) {
            error(idNode, "Variable '" + name + "' not declared.");
        }

        String expectedType = symbolTable.get(name);
        Node expr = node.getChildren().get(1);
        String actualType = getExpressionType(expr);

        System.out.println("Expected type: " + expectedType + ", Actual type: " + actualType);

        if (!expectedType.equals(actualType)) {
            error(node, "Type mismatch: cannot assign '" + actualType + "' to '" + expectedType + "'.");
        }
    }



    private void handleReturn(Node node) {
        if (!insideFunction) {
            error(node, "'xinpum' (return) statement is only allowed inside a function.");
        }

        System.out.println("Checking return statement...");
        if (node.getChildren().size() > 1) {
            String retType = getExpressionType(node.getChildren().get(1));
            System.out.println("Returned value type: " + retType);

            if (currentFunctionReturnType != null && !retType.equals(currentFunctionReturnType)) {
                error(node, "Return type mismatch: expected '" + currentFunctionReturnType + "', got '" + retType + "'");
            }
        }
    }

    private void handleFunctionCall(Node node) {
        Token funcToken = node.getChildren().get(0).getToken();
        String funcName = funcToken.getLexeme();

        System.out.println("Calling function: " + funcName);

        FunctionSignature sig = functionsDeclared.get(funcName);
        if (sig == null) {
            error(node, "Function '" + funcName + "' not declared.");
        }

        List<String> argTypes = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            if (child.getSymbol().equals("OPEN_PARENTESIS") || child.getSymbol().equals("CLOSE_PARENTESIS") || child.getSymbol().equals("LINE_DELIMITER"))
                continue;
            argTypes.add(getExpressionType(child));
        }

        if (argTypes.size() != sig.paramTypes.size()) {
            error(node, "Function '" + funcName + "' expects " + sig.paramTypes.size() + " arguments but got " + argTypes.size());
        }

        for (int i = 0; i < argTypes.size(); i++) {
            if (!argTypes.get(i).equals(sig.paramTypes.get(i))) {
                error(node, "Parameter " + (i + 1) + " of function '" + funcName + "' expected type '" + sig.paramTypes.get(i) + "', got '" + argTypes.get(i) + "'");
            }
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
                    if (symbolTable.containsKey(name)) {
                        return symbolTable.get(name);
                    } else if (functionsDeclared.containsKey(name)) {
                        // Assume it's a function call with no parameters
                        return functionsDeclared.get(name).returnType;
                    } else {
                        error(node, "Variable or function '" + name + "' not declared.");
                    }
            }
        } else if (!node.getChildren().isEmpty()) {
            if (node.getChildren().size() == 3 && isOperator(node.getChildren().get(1))) {
                String op = node.getChildren().get(1).getSymbol();
                if (op.equals("DIVISION")) {
                    Node divisor = node.getChildren().get(2);
                    if (divisor.getToken() != null) {
                        String val = divisor.getToken().getLexeme();
                        String type = divisor.getToken().getType();

                        // Cas 1: literal 0 o 0.0
                        if (val.equals("0") || val.equals("0.0")) {
                            error(divisor, "Division by zero.");
                        }

                        // Cas 2: variable ID
                        if (type.equals("ID")) {
                            String name = val;
                            if (!symbolTable.containsKey(name)) {
                                error(divisor, "Variable '" + name + "' not declared.");
                            }

                            String declaredType = symbolTable.get(name);
                            if (!declaredType.equals("INT") && !declaredType.equals("FLOAT")) {
                                error(divisor, "Division by non-numeric type '" + declaredType + "'.");
                            }

                            // 🔸 Si vols detectar que és zero literalment aquí (ex. x = 0; y = 5 / x;)
                            // hauries de portar un map de valors assignats (no implementat aquí).
                        }
                    }
                }

                String left = getExpressionType(node.getChildren().get(0));
                String right = getExpressionType(node.getChildren().get(2));
                if (!left.equals(right)) {
                    error(node, "Expression types mismatch: '" + left + "' and '" + right + "'.");
                }
                return left;
            } else {
                for (Node child : node.getChildren()) {
                    String type = getExpressionType(child);
                    if (!type.equals("UNKNOWN")) return type;
                }            }
        }
        return "UNKNOWN";
    }

    private boolean isOperator(Node node) {
        String type = node.getSymbol();
        return type.equals("SUM") || type.equals("MINUS") ||
                type.equals("MULTIPLY") || type.equals("DIVISION");
    }

    private void error(Node node, String message) {
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
        throw new RuntimeException("[Line " + line + "] Semantic Error: " + message);
    }
}
