package org.example.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class NavigationService {
    private final BooleanProperty extendedMode = new SimpleBooleanProperty(false);

    public BooleanProperty extendedModeProperty() {
        return extendedMode;
    }

    public boolean isExtendedMode() {
        return extendedMode.get();
    }

    public void toggleNavigationMode() {
        extendedMode.set(!extendedMode.get());
    }

    public void collapseNavigation() {
        extendedMode.set(false);
    }
}
