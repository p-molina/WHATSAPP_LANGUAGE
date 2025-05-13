import MIPS.MIPSGenerator;
import ParserAnalyzer.ParserAnalyzer;
import TAC.TACGenerator;
import SemanticAnalyzer.SemanticAnalyzer;
import entities.*;
import LexicalAnalyzer.LexicalAnalyzer;
import entities.Dictionary;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Dictionary dict = new Dictionary("resources/diccionari.json");
            Grammar grammar = new Grammar("resources/grammar.json");

            ParserTableBuilder builder = new ParserTableBuilder(dict, grammar);
            builder.buildParsingTable();

            LexicalAnalyzer lexer = new LexicalAnalyzer(dict);
            lexer.tokenize("testing/test2.wsp");

            ParserAnalyzer parser = new ParserAnalyzer(grammar, builder);
            Node root = parser.parse(lexer);
            printTree(root, "", true);

            SymbolTable symbolTable = new SymbolTable();

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(root, symbolTable);
            semanticAnalyzer.analyze();

            TACGenerator tacGen = new TACGenerator(root);
            List<String> tac = tacGen.generate(root);
            //tac = tacGen.generate(root);  //Generar fitxer tac_test1.txt
            tac.forEach(System.out::println);

            MIPSGenerator mipsGen = new MIPSGenerator("outputFiles/tac/tac_test1.txt",
                                                    "outputFiles/mips/mips_test1.asm");
            mipsGen.generate();

        } catch (RuntimeException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printTree(Node node, String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node);
        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean last = (i == children.size() - 1);
            printTree(children.get(i), prefix + (isTail ? "    " : "│   "), last);
        }
    }
}
