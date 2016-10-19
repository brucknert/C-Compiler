import exceptions.SemanticException;
import grammar.custom.VYPeExpressionVisitor;
import tables.FunctionTable;
import tables.SymbolTable;
import util.Constant;
import grammar.gen.*;
import org.antlr.v4.runtime.*;

/*************************************************************
 * Filename: Main.java
 * Project: Compiler Implementation for VYPe16 Programming Language
 * Compiler Team: 04
 * Authors: Filip Benna, xbenna01
 *          Tomas Bruckner, xbruck02
 * Date: 28/9/2016
 *************************************************************/

public class Main {
    public static void main(String[] args) {
        args = new String[] {"", "tests/valid/testcase00.c"};
        if(args.length < 2 || args.length > 3){
            System.exit(Constant.INTERNAL_ERROR);
        }

        String outputFilename = (args.length == 3) ? args[2] : "out.asm";
        VYPeLexer lexer = null;
        try{
            lexer = new VYPeLexer(new ANTLRFileStream(args[1]));
        }catch(java.io.IOException e){
            System.exit(Constant.INTERNAL_ERROR);
        }

        CommonTokenStream tokenStream = new CommonTokenStream (lexer);

        tokenStream.fill();

        // must be final in lambda
        final int errorTokenType = lexer.getTokenType("Error");
        tokenStream.getTokens().forEach(token -> {
            if(token.getType() == errorTokenType){
                System.exit(Constant.LEXICAL_ERROR);
            }
        });

        VYPeParserParser parser = new VYPeParserParser(tokenStream);
        VYPeParserParser.StartContext parseTree = parser.start();

        if(parser.getNumberOfSyntaxErrors() > 0) {
            System.exit(Constant.SYNTAX_ERROR);
        }

        VYPeExpressionVisitor visitor = new VYPeExpressionVisitor("", new FunctionTable(), new SymbolTable());

        try {
            visitor.doSemanticCheck(parseTree);
        }
        catch(SemanticException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(Constant.SEMANTIC_ERROR);
        }

        System.exit(Constant.NO_ERROR);
    }
}
