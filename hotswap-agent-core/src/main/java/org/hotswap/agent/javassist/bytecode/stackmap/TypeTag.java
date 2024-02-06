

package org.hotswap.agent.javassist.bytecode.stackmap;

import org.hotswap.agent.javassist.bytecode.StackMapTable;

public interface TypeTag {
    String TOP_TYPE = "*top*";
    TypeData.BasicType TOP = new TypeData.BasicType(TOP_TYPE, StackMapTable.TOP, ' ');
    TypeData.BasicType INTEGER = new TypeData.BasicType("int", StackMapTable.INTEGER, 'I');
    TypeData.BasicType FLOAT = new TypeData.BasicType("float", StackMapTable.FLOAT, 'F');
    TypeData.BasicType DOUBLE = new TypeData.BasicType("double", StackMapTable.DOUBLE, 'D');
    TypeData.BasicType LONG = new TypeData.BasicType("long", StackMapTable.LONG, 'J');


}
