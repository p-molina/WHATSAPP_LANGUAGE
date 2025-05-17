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

    private static void compileAndGenerate(LexicalAnalyzer lexer, ParserAnalyzer parser) throws Exception {
        lexer.tokenize(wspFilePath);
        Node tree = parser.parse(lexer);

        SymbolTable symbolTable = new SymbolTable();
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(tree, symbolTable);
        semanticAnalyzer.analyze();

        TACGenerator tac = new TACGenerator();
        tac.generateFile(tree, tacFilePath);
        System.out.println("TAC file generated at: " + tacFilePath);

        MIPSGenerator mipsGen = new MIPSGenerator();
        mipsGen.generate(tacFilePath, mipsFilePath);
        System.out.println("MIPS file generated at: " + mipsFilePath);
    }
}
