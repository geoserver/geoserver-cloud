<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
                      xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                      xmlns="http://www.opengis.net/sld"
                      xmlns:ogc="http://www.opengis.net/ogc"
                      xmlns:se="http://www.opengis.net/se"
                      xmlns:xlink="http://www.w3.org/1999/xlink"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>localized_labels</Name>
    <UserStyle>
      <Name>localized_labels</Name>
      <FeatureTypeStyle>
        <Rule>
          <Name>Localized labels</Name>
          <TextSymbolizer>
                <Label>
                <ogc:Function name="Recode">
                    <ogc:Function name="language"/>
                    <ogc:Literal/>
                    <ogc:PropertyName>label_default</ogc:PropertyName>
                    <ogc:Literal>de</ogc:Literal>
                    <ogc:PropertyName>label_de</ogc:PropertyName>
                    <ogc:Literal>fr</ogc:Literal>
                    <ogc:PropertyName>label_fr</ogc:PropertyName>
                </ogc:Function>
                </Label>
                <Fill>
                <CssParameter name="fill">#000000</CssParameter>
                </Fill>
            </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
