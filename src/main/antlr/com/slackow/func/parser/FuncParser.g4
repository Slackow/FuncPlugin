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

command: (OPEN_COMMAND | NEWLINE_COMMAND) commandPart* (OPEN_FUNCTION expr? block | EXIT_COMMAND)?;

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

defineFunctionStatement: FUNCTION IDEN LPAREN idenList? RPAREN block;
defineInstanceFunctionStatement: FUNCTION expr DOT IDEN LPAREN idenList? RPAREN block;

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
: IDEN #modifiableIden
| expr LBRACKET expr RBRACKET #modifiableArray
| expr DOT IDEN #modifiableObject
;

expr
: LPAREN expr RPAREN # parExpr
| main=expr LBRACKET key=expr RBRACKET # getItemExpr
| main=expr LBRACKET start=expr? COLON end=expr? (COLON inc=expr)? RBRACKET # subExpr
| main=expr QUESTION? DOT key=IDEN # getObjectExpr
| expr LPAREN exprList? RPAREN # runFunctionExpr
| MINUS expr # negationExpr
| NOT expr # notExpr
| TYPEOF expr # typeOfExpr
| left=expr ELVIS right=expr # elvisExpr
| <assoc=right> left=expr POW right=expr # powExpr
| left=expr op=(MULT | DIV | MOD) right=expr # multExpr
| left=expr op=(PLUS | MINUS) right=expr # addExpr
| left=expr op=(LT | LE | GT | GE) right=expr # relationalExpr
| left=expr op=(EQ | NE) right=expr # equalityExpr
| left=expr IS right=expr # isExpr
| left=expr AND right=expr # andExpr
| left=expr OR right=expr # orExpr
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
| IDEN # varAtom
;

objectPart: (IDEN | string) COLON expr;

string: OPEN_STRING stringPart* CLOSE_STRING;

stringPart
: TEXT # textStringPart
| ESCAPE # escapeStringPart
| ID_INTERP # idInterpPart
| EXPR_INTERP expr RBRACE # exprInterpPart
;