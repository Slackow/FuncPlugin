parser grammar FuncParser;
options {tokenVocab = FuncLexer;}
program: statement* EOF;

statement
: line SEMI
| command
| ifStatement
| forLoop
| forEachLoop
| whileLoop
| doWhileLoop
| defineFunctionStatement
| defineInstanceFunctionStatement
| genMCfunctionStatement
;

line
: varDefinition
| varModification
| assertLine
| functionCallLine
;

genMCfunctionStatement: GEN FUNCTION exprBlock;

endingLine: (RETURN expr? | BREAK | CONTINUE) SEMI;

exprBlock: expr statBlock;

statBlock: block | statement | endingLine;

block: LBRACE statement* endingLine? RBRACE;

forLoop: FOR LPAREN first=line? SEMI condition=expr? SEMI last=line? RPAREN statBlock;

forEachLoop: FOR IDEN (COMMA IDEN)? IN expr statBlock
| FOR LPAREN IDEN (COMMA IDEN)? IN expr RPAREN statBlock;

command: (OPEN_COMMAND | NEWLINE_COMMAND) commandPart* (OPEN_FUNCTION expr? statBlock | EXIT_COMMAND)?;

commandPart
: TEXT_COMMAND  # commandText
| ID_INTERP_COMMAND  # commandIdInterpPart
| EXPR_INTERP_COMMAND expr RBRACE  # commandExprInterpPart
| CONTINUE_COMMAND  # commandGoOnPart
| OPEN_FUNCTION_NAME stringPart* CLOSE_STRING  # functionReferencePart
| THIS_FUNCTION  # thisFunctionPart
;

whileLoop: WHILE exprBlock;

doWhileLoop: DO statBlock WHILE expr SEMI;

ifStatement: IF exprBlock (ELSE statBlock)?;

defineFunctionStatement: FUNCTION IDEN LPAREN idenList? RPAREN statBlock;
defineInstanceFunctionStatement: FUNCTION expr DOT IDEN LPAREN idenList? RPAREN statBlock;

varDefinition: VAR idenList EQUAL expr;

varModification
: modifiableExpr op=(EQUAL | PLUSEQ | MINUSEQ | MULTEQ | DIVEQ) expr
| modifiableExpr op=(PLUSPLUS | MINUSMINUS)
;

assertLine: ASSERT expr;

functionCallLine: expr LPAREN exprList? RPAREN;

exprList: expr (COMMA expr)*;

idenList: IDEN (COMMA IDEN)*;

modifiableExpr
: IDEN
| expr LBRACKET expr RBRACKET
| expr DOT IDEN;

expr
: LPAREN expr RPAREN # parExpr
| expr LBRACKET expr RBRACKET # getItemExpr
| expr LBRACKET expr COLON expr (COLON expr)? RBRACKET # subExpr
| expr QUESTION? DOT IDEN # getObjectExpr
| expr LPAREN exprList? RPAREN # runFunctionExpr
| MINUS expr # negationExpr
| NOT expr # notExpr
| TYPEOF expr # typeOfExpr
| expr ELVIS expr # elvisExpr
| <assoc=right> expr POW expr # powExpr
| expr op=(MULT | DIV | MOD) expr # multExpr
| expr op=(PLUS | MINUS) expr # addExpr
| expr op=(LT | LE | GT | GE) expr # relationalExpr
| expr op=(EQ | NE) expr # equalityExpr
| expr IS expr # isExpr
| expr AND expr # andExpr
| expr OR expr # orExpr
| condition=expr QUESTION left=expr COLON right=expr # ternaryExpr

| (IDEN | LPAREN idenList RPAREN) ARROW (expr | block) # lambdaAtom
| LBRACE (objectPart (COMMA objectPart)*)? RBRACE # objectAtom
| LBRACKET exprList? RBRACKET # listAtom
| string # stringAtom
| NUM # numAtom
| NULL # nullAtom
| UNDEFINED #undefinedAtom
| THIS # thisAtom
| BOOL # boolAtom
| IDEN # varName
;

objectPart: (IDEN | string) COLON expr;

string: OPEN_STRING stringPart* CLOSE_STRING;

stringPart
: TEXT # textStringPart
| ESCAPE # escapeStringPart
| ID_INTERP # idInterpPart
| EXPR_INTERP expr RBRACE # exprInterpPart
;