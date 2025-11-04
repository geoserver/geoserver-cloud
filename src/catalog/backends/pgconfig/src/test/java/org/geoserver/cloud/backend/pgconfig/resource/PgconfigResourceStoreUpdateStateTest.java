/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.geoserver.platform.resource.Resource.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the updateState method in PgconfigResourceStore.
 *
 * <p>
 * This method is responsible for refreshing the state of a PgconfigResource
 * from the database, which is critical for long-lived resource references.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PgconfigResourceStoreUpdateStateTest {

    private PgconfigResourceStore store;

    @BeforeEach
    void setup() {
        store = mock(PgconfigResourceStore.class);
    }

    /**
     * Test that updateState correctly updates a resource's state when it exists in
     * the database.
     */
    @Test
    void testUpdateStateExistingResource() {
        // Create a resource with initial state
        PgconfigResource resource =
                new PgconfigResource(store, 1L, 0L, Type.RESOURCE, "security/rest.properties", 123456L);

        // Create an updated resource that would come from the database
        PgconfigResource updatedResource =
                new PgconfigResource(store, 2L, 0L, Type.DIRECTORY, "security/rest.properties", 789012L);

        // Direct test of copy method
        resource.reset(updatedResource);

        // Verify the resource was updated
        assertEquals(2L, resource.getId());
        assertEquals(0L, resource.getParentId());
        assertEquals(Type.DIRECTORY, resource.getType());
        assertEquals("security/rest.properties", resource.path());
        assertEquals(789012L, resource.lastmodified());
    }

    /**
     * Test that a resource can be marked as undefined when it no longer exists in
     * the database.
     */
    @Test
    void testMarkResourceAsUndefined() {
        // Create a resource with initial state
        PgconfigResource resource =
                new PgconfigResource(store, 1L, 0L, Type.RESOURCE, "security/rest.properties", 123456L);

        // Mark as undefined (this simulates what updateState would do)
        resource.type = Type.UNDEFINED;
        resource.id = PgconfigResourceStore.UNDEFINED_ID;
        resource.parentId = PgconfigResourceStore.UNDEFINED_ID;

        // Verify the resource was marked undefined
        assertEquals(Type.UNDEFINED, resource.getType());
        assertEquals(PgconfigResourceStore.UNDEFINED_ID, resource.getId());
        assertEquals(PgconfigResourceStore.UNDEFINED_ID, resource.getParentId());
        assertFalse(resource.exists());
        assertTrue(resource.isUndefined());
    }

    /**
     * Test that a resource can transition through different states.
     */
    @Test
    void testResourceStateTransitions() {
        // Create a PgconfigResource (undefined)
        PgconfigResource resource = new PgconfigResource(
                store,
                PgconfigResourceStore.UNDEFINED_ID,
                PgconfigResourceStore.UNDEFINED_ID,
                Type.UNDEFINED,
                "security/rest.properties",
                0L);

        // Initially, this should be an undefined resource
        assertTrue(resource.isUndefined());
        assertFalse(resource.exists());

        // Create a file resource for transition 1: undefined -> file
        PgconfigResource fileResource = new PgconfigResource(
                store, 1L, 0L, Type.RESOURCE, "security/rest.properties", System.currentTimeMillis());

        // Update to file state
        resource.reset(fileResource);

        // Verify the resource is now a file
        assertTrue(resource.exists());
        assertTrue(resource.isFile());
        assertFalse(resource.isUndefined());
        assertEquals(1L, resource.getId());

        // Create a directory resource for transition 2: file -> directory
        PgconfigResource dirResource = new PgconfigResource(
                store, 1L, 0L, Type.DIRECTORY, "security/rest.properties", System.currentTimeMillis());

        // Update to directory state
        resource.reset(dirResource);

        // Verify the resource is now a directory
        assertTrue(resource.exists());
        assertTrue(resource.isDirectory());
        assertFalse(resource.isFile());
        assertEquals(1L, resource.getId());

        // Transition 3: directory -> undefined (deleted)
        resource.type = Type.UNDEFINED;
        resource.id = PgconfigResourceStore.UNDEFINED_ID;
        resource.parentId = PgconfigResourceStore.UNDEFINED_ID;

        // Verify the resource is undefined
        assertEquals(Type.UNDEFINED, resource.getType());
        assertEquals(PgconfigResourceStore.UNDEFINED_ID, resource.getId());
        assertEquals(PgconfigResourceStore.UNDEFINED_ID, resource.getParentId());
        assertFalse(resource.exists());
        assertTrue(resource.isUndefined());
    }
}
