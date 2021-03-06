package grammar.custom;

import asm.*;
import exceptions.SemanticException;
import grammar.gen.VYPeParserBaseVisitor;
import grammar.gen.VYPeParserParser;
import util.Constant;
import util.ISA;
import util.Utility;
import values.Value;

import java.util.ArrayList;
import java.util.List;

/*************************************************************
 * Filename: VYPeExpressionLow.java
 * Project: Compiler Implementation for VYPe16 Programming Language
 * Compiler Team: 04
 * Authors: Filip Benna, xbenna01
 *          Tomas Bruckner, xbruck02
 * Date: 29/10/2016
 *************************************************************/

public class VYPeExpressionLow extends VYPeParserBaseVisitor<ASMVariable> {

    ASMProgram program;
    ASMRegisterAllocator regAlloc;

    public VYPeExpressionLow(ASMProgram program, ASMRegisterAllocator regAlloc) {
        this.program = program;
        this.regAlloc = regAlloc;
    }

    @Override
    public ASMVariable visitExpressionLabel(VYPeParserParser.ExpressionLabelContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public ASMVariable visitPlusMinusLabel(VYPeParserParser.PlusMinusLabelContext ctx) {
        String op = ctx.op.getText();
//        System.out.print("op: " + op + "\n");
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));
        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
        ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);
        ASMRegister regDst = this.regAlloc.getRegister(varDst);

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
//        System.out.print("op: " + op + "\n");
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
//        System.out.print("op: " + op + "\n");
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));

        VYPePrintVisitor visitor = new VYPePrintVisitor(this.program.getFunctionTable(), this.regAlloc);
        Constant.Type type =  visitor.getExpressionType(ctx.expression(0));

        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        ASMVariable varDst = this.regAlloc.getTempVar();

        if (op.equals(">") || op.equals(">=")) {
            ASMVariable temp = varResLeft;
            varResLeft = varResRight;
            varResRight = temp;
        }

        if (type == Constant.Type.INT || type == Constant.Type.CHAR) {
            ASMRegister regDst; // better locality of spilling
            ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
            ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);

//            if (op.equals(">") || op.equals(">=")) {
//                ASMRegister temp = regOp1;
//                regOp1 = regOp2;
//                regOp2 = temp;
//            }

            if (op.equals(">") || op.equals("<")) {
                regDst = this.regAlloc.getRegister(varDst);
                this.program.addInstruction(ISA.ASMOpCode.SLT, regDst, regOp1, regOp2);
                this.regAlloc.killVariable(varResLeft);
                this.regAlloc.killVariable(varResRight);
            } else if (op.equals(">=") || op.equals("<=")) {
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
            } else {
                System.err.print("Unreachable\n");
                System.exit(Constant.INTERNAL_ERROR);
            }
        }
        else if (type == Constant.Type.STRING) {
            if (op.equals(">") || op.equals("<")) {
                varDst = this.compareStrings(varResLeft, varResRight, 0, 1, 0);
            }
            else if (op.equals(">=") || op.equals("<=")) {
                varDst = this.compareStrings(varResLeft, varResRight, 0, 1, 1);
            }
            else {
                System.err.print("Unreachable\n");
                System.exit(Constant.INTERNAL_ERROR);
            }
        }
        else {
            System.err.print("Unreachable\n");
            System.exit(Constant.INTERNAL_ERROR);
        }

        return varDst;
    }

    private ASMVariable compareStrings(ASMVariable varOp1, ASMVariable varOp2, int op1Gr, int op2Gr, int opEq) {
        ASMVariable varPtr1 = this.regAlloc.getTempVar();
        ASMVariable varPtr2 = this.regAlloc.getTempVar();
        ASMVariable varChar1 = this.regAlloc.getTempVar();
        ASMVariable varChar2 = this.regAlloc.getTempVar();
        ASMVariable varRes = this.regAlloc.getTempVar();

        ASMRegister regOp1 = this.regAlloc.getRegister(varOp1);
        ASMRegister regOp2 = this.regAlloc.getRegister(varOp2);
        ASMRegister regPtr1 = this.regAlloc.getRegister(varPtr1);
        ASMRegister regPtr2 = this.regAlloc.getRegister(varPtr2);
        ASMRegister regChar1 = this.regAlloc.getRegister(varChar1);
        ASMRegister regChar2 = this.regAlloc.getRegister(varChar2);
        ASMRegister regZero = this.regAlloc.getZeroReg();
        ASMRegister regRes;

        ASMImmediate immZero = new ASMImmediate(0);
        ASMImmediate immOne = new ASMImmediate(1);
        ASMImmediate immOp1Gr = new ASMImmediate(op1Gr);
        ASMImmediate immOp2Gr = new ASMImmediate(op2Gr);
        ASMImmediate immOpEq = new ASMImmediate(opEq);

        ASMLabel labStart = this.program.getTempLabel();
        ASMLabel labOp1Gr = this.program.getTempLabel();
        ASMLabel labOp2Gr = this.program.getTempLabel();
        ASMLabel labEqual = this.program.getTempLabel();
        ASMLabel labEnd = this.program.getTempLabel();

        // backup the string pointers
        this.program.addInstruction(ISA.ASMOpCode.MOV, regPtr1, regOp1);
        this.program.addInstruction(ISA.ASMOpCode.MOV, regPtr2, regOp2);
        // start the comparison
        String comment = "string compare start";
        this.program.addLabel(labStart, comment);
        this.program.addInstruction(ISA.ASMOpCode.LBU, regChar1, immZero, regPtr1);
        this.program.addInstruction(ISA.ASMOpCode.LBU, regChar2, immZero, regPtr2);
        this.program.addInstruction(ISA.ASMOpCode.SUB, regChar1, regChar1, regChar2);
        // op1 is greater
        this.program.addInstruction(ISA.ASMOpCode.BGTZ, regChar1, labOp1Gr);
        // op2 is greater
        this.program.addInstruction(ISA.ASMOpCode.BLTZ, regChar1, labOp2Gr);
        this.regAlloc.killVariable(varChar1);
        // is it end of the strings
        this.program.addInstruction(ISA.ASMOpCode.BEQ, regChar2, regZero, labEqual); // char1 is diff, char2 is the value
        this.regAlloc.killVariable(varChar2);
        this.program.addInstruction(ISA.ASMOpCode.ADDU, regPtr1, immOne);
        this.program.addInstruction(ISA.ASMOpCode.ADDU, regPtr2, immOne);
        this.regAlloc.killVariable(varPtr1);
        this.regAlloc.killVariable(varPtr2);
        this.program.addInstruction(ISA.ASMOpCode.J, labStart);
        // store the result
        regRes = this.regAlloc.getRegister(varRes);
        comment = "string 1 greater";
        this.program.addLabel(labOp1Gr, comment);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regRes, immOp1Gr);
        this.program.addInstruction(ISA.ASMOpCode.J, labEnd);
        comment = "string 2 greater";
        this.program.addLabel(labOp2Gr, comment);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regRes, immOp2Gr);
        this.program.addInstruction(ISA.ASMOpCode.J, labEnd);
        comment = "strings equal";
        this.program.addLabel(labEqual, comment);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regRes, immOpEq);
        comment = "string compare end";
        this.program.addLabel(labEnd, comment);

        return varRes;
    }

    @Override
    public ASMVariable visitEqualLabel(VYPeParserParser.EqualLabelContext ctx) {
        String op = ctx.op.getText();
//        System.out.print("op: " + op + "\n");
        VYPeExpressionLow lowLeft = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResLeft = lowLeft.visit(ctx.expression(0));
        VYPeExpressionLow lowRight = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varResRight = lowRight.visit(ctx.expression(1));

        VYPePrintVisitor visitor = new VYPePrintVisitor(this.program.getFunctionTable(), this.regAlloc);
        Constant.Type type =  visitor.getExpressionType(ctx.expression(0));

        ASMVariable varDst = this.regAlloc.getTempVar();

        if (type == Constant.Type.INT || type == Constant.Type.CHAR) {
            ASMRegister regDst; //better locality of spilling
            ASMRegister regOp1 = this.regAlloc.getRegister(varResLeft);
            ASMRegister regOp2 = this.regAlloc.getRegister(varResRight);

            ASMLabel labEqual = this.program.getTempLabel();
            ASMLabel labNotEqual = this.program.getTempLabel();
            ASMImmediate immFalse = new ASMImmediate(0);
            ASMImmediate immTrue = new ASMImmediate(1);

            if (op.equals("==")) {
                this.program.addInstruction(ISA.ASMOpCode.BEQ, regOp1, regOp2, labEqual);
            }
            else if (op.equals("!=")) {
                this.program.addInstruction(ISA.ASMOpCode.BNE, regOp1, regOp2, labEqual);
            }
            else {
                System.err.print("Unreachable\n");
                System.exit(Constant.INTERNAL_ERROR);
            }
            this.regAlloc.killVariable(varResLeft);
            this.regAlloc.killVariable(varResRight);
            regDst = this.regAlloc.getRegister(varDst);
            this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immFalse);
            this.program.addInstruction(ISA.ASMOpCode.J, labNotEqual);
            this.program.addLabel(labEqual);
            this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, immTrue);
            this.program.addLabel(labNotEqual);
        }
        else if (type == Constant.Type.STRING) {
            if (op.equals("==")) {
                varDst = this.compareStrings(varResLeft, varResRight, 0, 0, 1);
            }
            else if (op.equals("!=")) {
                varDst = this.compareStrings(varResLeft, varResRight, 1, 1, 0);
            }
            else {
                System.err.print("Unreachable\n");
                System.exit(Constant.INTERNAL_ERROR);
            }
        }
        else {
            System.err.print("Unreachable\n");
            System.exit(Constant.INTERNAL_ERROR);
        }

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
//        System.out.print("int literal: " + ctx.getText() + "\n");

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst = this.regAlloc.getRegister(varDst);
        ASMImmediate imm = new ASMImmediate(Integer.valueOf(ctx.getText()));
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, imm);

        return varDst;
    }

    @Override
    public ASMVariable visitCharLiteralLabel(VYPeParserParser.CharLiteralLabelContext ctx) {
//        System.out.print("char literal: " + ctx.getText() + "\n");

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst = this.regAlloc.getRegister(varDst);

        int value = (int) ctx.getText().charAt(1); // because of ''
        if (value == '\\' && (ctx.getText().length() > 3)) {
            value = (int) ctx.getText().charAt(2);
            if (value == 'n') {
                value = (int) '\n';
            } else if (value == 't') {
                value = (int) '\t';
            } else if (value == '"') {
                value = (int) '\"';
            } else if (value == '\'') {
                value = (int) '\'';
            }
        }
        ASMImmediate imm = new ASMImmediate(value);
        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, imm);

        return varDst;
    }

    @Override
    public ASMVariable visitStringLiteralLabel(VYPeParserParser.StringLiteralLabelContext ctx) {
//        System.out.print("string literal: " + ctx.getText() + "\n");

        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst = this.regAlloc.getRegister(varDst);
        String value = ctx.getText();
        ASMLabel labString = this.program.getStringLabel();
        this.program.addString(labString, value);

        this.program.addInstruction(ISA.ASMOpCode.MOVSI, regDst, labString);

        return varDst;
    }

    @Override
    public ASMVariable visitIdentifierLabel(VYPeParserParser.IdentifierLabelContext ctx) {
//        System.out.print("identifier: " + ctx.getText() + "\n");

        ASMVariable varId = this.regAlloc.checkVariable(ctx.getText());

        // set the default value if needed
//        if (!this.regAlloc.varIsDefined(varId)) {
//            Constant.Type type = this.regAlloc.getVariableDataType(varId);
//            ASMRegister regId = this.regAlloc.getRegister(varId);
//            ASMRegister regZero = this.regAlloc.getZeroReg();
//            String comment = ctx.Identifier().getText() + "(" + regId.getText() + ") = default value";
//
//            if (type == Constant.Type.INT || type == Constant.Type.CHAR) {
//                this.program.addInstruction(ISA.ASMOpCode.MOV, regId, regZero, comment);
//            }
//            else if (type == Constant.Type.STRING) {
//                ASMRegister regGlobalPtr = this.regAlloc.getGlobalPtrReg();
//                ASMImmediate immOne = new ASMImmediate(1);
//                ASMImmediate immOffset = new ASMImmediate(0);
//                this.program.addInstruction(ISA.ASMOpCode.MOV, regId, regGlobalPtr, comment);
//                // store zero so that simulator does not print warnings
//                this.program.addInstruction(ISA.ASMOpCode.SB, regZero, immOffset, regGlobalPtr);
//                this.program.addInstruction(ISA.ASMOpCode.ADDU, regGlobalPtr, immOne);
//            }
//            else {
//                System.err.print("Unreachable\n");
//                System.exit(Constant.INTERNAL_ERROR);
//            }
//        }

        return varId;
    }

    private List<ASMVariable> getFunctionCallParameters(String functionName, VYPeParserParser.Function_callContext ctx) {
        List<ASMVariable> parameterList = new ArrayList<>();

        int paramIndex = 0;
        for(int i = 2, len = ctx.getChildCount() - 1; i < len; i += 2) {
            VYPeExpressionLow lowParam = new VYPeExpressionLow(this.program, this.regAlloc);
            ASMVariable varParam = lowParam.visit(ctx.getChild(i));
            parameterList.add(varParam);

//            // if param is string it has to be copied to a new destination
//            if (this.program.isParamString(functionName, paramIndex)) {
//                ASMVariable varCopy = this.copyString(varParam);
//                parameterList.add(varCopy);
//            }

            paramIndex++;
        }

        return parameterList;
    }

    private ASMVariable genReadFunction(String function) {
        ASMVariable varDst = this.regAlloc.getTempVar();
        ASMRegister regDst = this.regAlloc.getRegister(varDst);

        if (function.equals(ISA.Function.READ_CHAR)) {
            this.program.addInstruction(ISA.ASMOpCode.READ_CHAR, regDst);
        }
        else if (function.equals(ISA.Function.READ_INT)) {
            this.program.addInstruction(ISA.ASMOpCode.READ_INT, regDst);
        }
        else if (function.equals(ISA.Function.READ_STRING)) {
            ASMRegister regGlobalPtr = this.regAlloc.getGlobalPtrReg();
            ASMVariable varLength = this.regAlloc.getTempVar();
            ASMRegister regLength = this.regAlloc.getRegister(varLength);
            ASMImmediate immOne = new ASMImmediate(1);

            this.program.addInstruction(ISA.ASMOpCode.READ_STRING, regGlobalPtr, regLength);
            this.program.addInstruction(ISA.ASMOpCode.MOV, regDst, regGlobalPtr);
            this.program.addInstruction(ISA.ASMOpCode.ADD, regGlobalPtr, regGlobalPtr, regLength);
            this.regAlloc.killVariable(varLength);
            // \0
            this.program.addInstruction(ISA.ASMOpCode.ADDI, regGlobalPtr, regGlobalPtr, immOne);
        }
        else {
            System.err.print("Unreachable\n");
            System.exit(Constant.INTERNAL_ERROR);
        }

        return varDst;
    }

    private void genPrintFunction(String function, List<ASMVariable> parameters, List<Value> parameterTypes) {
        ISA.ASMOpCode op = null;
        int i = 0;

        for (Value varType : parameterTypes) {
            ASMVariable varParam = parameters.get(i++);
            ASMRegister regParam = this.regAlloc.getRegister(varParam);
//            System.out.print("data type: " + this.regAlloc.getVariableDataType(varParam) + "\n");

            op = this.getPrintOpCode(varType.getType());
            this.program.addInstruction(op, regParam);

            this.regAlloc.killVariable(varParam);
        }
    }

    private ISA.ASMOpCode getPrintOpCode (Constant.Type varType) {
        ISA.ASMOpCode op = null;

        switch(varType) {
            case CHAR:
                op = ISA.ASMOpCode.PRINT_CHAR;
                break;
            case INT:
                op = ISA.ASMOpCode.PRINT_INT;
                break;
            case STRING:
                op = ISA.ASMOpCode.PRINT_STRING;
                break;
            default:
                throw new SemanticException("Invalid var type in print function!");
        }

        return op;
    }

    private ASMVariable genGetAtFunction(List<ASMVariable> parameters) {
        ASMVariable varString = parameters.get(0);
        ASMVariable varIndex = parameters.get(1);
        ASMVariable varRes = this.regAlloc.getTempVar();
        ASMRegister regString = this.regAlloc.getRegister(varString);
        ASMRegister regIndex = this.regAlloc.getRegister(varIndex);
        ASMRegister regRes = this.regAlloc.getRegister(varRes);
        ASMImmediate immOffset = new ASMImmediate(0);

        this.program.addInstruction(ISA.ASMOpCode.ADD, regRes, regString, regIndex);
        this.program.addInstruction(ISA.ASMOpCode.LBU, regRes, immOffset, regRes);

        this.regAlloc.killVariable(varString);
        this.regAlloc.killVariable(varIndex);

        return varRes;
    }

    private ASMVariable copyString(ASMVariable varSource) {
        ASMVariable varSourceCopy = this.regAlloc.getTempVar();
        ASMVariable varCopyChar = this.regAlloc.getTempVar();
        ASMVariable varRes = this.regAlloc.getTempVar();

        ASMRegister regSource = this.regAlloc.getRegister(varSource);
        ASMRegister regSourceCopy = this.regAlloc.getRegister(varSourceCopy);
        ASMRegister regCopyChar = this.regAlloc.getRegister(varCopyChar);
        ASMRegister regGlobalPtr = this.regAlloc.getGlobalPtrReg();
        ASMRegister regRes = this.regAlloc.getRegister(varRes);
        ASMRegister regZero = this.regAlloc.getZeroReg();

        ASMLabel labCopy = this.program.getTempLabel();

        ASMImmediate immOffset = new ASMImmediate(0);
        ASMImmediate immOne = new ASMImmediate(1);

        this.program.addInstruction(ISA.ASMOpCode.MOV, regSourceCopy, regSource);
        this.program.addInstruction(ISA.ASMOpCode.MOV, regRes, regGlobalPtr);
        this.program.addLabel(labCopy);
        this.program.addInstruction(ISA.ASMOpCode.LBU, regCopyChar, immOffset, regSourceCopy);
        this.program.addInstruction(ISA.ASMOpCode.SB, regCopyChar, immOffset, regGlobalPtr);
        this.program.addInstruction(ISA.ASMOpCode.ADDU, regGlobalPtr, immOne);
        this.program.addInstruction(ISA.ASMOpCode.ADDU, regSourceCopy, immOne);
        this.program.addInstruction(ISA.ASMOpCode.BNE, regCopyChar, regZero, labCopy);

        this.regAlloc.killVariable(varSourceCopy);
        this.regAlloc.killVariable(varCopyChar);

        return varRes;
    }

    private ASMVariable genSetAtFunction(List<ASMVariable> parameters) {
        ASMVariable varOriginal = parameters.get(0);

        // copy the source string
        ASMVariable varString = this.copyString(varOriginal);

        ASMRegister regString = this.regAlloc.getRegister(varString);
        ASMVariable varIndex = parameters.get(1);
        ASMVariable varChar = parameters.get(2);
        ASMVariable varPtr = this.regAlloc.getTempVar();
        ASMRegister regIndex = this.regAlloc.getRegister(varIndex);
        ASMRegister regChar = this.regAlloc.getRegister(varChar);
        ASMRegister regPtr = this.regAlloc.getRegister(varPtr);
        ASMImmediate immOffset = new ASMImmediate(0);

        this.program.addInstruction(ISA.ASMOpCode.ADD, regPtr, regString, regIndex);
        this.program.addInstruction(ISA.ASMOpCode.SB, regChar, immOffset, regPtr);

        this.regAlloc.killVariable(varIndex);
        this.regAlloc.killVariable(varChar);

        return varString;
    }

    private ASMVariable genStrcatFunction(List<ASMVariable> parameters) {
        ASMVariable varString1 = parameters.get(0);
        ASMVariable varString2 = parameters.get(1);
        ASMVariable varRes, varAux;

        // copy the first string
        varRes = this.copyString(varString1);
        this.regAlloc.killVariable(varString1);

        // hack - delete the null char at the end
        ASMRegister regGlobalPtr = this.regAlloc.getGlobalPtrReg();
        ASMImmediate immOne = new ASMImmediate(1);
        this.program.addInstruction(ISA.ASMOpCode.SUBU, regGlobalPtr, immOne);

        // copy the second string
        varAux = this.copyString(varString2);
        this.regAlloc.killVariable(varString2);

        this.regAlloc.killVariable(varAux);

        return varRes;
    }

    @Override
    public ASMVariable visitFunction_call(VYPeParserParser.Function_callContext ctx) {
        String name = ctx.getChild(0).getText();
//        System.out.print("function call: " + name + "\n");
        ASMVariable varRes = null;
        List<ASMVariable> parameters = this.getFunctionCallParameters(name, ctx);

        if (name.equals(ISA.Function.READ_CHAR) ||
                name.equals(ISA.Function.READ_INT) ||
                name.equals(ISA.Function.READ_STRING)) {
            varRes = this.genReadFunction(name);
        }
        else if (name.equals(ISA.Function.PRINT)) {
            VYPePrintVisitor visitor = new VYPePrintVisitor(this.program.getFunctionTable(), regAlloc);
            List<Value> parameterTypes = visitor.getFunctionCallParameters(ctx);
            this.genPrintFunction(name, parameters, parameterTypes);
        }
        else if (name.equals(ISA.Function.GET_AT)) {
            varRes = this.genGetAtFunction(parameters);
        }
        else if (name.equals(ISA.Function.SET_AT)) {
            varRes = this.genSetAtFunction(parameters);
        }
        else if (name.equals(ISA.Function.STRCAT)) {
            varRes = this.genStrcatFunction(parameters);
        }
        else {
            List<ASMRegister> regsSaved;
            // save used registers on the stack
            regsSaved = this.regAlloc.saveRegisters();

            int paramCnt = parameters.size();
            int size = ISA.REGISTER_SIZE * paramCnt;
            ASMImmediate immSize = new ASMImmediate(size);
            ASMRegister regStackPtr = this.regAlloc.getStackPtrReg();

            if (paramCnt > 0) {
                // allocate space for the arguments on the stack
                this.program.addInstruction(ISA.ASMOpCode.SUBU, regStackPtr, immSize);

                // push arguments on the stack
                int offset = ISA.REGISTER_SIZE * (parameters.size() - 1);
                for (ASMVariable varParam : parameters) {
                    ASMImmediate immOffset = new ASMImmediate(offset);
                    // spilling would break the call frame
                    ASMRegister regParam = this.regAlloc.getRegisterNoSpill(varParam);
                    String comment = "param " + varParam.getText();
                    this.program.addInstruction(ISA.ASMOpCode.SW, regParam, immOffset, regStackPtr, comment);
                    offset -= ISA.REGISTER_SIZE;
                    this.regAlloc.killVariable(varParam);
                }
            }

            // call the function
            ASMLabel labFunc;
            if (name.equals("main")) {
                labFunc = new ASMLabel(name);
            }
            else {
                labFunc = new ASMLabel("$" + name);
            }
            this.program.addInstruction(ISA.ASMOpCode.JAL, labFunc);

            if (paramCnt > 0) {
                // free the parameters from the stack
                this.program.addInstruction(ISA.ASMOpCode.ADDU, regStackPtr, immSize);
            }

            // restore the registers from the stack
            this.regAlloc.restoreRegisters(regsSaved);

            // result is in the R2
            varRes = this.regAlloc.getTempVar();
            ASMRegister regRes = this.regAlloc.getRegister(varRes);
            ASMRegister regReturnVal = this.regAlloc.getReturnValReg();
            this.program.addInstruction(ISA.ASMOpCode.MOV, regRes, regReturnVal);
        }

        return varRes;
    }

    @Override
    public ASMVariable visitCastingLabel(VYPeParserParser.CastingLabelContext ctx) {
        Constant.Type castType = Utility.getType(ctx.getChild(1).getText());

        VYPeExpressionLow lowOp = new VYPeExpressionLow(this.program, this.regAlloc);
        ASMVariable varOp = lowOp.visit(ctx.expression());
        ASMVariable varRes = this.regAlloc.getTempVar();

        ASMRegister regOp = this.regAlloc.getRegister(varOp);
        ASMRegister regRes = this.regAlloc.getRegister(varRes);

        // int -> char
        if (castType == Constant.Type.CHAR) {
            ASMImmediate immMask = new ASMImmediate(0xff);
            this.program.addInstruction(ISA.ASMOpCode.ANDI, regRes, regOp, immMask);
        }
        // char -> int
        else if (castType == Constant.Type.INT) {
            this.program.addInstruction(ISA.ASMOpCode.MOV, regRes, regOp);
        }
        // char -> string
        else if (castType == Constant.Type.STRING) {
            ASMRegister regGlobalPtr = this.regAlloc.getGlobalPtrReg();
            ASMImmediate immOffset = new ASMImmediate(0);
            ASMImmediate immTwo = new ASMImmediate(2);

            this.program.addInstruction(ISA.ASMOpCode.MOV, regRes, regGlobalPtr);
            this.program.addInstruction(ISA.ASMOpCode.SB, regOp, immOffset, regGlobalPtr);
            // char + \0
            this.program.addInstruction(ISA.ASMOpCode.ADDU, regGlobalPtr, immTwo);
        } else {
            System.err.print("Unreachable\n");
            System.exit(Constant.INTERNAL_ERROR);
        }

        return varRes;
    }
}
