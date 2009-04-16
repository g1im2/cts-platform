/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.os.cts;

import junit.framework.TestCase;
import android.os.DeadObjectException;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(DeadObjectException.class)
public class DeadObjectExceptionTest extends TestCase {
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: DeadObjectException",
        method = "DeadObjectException",
        args = {}
    )
    public void testDeadObjectException(){
        DeadObjectException ne = null;
        boolean isThrowed = false;

        try {
            ne = new DeadObjectException();
            throw ne;
        } catch (DeadObjectException e) {
            assertSame(ne, e);
            isThrowed = true;
        } finally {
            if (!isThrowed) {
                fail("should throw out DeadObjectException");
            }
        }
    }

}
