package com.envelopes.apps.labelprinter;

import com.envelopes.apps.labelprinter.paper.Label3x2;
import com.sun.javafx.PlatformUtil;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by Manu on 7/27/2016.
 */
public class LabelPrinterWidget extends LabelPrinter {


    protected StackPane root;
    protected static int uiWidth = 350;
    protected static int uiHeight = 40;
    protected static int minY = -3;
    protected boolean panelVisible = false;

    public static void main(String[] args) {
        try {
            LabelHelper.initializeLabelPrinter();
        } catch (Exception e) {
            initializationException = new Exception("An initialization error occurred and can't open Label Printer Widget", e);
        }
        launch(LabelPrinterWidget.class, args);
    }

    protected void buildUI(Stage primaryStage) {

        this.createRootNode();
        this.setUpPrimaryStage(primaryStage);
        VBox mainContainer = new VBox();
        mainContainer.getChildren().add(createUIBody());
        root.getChildren().add(mainContainer);
        this.snapToTopCenter();
        primaryStage.setScene(createScene());
//        ResizeHelper.addResizeListener(primaryStage);
        primaryStage.show();
    }

    protected void createRootNode() {
        root = new StackPane();
        root.setId("widget-root");
//        this.bindDraggable();
    }

    protected void setUpPrimaryStage(Stage primaryStage) {
        primaryStage.setAlwaysOnTop(true);
        primaryStage.getIcons().add(new Image(LabelPrinter.class.getResourceAsStream("/assets/images/logo.png")));
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.focusedProperty().addListener((observableValue, oldValue, newValue) -> {
            if(newValue) {
                root.setStyle("-fx-opacity: 100%");
            } else {
                root.setStyle("-fx-opacity: 90%");
            }
        });
        this.primaryStage = primaryStage;
    }

    protected Node createUIBody() {
        StackPane body = new StackPane();
        body.setId("widget-ui-body");
        body.getChildren().addAll(createMiniWidgetBody()/*, createFullWidgetBody()*/);
        return body;
    }

    protected Node createMiniWidgetBody() {
        VBox miniWidgetBody = new VBox();
        miniWidgetBody.setId("mini-widget-body");
        miniWidgetBody.getChildren().add(createMiniTitleBar());
        return miniWidgetBody;
    }

    protected Node createFullWidgetBody() {
        VBox fullWidgetBody = new VBox();
        fullWidgetBody.setId("full-widget-body");
        fullWidgetBody.setOpacity(0);
        fullWidgetBody.setVisible(false);
        fullWidgetBody.getChildren().addAll(createFullTitleBar(), createLabelPanel(), createPanelFooter());
        return fullWidgetBody;
    }

    protected void showLabels(String orderIdOrProductIdWithQty) {
        if(!panelVisible) {
            showLabelPanel();
        }
        maskerPane.setVisible(true);
        executorService.submit(() -> {
            Future<List<Map<String, String>>> future = executorService.submit(() -> LabelHelper.getLabelsForOrderIdOrProductId(orderIdOrProductIdWithQty, true));
            List<Map<String, String>> labels = null;
            try {
                labels = future.get();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    maskerPane.setVisible(false);
                    if(e.getCause() != null) {
                        if(e.getCause().getCause() != null) {
                            showError(e.getCause().getCause());
                        } else {
                            showAlert(Alert.AlertType.ERROR, new String[] {e.getCause().getMessage()}, null);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, new String[] {e.getMessage()}, null);
                    }
                    hideLabelPanel();
                });
            }
            if(labels != null) {

                final List<Map<String, String>> labelList = labels;
                Platform.runLater(() -> {
                    renderLabels(labelList);
                    maskerPane.setVisible(false);
                });
            }
        });
    }

    protected void reRenderLabel(Map<String, String> label) {
        try {
            FileInputStream inputStream = new FileInputStream(LabelHelper.LABEL_PRINTER_CACHE_LOCATION + label.get("labelPath"));
            ((ImageView)primaryStage.getScene().lookup("#label-image-" + label.get("productId"))).setImage(new Image(inputStream));
            inputStream.close();
            hideLabelEditOption();
            ((Button)primaryStage.getScene().lookup("#label-edit-button-" + label.get("productId"))).setOnAction(actionEvent -> showLabelEditOption(label.get("productId"), label.get("labelQty"), label.get("copies")));
            labelVOs.put(label.get("productId"), new LabelVO(label.get("productId"), label.get("labelQty"), label.get("copies"), label.get("copies"), label.get("labelPDFPath")));
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, new String[] {e.getCause().getMessage()}, null);
            hideLabelEditOption();
        }
    }

    protected void renderLabels(List<Map<String, String>> labels) {
        FlowPane labelsPane = (FlowPane)primaryStage.getScene().lookup("#labels-pane");
        labelsPane.getChildren().clear();
        for(Map<String, String> label : labels) {
            try {
                StackPane labelPane = new StackPane();
                labelPane.setId("label-pane-" + label.get("productId"));
                labelPane.getStyleClass().add("label-pane");
                labelPane.setAlignment(Pos.BOTTOM_CENTER);
                Button editButton = new Button("  ");
                editButton.setTooltip(new Tooltip("Edit Label"));
                editButton.setId("label-edit-button-" + label.get("productId"));
                editButton.setStyle("-fx-cursor: hand");
                editButton.setBackground(new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/edit-icon1.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
                editButton.setOnAction(actionEvent -> showLabelEditOption(label.get("productId"), label.get("labelQty"), label.get("copies")));

                Button printButton = new Button(" ");
                printButton.setStyle("-fx-cursor: hand");
                printButton.setId("label-toggle-print-" + label.get("productId"));
                printButton.setTooltip(new Tooltip("Don't Print This Label"));
                printButton.setBackground(new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/icon-print.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
                printButton.setOnAction(actionEvent -> toggleLabel(label.get("productId"), false));

                HBox printSettings = new HBox();
                printSettings.setSpacing(5);
                printSettings.getChildren().addAll(editButton, printButton);
                printSettings.setAlignment(Pos.TOP_RIGHT);

                labelPane.setStyle("-fx-background-color: #ffffff;-fx-background-radius: 5;-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");
                labelPane.setMaxWidth(245);
                StackPane disablePrintingPane = new StackPane();
                disablePrintingPane.setVisible(false);
                disablePrintingPane.setId("label-no-print-" + label.get("productId"));
                disablePrintingPane.setStyle("-fx-cursor: hand");
                disablePrintingPane.setBackground(new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/image-no-print.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
                FileInputStream inputStream = new FileInputStream(LabelHelper.LABEL_PRINTER_CACHE_LOCATION + label.get("labelPath"));
                ImageView labelImage = new ImageView(new Image(inputStream));
                labelImage.setId("label-image-" + label.get("productId"));
                labelImage.setSmooth(false);
                labelImage.setPreserveRatio(true);
                labelImage.setFitWidth(245);
                StackPane labelImagePane = new StackPane();
                labelImagePane.setAlignment(Pos.TOP_RIGHT);
                labelImagePane.setPadding(new Insets(10));
                labelImagePane.setOnMouseClicked((mouseEvent) -> hideLabelEditOption());
                labelImagePane.getChildren().addAll(labelImage, disablePrintingPane, printSettings);

                labelPane.getChildren().addAll(labelImagePane);
                inputStream.close();
                labelsPane.getChildren().addAll(labelPane);
                labelVOs.put(label.get("productId"), new LabelVO(label.get("productId"), label.get("labelQty"), label.get("copies"), label.get("copies"), label.get("labelPDFPath")));
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, new String[] {e.getCause().getMessage()}, null);
            }
        }
        ((Label)primaryStage.getScene().lookup("#items-number-label")).setText("Labels(s) : " + labels.size());
        ((Button)primaryStage.getScene().lookup("#print-button")).setText("Print (" + labels.size() + ")");
    }

    protected String editingProductId = "";
    protected Map<String, LabelVO> labelVOs = new HashMap<>();
    protected void showLabelEditOption(String productId, String qty, String copies) {
        hideLabelEditOption();
        editingProductId = productId;
        primaryStage.getScene().lookup("#label-edit-button-" + editingProductId).setVisible(false);
        HBox labelEditOptionsPanel = createLabelEditOptionsPanel(productId);
        ((TextField)labelEditOptionsPanel.lookup("#label-qty")).setText(qty);
        ((TextField)labelEditOptionsPanel.lookup("#label-copies")).setText(copies);
        ((StackPane)primaryStage.getScene().lookup("#label-pane-" + productId)).getChildren().add(labelEditOptionsPanel);
    }

    protected void hideLabelEditOption() {
        if(!editingProductId.isEmpty()) {
            primaryStage.getScene().lookup("#label-edit-button-" + editingProductId).setVisible(true);
            StackPane labelPane = ((StackPane)primaryStage.getScene().lookup("#label-pane-" + editingProductId));
            if(labelPane.getChildren().size() > 1) {
                labelPane.getChildren().remove(1);
            }
            editingProductId = "";
        }
    }

    protected void updatePrintLabelCount() {
        int count = getSelectedLabels().size();
        Button printButton = ((Button)primaryStage.getScene().lookup("#print-button"));
        printButton.setText("Print (" + count + ")");
        if(count > 0) {
            printButton.setStyle("-fx-background-color: linear-gradient(green, darkgreen)");
        } else {
            printButton.setStyle("-fx-background-color: linear-gradient(grey, darkgrey)");
        }
    }

    protected List<LabelVO> getSelectedLabels() {
        List<LabelVO> selectedLabels = new ArrayList<>();
        for(String key : labelVOs.keySet()) {
            LabelVO labelVO = labelVOs.get(key);
            if(labelVO.isPrintable()) {
                selectedLabels.add(labelVO);
            }
        }
        return selectedLabels;
    }

    protected HBox createLabelEditOptionsPanel(String productId) {
        HBox editOptionsPanel = new HBox();
        editOptionsPanel.setId("label-edit-panel");
        editOptionsPanel.setAlignment(Pos.CENTER);
        editOptionsPanel.setSpacing(10);
        editOptionsPanel.setPadding(new Insets(2, 10, 2, 10));
        editOptionsPanel.setStyle("-fx-background-color: #394046;-fx-background-radius: 0;");
        editOptionsPanel.setMaxHeight(20);

        Label qtyLabel = new Label("Qty:");

        qtyLabel.setStyle("-fx-text-fill: white;-fx-font-family: Lato; -fx-font-size: 14px; -fx-font-weight: bold");
        TextField qtyTextField = new TextField();
        qtyTextField.setId("label-qty");
        qtyTextField.setMaxWidth(60);
        qtyTextField.setAlignment(Pos.CENTER);
        qtyTextField.setPadding(new Insets(0));
        qtyTextField.setText("");


        Label copiesLabel = new Label("Copies:");
        copiesLabel.setStyle("-fx-text-fill: white;-fx-font-family: Lato; -fx-font-size: 14px; -fx-font-weight: bold");
        TextField copiesTextField = new TextField();
        copiesTextField.setId("label-copies");
        copiesTextField.setMaxWidth(30);
        copiesTextField.setAlignment(Pos.CENTER);
        copiesTextField.setPadding(new Insets(0));
        copiesTextField.setText("");

        Button applyButton = new Button("O K");
        applyButton.getStyleClass().addAll("micro-btn", "green-btn");
        applyButton.setOnAction(actionEvent -> updateLabel(productId, qtyTextField.getText().trim(), copiesTextField.getText().trim()));

        editOptionsPanel.getChildren().addAll(qtyLabel, qtyTextField, copiesLabel, copiesTextField, applyButton);

        return editOptionsPanel;
    }

    protected void updateLabel(String productId, final String qty, String copies) {
        LabelVO labelVO = labelVOs.get(productId);
        if(qty.equals(labelVO.getLabelQty())) {
            hideLabelEditOption();
            if(!copies.equals(labelVO.getRequiredCopies())) {
                labelVO.setRequiredCopies(copies);
                toggleLabel(productId, labelVO.isPrintable());
            }
        } else {
            maskerPane.setVisible(true);
            executorService.submit(() -> {
                Future<List<Map<String, String>>> future = executorService.submit(() -> LabelHelper.getLabelsForOrderIdOrProductId(productId + "-" + qty, true));
                List<Map<String, String>> labels = null;
                try {
                    labels = future.get();
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        maskerPane.setVisible(false);
                        if (e.getCause() != null) {
                            if (e.getCause().getCause() != null) {
                                showError(e.getCause().getCause());
                            } else {
                                showAlert(Alert.AlertType.ERROR, new String[]{e.getCause().getMessage()}, null);
                            }
                        } else {
                            showAlert(Alert.AlertType.ERROR, new String[]{e.getMessage()}, null);
                        }
                        hideLabelPanel();
                    });
                }
                if (labels != null) {
                    final List<Map<String, String>> labelList = labels;
                    if (labels.size() > 0) {
                        Platform.runLater(() -> {
                            reRenderLabel(labelList.get(0));
                            if(!copies.equals(labelVO.getRequiredCopies())) {
                                labelVO.setRequiredCopies(copies);
                                toggleLabel(productId, labelVO.isPrintable());
                            }
                            maskerPane.setVisible(false);
                        });
                    }
                }
            });
        }
    }

    protected void toggleLabel(String productId, boolean activeFlag) {
        LabelVO labelVO = labelVOs.get(productId);
        if(activeFlag) {
            primaryStage.getScene().lookup("#label-image-" + productId).setStyle("-fx-opacity: 100%");
            primaryStage.getScene().lookup("#label-no-print-" + productId).setVisible(false);
            Button togglePrintButton = (Button)primaryStage.getScene().lookup("#label-toggle-print-" + productId);
            togglePrintButton.setBackground(new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/icon-print.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
            togglePrintButton.setOnAction(actionEvent -> toggleLabel(productId, false));
            togglePrintButton.setTooltip(new Tooltip("Don't Print This label"));
            labelVO.setRequiredCopies(labelVO.getCopies());
        } else {
            primaryStage.getScene().lookup("#label-image-" + productId).setStyle("-fx-opacity: 20%");
            primaryStage.getScene().lookup("#label-no-print-" + productId).setVisible(true);
            Button togglePrintButton = (Button)primaryStage.getScene().lookup("#label-toggle-print-" + productId);
            togglePrintButton.setBackground(new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/icon-no-print.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
            togglePrintButton.setOnAction(actionEvent -> toggleLabel(productId, true));
            togglePrintButton.setTooltip(new Tooltip("Print This label"));
            labelVO.setRequiredCopies("0");
        }
        hideLabelEditOption();
        ((Button)primaryStage.getScene().lookup("#label-edit-button-" + labelVO.getProductId())).setOnAction(actionEvent -> showLabelEditOption(labelVO.getProductId(), labelVO.getLabelQty(), labelVO.getRequiredCopies()));
        updatePrintLabelCount();
    }

    protected Node createMiniTitleBar() {
        HBox miniTitleBar = new HBox();
        miniTitleBar.setId("mini-title-bar");
        miniTitleBar.setSpacing(10);
        miniTitleBar.setAlignment(Pos.CENTER_LEFT);
        miniTitleBar.setPadding(new Insets(5, 5, 5, 12));

        ImageView logo = new ImageView(new Image(LabelPrinter.class.getResourceAsStream("/assets/images/logo.png")));
        logo.setFitWidth(60);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setCache(true);

        final TextField idField = new TextField();
        idField.setId("id-field");
        idField.setTextFormatter(textFormatter);
        idField.setPrefSize(140, 20);
        idField.setOnAction(actionEvent -> {
            if(idField.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, new String[] {"\nPlease enter a valid Order Id."}, null);
            } else {
                showLabels(idField.getText());
            }

        });
        nodeReferences.add(idField);                    //TODO replace with selector
        nodeReferenceMap.put("idTextField", idField);   //TODO replace with selector

        final Button goButton = new Button("G O");
        goButton.getStyleClass().addAll("mini-btn", "green-btn");
        goButton.setPrefSize(50, 10);
        goButton.setOnAction(actionEvent -> {
            if(idField.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, new String[] {"\nPlease enter a valid Order Id."}, null);
            } else {
                showLabels(idField.getText());
            }
        });

        miniTitleBar.getChildren().addAll(logo, idField, goButton);

        HBox titleBarButtons = (HBox)createMiniTitleBarButtons();
        miniTitleBar.getChildren().addAll(titleBarButtons);
        HBox.setHgrow(titleBarButtons, Priority.ALWAYS);

        miniTitleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        miniTitleBar.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
            if(primaryStage.getY() < minY) {
                primaryStage.setY(minY);
            }
        });

        return miniTitleBar;
    }

   /* protected void showMiniLabelForSKU(String productOrOrderId) {

        showLabelPanel();
        maskerPane.setVisible(true);
        executorService.submit(() -> {
            Future<LabelObject> future = executorService.submit(() -> LabelHelper.getLabelForProductId(productOrOrderId, true, LabelHelper.DEFAULT_LABEL_TYPE.startsWith("MINI")));
            LabelObject labelObject = null;
            try {
                labelObject = future.get();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    maskerPane.setVisible(false);
                    if(e.getCause() != null) {
                        if(e.getCause().getCause() != null) {
                            showError(e.getCause().getCause());
                        } else {
                            showAlert(Alert.AlertType.ERROR, new String[] {e.getCause().getMessage()}, primaryStage);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, new String[] {e.getMessage()}, primaryStage);
                    }
                });
            }
            if(labelObject != null) {

                final LabelObject obj = labelObject;
                Platform.runLater(() -> {
                    rootPane.setCenter(addMainArea(obj));
                    if(!obj.isMiniLabel()) {
                        nodeReferences.get(1).requestFocus();
                    }
                    maskerPane.setVisible(false);
                });
//            rootPane.setRight(addRightAreaForSKU());
            }
        });
    }*/

    protected Node createFullTitleBar() {
        HBox fullTitleBar = (HBox)createTitleBar();
        fullTitleBar.setId("full-title-bar");
        return fullTitleBar;
    }

    protected Node createMiniTitleBarButtons() {
        HBox titleBarButtons = new HBox();
        titleBarButtons.setPadding(new Insets(3, 5, 3, 0));
        titleBarButtons.setStyle("-fx-background-color: #242C34;");
        titleBarButtons.setAlignment(Pos.CENTER_RIGHT);

        Background minBackground = new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/min-button.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT));
        Background closeBackground = new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/close-button.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT));

        Button minBtn = new Button("   ");
        minBtn.setStyle("-fx-cursor: default");
        minBtn.setBackground(minBackground);
        minBtn.setOnAction(actionEvent -> ((Stage) primaryStage.getScene().getWindow()).setIconified(true));

        Button closeBtn = new Button("  ");
        closeBtn.setStyle("-fx-cursor: default");
        closeBtn.setBackground(closeBackground);
        closeBtn.setOnAction(actionEvent -> Platform.exit());

        titleBarButtons.getChildren().addAll(minBtn, closeBtn);

        return titleBarButtons;
    }

    protected Node createPanelFooter() {

        HBox panelFooterBar = new HBox();
        panelFooterBar.setSpacing(10);
        panelFooterBar.setAlignment(Pos.CENTER_LEFT);
        panelFooterBar.setStyle("-fx-background-color: #394046;-fx-font-weight: bold;-fx-font-size: 14px ");
        panelFooterBar.setPadding(new Insets(5, 5, 5, 12));

        Label itemsLabel = new Label("");
        itemsLabel.setId("items-number-label");
        itemsLabel.getStyleClass().add("oswald-bold");

        panelFooterBar.getChildren().addAll(itemsLabel);

        HBox panelFooterBarButtons = new HBox();
        panelFooterBarButtons.setSpacing(20);

        Button printButton = new Button("Print");
        printButton.setId("print-button");
        printButton.getStyleClass().addAll("mini-btn", "green-btn");
        printButton.setMinWidth(75);
        printButton.setOnAction(actionEvent -> printSelectedLabels());

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("mini-btn", "red-btn");
        cancelButton.setMinWidth(75);
        cancelButton.setOnAction(actionEvent -> {
            hideLabelPanel();
        });

        panelFooterBarButtons.getChildren().addAll(printButton, cancelButton);

        panelFooterBarButtons.setAlignment(Pos.CENTER_RIGHT);
        panelFooterBar.getChildren().addAll(panelFooterBarButtons);
        HBox.setHgrow(panelFooterBarButtons, Priority.ALWAYS);

        return panelFooterBar;
    }

    protected void printSelectedLabels() {
        List<LabelVO> selectedLabels = getSelectedLabels();
        if (selectedLabels.size() <= 0) {
            showAlert(Alert.AlertType.ERROR, new String[]{"Please select at least one label to print"}, primaryStage);
        }

        for(LabelVO label : selectedLabels) {
            try {
                new PDFPrinter(new File(LabelHelper.LABEL_PRINTER_CACHE_LOCATION + label.getLabelPDFPath()), Integer.parseInt(label.getRequiredCopies()), new Label3x2());
            } catch (Exception e) {
                showError(e);
            }
        }
    }

    protected Node createLabelPanel() {
        StackPane stackPane = new StackPane();
        stackPane.setStyle("-fx-background-color: #899096;");
        stackPane.setMinHeight(200);
        VBox vBox = new VBox();
        vBox.setStyle("-fx-background-color: #899096;");
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setPadding(new Insets(10, 2, 10, 10));
        vBox.setSpacing(20);

        ScrollPane scrollPane = new ScrollPane();
        vBox.getStyleClass().add("label");
        vBox.getChildren().addAll(scrollPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        FlowPane flowPane = new FlowPane();
        flowPane.setId("labels-pane");
        flowPane.setPadding(new Insets(10));
        flowPane.setAlignment(Pos.CENTER);


        flowPane.setStyle("-fx-background-color: #899096");
        flowPane.setVgap(10);
        flowPane.setHgap(10);
        /*for(int i = 0; i < 15; i ++) {
            try {
                StackPane labelPane = new StackPane();
                labelPane.setPadding(new Insets(10));
                labelPane.setStyle("fx-padding: 10;-fx-background-color: #ffffff;-fx-background-radius: 5;-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");
                labelPane.setMaxWidth(245);
                FileInputStream inputStream = new FileInputStream("E:\\usr\\local\\LabelPrinter\\ProductLabels\\MiniLabels\\A7FFW-B.png");
                ImageView labelImage = new ImageView(new Image(inputStream));
                labelImage.setSmooth(false);
                labelImage.setPreserveRatio(true);

                labelImage.setFitWidth(245);
                labelPane.getChildren().add(labelImage);
                inputStream.close();
                flowPane.getChildren().add(labelPane);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        scrollPane.setContent(flowPane);
        stackPane.getChildren().addAll(scrollPane, maskerPane);
        return stackPane;
    }

    protected Node createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setSpacing(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-background-color: #242C34;-fx-font-weight: bold;-fx-font-size: 14px ");
        titleBar.setPadding(new Insets(5, 5, 5, 12));

        ImageView logo = new ImageView(new Image(LabelPrinter.class.getResourceAsStream("/assets/images/logo.png")));
        logo.setFitWidth(60);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setCache(true);

        Label titleText = new Label("L A B E L    P R I N T E R");
        titleText.setId("title-text");
        titleText.setStyle("-fx-text-fill: aliceblue;-fx-font-weight:bold;-fx-font-family:Arial;");
        titleText.setOnMousePressed((mouseEvent) -> hideLabelPanel());
        titleBar.getChildren().addAll(logo, titleText);

        HBox titleBarButtons = new HBox();
        titleBarButtons.setStyle("-fx-background-color: #242C34;");
        Background minBackground = new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/min-button.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT));
        Background closeBackground = new Background(new BackgroundImage( new Image( getClass().getResource("/assets/images/close-button.png").toExternalForm()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT));

        Button minBtn = new Button("   ");
        minBtn.setBackground(minBackground);
        minBtn.setOnAction(actionEvent -> ((Stage) primaryStage.getScene().getWindow()).setIconified(true));

        Button closeBtn = new Button("  ");
        closeBtn.setBackground(closeBackground);
        closeBtn.setOnAction(actionEvent -> hideLabelPanel());

        titleBarButtons.getChildren().addAll(minBtn, closeBtn);
        titleBarButtons.setAlignment(Pos.CENTER_RIGHT);
        titleBar.getChildren().addAll(titleBarButtons);
        HBox.setHgrow(titleBarButtons, Priority.ALWAYS);

        return titleBar;
    }

    protected Scene createScene() {
        Scene scene = new Scene(root, uiWidth, uiHeight);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(LabelPrinter.class.getResource("/assets/css/LabelPrinter.css").toExternalForm());
        scene.getStylesheets().add("http://fonts.googleapis.com/css?family=Lato&subset=latin,latin-ext");
        scene.getStylesheets().add("https://fonts.googleapis.com/css?family=Oswald");
        return scene;
    }

    protected void snapToTopCenter() {
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX((primaryScreenBounds.getWidth() / 2) - 150);
        primaryStage.setY(minY);
    }
    protected void bindDraggable() {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
            if(primaryStage.getX() < 0) {
                primaryStage.setX(0);
            }

            if(primaryStage.getY() < minY) {
                primaryStage.setY(minY);
            }

            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            if(primaryStage.getX() > primaryScreenBounds.getWidth() - primaryStage.getWidth()) {
                primaryStage.setX(primaryScreenBounds.getWidth() - primaryStage.getWidth());
            }

            if(primaryStage.getY() > primaryScreenBounds.getHeight() - primaryStage.getHeight()) {
                primaryStage.setY(primaryScreenBounds.getHeight() - primaryStage.getHeight());
            }
        });
    }

    protected void showLabelPanel() {
        ((StackPane)primaryStage.getScene().lookup("#widget-ui-body")).getChildren().add(1, createFullWidgetBody());
        ((Label)primaryStage.getScene().lookup("#title-text")).setText(((TextField)primaryStage.getScene().lookup("#id-field")).getText());

        SimpleDoubleProperty stageWidthProperty = new SimpleDoubleProperty(primaryStage.getWidth());
        stageWidthProperty.addListener((observableValue, oldStageWidth, newStageWidth) -> {
            primaryStage.setWidth(newStageWidth.doubleValue());
        });

        SimpleDoubleProperty stageHeightProperty = new SimpleDoubleProperty(primaryStage.getHeight());
        stageHeightProperty.addListener((observableValue, oldStageHeight, newStageHeight) -> {
            primaryStage.setHeight(newStageHeight.doubleValue());
        });

        SimpleDoubleProperty stageXProperty = new SimpleDoubleProperty(primaryStage.getX());
        stageXProperty.addListener((observableValue, oldStageX, newStageX) -> {
            primaryStage.setX(newStageX.doubleValue());
        });

        VBox miniWidget = (VBox)primaryStage.getScene().lookup("#mini-widget-body");
        miniWidget.setVisible(true);
        SimpleDoubleProperty miniWidgetOpacityProperty = new SimpleDoubleProperty(miniWidget.getOpacity());
        miniWidgetOpacityProperty.addListener((observableValue, oldOpacity, newOpacity) -> {
            miniWidget.setOpacity(newOpacity.doubleValue());
        });

        VBox fullWidget = (VBox)primaryStage.getScene().lookup("#full-widget-body");
        fullWidget.setVisible(true);
        SimpleDoubleProperty fullWidgetOpacityProperty = new SimpleDoubleProperty(fullWidget.getOpacity());
        fullWidgetOpacityProperty.addListener((observableValue, oldOpacity, newOpacity) -> {
            fullWidget.setOpacity(newOpacity.doubleValue());
        });

        Timeline resizer = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(stageWidthProperty, primaryStage.getWidth(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.ZERO,       new KeyValue(stageHeightProperty, primaryStage.getHeight(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.ZERO,       new KeyValue(stageXProperty, primaryStage.getX(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.ZERO,       new KeyValue(miniWidgetOpacityProperty, miniWidget.getOpacity(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.ZERO,       new KeyValue(fullWidgetOpacityProperty, fullWidget.getOpacity(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(stageWidthProperty, primaryStage.getWidth() + 500, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(stageHeightProperty, primaryStage.getHeight() + 240, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(stageXProperty, primaryStage.getX() - 250, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(200), new KeyValue(miniWidgetOpacityProperty, 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(200), new KeyValue(fullWidgetOpacityProperty, 100, Interpolator.EASE_BOTH))
        );
        resizer.play();
        miniWidget.setVisible(false);
        panelVisible = true;
    }

    protected void hideLabelPanel() {


        SimpleDoubleProperty stageWidthProperty = new SimpleDoubleProperty(primaryStage.getWidth());
        stageWidthProperty.addListener((observableValue, oldStageWidth, newStageWidth) -> {
            primaryStage.setWidth(newStageWidth.doubleValue());
        });

        SimpleDoubleProperty stageHeightProperty = new SimpleDoubleProperty(primaryStage.getHeight());
        stageHeightProperty.addListener((observableValue, oldStageHeight, newStageHeight) -> {
            primaryStage.setHeight(newStageHeight.doubleValue());
        });

        SimpleDoubleProperty stageXProperty = new SimpleDoubleProperty(primaryStage.getX());
        stageXProperty.addListener((observableValue, oldStageX, newStageX) -> {
            primaryStage.setX(newStageX.doubleValue());
        });

        VBox miniWidget = (VBox)primaryStage.getScene().lookup("#mini-widget-body");
        miniWidget.setVisible(true);
        SimpleDoubleProperty miniWidgetOpacityProperty = new SimpleDoubleProperty(miniWidget.getOpacity());
        miniWidgetOpacityProperty.addListener((observableValue, oldOpacity, newOpacity) -> {
            miniWidget.setOpacity(newOpacity.doubleValue());
        });

        VBox fullWidget = (VBox)primaryStage.getScene().lookup("#full-widget-body");
        fullWidget.setVisible(true);
        SimpleDoubleProperty fullWidgetOpacityProperty = new SimpleDoubleProperty(fullWidget.getOpacity());
        fullWidgetOpacityProperty.addListener((observableValue, oldOpacity, newOpacity) -> {
            fullWidget.setOpacity(newOpacity.doubleValue());
        });

        Timeline resizer = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(stageWidthProperty, primaryStage.getWidth(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.ZERO,       new KeyValue(stageHeightProperty, primaryStage.getHeight(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.ZERO,       new KeyValue(stageXProperty, primaryStage.getX(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.ZERO,       new KeyValue(miniWidgetOpacityProperty, miniWidget.getOpacity(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.ZERO,       new KeyValue(fullWidgetOpacityProperty, fullWidget.getOpacity(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(stageWidthProperty, uiWidth, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(stageHeightProperty, uiHeight, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(stageXProperty, primaryStage.getX() + 250, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(200), new KeyValue(miniWidgetOpacityProperty, 100, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(200), new KeyValue(fullWidgetOpacityProperty, 0, Interpolator.EASE_BOTH))
        );
        resizer.play();
        ((StackPane)primaryStage.getScene().lookup("#widget-ui-body")).getChildren().remove(1);
        fullWidget.setVisible(false);
        panelVisible = false;
    }

    class LabelVO {
        String productId = "", labelQty = "", copies = "", requiredCopies = "", labelPDFPath = "";
        LabelVO(String productId, String labelQty, String copies, String requiredCopies, String labelPDFPath) {
            this.productId = productId;
            this.labelQty = labelQty;
            this.copies = copies;
            this.requiredCopies = requiredCopies;
            this.labelPDFPath = labelPDFPath;
        }

        public String getProductId() {
            return productId;
        }

        public String getLabelQty() {
            return labelQty;
        }

        public String getCopies() {
            return copies;
        }

        public String getRequiredCopies() {
            return requiredCopies;
        }

        public void setRequiredCopies(String requiredCopies) {
            this.requiredCopies = requiredCopies;
        }

        public String getLabelPDFPath() {
            return labelPDFPath;
        }

        public boolean isPrintable() {
            return NumberUtils.toInt(requiredCopies, 0) > 0;
        }
    }
}
