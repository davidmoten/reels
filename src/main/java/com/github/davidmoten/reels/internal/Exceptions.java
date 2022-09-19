/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.davidmoten.reels.internal;

/**
 * Utility class to help propagate checked exceptions and rethrow exceptions
 * designated as fatal.
 */
public final class Exceptions {

    /** Utility class. */
    private Exceptions() {
        // prevent instantiation
    }
    
    /**
     * If the provided Throwable is an Error this method
     * throws it, otherwise returns a RuntimeException wrapping the error
     * if that error is a checked exception.
     * @param error the error to wrap or throw
     * @return the (wrapped) error
     */
    public static RuntimeException wrapOrThrow(Throwable error) {
        if (error instanceof Error) {
            throw (Error)error;
        }
        if (error instanceof RuntimeException) {
            return (RuntimeException)error;
        }
        return new RuntimeException(error);
    }

    /**
     * Throws a particular {@code Throwable} only if it belongs to a set of "fatal" error varieties. These
     * varieties are as follows:
     * <ul>
     * <li>{@code VirtualMachineError}</li>
     * <li>{@code ThreadDeath}</li>
     * <li>{@code LinkageError}</li>
     * </ul>
     * This can be useful if you are writing an operator that calls user-supplied code, and you want to
     * notify subscribers of errors encountered in that code by calling their {@code onError} methods, but only
     * if the errors are not so catastrophic that such a call would be futile, in which case you simply want to
     * rethrow the error.
     *
     * @param t
     *         the {@code Throwable} to test and perhaps throw
     * @see <a href="https://github.com/ReactiveX/RxJava/issues/748#issuecomment-32471495">RxJava: StackOverflowError is swallowed (Issue #748)</a>
     */
    public static void throwIfFatal( Throwable t) {
        // values here derived from https://github.com/ReactiveX/RxJava/issues/748#issuecomment-32471495
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        } else if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        } else if (t instanceof LinkageError) {
            throw (LinkageError) t;
        }
    }
}
