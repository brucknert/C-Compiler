/*************************************************************
* Filename: Parser.g4
* Project: Compiler Implementation for VYPe16 Programming Language
* Compiler Team: 04
* Authors: Filip Benna, xbenna01
*          Tomas Bruckner, xbruck02
* Date: 28/9/2016
*************************************************************/

grammar VYPeParser;
import VYPeLexer;

any_value:
    Identifier
    | IntLiteral
    | CharLiteral
    | StringLiteral ;

number_value:
    Identifier
    | IntLiteral ;

data_type:
    Int
    | Char
    | String ;

type:
    data_type
    | Void ;

function_identifier:
    Identifier ;

parse: start ;

start:
    (function_declaration | function_definition)+ EOF;


/************************************ FUNCTION DECLARATION ******************************************************/
function_declaration:
    type Identifier LeftParenthesis param_type_list RightParenthesis Semicolon ;

param_type_list:
    data_type (Colon data_type)*
    | Void ;


/***************************************FUNCTION DEFINITION ******************************************************/
function_definition:
    type Identifier LeftParenthesis param_list RightParenthesis block_statements ;

param_list:
    data_type Identifier (Colon data_type Identifier)*
    | Void ;

/******************************************* STATEMENTS ******************************************************/
statement:
    variable_definition_statement
    | assignment_statement
    | conditional_statement
    | while_statement
    | function_call_statement
    | return_statement ;

variable_definition_statement:
    data_type Identifier (Colon Identifier)* Semicolon ;

assignment_statement:
    Identifier AssignSign expression Semicolon ;

conditional_statement:
    If condition_expression block_statements Else block_statements ;

while_statement:
    While condition_expression block_statements ;

function_call_statement:
    function_call Semicolon ;

return_statement:
    Return (expression)? Semicolon ;

/******************************************* HELP RULES ******************************************************/
block_statements:
    LeftCurlyParenthesis (statement)* RightCurlyParenthesis ;

function_call:
    function_identifier LeftParenthesis (expression (Colon expression)*)? RightParenthesis ;

condition_expression:
    LeftParenthesis expression RightParenthesis ;

/******************************************* EXPRESSION ******************************************************/
expression:
    any_value
    | LeftParenthesis expression RightParenthesis
    | LeftParenthesis data_type RightParenthesis expression
    | function_call
    | ExclamationMark expression
    | expression op=(MultiplicationSign | DivisionSign | ModuloSign) expression
    | expression op=(PlusSign | MinusSign) expression
    | expression op=(LowerSign | GreaterSign | LowerEqualSign | GreaterEqualSign) expression
    | expression op=(EqualSign | NotEqualSign) expression
    | expression LogicalAndSign expression
    | expression LogicalOrSign expression ;
