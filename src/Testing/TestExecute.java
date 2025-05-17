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
 * Automatically executes all .wsp tests located in the resources/tests/ folder.
 */
public class TestExecute {
    private static final String TEST_DIR = "resources/tests/";

    private final LexicalAnalyzer lexer;
    private final ParserAnalyzer parser;
    private final List<Test> tests = new ArrayList<>();

    public TestExecute(LexicalAnalyzer lexer, ParserAnalyzer parser) throws IOException {
        this.lexer = lexer;
        this.parser = parser;
        loadTestFiles();
    }

    /**
     * Loads all .wsp files from TEST_DIR into the tests list,
     * extracting the description and test code.
     */
    private void loadTestFiles() throws IOException {
        Path dir = Paths.get(TEST_DIR);
        if (!Files.isDirectory(dir)) {
            throw new IOException("Directory does not exist: " + TEST_DIR);
        }

        int id = 1;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.wsp")) {
            for (Path p : ds) {
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);

                // Extract description (first line starting with //)
                String description = "";
                int startLine = 0;
                if (!lines.isEmpty() && lines.get(0).trim().startsWith("//")) {
                    description = lines.get(0).trim().substring(2).trim();
                    startLine = 1;
                }

                // Remaining lines are test code
                List<String> codeLines = lines.subList(startLine, lines.size());
                String code = String.join("\n", codeLines);

                tests.add(new Test(id++, description, code, p));
            }
        }
    }

    /**
     * Runs parsing, semantic analysis, TAC and MIPS generation for each test.
     * Uses a temporary file to tokenize the in-memory test code.
     */
    private void runTests() {
        for (Test test : tests) {
            lexer.clear();
            String[] pathParts = test.getFilePath().toString().split("\\\\");

            System.out.println("=== File: " + pathParts[pathParts.length - 1] +
                    (test.getDescription().isEmpty() ? "" : " - " + test.getDescription()) +
                    " ===");

            try {
                // Write test code to a temporary .wsp file
                Path tmp = Files.createTempFile("test", ".wsp");
                Files.write(tmp, test.getCode().getBytes(StandardCharsets.UTF_8));
                tmp.toFile().deleteOnExit();

                // Lexical analysis and parsing
                lexer.tokenize(tmp.toString());
                Node syntaxTree = parser.parse(lexer);
                System.out.println("  [OK] Parsing completed");

                // Semantic analysis
                SymbolTable symbolTable = new SymbolTable();
                new SemanticAnalyzer(syntaxTree, symbolTable).analyze();
                System.out.println("  [OK] Semantic analysis completed");

                // Generate TAC
                String tacPath = "resources/tests/production/tac/" + test.getId() + ".tac";
                Files.createDirectories(Paths.get("resources/tests/production/tac/"));
                TACGenerator tacGen = new TACGenerator();
                tacGen.generateFile(syntaxTree, symbolTable, tacPath);
                System.out.println("  [OK] TAC generated at " + tacPath);

                // Generate MIPS
                String mipsPath = "tests/production/mips/" + test.getId() + ".asm";
                Files.createDirectories(Paths.get("tests/production/mips/"));
                new MIPSGenerator().generate(tacPath, mipsPath);
                System.out.println("  [OK] MIPS generated at " + mipsPath + "\n");

            } catch (Exception e) {
                System.out.println("  [FAIL] Error in Test " + test.getId() + ": " + e.getMessage() + "\n");
            }
        }
    }

    /** Runs all loaded tests. */
    public void runAll() {
        runTests();
    }

    /** Prints the syntax tree (optional, unused by default). */
    private static void printTree(Node node, String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node);
        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printTree(children.get(i),
                    prefix + (isTail ? "    " : "│   "),
                    i == children.size() - 1);
        }
    }

    /** Standalone launcher for tests (can be run directly). */
    public static void main(String[] args) throws Exception {
        entities.Dictionary dict = new entities.Dictionary("resources/diccionari.json");
        entities.Grammar grammar = new entities.Grammar("resources/grammar.json");
        entities.ParserTableBuilder builder = new entities.ParserTableBuilder(dict, grammar);
        builder.buildParsingTable();

        LexicalAnalyzer lexer = new LexicalAnalyzer(dict);
        ParserAnalyzer parser = new ParserAnalyzer(grammar, builder);

        new TestExecute(lexer, parser).runAll();
    }
}
