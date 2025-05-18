package SemanticAnalyzer;

/**
 * Tipus d'errors semàntics emprats durant l'anàlisi semàntic.
 * Cada constant defineix un patró de missatge amb possibles placeholders
 * per a ser formatats amb {@link #format(Object...)}.
 */
public enum SemanticErrorType {
    /**
     * Error quan s'intenta usar una variable que no ha estat declarada.
     * Usage: VARIABLE_NOT_DECLARED.format("varName")
     */
    VARIABLE_NOT_DECLARED("Variable '%s' no declarada."),

    /**
     * Error quan una variable ja ha estat declarada amb el mateix nom.
     * Usage: VARIABLE_ALREADY_DECLARED.format("varName")
     */
    VARIABLE_ALREADY_DECLARED("Variable '%s' ja declarada."),

    /**
     * Error de tipus en assignació: el valor a assignar no concorda amb el tipus.
     * Usage: TYPE_MISMATCH_ASSIGN.format(actualType, expectedType)
     */
    TYPE_MISMATCH_ASSIGN("Error de tipus: no es pot assignar '%s' a '%s'."),

    /**
     * Error quan el tipus de retorn d'una funció difereix del que s'esperava.
     * Usage: RETURN_TYPE_MISMATCH.format(expectedType, actualType)
     */
    RETURN_TYPE_MISMATCH("Tipus de retorn incorrecte: s'esperava '%s', s'ha obtingut '%s'."),

    /**
     * Error quan s'intenta tractar un símbol que no és un array com un array.
     * Usage: NOT_AN_ARRAY.format(symbolName)
     */
    NOT_AN_ARRAY("'%s' no és un array."),

    /**
     * Error quan l'índex d'un array no és de tipus INT.
     * Usage: ARRAY_INDEX_TYPE.format(actualIndexType)
     */
    ARRAY_INDEX_TYPE("L'índex de l'array ha de ser de tipus 'INT', però s'ha obtingut '%s'."),

    /**
     * Error de tipus en assignació a un array: el tipus a assignar no concorda.
     * Usage: ARRAY_ASSIGN_TYPE.format(valueType, arrayBaseType)
     */
    ARRAY_ASSIGN_TYPE("Error de tipus: no es pot assignar '%s' a un array de '%s'."),

    /**
     * Error quan s'intenta definir més d'una vegada la funció main.
     */
    MAIN_ALREADY_DEFINED("La funció main 'xat' ja està definida."),

    /**
     * Error quan falta la definició de la funció main.
     */
    MISSING_MAIN("Error semàntic: manca la funció main 'xat'."),

    /**
     * Error quan es declaren funcions després de la funció main.
     */
    FUNCTION_AFTER_MAIN("No es poden declarar funcions després de la funció main 'xat'."),

    /**
     * Error quan una funció té el mateix nom que una variable existent.
     * Usage: FUNCTION_NAME_CONFLICT.format(name)
     */
    FUNCTION_NAME_CONFLICT("La funció '%s' no pot tenir el mateix nom que una variable."),

    /**
     * Error quan una variable té el mateix nom que una funció existent.
     * Usage: VARIABLE_NAME_CONFLICT.format(name)
     */
    VARIABLE_NAME_CONFLICT("La variable '%s' no pot tenir el mateix nom que una funció."),

    /**
     * Error quan s'intenta cridar una funció no declarada.
     * Usage: FUNCTION_NOT_DECLARED.format(functionName)
     */
    FUNCTION_NOT_DECLARED("Funció '%s' no declarada."),

    /**
     * Error de tipus en paràmetres de funció.
     * Usage: FUNCTION_PARAMETER_TYPE.format(paramIndex, functionName, expectedType, actualType)
     */
    FUNCTION_PARAMETER_TYPE("El paràmetre %d de la funció '%s' esperava tipus '%s', però s'ha obtingut '%s'."),

    /**
     * Error quan s'usa 'xinpum' (return) fora d'una funció.
     */
    RETURN_OUTSIDE_FUNCTION("'xinpum' (retorn) només és vàlid dins d'una funció."),

    /**
     * Error quan un símbol ja està declarat en el mateix àmbit.
     * Usage: SYMBOL_REDECLARED_IN_SCOPE.format(symbolName, scopeId)
     */
    SYMBOL_REDECLARED_IN_SCOPE("El símbol '%s' ja està declarat en l’àmbit %d."),

    /**
     * Error quan es fa referència a un símbol desconegut (variable o funció).
     * Usage: UNKNOWN_SYMBOL.format(name)
     */
    UNKNOWN_SYMBOL("Variable o funció '%s' no declarada."),

    /**
     * Error quan els tipus d'una expressió no coincideixen.
     * Usage: EXPRESSION_TYPE_MISMATCH.format(type1, type2)
     */
    EXPRESSION_TYPE_MISMATCH("Tipus d'expressió incompatibles: '%s' i '%s'."),

    /**
     * Error quan una funció ja ha estat declarada amb el mateix nom.
     * Usage: FUNCTION_REDECLARED.format(functionName)
     */
    FUNCTION_REDECLARED("Funció '%s' ja declarada."),

    /**
     * Error quan una variable ja ha estat redeclarada en el mateix àmbit.
     * Usage: VARIABLE_REDECLARED.format(variableName)
     */
    VARIABLE_REDECLARED("Variable '%s' ja declarada.");


    /** Text base del missatge d'error amb placeholders. */
    private final String message;

    /**
     * Crea un nou tipus d'error semàntic amb el patró de missatge especificat.
     *
     * @param message
     *   Patró de missatge amb placeholders per a {@link String#format}.
     */
    SemanticErrorType(String message) {
        this.message = message;
    }

    /**
     * Retorna el missatge d'error formatat amb els arguments donats.
     *
     * @param args
     *   Arguments que substitueixen els placeholders del missatge.
     * @return
     *   Missatge d'error complet, amb els placeholders substituïts.
     */
    public String format(Object... args) {
        return String.format(message, args);
    }

    @Override
    public String toString() {
        return message;
    }
}