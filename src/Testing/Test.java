package Testing;

import java.nio.file.Path;

/**
 * Representa test amb un identificador seqüencial, una descripció,
 * el codi de prova i la ruta al fitxer de recursos (.wsp).
 * <p>Aquesta classe encapsula tota la informació relacionada amb un test,
 * incloent la seva descripció original extreta del fitxer i el codi literal.</p>
 */
public class Test {
    /**
     * Identificador únic del test dins de la seqüència de proves.
     */
    private final int id;
    /**
     * Descripció del test, tal com s'extreu de la primera línia comentada del fitxer.
     */
    private final String description;
    /**
     * Codi literal del test, recollit des del fitxer .wsp.
     */
    private final String code;
    /**
     * Ruta al fitxer .wsp associat amb aquest test.
     */
    private final Path filePath;

    /**
     * Crea una nova instància de Test amb tots els camps obligatoris.
     *
     * @param id          Identificador del test en la seqüència.
     * @param description Descripció llegida de la primera línia comentada.
     * @param code        Codi sencer del test, incloent totes les instruccions.
     * @param filePath    Ruta al fitxer .wsp que conté el test.
     */
    public Test(int id, String description, String code, Path filePath) {
        this.id = id;
        this.description = description;
        this.code = code;
        this.filePath = filePath;
    }

    /**
     * Retorna l'identificador del test.
     *
     * @return Enter que representa l'id del test.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna el codi literal del test.
     *
     * @return Cadena amb tot el codi del test.
     */
    public String getCode() {
        return code;
    }

    /**
     * Retorna la descripció assignada al test.
     *
     * @return Cadena amb la descripció del test.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retorna la ruta al fitxer .wsp associat amb aquest test.
     *
     * @return Objecte Path que apunta al fitxer de prova.
     */
    public Path getFilePath() {
        return filePath;
    }
}