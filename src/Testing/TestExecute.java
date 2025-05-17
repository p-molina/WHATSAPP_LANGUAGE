package Testing;

import LexicalAnalyzer.LexicalAnalyzer;
import MIPS.MIPSGenerator;
import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import TAC.TACGenerator;
import entities.Node;
import entities.SymbolTable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta automáticamente todos los tests .wsp de la carpeta resources/tests/.
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
     * Carga todos los .wsp de TEST_DIR en la lista tests,
     * extrayendo descripción y el resto del código en el campo code.
     */
    private void loadFiles() throws IOException {
        Path dir = Paths.get(TEST_DIR);
        if (!Files.isDirectory(dir)) {
            throw new IOException("No existe el directorio: " + TEST_DIR);
        }

        int id = 1;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.wsp")) {
            for (Path p : ds) {
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);

                // Extraer descripción (línea que comienza con //)
                String desc = "";
                int start = 0;
                if (!lines.isEmpty() && lines.get(0).trim().startsWith("//")) {
                    desc = lines.get(0).trim().substring(2).trim();
                    start = 1;
                }

                // El resto del fichero es el código a testear
                List<String> codeLines = lines.subList(start, lines.size());
                String code = String.join("\n", codeLines);

                tests.add(new Test(id++, desc, code, p));
            }
        }
    }

    /**
     * Ejecuta parsing, análisis semántico, genera TAC y MIPS para cada test.
     * Para tokenizar usamos un fichero temporal construido a partir de code.
     */
    private void passTests() {
        for (Test t : tests) {
            lexer.clear();
            String[] parts = t.getFilePath().toString().split("\\\\");

            System.out.println("=== File:" + parts[2] +
                    (t.getDescription().isEmpty() ? "" : " " + t.getDescription()) +
                    " ===");
            try {
                // Escribimos code en un fichero temporal
                Path tmp = Files.createTempFile("test", ".wsp");
                Files.write(tmp,
                        t.getCode().getBytes(StandardCharsets.UTF_8));
                tmp.toFile().deleteOnExit();

                // Tokenizar y parsear
                lexer.tokenize(tmp.toString());
                Node root = parser.parse(lexer);
                System.out.println("  [OK] Parsing completado");

//                if (parts[2].equals("Test14.wsp")) {
//                    printTree(root, "", true);
//                }

                // Análisis semántico
                SymbolTable symbolTable = new SymbolTable();
                new SemanticAnalyzer(root, symbolTable).analyze();
                System.out.println("  [OK] Análisis semántico completado");

                // Generar TAC
                String tacPath = "outputFiles/tac/tac_test" + t.getId() + ".txt";

                TACGenerator tacGen = new TACGenerator();
                tacGen.generateFile(root, tacPath);
                System.out.println("  [OK] TAC generado en " + tacPath);

                // Generar MIPS
                String mipsPath = "outputFiles/mips/mips_test" + t.getId() + ".asm";
                Files.createDirectories(Paths.get("outputFiles/mips"));
                new MIPSGenerator().generate(tacPath, mipsPath);
                System.out.println("  [OK] MIPS generado en " + mipsPath + "\n");

            } catch (Exception e) {
                System.out.println("  [FAIL] Error en Test " + t.getId() + ": " + e.getMessage() + "\n");
            }
        }
    }

    /** Lanza todos los tests cargados. */
    public void runAll() {
        passTests();
    }

    private static void printTree(Node node, String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node);
        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printTree(children.get(i),
                    prefix + (isTail ? "    " : "│   "),
                    i == children.size() - 1);
        }
    }

    /** Main auxiliar para ejecutar tests desde línea de comandos. */
    public static void main(String[] args) throws Exception {
        entities.Dictionary dict    = new entities.Dictionary("resources/diccionari.json");
        entities.Grammar    grammar = new entities.Grammar("resources/grammar.json");
        entities.ParserTableBuilder builder = new entities.ParserTableBuilder(dict, grammar);
        builder.buildParsingTable();

        LexicalAnalyzer lexer   = new LexicalAnalyzer(dict);
        ParserAnalyzer  parser  = new ParserAnalyzer(grammar, builder);

        new TestExecute(lexer, parser).runAll();
    }
}
