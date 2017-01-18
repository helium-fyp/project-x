package lk.ac.mrt.projectx.buildex.trees;

import lk.ac.mrt.projectx.buildex.DefinesDotH;

import static lk.ac.mrt.projectx.buildex.DefinesDotH.DR_REG.DR_REG_INVALID;
import static lk.ac.mrt.projectx.buildex.x86.X86Analysis.MAX_SIZE_OF_REG;

/**
 * Created by krv on 12/4/2016.
 */
//TODO : index sort comparable (check output.h line 24-33), this may be a linked list
//TODO : DUPLICATE CLASS IN InstructionTraceUnit , package lk.ac.mrt.projectx.buildex.models.output
public class Operand <T> implements Comparable {

    //region public enum

    OperandType type;

    //endregion public enum

    //region variables
    Integer width;
    T value;
    Operand addr;
    public Operand() {
        this.type = null;
        this.width = null;
        this.value = null;
        this.addr = null;
    }

    //endregion variables

    //region public constructors

    public Operand(Operand operand) {
        this(operand.type, (T) operand.value, operand.width);
    }

    public Operand(Operand.OperandType type, T value, Integer width) {
        this.type = type;
        this.value = value;
        this.width = width;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (type == OperandType.REG_TYPE) {
            Integer offset = (Integer) value - ((Integer) value / MAX_SIZE_OF_REG) * MAX_SIZE_OF_REG;
            stringBuilder.append(((Integer) value).toString());
            stringBuilder.append(":r[");
            stringBuilder.append(this.getRegName());
            stringBuilder.append(":");
            stringBuilder.append(offset.toString());
        } else {
            if (this.type == OperandType.IMM_FLOAT_TYPE || this.type == OperandType.IMM_INT_TYPE) {
                stringBuilder.append("imm[");
            } else if (this.type == OperandType.MEM_STACK_TYPE) {
                stringBuilder.append("ms[");
            } else if (this.type == OperandType.MEM_HEAP_TYPE) {
                stringBuilder.append("mh[");
            }
            stringBuilder.append(this.value.toString());
        }
        stringBuilder.append("]");
        stringBuilder.append("{");
        stringBuilder.append(this.width.toString());
        stringBuilder.append("}");
        return super.toString();
    }

    //endregion public constructors

    //region public methods

    public String getRegName() {
        DefinesDotH.DR_REG reg;
        reg = memRangeToRegister();
        String name = reg.name().substring(reg.name().lastIndexOf("_") + 1);
        return name.toLowerCase();
    }

    // TODO 1        : Check why X86_analysis.cpp (mem_range_to_reg) switch case values are different from defines.h
    // TODO 1 contd. : But currently project-x gets the same value in the defines.h enum from DynamoRIO as you see
    private DefinesDotH.DR_REG memRangeToRegister() {
        DefinesDotH.DR_REG ret;
        if (this.type == OperandType.REG_TYPE) {
            int range = (Integer) this.value / MAX_SIZE_OF_REG + 1;
            if (range > 0 && range <= 56) {
                ret = DefinesDotH.DR_REG.values()[ range ];
            } else {
                ret = DR_REG_INVALID;
            }
        } else {
            ret = DR_REG_INVALID;
        }
        return ret;
    }

    @Override
    public int compareTo(Object other) {
        //TODO : not complete but similar to Helium
        if (!(other instanceof Operand))
            throw new ClassCastException("A Operand object expected.");
        if (this.value instanceof Float) {
            Float f1 = (Float) this.value;
            Float f2 = (Float) ((Operand) other).value;
            return Float.compare(f1, f2);
        } else {
            Integer f1 = (Integer) this.value;
            Integer f2 = (Integer) ((Operand) other).value;
            return Integer.compare(f1, f2);
        }
    }

    //endregion public methods

    //region private methods

    public static enum OperandType {
        REG_TYPE,
        MEM_STACK_TYPE,
        MEM_HEAP_TYPE,
        IMM_FLOAT_TYPE,
        IMM_INT_TYPE,
        DEFAULT_TYPE;
    }

    //endregion private methods
}

