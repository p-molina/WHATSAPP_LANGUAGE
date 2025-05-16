package ParserAnalyzer;

public enum GramaticalErrorType {
    GRAMATICAL_ERROR_TYPE("Gramatical error on line %s, %s not supported.");

    private final String message;

    GramaticalErrorType(String message) {
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
