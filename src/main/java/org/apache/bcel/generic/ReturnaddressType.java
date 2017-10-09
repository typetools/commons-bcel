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

/*>>>
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.apache.bcel.Const;

/**
 * Returnaddress, the type JSR or JSR_W instructions push upon the stack.
 *
 * see vmspec2 �3.3.3
 * @version $Id$
 */
public class ReturnaddressType extends Type {

    public static final /*@InternedDistinct*/ ReturnaddressType NO_TARGET = new /*@InternedDistinct*/ ReturnaddressType();
    private InstructionHandle returnTarget;


    /**
     * A Returnaddress [that doesn't know where to return to].
     */
    @SuppressWarnings("signature") // special-case object
    private ReturnaddressType() {
        super(Const.T_ADDRESS, "<return address>");
    }


    /**
     * Creates a ReturnaddressType object with a target.
     */
    @SuppressWarnings("signature") // special-case object
    public ReturnaddressType(final InstructionHandle returnTarget) {
        super(Const.T_ADDRESS, "<return address targeting " + returnTarget + ">");
        this.returnTarget = returnTarget;
    }


    /** @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        if (returnTarget == null) {
            return 0;
        }
        return returnTarget.hashCode();
    }


    /**
     * Returns if the two Returnaddresses refer to the same target.
     */
    @Override
    public boolean equals( final /*@Nullable*/ Object rat ) {
        if (!(rat instanceof ReturnaddressType)) {
            return false;
        }
        final ReturnaddressType that = (ReturnaddressType) rat;
        if (this.returnTarget == null || that.returnTarget == null) {
            return that.returnTarget == this.returnTarget;
        }
        return that.returnTarget.equals(this.returnTarget);
    }


    /**
     * @return the target of this ReturnaddressType
     */
    public InstructionHandle getTarget() {
        return returnTarget;
    }
}
