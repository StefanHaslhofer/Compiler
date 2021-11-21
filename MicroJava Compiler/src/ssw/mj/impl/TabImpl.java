package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Scope;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import static ssw.mj.Errors.Message.*;

public final class TabImpl extends Tab {

    /**
     * Set up "universe" (= predefined names).
     */
    public TabImpl(Parser p) {
        super(p);
    }

    public void openScope() {
        curScope = new Scope(curScope);
        curLevel++;
    }

    public void closeScope() {
        curScope = curScope.outer();
        curLevel--;
    }

    public Obj insert(Obj.Kind kind, String name, Struct type) {
        // report error if the found object is not "noObj", which means that it is already declared
        if (!find(name).equals(noObj)) {
            parser.error(DECL_NAME, name);
            return noObj;
        }

        Obj newObj = new Obj(kind, name, (StructImpl) type);

        if (kind == Obj.Kind.Var) {
            newObj.adr = curScope.nVars();
            newObj.level = curLevel;
        } else if (kind == Obj.Kind.Meth) {
            newObj.adr = curScope.nVars();
        }

        curScope.insert(newObj);
        return newObj;
    }

    public Obj find(String name) {
        Obj obj = curScope.findGlobal(name);
        if (obj == null) {
            parser.error(NOT_FOUND, name);
            return noObj;
        }

        return obj;
    }

    public Obj findField(String name, Struct type) {
        Obj obj = type.findField(name);

        if (obj == null) {
            parser.error(NO_FIELD, name);
            return noObj;
        }

        return obj;
    }
}
