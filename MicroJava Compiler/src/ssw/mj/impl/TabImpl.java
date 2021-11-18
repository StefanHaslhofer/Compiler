package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

public final class TabImpl extends Tab {

    // TODO Exercise 4: implementation of symbol table

    /**
     * Set up "universe" (= predefined names).
     */
    public TabImpl(Parser p) {
        super(p);
    }

    public void openScope() {

    }

    public void closeScope(){

    }

    public Obj insert(Obj.Kind kind, String name, Struct type){

    }

    public Obj find(String name){

    }

    public Obj findField(String name, Struct type){

    }
}
