package SemanticAnalyzer;

import entities.Node;
import entities.Token;

public class SampleTreeBuilder {
    public static Node createSampleTree() {
        Node root = new Node("AXIOMA");

        // 1. Correct declaration: num a -> 4 xd
        Node declA = new Node("DECLARACIO");
        declA.addChild(new Node(new Token("INT", "num", 1, 1)));
        declA.addChild(new Node(new Token("ID", "a", 1, 5)));
        declA.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 1, 7)));
        declA.addChild(new Node(new Token("INT_VALUE", "4", 1, 10)));
        declA.addChild(new Node(new Token("LINE_DELIMITER", "xd", 1, 12)));
        root.addChild(declA);

        // Global declaration for b: decimal b -> 0.0 xd
        Node declB = new Node("DECLARACIO");
        //declB.addChild(new Node(new Token("FLOAT", "decimal", 0, 1)));        |  descomentar per error mismatch
        declB.addChild(new Node(new Token("INT", "num", 0, 1)));
        declB.addChild(new Node(new Token("ID", "b", 0, 9)));
        declB.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 0, 11)));
        //declB.addChild(new Node(new Token("FLOAT_VALUE", "0.0", 0, 14)));     |  descomentar per error mismatch
        declB.addChild(new Node(new Token("INT_VALUE", "0", 0, 14)));
        declB.addChild(new Node(new Token("LINE_DELIMITER", "xd", 0, 17)));
        root.addChild(declB);

        // 3. Array declaration: textaco de 3 num arr xd
        Node decl3 = new Node("DECLARACIO");
        decl3.addChild(new Node(new Token("ARRAY", "textaco", 3, 1)));
        decl3.addChild(new Node(new Token("ID", "arr", 3, 10)));
        decl3.addChild(new Node(new Token("DE", "de", 3, 14)));
        decl3.addChild(new Node(new Token("INT_VALUE", "3", 3, 17)));
        decl3.addChild(new Node(new Token("INT", "num", 3, 19)));
        decl3.addChild(new Node(new Token("LINE_DELIMITER", "xd", 3, 22)));
        root.addChild(decl3);

        // 4. Function: decimal mitjana jajaj ... jejej
        Node func = new Node("CREA_FUNCIO");
        func.addChild(new Node(new Token("FLOAT", "decimal", 4, 1)));
        func.addChild(new Node(new Token("ID", "mitjana", 4, 9)));

        Node funcBody = new Node("FUNC_BODY");
        funcBody.addChild(new Node(new Token("OPEN_CLAUDATOR", "jajaj", 4, 18)));

        // b -> (a + 1) xd  (expression with operator)
        Node assign = new Node("ASSIGNACIO");
        Node assignPrim = new Node("ASSIGNACIO_PRIM");
        assignPrim.addChild(new Node(new Token("ID", "b", 5, 2)));
        assignPrim.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 5, 4)));
        assign.addChild(assignPrim);

        Node expr = new Node("EXPRESSIO");
        expr.addChild(new Node(new Token("ID", "a", 5, 7)));
        expr.addChild(new Node(new Token("SUM", "+", 5, 9)));
        expr.addChild(new Node(new Token("INT_VALUE", "1", 5, 11)));
        assign.addChild(expr);
        assign.addChild(new Node(new Token("LINE_DELIMITER", "xd", 5, 12)));
        funcBody.addChild(assign);

        // Return: xinpum b xd
        Node ret = new Node("XINPUM");
        ret.addChild(new Node(new Token("XINPUM", "xinpum", 6, 1)));
        ret.addChild(new Node(new Token("ID", "b", 6, 9)));
        ret.addChild(new Node(new Token("LINE_DELIMITER", "xd", 6, 11)));
        funcBody.addChild(ret);

        funcBody.addChild(new Node(new Token("CLOSE_CLAUDATOR", "jejej", 7, 1)));
        func.addChild(funcBody);
        root.addChild(func);

        // 5. Main function: num xat jajaj ... jejej
        Node mainFunc = new Node("CREA_MAIN");
        mainFunc.addChild(new Node(new Token("INT", "num", 8, 1)));
        mainFunc.addChild(new Node(new Token("MAIN", "xat", 8, 5)));

        Node mainBody = new Node("FUNC_BODY");
        mainBody.addChild(new Node(new Token("OPEN_CLAUDATOR", "jajaj", 8, 9)));

        // Function call: mitjana() xd
        Node call = new Node("CALL_FUNCIO");
        call.addChild(new Node(new Token("ID", "mitjana", 9, 2)));
        call.addChild(new Node(new Token("OPEN_PARENTESIS", "¿", 9, 10)));
        call.addChild(new Node(new Token("CLOSE_PARENTESIS", "?", 9, 11)));
        call.addChild(new Node(new Token("LINE_DELIMITER", "xd", 9, 13)));
        mainBody.addChild(call);

        // Return: xinpum a xd
        Node mainRet = new Node("XINPUM");
        mainRet.addChild(new Node(new Token("XINPUM", "xinpum", 10, 1)));
        mainRet.addChild(new Node(new Token("ID", "a", 10, 8)));
        mainRet.addChild(new Node(new Token("LINE_DELIMITER", "xd", 10, 10)));
        mainBody.addChild(mainRet);

        mainBody.addChild(new Node(new Token("CLOSE_CLAUDATOR", "jejej", 11, 1)));
        mainFunc.addChild(mainBody);
        root.addChild(mainFunc);

        return root;
    }
}