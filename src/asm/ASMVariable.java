package asm;

import util.Constant;

/*************************************************************
 * Filename: ASMVariable.java
 * Project: Compiler Implementation for VYPe16 Programming Language
 * Compiler Team: 04
 * Authors: Filip Benna, xbenna01
 *          Tomas Bruckner, xbruck02
 * Date: 9/11/2016
 *************************************************************/

public class ASMVariable {
    boolean empty;
    boolean temporary;
    Integer index;
    String name;
    int scope;
    Constant.Type type = Constant.Type.UNKNOWN; // data type

    // default initialization of the register field
    public ASMVariable() {
        this.empty = true;
    }

    // constructs temporary variable
    public ASMVariable(int index) {
        this.empty = false;
        this.temporary = true;
        this.index = index;
        this.name = ""; // TODO delete
    }

    // constructs program local variable
    public ASMVariable(String name) {
        this.empty = false;
        this.temporary = false;
        this.index = -1;
        this.name = name;
    }

    public ASMVariable(String name, Constant.Type type) {
        this.empty = false;
        this.temporary = false;
        this.index = -1;
        this.name = name;
        this.type = type;
    }

    public void setScope(int index) { this.scope = index; }
    public int getScope() { return this.scope; }

    public boolean isTemporary() { return this.temporary; }
    public int getIndex() { return  this.index; }
    public String getName() { return  this.name; }
    public boolean isEmpty() { return  this.empty; }

    public Constant.Type getType() { return this.type; }

    public boolean equals(ASMVariable var) {
        if (this.empty || var.isEmpty()) {
            return false;
        }

        if (this.scope != var.getScope()) {
            return false;
        }

        boolean equal = false;

        if (this.temporary) {
            if (var.isTemporary()) {
                if (this.index == var.getIndex()) {
                    equal = true;
                }
            }
        }
        else {
            if(!var.isTemporary()) {
                if(this.name.equals(var.getName())) {
                    equal = true;
                }
            }
        }

        return equal;
    }

    public String getText() {
        if(this.temporary) {
            return(new String("$t" + this.index));
        }
        else {
            return this.name;
        }
    }
}
