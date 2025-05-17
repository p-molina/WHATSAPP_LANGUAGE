import MIPS.MIPSGenerator;
import TAC.TACGenerator;
import Testing.TestExecute;
import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import entities.*;
import LexicalAnalyzer.LexicalAnalyzer;

import java.io.IOException;
import java.util.Scanner;


public class Main {
    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_BLACK  = "\u001B[30m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_GREEN  = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE   = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN   = "\u001B[36m";
    public static final String ANSI_WHITE  = "\u001B[37m";

    static String wspFilePath;
    static String tacFilePath;
    static String mipsFilePath;
    static final String dictionaryFilePath = "resources/diccionari.json";
    static final String grammarFilePath = "resources/grammar.json";

    public static void main(String[] args) {
        try {
            Dictionary dictionary = new Dictionary(dictionaryFilePath);
            Grammar grammar = new Grammar(grammarFilePath);

            ParserTableBuilder builder = new ParserTableBuilder(dictionary, grammar);
            builder.buildParsingTable();

            LexicalAnalyzer lexer = new LexicalAnalyzer(dictionary);
            ParserAnalyzer parser = new ParserAnalyzer(grammar, builder);

            if (args.length > 0 && args[0].equals("-test")) {
                runTests(lexer, parser);
            } else {
                askForInputPaths();
                compileAndGenerate(lexer, parser);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void askForInputPaths() {
        Scanner scanner = new Scanner(System.in);

        printBanner();

        System.out.print("Enter the name of the .wsp file (without extension): ");
        String name = scanner.nextLine();

        wspFilePath = "resources/files/wsp/" + name + ".wsp";
        tacFilePath = "resources/files/tac/" + name + ".tac";
        mipsFilePath = "resources/files/mips/" + name + ".asm";
    }

    private static void runTests(LexicalAnalyzer lexer, ParserAnalyzer parser) throws IOException {
        System.out.println("Running tests...");
        TestExecute tests = new TestExecute(lexer, parser);
        tests.runAll();
        System.out.println("All tests completed.");
    }

    private static void compileAndGenerate(LexicalAnalyzer lexer, ParserAnalyzer parser) {
        long startTime = System.currentTimeMillis();

        lexer.tokenize(wspFilePath);
        Node tree = parser.parse(lexer);

        SymbolTable symbolTable = new SymbolTable();
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(tree, symbolTable);
        semanticAnalyzer.analyze();


        TACGenerator tac = new TACGenerator();
        tac.generateFile(tree, symbolTable, tacFilePath);

        MIPSGenerator mipsGen = new MIPSGenerator();
        mipsGen.generate(tacFilePath, mipsFilePath);

        System.out.println(ANSI_GREEN + "\nCompilation finished successfully!" + ANSI_RESET);


        System.out.println("\n╔══════════════════════════════╗");
        System.out.println("║      Compilation Summary     ║");
        System.out.println("╚══════════════════════════════╝\n");

        System.out.println("Symbol Table Generated");
        symbolTable.printTable();

        System.out.println("------------------------");
        System.out.printf("Source file:     " + ANSI_GREEN +"%s\n" + ANSI_RESET, wspFilePath);
        System.out.printf("TAC output:      " + ANSI_GREEN +"%s\n" + ANSI_RESET, tacFilePath);
        System.out.printf("MIPS output:     " + ANSI_GREEN +"%s\n" + ANSI_RESET, mipsFilePath);
        System.out.println("------------------------\n");

        long endTime = System.currentTimeMillis();
        System.out.printf("Total compilation time: " + ANSI_GREEN + "%.2f seconds\n" + ANSI_RESET, (endTime - startTime) / 1000.0);

    }

    public static void printBanner() {
        System.out.println(ANSI_GREEN +"""
     _    _ _           _                           _____                       _ _          \s
    | |  | | |         | |                         /  __ \\                     (_) |         \s
    | |  | | |__   __ _| |_ ___  __ _ _ __  _ __   | /  \\/ ___  _ __ ___  _ __  _| | ___ _ __\s
    | |/\\| | '_ \\ / _` | __/ __|/ _` | '_ \\| '_ \\  | |    / _ \\| '_ ` _ \\| '_ \\| | |/ _ \\ '__|
    \\  /\\  / | | | (_| | |_\\__ \\ (_| | |_) | |_) | | \\__/\\ (_) | | | | | | |_) | | |  __/ |  \s
     \\/  \\/|_| |_|\\__,_|\\__|___/\\__,_| .__/| .__/   \\____/\\___/|_| |_| |_| .__/|_|_|\\___|_|  \s
                                     | |   | |                           | |                 \s
                                     |_|   |_|                           |_|                 \s                                                                                                                                                   \s                                                 
    """ + ANSI_RESET);
    }



}
