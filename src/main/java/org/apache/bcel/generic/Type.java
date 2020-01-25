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
import java.util.List;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Utility;

import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.FieldDescriptor;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Abstract super class for all possible java types, namely basic types
 * such as int, object types like String and array types, e.g. int[]
 *
 */
@SuppressWarnings("interning")  // sets constants
@AnnotatedFor({"nullness","signature"})
public abstract class Type {

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
    /** Predefined constants
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
    public static final Type[] NO_ARGS = new Type[0]; // EMPTY, so immutable
    public static final @InternedDistinct ReferenceType NULL = new ReferenceType() {
    };
    @SuppressWarnings("signature") // illegal object
    public static final @InternedDistinct Type UNKNOWN = new Type(Const.T_UNKNOWN, "<unknown object>") {
    };


    protected Type(final byte t, final @FieldDescriptor String s) {
        type = t;
        signature = s;
    }


    /**
     * @return hashcode of Type
     */
    @Override
    public int hashCode() {
        return type ^ signature.hashCode();
    }


    /**
     * @return whether the Types are equal
     */
    @Override
    public boolean equals(final @Nullable Object o) {
          if (o instanceof Type) {
              final Type t = (Type)o;
              return (type == t.type) && signature.equals(t.signature);
          }
          return false;
    }


    /**
     * @return signature for given type.
     */
    public @FieldDescriptor String getSignature() {
        return signature;
    }


    /**
     * @return type as defined in Constants
     */
    public byte getType() {
        return type;
    }

    /**
     * boolean, short and char variable are considered as int in the stack or local variable area.
     * Returns {@link Type#INT} for {@link Type#BOOLEAN}, {@link Type#SHORT} or {@link Type#CHAR}, otherwise
     * returns the given type.
     * @since 6.0
     */
    @SuppressWarnings("interned") // primitive types are interned
    public Type normalizeForStackOrLocal() {
        if (this == Type.BOOLEAN || this == Type.BYTE || this == Type.SHORT || this == Type.CHAR) {
            return Type.INT;
        }
        return this;
    }

    /**
     * @return stack size of this type (2 for long and double, 0 for void, 1 otherwise)
     */
    public int getSize() {
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
     * @return Type string, e.g. `int[]'
     */
    @Override
    // TODO: result must be one of fully-qualified name, binary name, internal form, Class.getSimpleName
    // TODO: looks like a bug, because signatureToString is called
    public @BinaryName String toString() {
        return ((this.equals(Type.NULL) || (type >= Const.T_UNKNOWN))) ? signature : Utility
                .signatureToString(signature, false);
    }


    /**
     * Convert type to Java method signature, e.g. int[] f(java.lang.String x)
     * becomes (Ljava/lang/String;)[I
     *
     * @param return_type what the method returns
     * @param arg_types what are the argument types
     * @return method signature for given type(s).
     */
    public static String getMethodSignature( final Type return_type, final Type[] arg_types ) {
        final StringBuilder buf = new StringBuilder("(");
        if (arg_types != null) {
            for (final Type arg_type : arg_types) {
                buf.append(arg_type.getSignature());
            }
        }
        buf.append(')');
        buf.append(return_type.getSignature());
        return buf.toString();
    }

    @SuppressWarnings("nullness:type.argument.type.incompatible") // initialValue returns non-null
    private static final ThreadLocal<Integer> consumed_chars = new ThreadLocal<Integer>() {

        @Override
        protected Integer initialValue() {
            return Integer.valueOf(0);
        }
    };//int consumed_chars=0; // Remember position in string, see getArgumentTypes


    @SuppressWarnings("nullness:type.argument.type.incompatible") // ThreadLocal workaround
    private static int unwrap( final ThreadLocal<Integer> tl ) {
        return tl.get().intValue();
    }


    private static void wrap( final ThreadLocal<Integer> tl, final int value ) {
        tl.set(Integer.valueOf(value));
    }


    /**
     * Convert signature to a Type object.
     * @param signature signature string such as Ljava/lang/String;
     * @return type object
     */
    // @since 6.0 no longer final
    // TODO: getType(Class) calls "getType(cl.getName())" for arrays, which passes a
    // @ClassGetName such as "[Ljava.lang.String;" rather than a
    // @FieldDescriptor as documented in this method's Javadoc.
    public static @NonNull Type getType( final @ClassGetName String signature ) throws StringIndexOutOfBoundsException {
        final byte type = Utility.typeOfSignature(signature);
        if (type <= Const.T_VOID) {
            //corrected concurrent private static field acess
            wrap(consumed_chars, 1);
            return BasicType.getType(type);
        } else if (type == Const.T_ARRAY) {
            int dim = 0;
            do { // Count dimensions
                dim++;
            } while (signature.charAt(dim) == '[');
            // Recurse, but just once, if the signature is ok
            @SuppressWarnings("signature") // string manipulation: strip off array brackets and make recursive call
            final Type t = getType(signature.substring(dim));
            //corrected concurrent private static field acess
            //  consumed_chars += dim; // update counter - is replaced by
            final int _temp = unwrap(consumed_chars) + dim;
            wrap(consumed_chars, _temp);
            return new ArrayType(t, dim);
        } else { // type == T_REFERENCE
            // Utility.typeSignatureToString understands how to parse generic types.
            // TODO: either "java/lang/String" or "java.lang.String" might flow here.
            final String parsedSignature = Utility.typeSignatureToString(signature, false);
            wrap(consumed_chars, parsedSignature.length() + 2); // "Lblabla;" `L' and `;' are removed
            @SuppressWarnings("signature") // string manipulation; known to be reference type
            @BinaryName String className = parsedSignature.replace('/', '.');
            return ObjectType.getInstance(className);
        }
    }


    /**
     * Convert return value of a method (signature) to a Type object.
     *
     * @param signature signature string such as (Ljava/lang/String;)V
     * @return return type
     */
    @SuppressWarnings("signature") // substring manipulation
    // Argument is a @FieldDescriptor, possibly following a close-paren ")"
    public static Type getReturnType( final String signature ) {
        try {
            // Read return type after `)'; also works if there is no `)'
            final int index = signature.lastIndexOf(')') + 1;
            return getType(signature.substring(index));
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
    }


    /**
     * Convert arguments of a method (signature) to an array of Type objects.
     * @param signature signature string such as (Ljava/lang/String;)V
     * @return array of argument types
     */
    @SuppressWarnings("signature") // substring manipulation
    public static Type[] getArgumentTypes( final String signature ) {
        final List<Type> vec = new ArrayList<>();
        int index;
        Type[] types;
        try {
            // Skip any type arguments to read argument declarations between `(' and `)'
            index = signature.indexOf('(') + 1;
            if (index <= 0) {
                throw new ClassFormatException("Invalid method signature: " + signature);
            }
            while (signature.charAt(index) != ')') {
                vec.add(getType(signature.substring(index)));
                //corrected concurrent private static field acess
                index += unwrap(consumed_chars); // update position
            }
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
        types = new Type[vec.size()];
        vec.toArray(types);
        return types;
    }


    /** Convert runtime java.lang.Class to BCEL Type object.
     * @param cl Java class
     * @return corresponding Type object
     */
    public static Type getType( final java.lang.Class<?> cl ) {
        if (cl == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        /* That's an amzingly easy case, because getName() returns
         * the signature. That's what we would have liked anyway.
         */
        if (cl.isArray()) {
            return getType(cl.getName());
        } else if (cl.isPrimitive()) {
            if (cl == Integer.TYPE) {
                return INT;
            } else if (cl == Void.TYPE) {
                return VOID;
            } else if (cl == Double.TYPE) {
                return DOUBLE;
            } else if (cl == Float.TYPE) {
                return FLOAT;
            } else if (cl == Boolean.TYPE) {
                return BOOLEAN;
            } else if (cl == Byte.TYPE) {
                return BYTE;
            } else if (cl == Short.TYPE) {
                return SHORT;
            } else if (cl == Byte.TYPE) {
                return BYTE;
            } else if (cl == Long.TYPE) {
                return LONG;
            } else if (cl == Character.TYPE) {
                return CHAR;
            } else {
                throw new IllegalStateException("Ooops, what primitive type is " + cl);
            }
        } else { // "Real" class
            // If a non-array reference, then @ClassGetName = @FullyQualifiedName, so this is OK.
            // TODO: Except not for inner classes, where they are different ("." vs "$" in name).
            return ObjectType.getInstance(cl.getName());
        }
    }


    /**
     * Convert runtime java.lang.Class[] to BCEL Type objects.
     * @param classes an array of runtime class objects
     * @return array of corresponding Type objects
     */
    public static Type[] getTypes( final java.lang.Class<?>[] classes ) {
        final Type[] ret = new Type[classes.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getType(classes[i]);
        }
        return ret;
    }


    public static String getSignature( final java.lang.reflect.Method meth ) {
        final StringBuilder sb = new StringBuilder("(");
        final Class<?>[] params = meth.getParameterTypes(); // avoid clone
        for (final Class<?> param : params) {
            sb.append(getType(param).getSignature());
        }
        sb.append(")");
        sb.append(getType(meth.getReturnType()).getSignature());
        return sb.toString();
    }

    static int size(final int coded) {
        return coded & 3;
    }

    static int consumed(final int coded) {
        return coded >> 2;
    }

    static int encode(final int size, final int consumed) {
        return consumed << 2 | size;
    }

    static int getArgumentTypesSize( final String signature ) {
        int res = 0;
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

    static int getTypeSize( final String signature ) throws StringIndexOutOfBoundsException {
        final byte type = Utility.typeOfSignature(signature);
        if (type <= Const.T_VOID) {
            return encode(BasicType.getType(type).getSize(), 1);
        } else if (type == Const.T_ARRAY) {
            int dim = 0;
            do { // Count dimensions
                dim++;
            } while (signature.charAt(dim) == '[');
            // Recurse, but just once, if the signature is ok
            final int consumed = consumed(getTypeSize(signature.substring(dim)));
            return encode(1, dim + consumed);
        } else { // type == T_REFERENCE
            final int index = signature.indexOf(';'); // Look for closing `;'
            if (index < 0) {
                throw new ClassFormatException("Invalid signature: " + signature);
            }
            return encode(1, index + 1);
        }
    }


    static int getReturnTypeSize(final String signature) {
        final int index = signature.lastIndexOf(')') + 1;
        return Type.size(getTypeSize(signature.substring(index)));
    }


    /*
     * Currently only used by the ArrayType constructor.
     * The signature has a complicated dependency on other parameter
     * so it's tricky to do it in a call to the super ctor.
     */
    void setSignature(final @FieldDescriptor String signature) {
        this.signature = signature;
    }
}
