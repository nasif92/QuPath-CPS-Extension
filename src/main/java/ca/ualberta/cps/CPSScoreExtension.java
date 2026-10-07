package ca.ualberta.cps;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

import qupath.fx.dialogs.Dialogs;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.IconFactory;
import qupath.lib.gui.tools.IconFactory.PathIcons;
import qupath.lib.images.ImageData;

/**
 * CPS Score extension: a toolbar toggle that starts/finishes timed PD-L1 (CPS) scoring,
 * plus the "PD-L1" tab in the analysis pane where the CPS is entered and exported.
 * Works with an unmodified QuPath install.
 */
public class CPSScoreExtension implements QuPathExtension {

    private static final String NAME = "CPS Score";
    private static final String DESCRIPTION =
            "Timed PD-L1 CPS scoring: viewport cell counts, CPS entry and CSV export.";
    private static final Version QUPATH_VERSION = Version.parse("0.7.0");

    private boolean isInstalled = false;

    @Override
    public void installExtension(QuPathGUI qupath) {
        if (isInstalled)
            return;
        isInstalled = true;

        ScoringTab.install(qupath);
        ToggleButton button = addToolbarButton(qupath);
        if (button != null)
            addMenuItem(qupath, button);
    }

    private void addMenuItem(QuPathGUI qupath, ToggleButton button) {
        var menu = qupath.getMenu("Extensions>" + NAME, true);
        // Same as clicking the toolbar button: starts scoring, or finishes it if already running
        MenuItem item = new MenuItem("Start / finish PD-L1 scoring");
        item.setOnAction(e -> button.fire());
        menu.getItems().add(item);
    }

    /** Builds the toggle (logic ported from the fork's ToolBarComponent) and adds it to the toolbar. */
    private ToggleButton addToolbarButton(QuPathGUI qupath) {
        var toolbar = qupath.getToolBar();
        if (toolbar == null)
            return null;

        var btn = new ToggleButton();
        btn.setId("cpsScoreButton");
        btn.setTooltip(new Tooltip("Count PD-L1 events in the view"));
        Node pdl1Icon = IconFactory.createNode(
                QuPathGUI.TOOLBAR_ICON_SIZE, QuPathGUI.TOOLBAR_ICON_SIZE, PathIcons.CELL_NUCLEI_BOTH);
        btn.setGraphic(pdl1Icon);

        // Shown while scoring is running
        Label timerIcon = new Label("⏱"); // stopwatch
        timerIcon.setStyle("-fx-font-size: 16px; -fx-padding: 0 2 0 2;");

        // Avoid re-entrant loops when we flip the toggle programmatically
        final AtomicBoolean toggleGuard = new AtomicBoolean(false);

        btn.selectedProperty().addListener((obs, was, is) -> {
            if (toggleGuard.get())
                return;

            var viewer = qupath.getViewer();
            if (viewer == null || viewer.getImageData() == null) {
                Dialogs.showInfoNotification("PD-L1", "Open an image first.");
                setSelectedQuietly(btn, toggleGuard, false);
                return;
            }
            ImageData<BufferedImage> imageData = viewer.getImageData();

            if (Boolean.TRUE.equals(is)) {
                // ---- START scoring ----
                // Consent + participant name
                String user = PDL1Tools.promptForUserWithPrivacy();
                if (user == null) {
                    setSelectedQuietly(btn, toggleGuard, false); // declined -> revert
                    return;
                }
                PDL1Tools.setCurrentUser(user); // used by the exporter later

                PDL1Timer.start(imageData);
                PDL1Tools.startViewportCounter(viewer);

                // Hide the analysis pane AFTER scoring begins
                qupath.showAnalysisPaneProperty().set(false);

                // Force magnification to 20x
                Platform.runLater(() -> PDL1Tools.magnify_viewer(viewer));

                btn.setGraphic(timerIcon);
                btn.setTooltip(new Tooltip("Finish scoring (enter CPS)"));

            } else {
                // ---- FINISH scoring (prompt CPS) ----
                Float current = PDL1Tools.readCpsFromProjectMetadata(imageData);
                Float cps = PDL1Tools.promptForCpsScore(current);
                if (cps == null) {
                    setSelectedQuietly(btn, toggleGuard, true); // cancelled -> keep scoring
                    return;
                }
                if (!PDL1Tools.writeCpsToProjectMetadata(imageData, cps)) {
                    setSelectedQuietly(btn, toggleGuard, true); // could not save -> keep scoring
                    return;
                }

                PDL1Timer.stop(imageData);
                PDL1Tools.stopViewportCounter();

                // Bring the analysis pane back and open the PD-L1 tab
                ScoringTab.show(qupath, imageData, true);

                btn.setGraphic(pdl1Icon);
                btn.setTooltip(new Tooltip("Start PD-L1 scoring"));
            }
        });

        // Sits right after the drawing tools (before the selection-mode group), like a tool button
        var items = toolbar.getItems();
        int at = indexOfSecondSeparator(items);
        if (at < 0)
            items.addAll(new Separator(), btn);
        else
            items.add(at, btn);
        return btn;
    }

    private static void setSelectedQuietly(ToggleButton btn, AtomicBoolean guard, boolean selected) {
        guard.set(true);
        try {
            btn.setSelected(selected);
        } finally {
            guard.set(false);
        }
    }

    /**
     * QuPath's toolbar is: [analysis pane] | [drawing tools] | [selection mode] | ...
     * so the second separator marks the end of the drawing tools.
     * Returns -1 if the layout isn't recognised.
     */
    private static int indexOfSecondSeparator(List<Node> items) {
        int separators = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Separator && ++separators == 2)
                return i;
        }
        return -1;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Version getQuPathVersion() {
        return QUPATH_VERSION;
    }
}
