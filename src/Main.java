import MIPS.MIPSGenerator;
import MIPS.MIPSGeneratorNEW;
import TAC.TACGenerator;
import TAC.TACGeneratorNEW;
import Testing.TestExecute;
import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import entities.*;
import LexicalAnalyzer.LexicalAnalyzer;

import java.util.List;

public class Main {
    static String wspFilePath = "testing/test3.wsp";
    static String tacFilePath = "outputFiles/tac/tac_test3.txt";
    static String mipsFilePath = "outputFiles/mips/mips_test3.asm";
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


                TACGeneratorNEW tacNEW = new TACGeneratorNEW();
                tacNEW.generateFile(tree, tacFilePath);

                MIPSGeneratorNEW mipsGen = new MIPSGeneratorNEW();
                mipsGen.generate(tacFilePath, mipsFilePath);
            }

        } catch (Exception e) {
            System.err.println("Error durante la ejecución:");
            e.printStackTrace();
            System.exit(2);
        }
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
}
