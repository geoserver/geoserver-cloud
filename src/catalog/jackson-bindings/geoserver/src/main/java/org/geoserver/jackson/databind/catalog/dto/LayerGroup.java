/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geotools.jackson.databind.dto.Envelope;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("LayerGroupInfo")
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
    protected String workspace;
    protected String rootLayer;
    protected String rootLayerStyle;
    protected List<String> layers;
    protected List<String> styles;
    protected List<MetadataLink> metadataLinks;
    protected Envelope bounds;
    private List<Keyword> keywords;

    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalTitle;

    /**
     * @since geoserver 2.20.0
     */
    private Map<String, String> internationalAbstract;

    /**
     * @since geoserver 2.21.0
     */
    private List<LayerGroupStyle> layerGroupStyles;
}
