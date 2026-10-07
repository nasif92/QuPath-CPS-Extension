package ca.ualberta.cps;

import java.awt.image.BufferedImage;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;

/**
 * Owns the "PD-L1" tab that sits in QuPath's analysis pane (next to Project, Image,
 * Annotations...). The tab is added once at startup, stays disabled until scoring finishes,
 * and is only touched through QuPath's public API.
 */
final class ScoringTab {

    private static final Logger logger = LoggerFactory.getLogger(ScoringTab.class);

    private static TabPane tabPane;
    private static Tab tab;
    private static PDL1ScoringPane pane;

    private ScoringTab() {}

    static void install(QuPathGUI qupath) {
        if (tab != null)
            return;
        tabPane = qupath.getAnalysisTabPane();
        if (tabPane == null) {
            logger.warn("Analysis tab pane not available - the PD-L1 scoring tab was not added");
            return;
        }
        pane = new PDL1ScoringPane();
        tab = new Tab("PD-L1");
        tab.setClosable(false);
        tab.setContent(pane);
        tab.setDisable(true); // enabled when scoring finishes
        tabPane.getTabs().add(tab);
    }

    /** Make the analysis pane visible and switch to the PD-L1 tab. */
    static void show(QuPathGUI qupath, ImageData<BufferedImage> imageData, boolean toolMode) {
        if (tab == null)
            return;
        qupath.showAnalysisPaneProperty().set(true);
        pane.setToolMode(toolMode);
        pane.bindTo(imageData);
        tab.setDisable(false);
        tabPane.getSelectionModel().select(tab);
    }

    static void hide() {
        if (tab != null)
            tab.setDisable(true);
    }

    /** The Project tab is the first tab in QuPath's analysis pane. */
    static void selectProjectTab() {
        if (tabPane != null && !tabPane.getTabs().isEmpty())
            tabPane.getSelectionModel().select(0);
    }
}
