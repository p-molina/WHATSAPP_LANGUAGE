import MIPS.MIPSGenerator;
import TAC.TACGenerator;
import Testing.TestExecute;
import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import entities.*;
import LexicalAnalyzer.LexicalAnalyzer;

public class Main {
    static String wspFilePath = "testing/fibonacci.wsp";
    static String tacFilePath = "outputFiles/tac/tac_fibonacci.txt";
    static String mipsFilePath = "outputFiles/mips/mips_fibonacci.asm";
    static String dicionaryFilePath = "resources/diccionari.json";
    static String grammarFilePath = "resources/grammar.json";


    public static void main(String[] args) {
        try {
            Dictionary  dict    = new Dictionary(dicionaryFilePath);
            Grammar     grammar = new Grammar(grammarFilePath);

            ParserTableBuilder builder = new ParserTableBuilder(dict, grammar);
            builder.buildParsingTable();

            LexicalAnalyzer lexer   = new LexicalAnalyzer(dict);
            ParserAnalyzer  parser  = new ParserAnalyzer(grammar, builder);

            boolean runTests = false;
            // Cambiar esto en un futuro para que el fichero sea un parametro de entrada
            for (String arg : args) {
                if ("-test".equals(arg)) {
                    runTests = true;
                } else {
                    wspFilePath = arg;
                }
            }

            if (runTests) {
                TestExecute tests = new TestExecute(lexer, parser);
                tests.runAll();
            } else {
                if (wspFilePath == null) {
                    System.err.println("Uso:");
                    System.err.println("  java Main -test               # Para correr todos los tests");
                    System.err.println("  java Main <archivo.wsp>       # Para parsear un único archivo");
                    System.exit(1);
                }

                lexer.tokenize(wspFilePath);
                Node tree = parser.parse(lexer);

                SymbolTable symbolTable = new SymbolTable();
                SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(tree, symbolTable);
                semanticAnalyzer.analyze();


                TACGenerator tac = new TACGenerator();
                tac.generateFile(tree, tacFilePath);

                MIPSGenerator mipsGen = new MIPSGenerator();
                mipsGen.generate(tacFilePath, mipsFilePath);
            }

        } catch (Exception e) {
            System.err.println("Error durante la ejecución:");
            e.printStackTrace();
            System.exit(2);
        }
    }

}
