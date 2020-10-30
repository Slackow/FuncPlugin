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
| genMcfunctionStatement
;

line
: varDefinition
| varModification
| assertLine
| functionCallLine
;

endingLine: RETURN expr | BREAK | CONTINUE;

exprBlock: expr statBlock;

statBlock: block | statement | endingLine;

block: LBRACE statement* endingLine? RBRACE;

forLoop: FOR LPAREN line? SEMI expr? SEMI line? RPAREN statBlock;

forEachLoop: FOR idenList IN expr statBlock
| FOR LPAREN idenList IN expr RPAREN statBlock;

command: ;

whileLoop: WHILE exprBlock;

doWhileLoop: DO statBlock WHILE expr SEMI;

ifStatement: IF exprBlock (ELSE statBlock)?;

defineFunctionStatement: FUNCTION IDEN LPAREN idenList? RPAREN statBlock;

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
: LPAREN expr RPAREN #parExpr

| <assoc=right> expr POW expr #powExpr
| expr op=(MULT | DIV) expr #multExpr
| expr op=(PLUS | MINUS) expr #addExpr

| LBRACE (objectPart (COMMA objectPart)*)? RBRACE #objectAtom
| LBRACKET exprList? RBRACKET #listAtom
| string #stringAtom
| NUM #numAtom
| BOOL #boolAtom
| IDEN #varName
;

objectPart: (IDEN | string) COLON expr;

string: OPEN_STRING stringPart* CLOSE_STRING;

stringPart: TEXT | ESCAPE | ID_INTERP | EXPR_INTERP expr RBRACE;