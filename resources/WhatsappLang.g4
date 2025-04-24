grammar WhatsappLang;

axioma: unit_list EOF;

unit_list: unit unit_list | ;

unit: tipus unit_tail;

unit_tail
    : MAIN OPEN_CLAUDATOR body CLOSE_CLAUDATOR
    | ID decl_or_func_tail;

decl_or_func_tail
    : EQUAL_ASSIGNATION expressio LINE_DELIMITER
    | OPEN_CLAUDATOR decl_or_func_tail_rest;

decl_or_func_tail_rest
    : init_array CLOSE_CLAUDATOR LINE_DELIMITER
    | body CLOSE_CLAUDATOR;

tipus: tipus_base
     | ARRAY DE INT_VALUE tipus_base;

tipus_base: INT | FLOAT | CHAR;

init_array: valor valor_succ;
valor_succ: ARGUMENT_SEPARATOR init_array | ;
valor: INT_VALUE | FLOAT_VALUE | CHAR_VALUE;

body: content body_succ;
body_succ: content body_succ | ;

content
    : POS INT_VALUE DE ID EQUAL_ASSIGNATION expressio LINE_DELIMITER
    | ID id_content
    | IF OPEN_PARENTESIS condicio CLOSE_PARENTESIS OPEN_CLAUDATOR body CLOSE_CLAUDATOR condicional_succ
    | BUCLE OPEN_PARENTESIS condicio CLOSE_PARENTESIS OPEN_CLAUDATOR body CLOSE_CLAUDATOR
    | RETURN expressio LINE_DELIMITER
    | LINE_DELIMITER;

id_content
    : EQUAL_ASSIGNATION expressio LINE_DELIMITER
    | OPEN_PARENTESIS CLOSE_PARENTESIS LINE_DELIMITER
    | post_id_expr LINE_DELIMITER;

post_id_expr: terme_succ expressio_succ;

expressio: terme expressio_succ;
expressio_succ
    : SUM terme expressio_succ
    | MINUS terme expressio_succ
    | ;

terme: factor terme_succ;
terme_succ
    : MULTIPLY factor terme_succ
    | DIVISION factor terme_succ
    | ;

factor
    : OPEN_PARENTESIS expressio CLOSE_PARENTESIS
    | ID
    | valor;

condicio: comparacio condicio_succ;
condicio_succ: token_concatenacio condicio | ;

comparacio: element comparacio_succ;
comparacio_succ: token_condicional element | ;

element: ID | valor;

condicional_succ: ELSE OPEN_CLAUDATOR body CLOSE_CLAUDATOR | ;

token_condicional
    : EQUAL_COMPARATION
    | DIFFERENT
    | BIGGER
    | LOWER
    | BIGGER_EQUAL
    | LOWER_EQUAL;

token_concatenacio: AND | OR;

// LEXER RULES
INT: 'num';
FLOAT: 'decimal';
CHAR: 'lletra';
ARRAY: 'textaco';
MAIN: 'xat';
RETURN: 'xinpum';
LINE_DELIMITER: 'xd';
OPEN_PARENTESIS: '¿';
CLOSE_PARENTESIS: '?';
OPEN_CLAUDATOR: 'jajaj';
CLOSE_CLAUDATOR: 'jejej';
IF: 'bro';
ELSE: 'sino';
BUCLE: 'tombarella';
POS: 'pos';
DE: 'de';
ARGUMENT_SEPARATOR: 'i';

EQUAL_ASSIGNATION: '->';
EQUAL_COMPARATION: '=';
DIFFERENT: '!=';
BIGGER: '>';
LOWER: '<';
BIGGER_EQUAL: '>=';
LOWER_EQUAL: '<=';
SUM: '+';
MINUS: '-';
MULTIPLY: '*';
DIVISION: '/';
AND: '&';
OR: '|';

INT_VALUE: [0-9]+;
FLOAT_VALUE: [0-9]+ '.' [0-9]+;
CHAR_VALUE: '\''[a-zA-Z]'\'';
ID: [a-zA-Z][a-zA-Z0-9]*;

WS: [ \t\r\n]+ -> skip;
