package Testing;

import LexicalAnalyzer.LexicalAnalyzer;
import MIPS.MIPSGenerator;
import ParserAnalyzer.ParserAnalyzer;
import ParserAnalyzer.ParserTableGenerator;
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
 * Executa automàticament tots els tests .wsp situats al directori resources/tests/.
 * <p>Aquesta classe carrega cada fitxer de prova, en extreu la descripció,
 * processa el codi amb els analitzadors lèxic, sintàctic i semàntic,
 * genera TAC i finalment codi MIPS per a cada test.</p>
 */
public class TestExecute {
    /**
     * Directori on es troben els fitxers de prova amb extensió .wsp.
     */
    private static final String TEST_DIR = "resources/tests/";
    /**
     * Analitzador lèxic reutilitzat per a tots els tests.
     */
    private final LexicalAnalyzer lexer;
    /**
     * Analitzador sintàctic reutilitzat per a tots els tests.
     */
    private final ParserAnalyzer parser;
    /**
     * Llista que conté tots els objectes Test carregats des del directori.
     */
    private final List<Test> tests = new ArrayList<>();

    /**
     * Construeix un executor de tests amb els analitzadors indicats i carrega els fitxers.
     *
     * @param lexer  Instància de LexicalAnalyzer per fer la tokenització.
     * @param parser Instància de ParserAnalyzer per construir l'arbre sintàctic.
     * @throws IOException Si no es pot accedir al directori de tests.
     */
    public TestExecute(LexicalAnalyzer lexer, ParserAnalyzer parser) throws IOException {
        this.lexer = lexer;
        this.parser = parser;
        // Carrega els fitxers .wsp en la llista tests
        loadTestFiles();
    }

    /**
     * Carrega tots els fitxers .wsp del directori TEST_DIR a la llista de tests.
     * Extreu la descripció (primer comentari) i el codi restant de cada fitxer.
     *
     * @throws IOException Si hi ha cap problema llegint els fitxers o el directori.
     */
    private void loadTestFiles() throws IOException {
        Path dir = Paths.get(TEST_DIR);
        // Verifica que el directori existeix
        if (!Files.isDirectory(dir)) {
            throw new IOException("Directory does not exist: " + TEST_DIR);
        }

        int id = 1;
        // Obre un DirectoryStream per iterar sobre fitxers amb extensió .wsp
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.wsp")) {
            for (Path p : ds) {
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);

                // Extreu la descripció de la primera línia si comença amb '//'
                String description = "";
                int startLine = 0;
                if (!lines.isEmpty() && lines.get(0).trim().startsWith("//")) {
                    description = lines.get(0).trim().substring(2).trim();
                    startLine = 1;
                }

                // Les línies restants formen el codi del test
                List<String> codeLines = lines.subList(startLine, lines.size());
                String code = String.join("\n", codeLines);

                // Afegeix el nou Test amb identificador, descripció, codi i ruta
                tests.add(new Test(id++, description, code, p));
            }
        }
    }

    /**
     * Executa l'anàlisi lèxic, sintàctic, semàntic, generació de TAC i MIPS per a cada test.
     * Utilitza fitxers temporals per aprocessar el codi en memòria.
     */
    private void runTests() {
        for (Test test : tests) {
            // Neteja l'estat previ de l'analitzador lèxic
            lexer.clear();
            // Obté el nom de fitxer per a imprimir-lo
            String[] pathParts = test.getFilePath().toString().split("\\\\");

            System.out.println("=== File: " + pathParts[pathParts.length - 1] +
                    (test.getDescription().isEmpty() ? "" : " - " + test.getDescription()) +
                    " ===");

            try {
                // Escriu el codi del test en un fitxer temporal .wsp
                Path tmp = Files.createTempFile("test", ".wsp");
                Files.write(tmp, test.getCode().getBytes(StandardCharsets.UTF_8));
                // Elimina al tancar JVM
                tmp.toFile().deleteOnExit();

                // Anàlisi lèxic i sintàctic
                lexer.tokenize(tmp.toString());
                Node syntaxTree = parser.parse(lexer);
                System.out.println("  [OK] Parsing completed");

                // Anàlisi semàntic
                SymbolTable symbolTable = new SymbolTable();
                new SemanticAnalyzer(syntaxTree, symbolTable).analyze();
                System.out.println("  [OK] Semantic analysis completed");

                // Generació de TAC
                String tacPath = "resources/tests/production/tac/" + test.getId() + ".tac";
                Files.createDirectories(Paths.get("resources/tests/production/tac/"));
                TACGenerator tacGen = new TACGenerator();
                tacGen.generateFile(syntaxTree, symbolTable, tacPath);
                System.out.println("  [OK] TAC generated at " + tacPath);

                // Generació de codi MIPS
                String mipsPath = "resources/tests/production/mips/" + test.getId() + ".asm";
                Files.createDirectories(Paths.get("resources/tests/production/mips/"));
                new MIPSGenerator().generate(tacPath, mipsPath);
                System.out.println("  [OK] MIPS generated at " + mipsPath + "\n");

            } catch (Exception e) {
                // Captura qualsevol error durant el procés i el mostra
                System.out.println("  [FAIL] Error in Test " + test.getId() + ": " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Executa tots els tests carregats.
     */
    public void runAll() {
        runTests();
    }

    /**
     * Imprimeix per pantalla l'estructura d'arbre del node sintàctic (opcions d'ús).
     *
     * @param node   Node arrel de l'arbre a imprimir.
     * @param prefix Cadena de prefix per l'indentació.
     * @param isTail Indica si el node és l'últim fill del seu pare.
     */
    private static void printTree(Node node, String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node);
        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printTree(children.get(i),
                    prefix + (isTail ? "    " : "│   "),
                    i == children.size() - 1);
        }
    }

    /**
     * Punt d'entrada principal per executar els tests en mode standalone.
     * Construeix diccionari, gramàtica, taula de parsing i executa tots els tests.
     *
     * @param args Arguments de línia de comandes (no s'utilitzen).
     * @throws Exception Si hi ha qualsevol error en la inicialització o execució.
     */
    public static void main(String[] args) throws Exception {
        // Inicialitza el diccionari i la gramàtica
        entities.Dictionary dict = new entities.Dictionary("resources/diccionari.json");
        entities.Grammar grammar = new entities.Grammar("resources/grammar.json");
        ParserTableGenerator builder = new ParserTableGenerator(dict, grammar);
        builder.buildParsingTable();

        // Crea els analitzadors lèxic i sintàctic
        LexicalAnalyzer lexer = new LexicalAnalyzer(dict);
        ParserAnalyzer parser = new ParserAnalyzer(grammar, builder);

        // Executa tots els tests
        new TestExecute(lexer, parser).runAll();
    }
}
