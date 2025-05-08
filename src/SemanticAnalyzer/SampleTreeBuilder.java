package SemanticAnalyzer;

import entities.Node;
import entities.Token;

public class SampleTreeBuilder {
    public static Node createSampleTree() {
        Node root = new Node("AXIOMA");

        // Global: num a -> 10 xd
        Node declA = new Node("DECLARACIO");
        declA.addChild(new Node(new Token("INT", "num", 1, 1)));
        declA.addChild(new Node(new Token("ID", "a", 1, 5)));
        declA.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 1, 7)));
        declA.addChild(new Node(new Token("INT_VALUE", "10", 1, 10)));
        declA.addChild(new Node(new Token("LINE_DELIMITER", "xd", 1, 12)));
        root.addChild(declA);

        //Test Error: Duplicació de variable
        /*
        Node dupVar = new Node("DECLARACIO");
        dupVar.addChild(new Node(new Token("INT", "num", 100, 1)));
        dupVar.addChild(new Node(new Token("ID", "a", 100, 5))); // ja existeix
        dupVar.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 100, 7)));
        dupVar.addChild(new Node(new Token("INT_VALUE", "3", 100, 10)));
        dupVar.addChild(new Node(new Token("LINE_DELIMITER", "xd", 100, 11)));
        root.addChild(dupVar);
        */

        // Test Error nom de funcio igual que el de una variable
        /*
        Node funcSameAsVar = new Node("CREA_FUNCIO");
        funcSameAsVar.addChild(new Node(new Token("INT", "num", 101, 1)));
        funcSameAsVar.addChild(new Node(new Token("ID", "a", 101, 5))); // "a" ja és variable
        Node body = new Node("FUNC_BODY");
        body.addChild(new Node(new Token("OPEN_CLAUDATOR", "jajaj", 101, 10)));
        body.addChild(new Node(new Token("CLOSE_CLAUDATOR", "jejej", 101, 20)));
        funcSameAsVar.addChild(body);
        root.addChild(funcSameAsVar);
        */

        // Global: decimal b -> 3.14 xd
        Node declB = new Node("DECLARACIO");
        declB.addChild(new Node(new Token("FLOAT", "decimal", 2, 1)));
        declB.addChild(new Node(new Token("ID", "b", 2, 9)));
        declB.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 2, 11)));
        declB.addChild(new Node(new Token("FLOAT_VALUE", "3.14", 2, 14)));
        declB.addChild(new Node(new Token("LINE_DELIMITER", "xd", 2, 18)));
        root.addChild(declB);

        // Global: lletra c -> 'x' xd
        Node declC = new Node("DECLARACIO");
        declC.addChild(new Node(new Token("CHAR", "lletra", 3, 1)));
        declC.addChild(new Node(new Token("ID", "c", 3, 8)));
        declC.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 3, 10)));
        declC.addChild(new Node(new Token("CHAR_VALUE", "'x'", 3, 13)));
        declC.addChild(new Node(new Token("LINE_DELIMITER", "xd", 3, 17)));
        root.addChild(declC);

        // Global: textaco de 3 num arr xd
        Node declArr = new Node("DECLARACIO");
        declArr.addChild(new Node(new Token("ARRAY", "textaco", 4, 1)));
        declArr.addChild(new Node(new Token("ID", "arr", 4, 10)));
        declArr.addChild(new Node(new Token("DE", "de", 4, 14)));
        declArr.addChild(new Node(new Token("INT_VALUE", "3", 4, 17)));
        declArr.addChild(new Node(new Token("INT", "num", 4, 19)));
        declArr.addChild(new Node(new Token("LINE_DELIMITER", "xd", 4, 22)));
        root.addChild(declArr);

        // Funció: decimal calculaMitjana jajaj b -> b + 1.86 xd xinpum b xd jejej
        Node func1 = new Node("CREA_FUNCIO");
        func1.addChild(new Node(new Token("FLOAT", "decimal", 5, 1)));
        func1.addChild(new Node(new Token("ID", "calculaMitjana", 5, 9)));

        Node body1 = new Node("FUNC_BODY");
        body1.addChild(new Node(new Token("OPEN_CLAUDATOR", "jajaj", 5, 25)));

        Node assign1 = new Node("ASSIGNACIO");
        Node prim1 = new Node("ASSIGNACIO_PRIM");
        prim1.addChild(new Node(new Token("ID", "b", 6, 1)));
        prim1.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 6, 3)));
        assign1.addChild(prim1);

        Node expr1 = new Node("EXPRESSIO");
        expr1.addChild(new Node(new Token("ID", "b", 6, 6)));
        expr1.addChild(new Node(new Token("SUM", "+", 6, 8)));
        expr1.addChild(new Node(new Token("FLOAT_VALUE", "1.86", 6, 10)));
        //expr1.addChild(new Node(new Token("INT_VALUE", "1", 6, 10))); //Test Error
        assign1.addChild(expr1);
        assign1.addChild(new Node(new Token("LINE_DELIMITER", "xd", 6, 15)));
        body1.addChild(assign1);

        Node ret1 = new Node("XINPUM");
        ret1.addChild(new Node(new Token("XINPUM", "xinpum", 7, 1)));
        ret1.addChild(new Node(new Token("ID", "b", 7, 9)));
        ret1.addChild(new Node(new Token("LINE_DELIMITER", "xd", 7, 11)));
        body1.addChild(ret1);
        body1.addChild(new Node(new Token("CLOSE_CLAUDATOR", "jejej", 8, 1)));
        func1.addChild(body1);
        root.addChild(func1);

        // Funció: num incrementa jajaj a -> a + 1 xd xinpum a xd jejej
        Node func2 = new Node("CREA_FUNCIO");
        func2.addChild(new Node(new Token("INT", "num", 9, 1)));
        func2.addChild(new Node(new Token("ID", "incrementa", 9, 5)));

        Node body2 = new Node("FUNC_BODY");
        body2.addChild(new Node(new Token("OPEN_CLAUDATOR", "jajaj", 9, 17)));

        Node assign2 = new Node("ASSIGNACIO");
        Node prim2 = new Node("ASSIGNACIO_PRIM");
        prim2.addChild(new Node(new Token("ID", "a", 10, 1)));
        prim2.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 10, 3)));
        assign2.addChild(prim2);

        Node expr2 = new Node("EXPRESSIO");
        expr2.addChild(new Node(new Token("ID", "a", 10, 6)));
        expr2.addChild(new Node(new Token("SUM", "+", 10, 8)));
        expr2.addChild(new Node(new Token("INT_VALUE", "1", 10, 10)));
        assign2.addChild(expr2);
        assign2.addChild(new Node(new Token("LINE_DELIMITER", "xd", 10, 12)));
        body2.addChild(assign2);

        Node ret2 = new Node("XINPUM");
        ret2.addChild(new Node(new Token("XINPUM", "xinpum", 11, 1)));
        ret2.addChild(new Node(new Token("ID", "a", 11, 9)));
        //ret2.addChild(new Node(new Token("ID", "b", 11, 9)));  // test error
        ret2.addChild(new Node(new Token("LINE_DELIMITER", "xd", 11, 11)));
        body2.addChild(ret2);
        body2.addChild(new Node(new Token("CLOSE_CLAUDATOR", "jejej", 12, 1)));
        func2.addChild(body2);
        root.addChild(func2);

        //Test Errpr: return fora d'una funció.
        /*
        Node badReturn = new Node("XINPUM");
        badReturn.addChild(new Node(new Token("XINPUM", "xinpum", 105, 1)));
        badReturn.addChild(new Node(new Token("INT_VALUE", "1", 105, 9)));
        badReturn.addChild(new Node(new Token("LINE_DELIMITER", "xd", 105, 11)));
        root.addChild(badReturn);
         */

        // Main function
        Node mainFunc = new Node("CREA_MAIN");
        mainFunc.addChild(new Node(new Token("INT", "num", 13, 1)));
        mainFunc.addChild(new Node(new Token("MAIN", "xat", 13, 5)));

        Node mainBody = new Node("FUNC_BODY");
        mainBody.addChild(new Node(new Token("OPEN_CLAUDATOR", "jajaj", 13, 9)));

        // Test Error: Assignar a una variable no creada

        /*
        Node wrongAssign = new Node("ASSIGNACIO");
        Node prim = new Node("ASSIGNACIO_PRIM");
        prim.addChild(new Node(new Token("ID", "x", 20, 1)));
        prim.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 20, 3)));
        wrongAssign.addChild(prim);
        wrongAssign.addChild(new Node(new Token("INT_VALUE", "5", 20, 6)));
        wrongAssign.addChild(new Node(new Token("LINE_DELIMITER", "xd", 20, 7)));
        mainBody.addChild(wrongAssign);
        */

        // Test Error: Divisió per 0
        /*
        Node divZero = new Node("ASSIGNACIO");
        Node prim = new Node("ASSIGNACIO_PRIM");
        prim.addChild(new Node(new Token("ID", "a", 106, 1)));
        prim.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 106, 3)));
        divZero.addChild(prim);

        Node expr = new Node("EXPRESSIO");
        expr.addChild(new Node(new Token("INT_VALUE", "5", 106, 6)));
        expr.addChild(new Node(new Token("DIVISION", "/", 106, 7)));
        expr.addChild(new Node(new Token("INT_VALUE", "0", 106, 8))); // ⛔ divisió per zero

        divZero.addChild(expr);
        divZero.addChild(new Node(new Token("LINE_DELIMITER", "xd", 106, 10)));
        root.addChild(divZero);
         */

        // a -> incrementa xd
        Node callAssign1 = new Node("ASSIGNACIO");
        Node callPrim1 = new Node("ASSIGNACIO_PRIM");
        callPrim1.addChild(new Node(new Token("ID", "a", 14, 1)));
        callPrim1.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 14, 3)));
        callAssign1.addChild(callPrim1);
        callAssign1.addChild(new Node(new Token("ID", "incrementa", 14, 6)));
        //callAssign1.addChild(new Node(new Token("ID", "noExisteix", 14, 6))); //Test Error
        callAssign1.addChild(new Node(new Token("LINE_DELIMITER", "xd", 14, 17)));
        mainBody.addChild(callAssign1);

        // b -> calculaMitjana xd
        Node callAssign2 = new Node("ASSIGNACIO");
        Node callPrim2 = new Node("ASSIGNACIO_PRIM");
        callPrim2.addChild(new Node(new Token("ID", "b", 15, 1)));
        callPrim2.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 15, 3)));
        callAssign2.addChild(callPrim2);
        callAssign2.addChild(new Node(new Token("ID", "calculaMitjana", 15, 6)));
        callAssign2.addChild(new Node(new Token("LINE_DELIMITER", "xd", 15, 22)));
        mainBody.addChild(callAssign2);

        // pos 2 de arr -> 99 xd
        Node arrayAssign = new Node("ASSIGNACIO");
        Node arrayPrim = new Node("ASSIGNACIO_PRIM");
        arrayPrim.addChild(new Node(new Token("POS", "pos", 16, 1)));
        arrayPrim.addChild(new Node(new Token("INT_VALUE", "2", 16, 5)));
        //arrayPrim.addChild(new Node(new Token("ID", "b", 16, 5))); // Test Error
        arrayPrim.addChild(new Node(new Token("DE", "de", 16, 7)));
        arrayPrim.addChild(new Node(new Token("ID", "arr", 16, 10)));
        //arrayPrim.addChild(new Node(new Token("ID", "noExisteix", 16, 10))); // Test Error
        arrayPrim.addChild(new Node(new Token("EQUAL_ASSIGNATION", "->", 16, 14)));
        arrayAssign.addChild(arrayPrim);
        arrayAssign.addChild(new Node(new Token("INT_VALUE", "99", 16, 17)));
        //arrayAssign.addChild(new Node(new Token("CHAR_VALUE", "'x'", 16, 17))); // Test error
        arrayAssign.addChild(new Node(new Token("LINE_DELIMITER", "xd", 16, 20)));
        mainBody.addChild(arrayAssign);

        Node mainRet = new Node("XINPUM");
        mainRet.addChild(new Node(new Token("XINPUM", "xinpum", 17, 1)));
        mainRet.addChild(new Node(new Token("INT_VALUE", "0", 17, 9)));
        mainRet.addChild(new Node(new Token("LINE_DELIMITER", "xd", 17, 11)));
        mainBody.addChild(mainRet);

        mainBody.addChild(new Node(new Token("CLOSE_CLAUDATOR", "jejej", 18, 1)));
        mainFunc.addChild(mainBody);
        root.addChild(mainFunc);


        return root;
    }
}
