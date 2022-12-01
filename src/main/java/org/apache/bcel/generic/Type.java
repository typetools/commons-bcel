/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.bcel.generic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Utility;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.FieldDescriptor;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Abstract super class for all possible java types, namely basic types such as int, object types like String and array
 * types, e.g. int[]
 *
 */
@SuppressWarnings("interning")  // sets constants
@AnnotatedFor({"nullness","signature","signedness"})
public abstract class Type {

    /**
     * Predefined constants
     */
    public static final @InternedDistinct BasicType VOID = new BasicType(Const.T_VOID);

    public static final @InternedDistinct BasicType BOOLEAN = new BasicType(Const.T_BOOLEAN);
    public static final @InternedDistinct BasicType INT = new BasicType(Const.T_INT);
    public static final @InternedDistinct BasicType SHORT = new BasicType(Const.T_SHORT);
    public static final @InternedDistinct BasicType BYTE = new BasicType(Const.T_BYTE);
    public static final @InternedDistinct BasicType LONG = new BasicType(Const.T_LONG);
    public static final @InternedDistinct BasicType DOUBLE = new BasicType(Const.T_DOUBLE);
    public static final @InternedDistinct BasicType FLOAT = new BasicType(Const.T_FLOAT);
    public static final @InternedDistinct BasicType CHAR = new BasicType(Const.T_CHAR);
    public static final @InternedDistinct ObjectType OBJECT = new ObjectType("java.lang.Object");
    public static final @InternedDistinct ObjectType CLASS = new ObjectType("java.lang.Class");
    public static final @InternedDistinct ObjectType STRING = new ObjectType("java.lang.String");
    public static final @InternedDistinct ObjectType STRINGBUFFER = new ObjectType("java.lang.StringBuffer");
    public static final @InternedDistinct ObjectType THROWABLE = new ObjectType("java.lang.Throwable");

    /**
     * Empty array.
     */
    public static final Type[] NO_ARGS = {};
    public static final @InternedDistinct ReferenceType NULL = new ReferenceType() {
    };

    @SuppressWarnings("signature") // illegal object
    public static final @InternedDistinct Type UNKNOWN = new Type(Const.T_UNKNOWN, "<unknown object>") {
    };

    @SuppressWarnings("nullness:type.argument.type.incompatible") // initialValue returns non-null
    private static final ThreadLocal<Integer> CONSUMED_CHARS = ThreadLocal.withInitial(() -> Integer.valueOf(0));

    // int consumed_chars=0; // Remember position in string, see getArgumentTypes
    static @NonNegative int consumed(final int coded) {
        return coded >> 2;
    }

    static @NonNegative int encode(final @NonNegative int size, final @NonNegative int consumed) {
        return consumed << 2 | size;
    }

    /**
     * Convert arguments of a method (signature) to an array of Type objects.
     *
     * @param signature signature string such as (Ljava/lang/String;)V
     * @return array of argument types
     */
    @SuppressWarnings("signature") // substring manipulation
    public static Type[] getArgumentTypes(final String signature) {
        final List<Type> vec = new ArrayList<>();
        int index;
        try {
            // Skip any type arguments to read argument declarations between `(' and `)'
            index = signature.indexOf('(') + 1;
            if (index <= 0) {
                throw new ClassFormatException("Invalid method signature: " + signature);
            }
            while (signature.charAt(index) != ')') {
                vec.add(getType(signature.substring(index)));
                // corrected concurrent private static field acess
                index += unwrap(CONSUMED_CHARS); // update position
            }
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
        final Type[] types = new Type[vec.size()];
        vec.toArray(types);
        return types;
    }

    static @NonNegative int getArgumentTypesSize(final String signature) {
        @NonNegative int res = 0;
        int index;
        try {
            // Skip any type arguments to read argument declarations between `(' and `)'
            index = signature.indexOf('(') + 1;
            if (index <= 0) {
                throw new ClassFormatException("Invalid method signature: " + signature);
            }
            while (signature.charAt(index) != ')') {
                final int coded = getTypeSize(signature.substring(index));
                res += size(coded);
                index += consumed(coded);
            }
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
        return res;
    }

    /**
     * Convert type to Java method signature, e.g. int[] f(java.lang.String x) becomes (Ljava/lang/String;)[I
     *
     * @param returnType what the method returns
     * @param argTypes what are the argument types
     * @return method signature for given type(s).
     */
    public static String getMethodSignature(final Type returnType, final Type[] argTypes) {
        final StringBuilder buf = new StringBuilder("(");
        if (argTypes != null) {
            for (final Type argType : argTypes) {
                buf.append(argType.getSignature());
            }
        }
        buf.append(')');
        buf.append(returnType.getSignature());
        return buf.toString();
    }

    /**
     * Convert return value of a method (signature) to a Type object.
     *
     * @param signature signature string such as (Ljava/lang/String;)V
     * @return return type
     */
    @SuppressWarnings("signature") // substring manipulation
    // Argument is a @FieldDescriptor, possibly following a close-paren ")"
    public static Type getReturnType(final String signature) {
        try {
            // Read return type after `)'; also works if there is no `)'
            final int index = signature.lastIndexOf(')') + 1;
            return getType(signature.substring(index));
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
    }

    static @NonNegative int getReturnTypeSize(final String signature) {
        final int index = signature.lastIndexOf(')') + 1;
        return Type.size(getTypeSize(signature.substring(index)));
    }

    public static String getSignature(final java.lang.reflect.Method meth) {
        final StringBuilder sb = new StringBuilder("(");
        final Class<?>[] params = meth.getParameterTypes(); // avoid clone
        for (final Class<?> param : params) {
            sb.append(getType(param).getSignature());
        }
        sb.append(")");
        sb.append(getType(meth.getReturnType()).getSignature());
        return sb.toString();
    }

    /**
     * Convert runtime java.lang.Class to BCEL Type object.
     *
     * @param cls Java class
     * @return corresponding Type object
     */
    public static Type getType(final Class<?> cls) {
        Objects.requireNonNull(cls, "cls");
        /*
         * That's an amzingly easy case, because getName() returns the signature. That's what we would have liked anyway.
         */
        if (cls.isArray()) {
            return getType(cls.getName());
        }
        if (!cls.isPrimitive()) { // "Real" class
            // If a non-array reference, then @ClassGetName = @FullyQualifiedName, so this is OK.
            // TODO: Except not for inner classes, where they are different ("." vs "$" in name).
            return ObjectType.getInstance(cls.getName());
        }
        if (cls == Integer.TYPE) {
            return INT;
        }
        if (cls == Void.TYPE) {
            return VOID;
        }
        if (cls == Double.TYPE) {
            return DOUBLE;
        }
        if (cls == Float.TYPE) {
            return FLOAT;
        }
        if (cls == Boolean.TYPE) {
            return BOOLEAN;
        }
        if (cls == Byte.TYPE) {
            return BYTE;
        }
        if (cls == Short.TYPE) {
            return SHORT;
        }
        if (cls == Long.TYPE) {
            return LONG;
        }
        if (cls == Character.TYPE) {
            return CHAR;
        }
        throw new IllegalStateException("Unknown primitive type " + cls);
    }

    /**
     * Convert signature to a Type object.
     *
     * @param signature signature string such as Ljava/lang/String;
     * @return type object
     */
    // @since 6.0 no longer final
    // TODO: getType(Class) calls "getType(cl.getName())" for arrays, which passes a
    // @ClassGetName such as "[Ljava.lang.String;" rather than a
    // @FieldDescriptor as documented in this method's Javadoc.
    public static @NonNull Type getType(final @ClassGetName String signature) throws StringIndexOutOfBoundsException {
        final byte type = Utility.typeOfSignature(signature);
        if (type <= Const.T_VOID) {
            // corrected concurrent private static field acess
            wrap(CONSUMED_CHARS, 1);
            return BasicType.getType(type);
        }
        if (type != Const.T_ARRAY) { // type == T_REFERENCE
            // Utility.typeSignatureToString understands how to parse generic types.
            // TODO: either "java/lang/String" or "java.lang.String" might flow here.
            final String parsedSignature = Utility.typeSignatureToString(signature, false);
            wrap(CONSUMED_CHARS, parsedSignature.length() + 2); // "Lblabla;" `L' and `;' are removed
            @SuppressWarnings("signature") // string manipulation; known to be reference type
            @BinaryName String className = Utility.pathToPackage(parsedSignature);
            return ObjectType.getInstance(className);
        }
        @NonNegative int dim = 0;
        do { // Count dimensions
            dim++;
        } while (signature.charAt(dim) == '[');
        // Recurse, but just once, if the signature is ok
        @SuppressWarnings("signature") // string manipulation: strip off array brackets and make recursive call
        final Type t = getType(signature.substring(dim));
        // corrected concurrent private static field acess
        // consumed_chars += dim; // update counter - is replaced by
        final int temp = unwrap(CONSUMED_CHARS) + dim;
        wrap(CONSUMED_CHARS, temp);
        return new ArrayType(t, dim);
    }

    /**
     * Convert runtime java.lang.Class[] to BCEL Type objects.
     *
     * @param classes an array of runtime class objects
     * @return array of corresponding Type objects
     */
    public static Type[] getTypes(final Class<?>[] classes) {
        final Type[] ret = new Type[classes.length];
        Arrays.setAll(ret, i -> getType(classes[i]));
        return ret;
    }

    static @NonNegative int getTypeSize(final String signature) throws StringIndexOutOfBoundsException {
        final byte type = Utility.typeOfSignature(signature);
        if (type <= Const.T_VOID) {
            return encode(BasicType.getType(type).getSize(), 1);
        }
        if (type == Const.T_ARRAY) {
            @NonNegative int dim = 0;
            do { // Count dimensions
                dim++;
            } while (signature.charAt(dim) == '[');
            // Recurse, but just once, if the signature is ok
            final int consumed = consumed(getTypeSize(signature.substring(dim)));
            return encode(1, dim + consumed);
        }
        // type == T_REFERENCE
        final int index = signature.indexOf(';'); // Look for closing `;'
        if (index < 0) {
            throw new ClassFormatException("Invalid signature: " + signature);
        }
        return encode(1, index + 1);
    }

    static @NonNegative int size(final int coded) {
        return coded & 3;
    }

    @SuppressWarnings("nullness:type.argument.type.incompatible") // ThreadLocal workaround
    private static int unwrap(final ThreadLocal<Integer> tl) {
        return tl.get().intValue();
    }

    private static void wrap(final ThreadLocal<Integer> tl, final int value) {
        tl.set(Integer.valueOf(value));
    }

    /**
     * @deprecated (since 6.0) will be made private; do not access directly, use getter/setter
     */
    @Deprecated
    protected byte type; // TODO should be final (and private)

    /**
     * @deprecated (since 6.0) will be made private; do not access directly, use getter/setter
     */
    @Deprecated
    // The string is mostly a @FieldDescriptor.
    // Subclasses are:
    //  * BasicType: the string is a @FieldDescriptor
    //  * ReferenceType
    //     * For ObjectType, the string is a @FieldDescriptor.
    //     * For ArrayType, the string is a @FieldDescriptor.
    //     * For UninitializedObjectType, s is a string "<UNINITIALIZED OBJECT OF TYPE ...>"
    //  * ReturnaddressType: the string is "<return address>"
    //  * DOUBLE_Upper: the string is "Double_Upper"
    //  * LONG_Upper: the string is "Long_Upper"
    protected @FieldDescriptor String signature; // signature for the type TODO should be private

    protected Type(final byte t, final @FieldDescriptor String s) {
        type = t;
        signature = s;
    }

    /**
     * @return whether the Types are equal
     */
    @Override
    public boolean equals(final @Nullable Object o) {
        if (o instanceof Type) {
            final Type t = (Type) o;
            return type == t.type && signature.equals(t.signature);
        }
        return false;
    }

    /**
     * @return signature for given type.
     */
    @Pure
    public @FieldDescriptor String getSignature() {
        return signature;
    }

    /**
     * @return stack size of this type (2 for long and double, 0 for void, 1 otherwise)
     */
    @Pure
    public @NonNegative int getSize() {
        switch (type) {
        case Const.T_DOUBLE:
        case Const.T_LONG:
            return 2;
        case Const.T_VOID:
            return 0;
        default:
            return 1;
        }
    }

    /**
     * @return type as defined in Constants
     */
    @Pure
    public byte getType() {
        return type;
    }

    /**
     * @return hashcode of Type
     */
    @Override
    public int hashCode() {
        return type ^ signature.hashCode();
    }

    /**
     * boolean, short and char variable are considered as int in the stack or local variable area. Returns {@link Type#INT}
     * for {@link Type#BOOLEAN}, {@link Type#SHORT} or {@link Type#CHAR}, otherwise returns the given type.
     *
     * @since 6.0
     */
    @SuppressWarnings("interned") // primitive types are interned
    public Type normalizeForStackOrLocal() {
        if (this == Type.BOOLEAN || this == Type.BYTE || this == Type.SHORT || this == Type.CHAR) {
            return Type.INT;
        }
        return this;
    }

    /*
     * Currently only used by the ArrayType constructor. The signature has a complicated dependency on other parameter so
     * it's tricky to do it in a call to the super ctor.
     */
    void setSignature(final @FieldDescriptor String signature) {
        this.signature = signature;
    }

    /**
     * @return Type string, e.g. `int[]'
     */
    @Override
    // TODO: result must be one of fully-qualified name, binary name, internal form, Class.getSimpleName
    // TODO: looks like a bug, because signatureToString is called
    public @BinaryName String toString() {
        return this.equals(Type.NULL) || type >= Const.T_UNKNOWN ? signature : Utility.signatureToString(signature, false);
    }
}
