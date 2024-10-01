<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
                      xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                      xmlns="http://www.opengis.net/sld"
                      xmlns:ogc="http://www.opengis.net/ogc"
                      xmlns:se="http://www.opengis.net/se"
                      xmlns:xlink="http://www.w3.org/1999/xlink"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>localized_style</Name>
    <UserStyle>
      <Name>localized_no_default</Name>
      <FeatureTypeStyle>
        <Rule>
          <Name>Localized, no default value</Name>
            <Title>
              <Localized lang="en">English</Localized>
              <Localized lang="de">Deutsch</Localized>
              <Localized lang="fr">Français</Localized>
              <Localized lang="it">Italiano</Localized>
            </Title>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#FF0000</CssParameter>
                </Fill>
              </Mark>
              <Size>6</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
