package grammar.custom;

import asm.*;
import grammar.gen.VYPeParserBaseVisitor;
import grammar.gen.VYPeParserParser;
import org.antlr.v4.runtime.Token;
import util.Constant;
import util.ISA;
import values.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Filip on 29-Oct-16.
 */
public class VYPeExpressionLow extends VYPeParserBaseVisitor<ASMVariable> {

    ASMProgram program;
    ASMRegisterAllocator regAlloc;

//    private enum Operation {
//        Addition,
//        Subtraction,
//        Multiplication,
//        Division,
//        Modulo,
//        GreaterThen,
//        GreaterThenEqual,
//        LessThen,
//        LessThenEqual,
//        Equal,
//        And,
//        Or
//    }

    public VYPeExpressionLow(ASMProgram program, ASMRegisterAllocator regAlloc) {
        this.program = program;
        this.regAlloc = regAlloc;
    }

    @Override
    public ASMVariable visitPlusMinusLabel(VYPeParserParser.PlusMinusLabelContext ctx) {
        String op = ctx.op.getText();
        System.out.print("op: " + op + "\n");
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));
        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst = this.regAlloc.getRegister(varDst);
        ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
        ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);

        if (op.equals("+")) {
            this.program.addInstruction(ISA.ASMOpCode.ADD, regDst, regOp1, regOp2);
        }
        else if (op.equals("-")) {
            this.program.addInstruction(ISA.ASMOpCode.SUB, regDst, regOp1, regOp2);
        }
        else {
            System.err.print("Unreachable\n");
            System.exit(Constant.INTERNAL_ERROR);
        }

        this.regAlloc.killVariable(varResLeft);
        this.regAlloc.killVariable(varResRight);

        return varDst;
    }

    @Override
    public ASMVariable visitMulDivModLabel(VYPeParserParser.MulDivModLabelContext ctx) {
        String op = ctx.op.getText();
        System.out.print("op: " + op + "\n");
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));
        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst; // better locality of spilling
        ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
        ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);

        if (op.equals("*")) {
            regDst = this.regAlloc.getRegister(varDst);
            // TODO maybe replace for MULT
            this.program.addInstruction(ISA.ASMOpCode.MUL, regDst, regOp1, regOp2);
            this.regAlloc.killVariable(varResLeft);
            this.regAlloc.killVariable(varResRight);
        }
        else if (op.equals("/")) {
            this.program.addInstruction(ISA.ASMOpCode.DIV, regOp1, regOp2);
            this.regAlloc.killVariable(varResLeft);
            this.regAlloc.killVariable(varResRight);
            regDst = this.regAlloc.getRegister(varDst);
            this.program.addInstruction(ISA.ASMOpCode.MFLO, regDst);
        }
        else if (op.equals("%")) {
            this.program.addInstruction(ISA.ASMOpCode.DIV, regOp1, regOp2);
            this.regAlloc.killVariable(varResLeft);
            this.regAlloc.killVariable(varResRight);
            regDst = this.regAlloc.getRegister(varDst);
            this.program.addInstruction(ISA.ASMOpCode.MFHI, regDst);
        }
        else {
            System.err.print("Unreachable\n");
            System.exit(Constant.INTERNAL_ERROR);
        }

        return varDst;
    }

    @Override
    public ASMVariable visitGreaterLowerLabel(VYPeParserParser.GreaterLowerLabelContext ctx) {
        String op = ctx.op.getText();
        System.out.print("op: " + op + "\n");
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));
        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst; // better locality of spilling
        ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
        ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);

        if (op.equals(">") || op.equals(">=")) {
            ASMRegister temp = regOp1;
            regOp1 = regOp2;
            regOp2 = temp;
        }

        if (op.equals(">") ||  op.equals("<")) {
            regDst = this.regAlloc.getRegister(varDst);
            this.program.addInstruction(ISA.ASMOpCode.SLT, regDst, regOp1, regOp2);
            this.regAlloc.killVariable(varResLeft);
            this.regAlloc.killVariable(varResRight);
        }
        else if (op.equals(">=") ||  op.equals("<=")) {
            ASMLabel labEqual = this.program.getTempLabel();
            ASMLabel labNotEqual = this.program.getTempLabel();
            ASMImmediate imm = new ASMImmediate(1);

            this.program.addInstruction(ISA.ASMOpCode.BEQ, regOp1, regOp2, labEqual);
            regDst = this.regAlloc.getRegister(varDst);
            this.program.addInstruction(ISA.ASMOpCode.SLT, regDst, regOp1, regOp2);
            this.regAlloc.killVariable(varResLeft);
            this.regAlloc.killVariable(varResRight);
            this.program.addInstruction(ISA.ASMOpCode.J, labNotEqual);
            this.program.addLabel(labEqual);
            this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, imm);
            this.program.addLabel(labNotEqual);
        }
        else {
            System.err.print("Unreachable\n");
            System.exit(Constant.INTERNAL_ERROR);
        }

        return varDst;
    }

    @Override
    public ASMVariable visitEqualLabel(VYPeParserParser.EqualLabelContext ctx) {
        String op = ctx.op.getText();
        System.out.print("op: " + op + "\n");
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));
        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst; //better locality of spilling
        ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
        ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);

        ASMLabel labEqual = this.program.getTempLabel();
        ASMLabel labNotEqual = this.program.getTempLabel();
        ASMImmediate immFalse = new ASMImmediate(0);
        ASMImmediate immTrue = new ASMImmediate(1);

        this.program.addInstruction(ISA.ASMOpCode.BEQ, regOp1, regOp2, labEqual);
        this.regAlloc.killVariable(varResLeft);
        this.regAlloc.killVariable(varResRight);
        regDst = this.regAlloc.getRegister(varDst);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immFalse);
        this.program.addInstruction(ISA.ASMOpCode.J, labNotEqual);
        this.program.addLabel(labEqual);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immTrue);
        this.program.addLabel(labNotEqual);

        return varDst;
    }

    @Override
    public ASMVariable visitAndLabel(VYPeParserParser.AndLabelContext ctx) {
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));
        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst; // better locality of spilling
        ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
        ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);
        ASMRegister regZero = this.regAlloc.getZeroReg();

        ASMLabel labFalse = this.program.getTempLabel();
        ASMLabel labTrue = this.program.getTempLabel();
        ASMImmediate immFalse = new ASMImmediate(0);
        ASMImmediate immTrue = new ASMImmediate(1);

        this.program.addInstruction(ISA.ASMOpCode.BEQ, regZero, regOp1, labFalse);
        this.regAlloc.killVariable(varResLeft);
        this.program.addInstruction(ISA.ASMOpCode.BEQ, regZero, regOp2, labFalse);
        this.regAlloc.killVariable(varResRight);
        regDst = this.regAlloc.getRegister(varDst);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immTrue);
        this.program.addInstruction(ISA.ASMOpCode.J, labTrue);
        this.program.addLabel(labFalse);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immFalse);
        this.program.addLabel(labTrue);

        return varDst;
    }

    @Override
    public ASMVariable visitOrLabel(VYPeParserParser.OrLabelContext ctx) {
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));
        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst; // better locality of spilling
        ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
        ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);
        ASMRegister regZero = this.regAlloc.getZeroReg();

        ASMLabel labFalse = this.program.getTempLabel();
        ASMLabel labTrue = this.program.getTempLabel();
        ASMImmediate immFalse = new ASMImmediate(0);
        ASMImmediate immTrue = new ASMImmediate(1);

        this.program.addInstruction(ISA.ASMOpCode.BNE, regZero, regOp1, labTrue);
        this.regAlloc.killVariable(varResLeft);
        this.program.addInstruction(ISA.ASMOpCode.BNE, regZero, regOp2, labTrue);
        this.regAlloc.killVariable(varResRight);
        regDst = this.regAlloc.getRegister(varDst);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immFalse);
        this.program.addInstruction(ISA.ASMOpCode.J, labFalse);
        this.program.addLabel(labTrue);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immTrue);
        this.program.addLabel(labFalse);

        return varDst;
    }

    @Override
    public ASMVariable visitNegationLabel(VYPeParserParser.NegationLabelContext ctx) {
        VYPeExpressionLow lowOp = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varOp = lowOp.visit(ctx.expression());

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst; // better locality of spilling
        ASMRegister regOp = this.regAlloc.getRegister(varOp);
        ASMRegister regZero = this.regAlloc.getZeroReg();

        ASMLabel labFalse = this.program.getTempLabel();
        ASMLabel labTrue = this.program.getTempLabel();
        ASMImmediate immFalse = new ASMImmediate(0);
        ASMImmediate immTrue = new ASMImmediate(1);

        this.program.addInstruction(ISA.ASMOpCode.BEQ, regZero, regOp, labTrue);
        this.regAlloc.killVariable(varOp);
        regDst = this.regAlloc.getRegister(varDst);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immFalse);
        this.program.addInstruction(ISA.ASMOpCode.J, labFalse);
        this.program.addLabel(labTrue);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immTrue);
        this.program.addLabel(labFalse);

        return varDst;
    }

    @Override
    public ASMVariable visitIntLiteralLabel(VYPeParserParser.IntLiteralLabelContext ctx) {
        System.out.print("int literal: " + ctx.getText() + "\n");

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst = this.regAlloc.getRegister(varDst);
        ASMImmediate imm = new ASMImmediate(Integer.valueOf(ctx.getText()));
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, imm);

        return varDst;
    }

    @Override
    public ASMVariable visitCharLiteralLabel(VYPeParserParser.CharLiteralLabelContext ctx) {
        System.out.print("char literal: " + ctx.getText() + "\n");

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst = this.regAlloc.getRegister(varDst);
        // TODO does it work for escape sequence?
        int value = (int) ctx.getText().charAt(1); // because of ''
        ASMImmediate imm = new ASMImmediate(value);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, imm);

        return varDst;
    }

    @Override
    public ASMVariable visitIdentifierLabel(VYPeParserParser.IdentifierLabelContext ctx) {
        System.out.print("identifier: " + ctx.getText() + "\n");

        ASMVariable varId = this.regAlloc.checkVariable(ctx.getText());

        return varId;
    }

//    private List<ASMVariable> getFunctionCallParameters(VYPeParserParser.Function_callContext ctx) {
//        List<Value> parameterList = new ArrayList<>();
//
//        for(int i = 2, len = ctx.getChildCount() - 1; i < len; i += 2) {
//            Object obj = this.visit(ctx.getChild(i));
//            if(!(obj instanceof Type)){
//                throw new SemanticException("Invalid expression value! Line: " + ctx.start.getLine());
//            }
//
//            Value value = this.toValue((Type)obj);
//            parameterList.add(value);
//        }
//        return parameterList;
//    }
//
//    @Override
//    public ASMVariable visitFunction_call(VYPeParserParser.Function_callContext ctx) {
//        String name = ctx.getChild(0).getText();
//        List<ASMVariable> parameters = this.getFunctionCallParameters(ctx);
//
//        if (name == "print") {
//
//        }
//        else {
//            System.err.print("Function call is not supported yet\n");
//            System.exit(Constant.INTERNAL_ERROR);
//        }
//    }
}