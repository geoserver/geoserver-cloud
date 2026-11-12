/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.resource.ResourceChanged.Change;
import org.geoserver.cloud.event.resource.ResourceChanged.Kind;
import org.geoserver.platform.resource.Resource;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ResourceChangedTest {

    private final ObjectMapper mapper = ObjectMapperUtil.newObjectMapper();

    @Test
    void roundTripKeepsFields() throws Exception {
        List<Change> changes = List.of(new Change("workspaces/ws/styles", Kind.MODIFIED));
        ResourceChanged local = ResourceChanged.createLocal(
                "workspaces/ws/styles/a.sld", Kind.MODIFIED, Resource.Type.RESOURCE, 1700000000000L, changes);
        assertThat(local.isLocal()).isTrue();

        String json = mapper.writeValueAsString(local);
        GeoServerEvent read = mapper.readValue(json, GeoServerEvent.class);

        assertThat(read).isInstanceOf(ResourceChanged.class);
        ResourceChanged event = (ResourceChanged) read;
        assertThat(event.getPath()).isEqualTo("workspaces/ws/styles/a.sld");
        assertThat(event.getKind()).isEqualTo(Kind.MODIFIED);
        assertThat(event.getType()).isEqualTo(Resource.Type.RESOURCE);
        assertThat(event.getMtime()).isEqualTo(1700000000000L);
        assertThat(event.getChanges()).isEqualTo(changes);
        assertThat(json).doesNotContain("remote");
    }

    // JSON omits an empty list under the NON_EMPTY inclusion of the mapper
    @Test
    void noChangesReadBackAsEmpty() throws Exception {
        ResourceChanged local = ResourceChanged.createLocal(
                "workspaces/ws/styles/a.sld", Kind.MODIFIED, Resource.Type.RESOURCE, 1700000000000L, List.of());

        String json = mapper.writeValueAsString(local);
        ResourceChanged read = (ResourceChanged) mapper.readValue(json, GeoServerEvent.class);

        assertThat(json).doesNotContain("changes");
        assertThat(read.getChanges()).isNotNull();
        assertThat(read.getChanges()).isEmpty();
        assertThat(read.getChanges()).isEqualTo(local.getChanges());
    }
}
