lexer grammar FuncLexer;



AND: '&&';
OR: '||';


GT: '>';
LT: '<';
GE: '>=';
LE: '<=';
EQ: '==';
NE: '!=';
NOT: '!';


PLUSEQ: '+=';
MINUSEQ: '-=';
MULTEQ: '*=';
DIVEQ: '/=';
PLUSPLUS: '++';
MINUSMINUS: '--';

POW: '^';
MULT: '*';
DIV: '/';
MOD: '%';
PLUS: '+';
MINUS: '-';

LPAREN: '(';
RPAREN: ')';

LBRACE: '{' -> pushMode(DEFAULT_MODE);
RBRACE: '}' {
    if(_modeStack.size() > 0){
        popMode();
    }
};
LBRACKET: '[';
RBRACKET: ']';

SEMI: ';';
EQUAL: '=';
ELVIS: '?:';
QUESTION: '?';
DOT: '.';
COLON: ':';
COMMA: ',';
ARROW: '->';

OPEN_STRING: '\'' -> pushMode(STRING);
OPEN_COMMAND: NEWLINE WS? '/' -> pushMode(COMMAND);
OPEN_MULTI_COMMAND: NEWLINE WS? '/:' (NEWLINE | WS)* -> pushMode(MULTI_LINE_COMMAND), type(OPEN_COMMAND);

ASSERT: 'assert';
BREAK: 'break';
BOOL: 'true' | 'false';
CASE: 'case';
CONSTRUCTOR: 'constructor';
CONTINUE: 'continue';
DO: 'do';
ELSE: 'else';
FOR: 'for';
GEN: 'gen';
IF: 'if';
IN: 'in';
IS: 'is';
FUNCTION: 'function';
NULL: 'null';
NEW: 'new';
RETURN: 'return';
SWITCH: 'switch';
THIS: 'this';
TICK: 'tick';
THROW: 'throw';
TYPEOF: 'typeof';
UNDEFINED: 'undefined';
VAR: 'var';
WHILE: 'while';

NUM: PINT ('.' PINT)?;
fragment PINT: [0-9] [0-9_]*;

IDEN: [a-zA-Z] [a-zA-Z0-9_]*;

WS: [ \t]+ -> channel(HIDDEN);
NEWLINE: ('\r'? '\n' | '\r') -> channel(HIDDEN);
COMMENT: '#' ~[\r\n]* -> channel(HIDDEN);
ERR_TOKEN: . -> channel(HIDDEN);

mode STRING;

EXPR_INTERP: '${' -> pushMode(DEFAULT_MODE);
ID_INTERP: '$' IDEN;

ESCAPE: '\\' (["'\\$rn] | 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT);
fragment HEX_DIGIT: [0-9a-fA-F];
TEXT: (~[$\r\n\\']+) | '$';

CLOSE_STRING: OPEN_STRING -> popMode;
ERR_TOKEN_STRING: . -> channel(HIDDEN);


mode COMMAND;
EXPR_INTERP_COMMAND: '${' -> pushMode(DEFAULT_MODE);

OPEN_FUNCTION: '$' FUNCTION -> popMode;
OPEN_FUNCTION_NAME: '$\'' -> pushMode(STRING);
THIS_FUNCTION: '$' THIS;
ID_INTERP_COMMAND: '$' IDEN;
TEXT_COMMAND: ('\\$' | ~[$\r\n])+ | '$';
OPEN_MULTI_COMMAND_COM: OPEN_MULTI_COMMAND -> popMode, pushMode(MULTI_LINE_COMMAND), type(NEWLINE_COMMAND);

NEWLINE_COMMAND: OPEN_COMMAND;
EXIT_COMMAND: NEWLINE -> popMode;

ERR_TOKEN_COMMAND: . -> channel(HIDDEN);

mode MULTI_LINE_COMMAND;
EXIT_MULTI_COMMAND: NEWLINE* WS? ':/' -> popMode, type(EXIT_COMMAND);
ID_INTERP_MULTI_COMMAND: ID_INTERP_COMMAND -> type(ID_INTERP_COMMAND);

EXPR_MULTI_INTERP: EXPR_INTERP -> pushMode(DEFAULT_MODE), type(EXPR_INTERP);

OPEN_MULTI_FUNCTION_NAME: OPEN_FUNCTION_NAME -> pushMode(STRING), type(OPEN_FUNCTION_NAME);
THIS_MULTI_FUNCTION: THIS_FUNCTION -> type(THIS_FUNCTION);

TEXT_MULTI_COMMAND: (('\\$' | ':' ~[$\r\n/] | ~[$\r\n:]+)+ | '$') -> type(TEXT_COMMAND);
CONTINUE_COMMAND: WS? NEWLINE WS?;

ERR_TOKEN_MULTI_COMMAND: . -> channel(HIDDEN);