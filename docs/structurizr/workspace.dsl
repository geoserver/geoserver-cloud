workspace "GeoServer Cloud" "Architecture documentation for GeoServer Cloud" {

    model {
        properties {
            "structurizr.groupSeparator" "/"
        }

        user = person "User" "A user of GeoServer Cloud"
        developer = person "Developer" "A developer extending or deploying GeoServer Cloud"

        geoserverCloud = softwareSystem "GeoServer Cloud" "Cloud Native GeoServer" {
            
            group "Infrastructure" {
                gateway = container "Gateway Service" "API Gateway and Load Balancer" "Spring Cloud Gateway"
                discovery = container "Discovery Service" "Service Registry" "Eureka"
                config = container "Config Service" "Centralized Configuration" "Spring Cloud Config"
            }

            group "Microservices" {
                wfs = container "WFS Service" "Web Feature Service" "GeoServer WFS"
                wms = container "WMS Service" "Web Map Service" "GeoServer WMS"
                wcs = container "WCS Service" "Web Coverage Service" "GeoServer WCS"
                rest = container "REST Service" "REST Configuration API" "GeoServer REST"
            }

            group "Frontends" {
                webui = container "Web UI" "Administration Interface" "GeoServer Web UI"
            }
        }

        user -> gateway "Uses"
        developer -> gateway "Configures"
        
        gateway -> wfs "Routes to"
        gateway -> wms "Routes to"
        gateway -> wcs "Routes to"
        gateway -> rest "Routes to"
        gateway -> webui "Routes to"

        wfs -> discovery "Registers with"
        wms -> discovery "Registers with"
        wcs -> discovery "Registers with"
        rest -> discovery "Registers with"
        webui -> discovery "Registers with"
        gateway -> discovery "Discovers services from"
        
        wfs -> config "Gets config from"
        wms -> config "Gets config from"
        wcs -> config "Gets config from"
        rest -> config "Gets config from"
        webui -> config "Gets config from"
        gateway -> config "Gets config from"
    }

    views {
        systemContext geoserverCloud "SystemContext" {
            include *
            autoLayout
        }

        container geoserverCloud "Containers" {
            include *
            autoLayout
        }

        styles {
            element "Person" {
                shape Person
                background #08427B
                color #ffffff
            }
            element "Software System" {
                background #1168BD
                color #ffffff
            }
            element "Container" {
                background #438DD5
                color #ffffff
            }
        }
    }
}