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
 */

package org.apache.bcel.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.bcel.Const;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Entry of the parameters table.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.24"> The class File Format :
 *      The MethodParameters Attribute</a>
 * @since 6.0
 */
@AnnotatedFor({"signedness"})
public class MethodParameter implements Cloneable, Node {

    /** Index of the CONSTANT_Utf8_info structure in the constant_pool table representing the name of the parameter */
    private @NonNegative int nameIndex;

    /** The access flags */
    private int accessFlags;

    public MethodParameter() {
    }

    /**
     * Construct object from input stream.
     *
     * @param input Input stream
     * @throws IOException if an I/O error occurs.
     * @throws ClassFormatException if a class is malformed or cannot be interpreted as a class file
     */
    MethodParameter(final DataInput input) throws IOException {
        nameIndex = input.readUnsignedShort();
        accessFlags = input.readUnsignedShort();
    }

    @Override
    public void accept(final Visitor v) {
        v.visitMethodParameter(this);
    }

    /**
     * @return deep copy of this object
     */
    @SuppressWarnings("nullness") // bug in BCEL: uncaught exception
    public MethodParameter copy() {
        try {
            return (MethodParameter) clone();
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return null;
    }

    /**
     * Dump object to file stream on binary format.
     *
     * @param file Output file stream
     * @throws IOException if an I/O error occurs.
     */
    @SuppressWarnings("argument") // writeShort only writes low 16 bits
    public final void dump(final DataOutputStream file) throws IOException {
        file.writeShort(nameIndex);
        file.writeShort(accessFlags);
    }

    @Pure
    public int getAccessFlags() {
        return accessFlags;
    }

    @Pure
    public @NonNegative int getNameIndex() {
        return nameIndex;
    }

    /**
     * Returns the name of the parameter.
     */
    public @Nullable String getParameterName(final ConstantPool constantPool) {
        if (nameIndex == 0) {
            return null;
        }
        return constantPool.getConstantUtf8(nameIndex).getBytes();
    }

    public boolean isFinal() {
        return (accessFlags & Const.ACC_FINAL) != 0;
    }

    @SuppressWarnings("signedness:operation.mixed.unsignedrhs") // ACC_MANDATED is 0x8000
    public boolean isMandated() {
        return (accessFlags & Const.ACC_MANDATED) != 0;
    }

    public boolean isSynthetic() {
        return (accessFlags & Const.ACC_SYNTHETIC) != 0;
    }

    public void setAccessFlags(final int accessFlags) {
        this.accessFlags = accessFlags;
    }

    public void setNameIndex(final @NonNegative int nameIndex) {
        this.nameIndex = nameIndex;
    }
}
