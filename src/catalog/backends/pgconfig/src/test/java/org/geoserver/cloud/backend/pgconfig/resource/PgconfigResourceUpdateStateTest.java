/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.geoserver.platform.resource.Resource.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the updateState mechanism in PgconfigResource.
 *
 * <p>
 * This mechanism is critical for compatibility with components like
 * AbstractAccessRuleDAO and RESTAccessRuleDAO that hold resource references as
 * instance variables, which can become stale in database-backed resource
 * implementations.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PgconfigResourceUpdateStateTest {

    @Mock
    private PgconfigResourceStore store;

    private PgconfigResource resource;

    @BeforeEach
    void setup() {
        resource = new PgconfigResource(store, 1L, 0L, Type.RESOURCE, "security/rest.properties", 123456L);
    }

    /**
     * Test that getType() triggers updateState when sufficient time has passed.
     */
    @Test
    void testGetTypeUpdatesState() {
        // Set lastChecked to a time in the past to force update
        resource.lastChecked = Instant.now().minusSeconds(10);

        // Call getType which should trigger updateState
        resource.getType();

        // Verify updateState was called
        verify(store).updateState(resource);
    }

    /**
     * Test that lastmodified() triggers updateState when sufficient time has
     * passed.
     */
    @Test
    void testLastModifiedUpdatesState() {
        // Set lastChecked to a time in the past to force update
        resource.lastChecked = Instant.now().minusSeconds(10);

        // Call lastmodified which should trigger updateState
        resource.lastmodified();

        // Verify updateState was called
        verify(store).updateState(resource);
    }

    /**
     * Test that multiple rapid calls to getType() only trigger updateState once.
     */
    @Test
    void testUpdateStateNotCalledRepeatedly() {
        // Set lastChecked to a time in the past to force update
        resource.lastChecked = Instant.now().minusSeconds(10);

        // First call should trigger updateState
        resource.getType();

        // Second call should not trigger updateState again
        resource.getType();

        // Verify updateState was called only once
        verify(store, times(1)).updateState(resource);
    }

    /**
     * Test that a resource's state is properly updated from the database.
     */
    @Test
    void testStateIsProperlyUpdated() {
        // Create a mock store that updates the resource on updateState
        PgconfigResourceStore mockStore = mock(PgconfigResourceStore.class);

        // Create a resource with initial state
        resource = new PgconfigResource(mockStore, 1L, 0L, Type.RESOURCE, "security/rest.properties", 123456L);

        // Create an "updated" resource to simulate what the DB would return
        PgconfigResource updatedResource =
                new PgconfigResource(mockStore, 2L, 0L, Type.DIRECTORY, "security/rest.properties", 789012L);

        // Make updateState copy the updated resource's state to our test resource
        // Use doAnswer instead of when for void methods
        doAnswer(invocation -> {
                    PgconfigResource r = invocation.getArgument(0);
                    r.copy(updatedResource);
                    return null;
                })
                .when(mockStore)
                .updateState(any());

        // Set lastChecked to a time in the past
        resource.lastChecked = Instant.now().minusSeconds(10);

        // Call getType which should trigger updateState
        Type type = resource.getType();

        // Verify that the resource was updated with new state
        assertEquals(Type.DIRECTORY, type);
        assertEquals(2L, resource.getId());
        assertEquals(789012L, resource.lastmodified());
    }

    /**
     * Test that updateState can handle the case where a resource no longer exists.
     */
    @Test
    void testUpdateStateHandlesDeletedResource() {
        // Create a mock store that marks the resource as undefined on updateState
        PgconfigResourceStore mockStore = mock(PgconfigResourceStore.class);

        // Create a resource with initial state
        resource = new PgconfigResource(mockStore, 1L, 0L, Type.RESOURCE, "security/rest.properties", 123456L);

        // Make updateState set the resource type to UNDEFINED
        // Use doAnswer instead of when for void methods
        doAnswer(invocation -> {
                    PgconfigResource r = invocation.getArgument(0);
                    r.type = Type.UNDEFINED;
                    r.id = PgconfigResourceStore.UNDEFINED_ID;
                    return null;
                })
                .when(mockStore)
                .updateState(any());

        // Set lastChecked to a time in the past
        resource.lastChecked = Instant.now().minusSeconds(10);

        // Call getType which should trigger updateState
        Type type = resource.getType();

        // Verify that the resource was marked as undefined
        assertEquals(Type.UNDEFINED, type);
        assertEquals(PgconfigResourceStore.UNDEFINED_ID, resource.getId());
        assertFalse(resource.exists());
        assertTrue(resource.isUndefined());
    }
}
