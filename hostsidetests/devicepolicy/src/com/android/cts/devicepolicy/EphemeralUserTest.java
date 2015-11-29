/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.cts.devicepolicy;

import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;

/**
 * Tests for emphemeral users and profiles.
 */
public class EphemeralUserTest extends BaseDevicePolicyTest {

    private static final int FLAG_EPHEMERAL = 0x00000100;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mHasFeature = getDevice().getApiLevel() >= 24; /* Build.VERSION_CODES.N */
    }

    /** The user should have the ephemeral flag set if it was created as ephemeral. */
    public void testCreateEphemeralUser() throws Exception {
        if (!mHasFeature) {
            return;
        }
        try {
            int userId = createUser(true);
            int flags = getUserFlags(userId);
            assertTrue("ephemeral flag must be set", FLAG_EPHEMERAL == (flags & FLAG_EPHEMERAL));
        } finally {
            removeTestUsers();
        }
    }

    /** The user should not have the ephemeral flag set if it was not created as ephemeral. */
    public void testCreateLongLivedUser() throws Exception {
        if (!mHasFeature) {
            return;
        }
        try {
            int userId = createUser(false);
            int flags = getUserFlags(userId);
            assertTrue("ephemeral flag must not be set", 0 == (flags & FLAG_EPHEMERAL));
        } finally {
            removeTestUsers();
        }
    }

    /**
     * The profile should have the ephemeral flag set automatically if its parent user is
     * ephemeral.
     */
    public void testProfileInheritsEphemeral() throws Exception {
        if (!mHasFeature || !hasDeviceFeature("android.software.managed_users")
                || !hasUserSplit()) {
            return;
        }
        try {
            int userId = createUser(true);
            int profileId = createManagedProfile(userId);
            int flags = getUserFlags(profileId);
            assertTrue("ephemeral flag must be set", FLAG_EPHEMERAL == (flags & FLAG_EPHEMERAL));
        } finally {
            removeTestUsers();
        }
    }

}