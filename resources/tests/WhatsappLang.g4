grammar WhatsappLang;

// ---------------------
// Parser rules
// ---------------------

axioma
    : unidadLista EOF
    ;

unidadLista
    : unidad unidadLista
    | /* ε */
    ;

unidad
    : tipo unidadSufijo
    ;

unidadSufijo
    : MAIN OPEN_CLAUDATOR bloque CLOSE_CLAUDATOR   # MainFunction
    | ID declFuncSufijo                            # DeclarationOrFunction
    ;

declFuncSufijo
    : EQUAL_ASSIGNATION expresion LINE_DELIMITER   # VariableInitialization
    | OPEN_CLAUDATOR declFuncCuerpo CLOSE_CLAUDATOR# FunctionBody
    ;

declFuncCuerpo
    : initArray LINE_DELIMITER                     # ArrayInitialization
    | bloque                                       # FunctionInner
    ;

tipo
    : tipoBase                                     # SimpleType
    | ARRAY DE INT_VALUE tipoBase                  # ArrayType
    ;

tipoBase
    : INT                                          # IntType
    | FLOAT                                        # FloatType
    | CHAR                                         # CharType
    ;

initArray
    : valor valorSufijo                            # InitArrayElements
    ;

valorSufijo
    : ARGUMENT_SEPARATOR initArray                 # MoreArrayElements
    | /* ε */                                       # EndArray
    ;

valor
    : INT_VALUE                                    # IntLiteral
    | FLOAT_VALUE                                  # FloatLiteral
    | CHAR_VALUE                                   # CharLiteral
    ;

bloque
    : contenido bloqueSufijo                       # Block
    ;

bloqueSufijo
    : contenido bloqueSufijo                       # MoreStatements
    | /* ε */                                       # EndBlock
    ;

// El único cambio principal viene aquí:
contenido
    : tipo ID localDeclSufijo LINE_DELIMITER                     # LocalVariableDecl
    | POS INT_VALUE DE ID EQUAL_ASSIGNATION expresion LINE_DELIMITER  # PosAssignment
    | ID idContenido                                            # IdentifierStatement
    | IF OPEN_PARENTHESIS condicion CLOSE_PARENTHESIS
      OPEN_CLAUDATOR bloque CLOSE_CLAUDATOR elseSufijo          # IfStatement
    | BUCLE OPEN_PARENTHESIS condicion CLOSE_PARENTHESIS
      OPEN_CLAUDATOR bloque CLOSE_CLAUDATOR                     # LoopStatement
    | RETURN expresion LINE_DELIMITER                            # ReturnStatement
    | LINE_DELIMITER                                             # EmptyStatement
    ;

idContenido
    : EQUAL_ASSIGNATION expresion LINE_DELIMITER     # AssignOrCall
    | OPEN_PARENTHESIS CLOSE_PARENTHESIS LINE_DELIMITER  # CallNoArgs
    | (SUM | MINUS | MULTIPLY | DIVISION) expresion LINE_DELIMITER  # ContinueWithArithmetic
    ;

// Eliminadas estas reglas recursivas para evitar lookahead >1
// postExpresion, terminoSufijo, expresionSufijo

expresion
    : termino expresionSufijo
    ;

expresionSufijo
    : SUM   termino expresionSufijo
    | MINUS termino expresionSufijo
    | /* ε */
    ;

termino
    : factor terminoSufijo
    ;

terminoSufijo
    : MULTIPLY factor terminoSufijo
    | DIVISION factor terminoSufijo
    | /* ε */
    ;

factor
    : OPEN_PARENTHESIS expresion CLOSE_PARENTHESIS     # ParenExpr
    | ID                                               # IdFactor
    | valor                                            # LiteralFactor
    ;

elseSufijo
    : ELSE OPEN_CLAUDATOR bloque CLOSE_CLAUDATOR    # ElseBlock
    | /* ε */                                        # NoElse
    ;

condicion
    : comparacion ( (AND | OR) condicion )?         # Condition
    ;

comparacion
    : elemento ( (EQUAL_COMPARATION | DIFFERENT
                | BIGGER_EQUAL | LOWER_EQUAL
                | BIGGER | LOWER)
                elemento )?                         # Comparison
    ;

elemento
    : ID                                            # IdElement
    | valor                                         # LiteralElement
    ;

localDeclSufijo
    : EQUAL_ASSIGNATION expresion                   # LocalInit
    | OPEN_CLAUDATOR initArray CLOSE_CLAUDATOR      # LocalArrayInit
    | /* ε */                                        # LocalNoInit
    ;

// ---------------------
// Lexer rules
// ---------------------

INT             : 'num';
FLOAT           : 'decimal';
CHAR            : 'lletra';
ARRAY           : 'textaco';
MAIN            : 'xat';
RETURN          : 'xinpum';
LINE_DELIMITER  : 'xd';
OPEN_PARENTHESIS: '¿';
CLOSE_PARENTHESIS: '?';
OPEN_CLAUDATOR  : 'jajaj';
CLOSE_CLAUDATOR : 'jejej';
IF              : 'bro';
ELSE            : 'sino';
BUCLE           : 'tombarella';
POS             : 'pos';
DE              : 'de';
ARGUMENT_SEPARATOR: 'i';

EQUAL_ASSIGNATION: '->';
EQUAL_COMPARATION : '=';
DIFFERENT         : '!=';
BIGGER            : '>';
LOWER             : '<';
BIGGER_EQUAL      : '>=';
LOWER_EQUAL       : '<=';
SUM               : '+';
MINUS             : '-';
MULTIPLY          : '*';
DIVISION          : '/';
AND               : '&';
OR                : '|';

INT_VALUE   : [0-9]+;
FLOAT_VALUE : [0-9]+'.'[0-9]+;
CHAR_VALUE  : '\'' [a-zA-Z] '\'';
ID          : [a-zA-Z][a-zA-Z0-9]*;

WS      : [ \t\r\n]+ -> skip;
COMMENT : '///' ~[\r\n]* -> skip;
