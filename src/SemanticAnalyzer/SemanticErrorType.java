package SemanticAnalyzer;

public enum SemanticErrorType {
    VARIABLE_NOT_DECLARED("Variable '%s' not declared."),
    VARIABLE_ALREADY_DECLARED("Variable '%s' already declared."),
    //TYPE_MISMATCH_ASSIGN("Type mismatch: cannot assign '%s' to '%s'."),
    //RETURN_TYPE_MISMATCH("Return type mismatch: expected '%s', got '%s'."),
    NOT_AN_ARRAY("'%s' is not an array."),
    ARRAY_INDEX_TYPE("Array index must be of type 'INT', but got '%s'."),
    ARRAY_ASSIGN_TYPE("Type mismatch: cannot assign '%s' to array of '%s'."),
    MAIN_ALREADY_DEFINED("Main function 'xat' already defined."),
    MISSING_MAIN("Semantic Error: Missing main function 'xat'."),
    FUNCTION_AFTER_MAIN("Functions cannot be declared after main function 'xat'."),
    FUNCTION_NAME_CONFLICT("Function '%s' cannot have same name as a variable."),
    VARIABLE_NAME_CONFLICT("Variable '%s' cannot have same name as a function."),
    FUNCTION_NOT_DECLARED("Function '%s' not declared."),
    FUNCTION_PARAMETER_TYPE("Parameter %d of function '%s' expected type '%s', got '%s'."),
    RETURN_OUTSIDE_FUNCTION("'xinpum' (return) is only valid inside functions."),
    SYMBOL_REDECLARED_IN_SCOPE("Symbol '%s' already declared in scope %d."),
    UNKNOWN_SYMBOL("Variable or function '%s' not declared."),
    EXPRESSION_TYPE_MISMATCH("Expression types mismatch: '%s' and '%s'."),
    FUNCTION_REDECLARED("Function '%s' already declared."),
    VARIABLE_REDECLARED("Variable '%s' already declared.");

    private final String message;

    SemanticErrorType(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }

    @Override
    public String toString() {
        return message;
    }
}