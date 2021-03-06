package grammar.custom;

import asm.*;
import grammar.gen.VYPeParserBaseVisitor;
import grammar.gen.VYPeParserParser;
import util.Constant;
import util.ISA;
import util.Utility;

/*************************************************************
 * Filename: VYPeStatementLow.java
 * Project: Compiler Implementation for VYPe16 Programming Language
 * Compiler Team: 04
 * Authors: Filip Benna, xbenna01
 *          Tomas Bruckner, xbruck02
 * Date: 29/10/2016
 *************************************************************/

public class VYPeStatementLow extends VYPeParserBaseVisitor<Void> {

    ASMProgram program;
    ASMRegisterAllocator regAlloc;

    public VYPeStatementLow(ASMProgram program, ASMRegisterAllocator regAlloc) {
        this.program = program;
        this.regAlloc = regAlloc;
    }

    @Override
    public Void visitVariable_definition_statement(VYPeParserParser.Variable_definition_statementContext ctx) {
        Constant.Type type = Utility.getType(ctx.getChild(0).getText());
        for(int i = 1, len = ctx.getChildCount() - 1; i < len; i += 2){
            String name = ctx.getChild(i).getText();
//            System.out.print("declaration of " + name + "\n");
            this.regAlloc.declareVariable(name, type);

            ASMVariable varNew = this.regAlloc.checkVariable(name);
            ASMRegister regNew = this.regAlloc.getRegister(varNew);
            ASMRegister regZero = this.regAlloc.getZeroReg();
            String comment = name + "(" + regNew.getText() + ") = default value";

            if (type == Constant.Type.INT || type == Constant.Type.CHAR) {
                this.program.addInstruction(ISA.ASMOpCode.MOV, regNew, regZero, comment);
            }
            else if (type == Constant.Type.STRING) {
                ASMRegister regGlobalPtr = this.regAlloc.getGlobalPtrReg();
                ASMImmediate immOne = new ASMImmediate(1);
                ASMImmediate immOffset = new ASMImmediate(0);
                this.program.addInstruction(ISA.ASMOpCode.MOV, regNew, regGlobalPtr, comment);
                // store zero so that simulator does not print warnings
                this.program.addInstruction(ISA.ASMOpCode.SB, regZero, immOffset, regGlobalPtr);
                this.program.addInstruction(ISA.ASMOpCode.ADDU, regGlobalPtr, immOne);
            }
            else {
                System.err.print("Unreachable\n");
                System.exit(Constant.INTERNAL_ERROR);
            }
        }
        return null;
    }

    @Override
    public Void visitAssignment_statement(VYPeParserParser.Assignment_statementContext ctx) {
//        System.out.print("assignment of " + ctx.Identifier().getText() + "\n");
        VYPeExpressionLow exprLow = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varRes = exprLow.visit(ctx.expression());

        ASMVariable varNew = this.regAlloc.checkVariable(ctx.Identifier().getText());
        ASMRegister regRes = this.regAlloc.getRegister(varRes);
        ASMRegister regNew = this.regAlloc.getRegister(varNew);

        String comment = ctx.Identifier().getText() + "(" + regNew.getText() + ") = " + regRes.getText();
        this.program.addInstruction(ISA.ASMOpCode.MOV, regNew, regRes, comment);
        this.regAlloc.killVariable(varRes);
        return null;
    }

    @Override
    public Void visitReturn_statement(VYPeParserParser.Return_statementContext ctx) {
//        System.out.print("return\n");
        VYPeExpressionLow exprLow = new VYPeExpressionLow(this.program, this.regAlloc);
        boolean returnsVal = (ctx.expression() == null) ? false : true;

        if (returnsVal) {
            ASMVariable varRes = exprLow.visit(ctx.expression());

            ASMRegister regReturn = this.regAlloc.getReturnValReg();
            ASMRegister regZero = this.regAlloc.getZeroReg();
            ASMRegister regRes = this.regAlloc.getRegister(varRes);

            this.program.addInstruction(ISA.ASMOpCode.MOVZ, regReturn, regRes, regZero);
            this.regAlloc.killVariable(varRes);
        }

        ASMRegister regRet = this.regAlloc.getReturnAddrReg();
        ASMRegister regStackPtr = this.regAlloc.getStackPtrReg();
        ASMRegister regFramePtr = this.regAlloc.getFramePtrReg();

        this.program.addInstruction(ISA.ASMOpCode.MOV, regStackPtr, regFramePtr);
        this.program.addInstruction(ISA.ASMOpCode.JR, regRet);

        this.regAlloc.setReturn();

        return null;
    }

    @Override
    public Void visitConditional_statement(VYPeParserParser.Conditional_statementContext ctx) {
        VYPeExpressionLow lowCond = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varCond = lowCond.visit(ctx.condition_expression().expression());

        ASMRegister regCond = this.regAlloc.getRegister(varCond);
        ASMRegister regZero = this.regAlloc.getZeroReg();

        ASMLabel labFalse = this.program.getTempLabel();
        ASMLabel labTrue = this.program.getTempLabel();

        String comIf = "If statement";
        this.program.addInstruction(ISA.ASMOpCode.BEQ, regCond, regZero, labFalse, comIf);
        this.regAlloc.killVariable(varCond);
        VYPeBlockLow lowTrue = new VYPeBlockLow(this.program, this.regAlloc);
        lowTrue.visit(ctx.block_statements(0));
        this.program.addInstruction(ISA.ASMOpCode.J, labTrue);
        String comFalse = "If false";
        this.program.addLabel(labFalse, comFalse);
        VYPeBlockLow lowFalse = new VYPeBlockLow(this.program, this.regAlloc);
        lowFalse.visit(ctx.block_statements(1));
        String comTrue = "If end";
        this.program.addLabel(labTrue, comTrue);

        return null;
    }

    @Override
    public Void visitWhile_statement(VYPeParserParser.While_statementContext ctx) {
        VYPeExpressionLow lowCond = new VYPeExpressionLow(this.program, this.regAlloc);

        ASMRegister regZero = this.regAlloc.getZeroReg();

        String comBegin = "While loop";
        String comEnd = "Loop end";
        ASMLabel labBegin = this.program.getTempLabel(comBegin);
        ASMLabel labEnd = this.program.getTempLabel(comEnd);

        this.program.addLabel(labBegin);
        // compute the condition in each loop
        ASMVariable varCond = lowCond.visit(ctx.condition_expression().expression());
        ASMRegister regCond = this.regAlloc.getRegister(varCond);
        // test the condition
        this.program.addInstruction(ISA.ASMOpCode.BEQ, regCond, regZero, labEnd);
        this.regAlloc.killVariable(varCond);
        //compute the body of the loop
        VYPeBlockLow lowBody = new VYPeBlockLow(this.program, this.regAlloc);
        lowBody.visit(ctx.block_statements());
        // jump back to the beginning of the loop
        this.program.addInstruction(ISA.ASMOpCode.J, labBegin);
        this.program.addLabel(labEnd);

        return null;
    }

    @Override
    public Void visitFunction_call_statement(VYPeParserParser.Function_call_statementContext ctx) {
        VYPeExpressionLow lowFuncCall = new VYPeExpressionLow(this.program, this.regAlloc);
        lowFuncCall.visitFunction_call(ctx.function_call());

        return null;
    }
}
