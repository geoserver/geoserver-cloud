package org.geoserver.cloud.catalog.dto;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LayerGroup extends Published {

    public enum Mode {
        SINGLE,
        OPAQUE_CONTAINER,
        NAMED,
        CONTAINER,
        EO
    }

    protected Mode mode = Mode.SINGLE;
    protected Boolean queryDisabled;
    protected InfoReference workspace;
    protected InfoReference rootLayer;
    protected InfoReference rootLayerStyle;
    protected List<InfoReference> layers;
    protected List<InfoReference> styles;
    protected List<MetadataLink> metadataLinks;
    protected Envelope bounds;
    private List<Keyword> keywords;
}
