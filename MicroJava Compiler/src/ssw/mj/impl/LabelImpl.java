package ssw.mj.impl;

import ssw.mj.codegen.Code;
import ssw.mj.codegen.Label;

import java.util.List;

public final class LabelImpl extends Label {

    Code code;
    List<Integer> fixupList;
    int adr;

    public LabelImpl(Code code) {
        super(code);
    }

    /**
     * Generates code for a jump to this label.
     */
    @Override
    public void put() {
        if (isDefined()) {
            code.put2(adr - (code.pc - 1));
        } else {
            fixupList.add(code.pc);
            code.put2(0);
        }
    }

    /**
     * Defines <code>this</code> label to be at the current pc position
     */
    @Override
    public void here() {
        if (isDefined()) {
            throw new IllegalStateException("label defined twice");
        }
        for (int pos : fixupList) {
            code.put2(pos, code.pc - (pos - 1));
        }
        fixupList = null;
        adr = code.pc;
    }

    public boolean isDefined() {
        return this.adr != 0;
    }
}
