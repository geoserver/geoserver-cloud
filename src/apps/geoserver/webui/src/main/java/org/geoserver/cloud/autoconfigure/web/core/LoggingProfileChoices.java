/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.web.core;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.application.ComponentInitializationListenerCollection;
import org.apache.wicket.application.IComponentInitializationListener;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.cloud.logging.profiles.LoggingProfiles;
import org.geoserver.cloud.web.ui.LoggingProfileLevelsPanel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerApplicationInitializer;
import org.geoserver.web.admin.GlobalSettingsPage;

/**
 * Lists the {@link LoggingProfiles} in the "Logging Profile" choice of the {@link GlobalSettingsPage}, and shows the
 * levels of the selected profile next to it, in a {@link LoggingProfileLevelsPanel}.
 *
 * <p>Upstream lists the logging configuration files found in the {@code logs} directory of the data directory, or the
 * standard profiles of vanilla GeoServer if there are none. GeoServer Cloud does not use those files.
 */
@RequiredArgsConstructor
class LoggingProfileChoices implements GeoServerApplicationInitializer, IComponentInitializationListener {

    /** The path of the choice in {@link GlobalSettingsPage} */
    static final String CHOICE_PATH = "form:log4jConfigFile";

    /**
     * The id of the {@link LoggingProfileLevelsPanel}, next to the choice in the markup of {@link GlobalSettingsPage}
     */
    static final String LEVELS_ID = "loggingProfileLevels";

    private final @NonNull LoggingProfiles profiles;

    @Override
    public void init(GeoServerApplication application) {
        ComponentInitializationListenerCollection listeners = application.getComponentInitializationListeners();
        // The application may initialize more than once, as in tests, and the listeners may be added twice
        listeners.remove(this);
        listeners.add(this);
    }

    @Override
    public void onInitialize(Component component) {
        if (component instanceof GlobalSettingsPage page) {
            ListChoice<String> choice = profileChoice(page);
            choice.setChoices(profiles.names());
            showLevels(choice);
        }
    }

    @SuppressWarnings("unchecked")
    private static ListChoice<String> profileChoice(GlobalSettingsPage page) {
        return (ListChoice<String>) page.get(CHOICE_PATH);
    }

    private static void showLevels(ListChoice<String> choice) {
        IModel<String> selectedProfile = Model.of(choice.getModelObject());
        LoggingProfileLevelsPanel levels = new LoggingProfileLevelsPanel(LEVELS_ID, selectedProfile);
        choice.getParent().add(levels);
        choice.add(new ShowLevelsOnChange(selectedProfile, levels));
    }

    /**
     * Shows the levels of the profile selected in the choice. It leaves the model of the choice unchanged, because the
     * page saves the selection with the other settings.
     */
    private static class ShowLevelsOnChange extends AjaxFormComponentUpdatingBehavior {

        private final IModel<String> selectedProfile;

        private final Component levels;

        ShowLevelsOnChange(IModel<String> selectedProfile, Component levels) {
            super("change");
            this.selectedProfile = selectedProfile;
            this.levels = levels;
        }

        @Override
        protected boolean getUpdateModel() {
            return false;
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            selectedProfile.setObject((String) getFormComponent().getConvertedInput());
            target.add(levels);
        }
    }
}
