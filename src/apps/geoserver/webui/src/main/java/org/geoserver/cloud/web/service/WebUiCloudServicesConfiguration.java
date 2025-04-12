/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.web.service;

import org.geoserver.cloud.web.ui.GeoServerCloudHomePageContentProvider;
import org.geoserver.cloud.web.ui.ServiceRegistryPage;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.Category;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.MenuPageInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = true)
public class WebUiCloudServicesConfiguration {

    @Bean
    ServiceInstanceRegistry cloudServiceRegistry(DiscoveryClient client) {
        return new ServiceInstanceRegistry(client);
    }

    @Bean
    Category cloudCategory() {
        Category category = new Category();
        category.setNameKey("category.cloud");
        category.setOrder(150);
        return category;
    }

    // <bean id="workspaceMenuPage" class="org.geoserver.web.MenuPageInfo">
    // <property name="id" value="workspaces" />
    // <property name="titleKey" value="WorkspacePage.title" />
    // <property name="descriptionKey" value="WorkspacePage.description" />
    // <property name="componentClass"
    // value="org.geoserver.web.data.workspace.WorkspacePage" />
    // <property name="category" ref="dataCategory" />
    // <property name="icon" value="../../img/icons/silk/folder.png" />
    // <property name="order" value="10" />
    // <property name="authorizer" ref="workspaceAdminAuthorizer"/>
    // </bean>

    @Bean
    MenuPageInfo<ServiceRegistryPage> serviceRegistryMenuPage(
            @Qualifier("aboutStatusCategory") Category aboutStatusCategory) {
        MenuPageInfo<ServiceRegistryPage> menu = new MenuPageInfo<>();
        menu.setId("serviceRegistry");
        menu.setCategory(aboutStatusCategory);
        menu.setTitleKey("ServiceRegistryPage.title");
        menu.setDescriptionKey("ServiceRegistryPage.description");
        menu.setComponentClass(ServiceRegistryPage.class);
        menu.setOrder(1000);
        menu.setAuthorizer(ComponentAuthorizer.ADMIN);
        return menu;
    }

    @Bean
    GeoServerCloudHomePageContentProvider geoServerCloudHomePageContentProvider(
            GeoServerSecurityManager secManager, BuildProperties props) {
        return new GeoServerCloudHomePageContentProvider(secManager, props);
    }
}
