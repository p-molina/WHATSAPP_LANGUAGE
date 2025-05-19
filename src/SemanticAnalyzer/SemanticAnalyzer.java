
package SemanticAnalyzer;

import entities.Node;
import entities.SymbolTable;
import entities.Token;
import entities.Symbol;

import java.util.*;

/**
 * Analitzador semàntic per a l’arbre sintàctic generat pel parser LL(1).
 * <p>
 * Realitza comprovacions de declaracions, àmbits, ús de variables i funcions,
 * validació de tipus en assignacions i retorns, i verifica la presència de main.
 * </p>
 */
public class SemanticAnalyzer {
    /** Arrel de l’arbre sintàctic sobre el qual s’aplica l’anàlisi. */
    private final Node root;
    /** Taula de símbols per gestionar variables i funcions amb els seus àmbits. */
    private final SymbolTable symbolTable;
    /** Pila d’identificadors d’àmbit (0 = global). */
    private final Deque<Integer> scopeStack = new ArrayDeque<>();
    /** Proper identificador d’àmbit a assignar. */
    private int nextScopeId = 1;
    /** Indica si actualment s’està dins d’una funció (per validar retorns). */
    private boolean insideFunction = false;
    /** Marca si ja s’ha declarat la funció main. */
    private boolean mainDeclared = false;
    /** Tipus de retorn esperat per a la funció en curs. */
    private String currentFunctionReturnType = null;

    /**
     * Crea un analitzador semàntic per a un arbre i una taula de símbols donada.
     *
     * @param root
     *   Node arrel de l’arbre sintàctic LL(1).
     * @param symbolTable
     *   Taula de símbols on registrar variables i funcions.
     */
    public SemanticAnalyzer(Node root, SymbolTable symbolTable) {
        this.root = root;
        this.symbolTable = symbolTable;
    }

    /**
     * Inicia l’anàlisi semàntic:
     * <ul>
     *   <li>Estableix l’àmbit global (0).</li>
     *   <li>Recorre l’arbre sintàctic validant cada node.</li>
     *   <li>Confirma que s’hagi declarat la funció main.</li>
     * </ul>
     *
     * @throws RuntimeException
     *   Si no es declara la funció main.
     */
    public void analyze() {
        scopeStack.push(0);
        traverse(root);
        if (!mainDeclared) throw new RuntimeException(SemanticErrorType.MISSING_MAIN.toString());
    }

    /**
     * Retorna l’identificador de l’àmbit actual (el top de la pila).
     *
     * @return
     *   Identificador numèric de l’àmbit actual.
     */
    private int currentScope() { return scopeStack.peek(); }

    /** Crea un nou àmbit per dins d’una funció o bloc. */
    private void enterScope() { scopeStack.push(nextScopeId++); }

    /** Tanca l’àmbit actual i torna al precedent. */
    private void exitScope() { scopeStack.pop(); }

    /**
     * Cerca un símbol a la taula de símbols dins l’àmbit actual.
     *
     * @param name
     *   Nom del símbol a cercar.
     * @return
     *   L’objecte {@link Symbol} si existeix, o {@code null} si no.
     */
    private Symbol getSymbol(String name) { return symbolTable.getSymbol(name, currentScope()); }

    /**
     * Recórrer l’arbre sintàctic i invocar el handler corresponent
     * segons el símbol del node.
     *
     * @param node
     *   Node actual a processar.
     */
    private void traverse(Node node) {
        if (node == null) return;

        // Obtenir el nom net del símbol (sense < >)
        String sym = node.getSymbol();
        if (sym.startsWith("<") && sym.endsWith(">")) sym = sym.substring(1, sym.length() - 1);

        // Seleccionar handler segons el tipus de node
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

    /**
     * Gestiona la declaració local (<DECLARACIO>),
     * declara la variable i valida la possible assignació inicial.
     *
     * @param node
     *   Node <DECLARACIO> amb fills [TIPUS, ID, (LOCAL_DECL_SUFFIX)].
     */
    private void handleLocalDeclaration(Node node) {
        // Extreure tipus i nom
        Node tipusNode = node.getChildren().get(0);
        Node idNode    = node.getChildren().get(1);
        String name    = idNode.getToken().getLexeme();
        String type    = getTypeFromTipus(tipusNode);

        // Comprovar re-declaració
        if (symbolTable.getScopeSymbols(currentScope()).containsKey(name)) {
            error(idNode, SemanticErrorType.VARIABLE_REDECLARED, name);
        }

        // Afegir la variable a la taula de símbols
        symbolTable.addSymbol(
                name,
                type,
                currentScope(),
                idNode.getToken().getLine(),
                idNode.getToken().getColumn()
        );

        // Si hi ha assignació, validar tipus
        if (node.getChildren().size() > 2) {
            Node suffix = node.getChildren().get(2);
            if (!suffix.getChildren().isEmpty()) {
                Node firstSuffixChild = suffix.getChildren().get(0);
                Token tok = firstSuffixChild.getToken();
                if (tok != null && "EQUAL_ASSIGNATION".equals(tok.getType())) {
                    Node expr = suffix.getChildren().get(1);
                    String actual = getExpressionType(expr);
                    if (!type.equals(actual)) {
                        error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN, actual, type);
                    }
                }
            }
        }
    }

    /**
     * Gestiona el contingut (<CONTENT>) d’un bloc de sentències,
     * triant l’acció segons el primer fill.
     *
     * @param node
     *   Node <CONTENT> amb diverses possibilitats.
     */
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

    /**
     * Gestiona el cas d’un identificador en <CONTENT>,
     * pot ser assignació o crida a funció.
     *
     * @param node
     *   Node <CONTENT> amb ID com a primer fill.
     * @param first
     *   Node que representa l’ID.
     */
    private void handleIDContent(Node node, Node first) {
        Node tail = node.getChildren().get(1);
        Token tailTok = tail.getChildren().get(0).getToken();
        String name = first.getToken().getLexeme();
        Symbol sym = getSymbol(name);

        if (tailTok != null && "EQUAL_ASSIGNATION".equals(tailTok.getType())) {
            // Validar assignació a la variable
            if (sym == null) error(first, SemanticErrorType.VARIABLE_NOT_DECLARED, name);
            String expected = sym.getType();
            String actual = getExpressionType(tail.getChildren().get(1));
            if (!expected.equals(actual)) error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN, actual, expected);
        } else if (tailTok != null && "OPEN_PARENTESIS".equals(tailTok.getType())) {
            // Crida a funció
            handleFunctionCall(node);
        } else {
            traverseChildren(node);
        }
    }

    /**
     * Gestiona l’assignació a un element d’un array (<POS>).
     *
     * @param node
     *   Node amb la sintaxi de POS (array index i assignació).
     */
    private void handleArrayAssignment(Node node) {
        Node idx = node.getChildren().get(1);
        Node arrId = node.getChildren().get(3);
        String arrName = arrId.getToken().getLexeme();
        Symbol sym = getSymbol(arrName);

        if (sym == null) error(arrId, SemanticErrorType.VARIABLE_NOT_DECLARED, arrName);

        String t = sym.getType();
        if (!t.startsWith("ARRAY")) error(arrId, SemanticErrorType.NOT_AN_ARRAY, arrName);

        // Validar tipus de l’índex
        String idxType = getExpressionType(idx);
        if (!"INT".equals(idxType)) error(idx, SemanticErrorType.ARRAY_INDEX_TYPE, idxType);

        // Validar tipus del valor assignat
        String base = t.substring(t.indexOf("]") + 1).replace("_VALUE", "");
        String valType = getExpressionType(node.getChildren().get(5));
        if (!base.equals(valType)) error(node, SemanticErrorType.ARRAY_ASSIGN_TYPE, valType, base);
    }

    /**
     * Gestiona l’instrucció de retorn (<XINPUM>) dins d’una funció.
     *
     * @param node
     *   Node <XINPUM> amb l’expressió de retorn.
     * @param first
     *   Primer fill del node, que conté el token RETURN.
     */
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

    /** Recórrer tots els fills d’un node amb {@link #traverse(Node)}. */
    private void traverseChildren(Node node) { node.getChildren().forEach(this::traverse); }

    /**
     * Gestiona el node <UNIT>, que pot ser declaració global,
     * definició de funció o definició de main.
     *
     * @param unitNode
     *   Node <UNIT> amb fills [TIPUS, UNIT_TAIL].
     */
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

    /**
     * Gestiona una declaració global amb assignació (<UNIT>).
     *
     * @param unitNode
     *   Node <UNIT> original.
     * @param tipusNode
     *   Node TIPUS del tipus de retorn.
     * @param idNode
     *   Node ID amb el nom de la variable.
     * @param declTail
     *   Node DECL_OR_FUNC_TAIL amb possible expressió.
     */
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

    /**
     * Gestiona la definició d’una funció no-main (<UNIT>).
     *
     * @param tipusNode
     *   Node TIPUS del tipus de retorn.
     * @param idNode
     *   Node ID amb el nom de la funció.
     * @param declTail
     *   Node DECL_OR_FUNC_TAIL que conté el cos de la funció.
     */
    private void handleFunctionUnit(Node tipusNode, Node idNode, Node declTail) {
        String name = idNode.getToken().getLexeme();
        String returnType = getTypeFromTipus(tipusNode);

        // Iniciar àmbit de funció
        currentFunctionReturnType = returnType;
        insideFunction = true;

        enterScope();

        if (getSymbol(name) != null) error(idNode, SemanticErrorType.FUNCTION_REDECLARED, name);

        symbolTable.addSymbol(name, returnType, currentScope(),
                                    idNode.getToken().getLine(), idNode.getToken().getColumn());

        // Recórrer el cos de la funció
        traverse(declTail.getChildren().get(1).getChildren().get(0));

        // Tancar àmbit de funció
        exitScope();
        insideFunction = false;
        currentFunctionReturnType = null;
    }

    /**
     * Gestiona la definició de la funció main (<UNIT>).
     *
     * @param tipusNode
     *   Node TIPUS del tipus de retorn (normalment VOID).
     * @param tail
     *   Node UNIT_TAIL amb el nom i el cos.
     */
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

    /**
     * Gestiona la definició de funció (<CREA_FUNCIO>):
     * declara la funció a la taula de símbols, obre un nou àmbit
     * i recorre el cos de la funció.
     *
     * @param node
     *   Node <CREA_FUNCIO> que conté fills [TIPUS, ID, ...] de la funció.
     */
    private void handleFunction(Node node) {
        // Extreure nom i tipus de retorn
        String name = node.getChildren().get(1).getToken().getLexeme();
        String returnType = node.getChildren().get(0).getSymbol();

        // Marcar que estem dins d’una funció
        currentFunctionReturnType = returnType;
        insideFunction = true;

        // Obrir nou àmbit de funció
        enterScope();

        // Afegir funció a la taula de símbols
        symbolTable.addSymbol(name, returnType, currentScope(),
                node.getChildren().get(1).getToken().getLine(), node.getChildren().get(1).getToken().getColumn());

        // Analitzar el cos de la funció
        node.getChildren().forEach(this::traverse);

        // Tancar àmbit de funció
        exitScope();
        insideFunction = false;
        currentFunctionReturnType = null;
    }

    /**
     * Gestiona la definició de la funció main (<CREA_MAIN>):
     * marca la declaració de main i analitza el seu cos.
     *
     * @param node
     *   Node <CREA_MAIN> que conté fills [TIPUS, ID, ..., cos_de_main].
     */
    private void handleMain(Node node) {
        // Extreure nom i tipus de retorn
        String name = node.getChildren().get(1).getToken().getLexeme();
        String returnType = node.getChildren().get(0).getSymbol();
        currentFunctionReturnType = node.getChildren().get(0).getSymbol();
        insideFunction = true;
        mainDeclared = true;

        // Obrir nou àmbit de main
        enterScope();

        // Afegir main a la taula de símbols
        symbolTable.addSymbol(name, returnType, currentScope(),
                node.getChildren().get(1).getToken().getLine(), node.getChildren().get(1).getToken().getColumn());

        // Analitzar el cos de main
        node.getChildren().forEach(this::traverse);

        // Tancar àmbit de main
        exitScope();
        insideFunction = false;
        currentFunctionReturnType = null;
    }

    /**
     * Gestiona una declaració simple (<DECLARACIO>):
     * declara la variable en l’àmbit actual sense assignació.
     *
     * @param node
     *   Node <DECLARACIO> amb fills [TIPUS, ID].
     */
    private void handleDeclaration(Node node) {
        // Extreure tipus i nom de la variable
        Node tipusNode = node.getChildren().get(0);
        Node idNode = node.getChildren().get(1);
        String name = idNode.getToken().getLexeme();
        String type = getTypeFromTipus(tipusNode);

        // Comprovar re-declaració
        if (symbolTable.getScopeSymbols(currentScope()).containsKey(name)) {
            error(idNode, SemanticErrorType.VARIABLE_REDECLARED, name);
        }

        // Afegir variable a la taula de símbols
        symbolTable.addSymbol(
                name,
                type,
                currentScope(),
                idNode.getToken().getLine(),
                idNode.getToken().getColumn());
    }

    /**
     * Gestiona una assignació simple (<ASSIGNACIO>).
     *
     * @param node
     *   Node <ASSIGNACIO> amb l’identificador i l’expressió.
     */
    private void handleAssignment(Node node) {
        String name = node.getChildren().get(0).getChildren().get(0).getToken().getLexeme();
        Symbol sym = getSymbol(name);
        if (sym == null) error(node, SemanticErrorType.VARIABLE_NOT_DECLARED, name);

        String expected = sym.getType();
        String actual = getExpressionType(node.getChildren().get(1));
        if (!expected.equals(actual)) error(node, SemanticErrorType.TYPE_MISMATCH_ASSIGN, actual, expected);
    }

    /**
     * Gestiona l’instrucció de retorn (<XINPUM>).
     *
     * @param node
     *   Node <XINPUM> amb l’expressió de retorn.
     */
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
                // En qualsevol altre cas (literal, expressió composta…), seguim utilitzant getExpressionType
                String retType = getExpressionType(expr);
                if (!retType.equals(currentFunctionReturnType)) {
                    error(node, SemanticErrorType.RETURN_TYPE_MISMATCH, currentFunctionReturnType, retType);
                }
            }
        }
    }

    /**
     * Gestiona la crida a funció (<CALL_FUNCIO>):
     * comprova que la funció existeixi prèviament declarada.
     *
     * @param node
     *   Node <CALL_FUNCIO> amb el nom i arguments de la crida.
     */
    private void handleFunctionCall(Node node) {
        // Extreure nom de la funció cridada
        String funcName = node.getChildren().get(0).getToken().getLexeme();
        Symbol sym = getSymbol(funcName);
        // Comprovar existència a la taula de símbols
        if (sym == null) error(node, SemanticErrorType.FUNCTION_NOT_DECLARED, funcName);
    }

    /**
     * Determina el tipus associat a un node <TIPUS>,
     * incloent arrays (ARRAY[n]TIPUS_BASE).
     *
     * @param tipusNode
     *   Node <TIPUS> amb estructures per a arrays o tipus simples.
     * @return
     *   Cadena representant el tipus (p. ex. "INT", "ARRAY[10]FLOAT").
     */
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

    /**
     * Calcula el tipus d’una expressió recursivament.
     *
     * @param node
     *   Node que representa l’expressió.
     * @return
     *   Cadena amb el tipus (p. ex. "INT", "FLOAT", "CHAR") o "UNKNOWN".
     */

    private String getExpressionType(Node node) {
        String sym = node.getSymbol();

        if (sym.startsWith("<") && sym.endsWith(">")) {
            sym = sym.substring(1, sym.length() - 1);
        }

        // Cas específic de TIPUS
        if ("TIPUS".equals(sym)) {return getTypeFromTipus(node);}

        // Si és un literal o ID
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
        // Cas d’operació binària
        if (node.getChildren().size() == 3 && isOperator(node.getChildren().get(1))) {
            String left  = getExpressionType(node.getChildren().get(0));
            String right = getExpressionType(node.getChildren().get(2));
            if (!left.equals(right)) {
                error(node, SemanticErrorType.EXPRESSION_TYPE_MISMATCH, left, right);
            }
            return left;
        }
        // Recorregut profund per trobar un tipus conegut
        for (Node child : node.getChildren()) {
            String t = getExpressionType(child);
            if (!"UNKNOWN".equals(t)) {
                return t;
            }
        }
        return "UNKNOWN";
    }

    /**
     * Comprova si el node representa un operador aritmètic.
     *
     * @param node
     *   Node a avaluar.
     * @return
     *   {@code true} si és SUM, MINUS, MULTIPLY o DIVISION.
     */
    private boolean isOperator(Node node) {
        return switch (node.getSymbol()) {
            case "SUM", "MINUS", "MULTIPLY", "DIVISION" -> true;
            default -> false;
        };
    }

    /**
     * Llança una excepció semàntica amb missatge formatat i número de línia.
     *
     * @param node
     *   Node on ha ocorregut l’error (s’usa per obtenir la línia).
     * @param type
     *   Tipus d’error definit a {@link SemanticErrorType}.
     * @param args
     *   Arguments per al missatge d’error.
     * @throws RuntimeException
     *   Excepció amb missatge formatat.
     */
    private void error(Node node, SemanticErrorType type, Object... args) {
        int line = -1;
        // Cercar línia al token o a qualsevol fill
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
