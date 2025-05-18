package ParserAnalyzer;

/**
 * Enumeració de tipus d'errors gramaticals detectats pel ParserAnalyzer.
 * Proporciona plantilles de missatge per informar sobre errors amb línia i detall.
 */
public enum GramaticalErrorType {
    /**
     * Error genèric de gramàtica quan un símbol o construcció no està suportat.
     * %s s'utilitza per la línia i el detall del símbol/construcció.
     */
    GRAMATICAL_ERROR_TYPE("Gramatical error on line %s, %s not supported.");
    /**
     * Plantilla de missatge amb paràmetres
     */
    private final String message;

    /**
     * Constructor de l'enumeració amb la plantilla de missatge.
     *
     * @param message Cadena de format que descriu l'error gramatical
     */
    GramaticalErrorType(String message) {
        this.message = message;
    }

    /**
     * Formata el missatge d'error substituint els paràmetres.
     *
     * @param args Arguments per omplir la plantilla (p.ex., número de línia, detall)
     * @return Cadena amb el missatge d'error complet
     */
    public String format(Object... args) {
        return String.format(message, args);
    }

    @Override
    public String toString() {
        return message;
    }
}
