

package org.hotswap.agent.javassist.bytecode.annotation;


public interface MemberValueVisitor {
   public void visitAnnotationMemberValue(AnnotationMemberValue node);
   public void visitArrayMemberValue(ArrayMemberValue node);
   public void visitBooleanMemberValue(BooleanMemberValue node);
   public void visitByteMemberValue(ByteMemberValue node);
   public void visitCharMemberValue(CharMemberValue node);
   public void visitDoubleMemberValue(DoubleMemberValue node);
   public void visitEnumMemberValue(EnumMemberValue node);
   public void visitFloatMemberValue(FloatMemberValue node);
   public void visitIntegerMemberValue(IntegerMemberValue node);
   public void visitLongMemberValue(LongMemberValue node);
   public void visitShortMemberValue(ShortMemberValue node);
   public void visitStringMemberValue(StringMemberValue node);
   public void visitClassMemberValue(ClassMemberValue node);
}
