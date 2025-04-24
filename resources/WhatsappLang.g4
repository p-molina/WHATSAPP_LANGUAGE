grammar WhatsappLang;


axioma: vars_globals funcions crea_main EOF;

vars_globals: (declaracio vars_globals) | ;
funcions: (crea_funcio funcions) | ;
crea_funcio: tipus ID func_body;
crea_main: INT MAIN func_body;

declaracio: tipus ID declaracio_prim LINE_DELIMITER;
declaracio_prim: EQUAL_ASSIGNATION valor
               | OPEN_CLAUDATOR init_array CLOSE_CLAUDATOR
               | ;
init_array: valor valor_succ;
valor: INT_VALUE | FLOAT_VALUE | CHAR_VALUE | ID;
valor_succ: ARGUMENT_SEPARATOR init_array | ;

assignacio: assignacio_prim expressio LINE_DELIMITER;
assignacio_prim: ID EQUAL_ASSIGNATION
               | POS INT_VALUE DE ID EQUAL_ASSIGNATION;

tipus_base: INT | FLOAT | CHAR;
tipus: tipus_base | ARRAY DE INT_VALUE tipus_base;

valor_return: expressio | valor | POS INT_VALUE DE ID | ID;

func_body: OPEN_CLAUDATOR body CLOSE_CLAUDATOR;
handle_return: RETURN valor_return LINE_DELIMITER;

expressio: terme expressio_succ;
expressio_succ: SUM terme expressio_succ
              | MINUS terme expressio_succ
              | ;
terme: factor terme_succ;
terme_succ: MULTIPLY factor terme_succ
          | DIVISION factor terme_succ
          | ;

factor: OPEN_PARENTESIS expressio CLOSE_PARENTESIS
      | ID
      | valor;

condicional: IF OPEN_PARENTESIS condicio CLOSE_PARENTESIS OPEN_CLAUDATOR body CLOSE_CLAUDATOR condicional_succ;
condicional_succ: ELSE OPEN_CLAUDATOR body CLOSE_CLAUDATOR | ;
condicio: comparacio condicio_succ;
condicio_succ: token_concatenacio condicio | ;
comparacio: element comparacio_succ;
comparacio_succ: token_condicional element | ;
element: ID | valor;

token_condicional: EQUAL_COMPARATION
                 | DIFFERENT
                 | BIGGER
                 | LOWER
                 | BIGGER_EQUAL
                 | LOWER_EQUAL
                 | ;

token_concatenacio: AND | OR | ;

bucle: BUCLE OPEN_PARENTESIS condicio CLOSE_PARENTESIS OPEN_CLAUDATOR body CLOSE_CLAUDATOR;

body: content body_succ;
body_succ: content body_succ | ;
content: designacio
       | expressio LINE_DELIMITER
       | condicional
       | bucle
       | call_funcio
       | handle_return
       | LINE_DELIMITER;

designacio: assignacio | call_funcio;

call_funcio: ID OPEN_PARENTESIS CLOSE_PARENTESIS LINE_DELIMITER;

// LEXER RULES
INT: 'num';
FLOAT: 'decimal';
CHAR: 'lletra';
ARRAY: 'textaco';
LINE_DELIMITER: 'xd';
OPEN_PARENTESIS: '¿';
CLOSE_PARENTESIS: '?';
OPEN_CLAUDATOR: 'jajaj';
CLOSE_CLAUDATOR: 'jejej';
IF: 'bro';
ELSE: 'sino';
BUCLE: 'tombarella';
MAIN: 'xat';
RETURN: 'xinpum';
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
NEGATION: '!';
SUM: '+';
MINUS: '-';
MULTIPLY: '*';
DIVISION: '/';
AND: '&';
OR: '|';

INT_VALUE: [0-9]+;
FLOAT_VALUE: [0-9]+'.'[0-9]+;
CHAR_VALUE: '\''[a-zA-Z]'\'';
ID: [a-zA-Z][a-zA-Z0-9]*;

WS: [ \t\r\n]+ -> skip;