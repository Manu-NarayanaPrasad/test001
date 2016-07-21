package com.envelopes.apps.labelprinter;

import com.envelopes.apps.labelprinter.paper.Label5x3;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.dialog.ExceptionDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.UnaryOperator;

/**
 * Created by Manu on 7/7/2016.
 */
public class LabelPrinter extends Application {

    private double xOffset = 0;
    private double yOffset = 0;
    protected Stage primaryStage;
    protected java.util.List<Node> nodeReferences = new ArrayList<>();

    protected static Exception initializationException = null;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private final MaskerPane maskerPane = new MaskerPane();
    {
        maskerPane.setText("Processing...");
        maskerPane.setVisible(false);
    }

    protected BorderPane rootPane;

    private UnaryOperator<TextFormatter.Change> filter = change -> {
        String text = change.getText();
        change.setText(text.toUpperCase());
        return change;
    };

    TextFormatter<String> textFormatter = new TextFormatter<>(filter);

    public static void main(String[] args) {
        try {
            LabelHelper.initializeLabelPrinter();
        } catch (Exception e) {
            initializationException = new Exception("An initialization error occurred and can't open Label Printer", e);
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        if(initializationException != null) {
            showError(initializationException);
        } else {
            this.primaryStage = primaryStage;
            primaryStage.initStyle(StageStyle.UNDECORATED);

            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            rootPane = new BorderPane();
            rootPane.setId("root");
            rootPane.setStyle("-fx-border-color: black; -fx-border-width: 1px; ");
            rootPane.setTop(addTopArea());
            rootPane.setBottom(addFooterArea());
            rootPane.setCenter(addMainArea(null));
            Scene scene = new Scene(rootPane, 1200, 700, Color.TRANSPARENT);
            primaryStage.setScene(scene);
            scene.getStylesheets().add(LabelPrinter.class.getResource("/assets/css/LabelPrinter.css").toExternalForm());
            ResizeHelper.addResizeListener(primaryStage);
            primaryStage.show();
            nodeReferences.get(0).requestFocus();
        }
    }

    private void showError(Throwable exception){
        ExceptionDialog dlg = new ExceptionDialog(exception);
        dlg.getDialogPane().setHeaderText("");
        dlg.setTitle("Label Printer - Envelopes.com");
        dlg.initStyle(StageStyle.UTILITY);
        dlg.initOwner(primaryStage);
        dlg.show();
        setFocus();
    }

    private void showAlert(Alert.AlertType type, String[] args) {
        Alert dlg = new Alert(type, "");
        dlg.setHeaderText("");
        dlg.setTitle("Label Printer - Envelopes.com");
        dlg.getDialogPane().setContentText(args[0]);
        dlg.initStyle(StageStyle.UTILITY);
        dlg.initOwner(primaryStage);
        dlg.show();
        setFocus();
    }

    protected void clearCenter() {
        rootPane.setRight(null);
        if(((StackPane)rootPane.getCenter()).getChildren().size() > 1) {
            ((StackPane) rootPane.getCenter()).getChildren().remove(0);
        }
    }

    protected void setFocus() {
        nodeReferences.get(0).requestFocus();
    }

    protected void showLabelForSKU(String productOrOrderId) {
        maskerPane.setVisible(true);
        clearCenter();
        executorService.submit(() -> {
            Future<LabelObject> future = executorService.submit(() -> LabelHelper.getLabelForProductId(productOrOrderId, true));
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
                            showAlert(Alert.AlertType.ERROR, new String[] {e.getCause().getMessage()});
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, new String[] {e.getMessage()});
                    }
                });
            }
            if(labelObject != null) {

                final LabelObject obj = labelObject;
                Platform.runLater(() -> {
                    rootPane.setCenter(addMainArea(obj));
                    nodeReferences.get(1).requestFocus();
                    maskerPane.setVisible(false);
                });
//            rootPane.setRight(addRightAreaForSKU());
            }
        });
    }

    public VBox addTopArea() {
        VBox vbox = new VBox();
        vbox.getChildren().addAll(addTitleBar(primaryStage), addSearchBox());
        return vbox;
    }


    public ScrollPane addRightArea() {

        VBox vbox = new VBox();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefWidth(235);
        vbox.getChildren().addAll(scrollPane);
        vbox.setStyle("-fx-background-color: #DAE6F3;");
        VBox vb = new VBox();
        vb.setPadding(new Insets(10));
        vb.setSpacing(10);

        Button button1 = new Button("1000 / CT");
        button1.setId("style1");
        button1.setPrefWidth(200);

        Button button2 = new Button("250 PER BOX");
        button2.setId("style2");
        button2.setPrefWidth(200);

        Button button3 = new Button("10 PACK");
        button3.setId("style3");
        button3.setPrefWidth(200);

        Button button4 = new Button("20 PACK");
        button4.setId("style4");
        button4.setPrefWidth(200);

        Button button5 = new Button("25 PACK");
        button5.setId("style5");
        button5.setPrefWidth(200);

        Button button6 = new Button("50 PACK");
        button6.setId("style6");
        button6.setPrefWidth(200);

        Button button7 = new Button("100 PACK");
        button7.setId("style7");
        button7.setPrefWidth(200);

        Button button8 = new Button("150 PACK");
        button8.setId("style8");
        button8.setPrefWidth(200);

        Button button9 = new Button("200 PACK");
        button9.setId("style9");
        button9.setPrefWidth(200);

        Button button10 = new Button("STYLE10");
        button10.setId("style10");
        button10.setPrefWidth(200);



        vb.getChildren().addAll(button1, button2, button3, button4, button5, button6, button7, button8, button9, button10);
        scrollPane.setVmax(440);
        scrollPane.setContent(vb);
        return scrollPane;
    }

    public ScrollPane addRightAreaForSKU() {

        VBox vbox = new VBox();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefWidth(335);
        scrollPane.setStyle("-fx-background-color: slategrey");
        vbox.getChildren().addAll(scrollPane);
        vbox.setStyle("-fx-background-color: #DAE6F3;");
        VBox vb = new VBox();
        vb.setPadding(new Insets(10));
        vb.setSpacing(10);

        for (int i = 0; i < 10; i ++) {

            HBox hBox1 = new HBox();

            Label label1 = new Label("Product #: BU120976");
            hBox1.setAlignment(Pos.CENTER_LEFT);
            hBox1.getChildren().add(label1);

            HBox hBox2 = new HBox();
            hBox2.setAlignment(Pos.CENTER_RIGHT);
            Label label2 = new Label("Qty:250 ");
            hBox2.getChildren().add(label2);
            hBox1.getChildren().add(hBox2);
            HBox.setHgrow(hBox2, Priority.ALWAYS);

            ImageView labelImage = new ImageView(new Image(LabelPrinter.class.getResourceAsStream("graphics/label_1.png")));
            labelImage.setSmooth(false);
            labelImage.setPreserveRatio(true);
            labelImage.setFitWidth(300);
            vb.getChildren().addAll(hBox1, labelImage);
        }



        scrollPane.setVmax(440);
        scrollPane.setContent(vb);
        return scrollPane;
    }

    public StackPane addMainArea(final LabelObject labelObject) {
        StackPane stackPane = (StackPane)rootPane.getCenter();
        if(stackPane == null) {
            stackPane = new StackPane();
        }
        if(labelObject != null) {
            VBox vBox = new VBox();
            vBox.setAlignment(Pos.TOP_CENTER);
            vBox.setPadding(new Insets(10));
            vBox.setSpacing(20);
            vBox.setPrefSize(600, 400);

            try {
                HBox hBox = new HBox();

                hBox.setAlignment(Pos.CENTER);
                hBox.setPadding(new Insets(15, 12, 15, 12));
                hBox.setSpacing(10);
                hBox.setMinHeight(60);
                final TextField copies = new TextField();
                copies.setPrefSize(60, 40);
                copies.setText("1");
                copies.selectAll();
                nodeReferences.add(1, copies);
                copies.setStyle("-fx-alignment: center;-fx-font-size:22px;-fx-font-weight: bold");

                Label label = new Label("Copies:");
                label.setStyle("-fx-alignment: center-right;-fx-font-size:28px;");

                hBox.getChildren().addAll(label, copies);
                vBox.getChildren().add(hBox);

                FileInputStream inputStream = new FileInputStream(labelObject.getLabelPath());
                ImageView labelImage = new ImageView(new Image(inputStream));
                labelImage.setSmooth(false);
                labelImage.setPreserveRatio(true);
                labelImage.setFitWidth(450);
                vBox.getChildren().add(labelImage);
                inputStream.close();

                HBox buttons = new HBox();
                buttons.setPadding(new Insets(10));
                buttons.setSpacing(20);
                buttons.setAlignment(Pos.CENTER);
                Button printButton = new Button("Print");
                printButton.setId("print");
                printButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        if (!copies.getText().isEmpty()) {
                            maskerPane.setVisible(true);
                            new PDFPrinter(new File(labelObject.getLabelPDFPath()), Integer.parseInt(copies.getText()), new Label5x3());
//                            maskerPane.setVisible(false);
                        }
                    }
                });
                printButton.setPrefWidth(200);
                Button cancelButton = new Button("Cancel");
                cancelButton.setId("cancel");
                cancelButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        clearCenter();
                        setFocus();
                    }
                });
                cancelButton.setPrefWidth(200);
                buttons.getChildren().addAll(printButton, cancelButton);
                vBox.getChildren().addAll(buttons);
            } catch (IOException e) {
                e.printStackTrace();
            }
            stackPane.getChildren().add(0, vBox);
        } else if(stackPane.getChildren().size() == 0) {
            stackPane.getChildren().add(maskerPane);
        }
        return stackPane;


    }

    public HBox addFooterArea() {
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(15, 10, 8, 12));
        hBox.setStyle("-fx-alignment: center-right;-fx-background-color: gainsboro");
        hBox.setSpacing(10);
        ImageView logo = new ImageView(new Image(LabelPrinter.class.getResourceAsStream("/assets/images/logo.png")));
        logo.setFitWidth(100);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setCache(true);
        hBox.getChildren().add(logo);
        return hBox;
    }

    public HBox addTitleBar(final Stage primaryStage) {

        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-background-color: #303641;");
        titleBar.setPadding(new Insets(5, 5, 5, 12));

        Label appName = new Label("Label Printer - Envelopes.com");
        appName.setStyle("-fx-text-fill: aliceblue;-fx-font-weight:bold;-fx-font-family:Arial;");
        titleBar.getChildren().add(appName);

        HBox titleBarButtons = new HBox();
        titleBarButtons.setStyle("-fx-background-color: #303641;");

        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: #303641;-fx-text-fill: aliceblue;-fx-font-family:Arial;");
        closeBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Platform.exit();
            }
        });

        titleBarButtons.getChildren().addAll(closeBtn);
        titleBarButtons.setAlignment(Pos.CENTER_RIGHT);
        titleBar.getChildren().addAll(titleBarButtons);
        HBox.setHgrow(titleBarButtons, Priority.ALWAYS);


        titleBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        titleBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            }
        });

        return titleBar;
    }

    public HBox addSearchBox() {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setPadding(new Insets(15, 12, 15, 12));
        hBox.setSpacing(10);   // Gap between nodes
        hBox.setStyle("-fx-background-color: #336699;");
        hBox.setMinHeight(60);
        final TextField id = new TextField();
        id.setTextFormatter(textFormatter);
        id.setPrefSize(300, 40);
        id.setStyle("-fx-font-size:22px;-fx-font-weight: bold;");
        id.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(id.getText().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, new String[] {"\nPlease enter a valid SKU."});
                } else {
                    showLabelForSKU(id.getText());
                }

            }
        });

        Label label = new Label("SKU : ");
        label.setStyle("-fx-alignment: center-right;-fx-font-size:28px;");

        Button findButton = new Button("Go");
        findButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(id.getText().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, new String[] {"\nPlease enter a valid SKU."});
                } else {
                    showLabelForSKU(id.getText());
                }

            }
        });

        findButton.setPrefSize(40, 25);
        nodeReferences.add(0, id);
        hBox.getChildren().addAll(label, id, findButton);
        return hBox;
    }

    public HBox addHBox(String color) {
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(15, 12, 15, 12));
        hBox.setSpacing(10);
        hBox.setStyle("-fx-background-color: " + color + ";");
        return hBox;
    }

    class WindowButtons extends HBox {

        public WindowButtons() {
            Button closeBtn = new Button("X");
            closeBtn.setStyle("-fx-background-color: #303641;-fx-text-fill: aliceblue;-fx-font-family:Arial;");
            closeBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    Platform.exit();
                }
            });
            this.getChildren().add(closeBtn);
        }
    }
}
