package Testing;

import LexicalAnalyzer.LexicalAnalyzer;
import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import TAC.TACGenerator;
import MIPS.MIPSGenerator;
import entities.Node;
import entities.SymbolTable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta automáticamente todos los tests .wsp de la carpeta TEST_DIR.
 */
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
     * Carga todos los .wsp de TEST_DIR en la lista tests, extrayendo descripción.
     */
    private void loadFiles() throws IOException {
        Path dir = Paths.get(TEST_DIR);
        if (!Files.isDirectory(dir)) {
            throw new IOException("No existe el directorio: " + TEST_DIR);
        }

        int id = 1;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.wsp")) {
            for (Path p : ds) {
                // Leer primera línea como descripción
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
     * Ejecuta parsing, análisis semántico, genera TAC y MIPS para cada test.
     */
    private void passTests() {
        for (Test t : tests) {
            System.out.println("=== Test " + t.getId() +
                    (t.getDescription().isEmpty() ? "" : ": " + t.getDescription()) +
                    " ===");
            try {
                // Resetear lexer antes de cada test
                lexer.tokenize(t.getFilePath());
                Node root = parser.parse(lexer);
                System.out.println("  [OK] Parsing completado");

                // Análisis semántico
                SymbolTable symbolTable = new SymbolTable();
                SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(root, symbolTable);
                semanticAnalyzer.analyze();
                System.out.println("  [OK] Análisis semántico completado");

                // Generar TAC y escribir a archivo
                TACGenerator tacGen = new TACGenerator(root);
                List<String> tac = tacGen.generate(root);
                String tacPath = "outputFiles/tac/tac_test" + t.getId() + ".txt";
                Files.createDirectories(Paths.get("outputFiles/tac"));
                Files.write(Paths.get(tacPath), tac, StandardCharsets.UTF_8);
                System.out.println("  [OK] TAC generado en " + tacPath);

                // Generar MIPS a partir del archivo TAC
                String mipsPath = "outputFiles/mips/mips_test" + t.getId() + ".asm";
                Files.createDirectories(Paths.get("outputFiles/mips"));
                MIPSGenerator mipsGen = new MIPSGenerator(tacPath, mipsPath);
                mipsGen.generate();
                System.out.println("  [OK] MIPS generado en " + mipsPath + "\n");

            } catch (Exception e) {
                System.out.println("  [FAIL] Error en Test " + t.getId() + ": " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Ejecuta todos los tests cargados.
     */
    public void runAll() {
        passTests();
    }

    /**
     * Main auxiliar para ejecutar tests desde línea de comandos.
     */
    public static void main(String[] args) throws Exception {
        entities.Dictionary dict = new entities.Dictionary("resources/diccionari.json");
        entities.Grammar grammar = new entities.Grammar("resources/grammar.json");
        entities.ParserTableBuilder builder = new entities.ParserTableBuilder(dict, grammar);
        builder.buildParsingTable();

        LexicalAnalyzer lexer = new LexicalAnalyzer(dict);
        ParserAnalyzer parser = new ParserAnalyzer(grammar, builder);

        TestExecute tests = new TestExecute(lexer, parser);
        tests.runAll();
    }
}