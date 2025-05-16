package Testing;

import java.nio.file.Path;

/**
 * Representa un test con id, descripción y ruta al fichero .wsp
 */
public class Test {
    private final int id;
    private final String description;
    private final String code;
    private final Path filePath;

    /**
     * Constructor.
     *
     * @param id          Identificador del test (secuencia)
     * @param description Descripción extraída de la primera línea comentada
     * @param filePath    Ruta al fichero .wsp de test
     */
    public Test(int id, String description, String code, Path filePath) {
        this.id = id;
        this.description = description;
        this.code = code;
        this.filePath = filePath;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Path getFilePath() {
        return filePath;
    }
}