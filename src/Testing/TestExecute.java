package Testing;

import LexicalAnalyzer.LexicalAnalyzer;
import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import entities.Node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class TestExecute {
    private static final String TEST_DIR = "resources/tests/";

    private final LexicalAnalyzer lexer;
    private final ParserAnalyzer parser;
    private final List<Test> tests = new ArrayList<>();

    public TestExecute(LexicalAnalyzer lexer, ParserAnalyzer parser) throws IOException {
        this.lexer  = lexer;
        this.parser = parser;
        loadFiles();
    }

    /**
     * Carga todos los .wsp de la carpeta en la lista tests.
     */
    private void loadFiles() throws IOException {
        Path dir = Paths.get(TEST_DIR);
        if (!Files.isDirectory(dir)) {
            throw new IOException("No existe el directorio: " + TEST_DIR);
        }

        int id = 1;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.wsp")) {
            for (Path p : ds) {
                // Lee la primera línea como descripción (opcional)
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                String desc = "";
                if (!lines.isEmpty() && lines.get(0).trim().startsWith("//")) {
                    desc = lines.get(0).trim().substring(2).trim();
                }
                tests.add(new Test(id++, desc, p.toString()));
            }
        }
    }

    /**
     * Ejecuta todos los tests cargados, informando PASS/FAIL por consola.
     */
    private void passTests() {
        for (Test t : tests) {
            System.out.println("=== Test " + t.getId() +
                    (t.getDescription().isEmpty() ? "" : ": " + t.getDescription())
                    + " ===");
            try {
                // Antes de cada test, asegúrate de reiniciar el estado del lexer
                lexer.tokenize(t.getFilePath());
                Node root = parser.parse(lexer);
                System.out.println("  [OK] Parsing completado");

                new SemanticAnalyzer(root).analyze();
                System.out.println("  [OK] Análisis semántico completado\n");

            } catch (Exception e) {
                System.err.println("  [FAIL] Error en Test " +
                        t.getId() + ": " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Método público que lanza la ejecución de todos los tests.
     */
    public void runAll() {
        passTests();
    }

    // Main auxiliar
    public static void main(String[] args) throws Exception {
        entities.Dictionary dict = new entities.Dictionary("resources/diccionari.json");
        entities.Grammar grammar = new entities.Grammar("resources/grammar.json");
        entities.ParserTableBuilder builder = new entities.ParserTableBuilder(dict, grammar);
        builder.buildParsingTable();

        LexicalAnalyzer lexer = new LexicalAnalyzer(dict);
        ParserAnalyzer  parser = new ParserAnalyzer(grammar, builder);

        TestExecute tests = new TestExecute(lexer, parser);
        tests.runAll();
    }
}
