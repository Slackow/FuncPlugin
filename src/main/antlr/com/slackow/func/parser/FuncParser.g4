parser grammar FuncParser;
options {tokenVocab = FuncLexer;}

program: statement* EOF;

statement: line SEMI;

line: varDefinition;

varDefinition: VAR IDEN EQUAL expr;

expr: IDEN;