package com.envelopes.apps.labelprinter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.dialog.ExceptionDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Manu on 7/7/2016.
 */
public class LabelPrinter extends Application {

    private double xOffset = 0;
    private double yOffset = 0;
    protected Stage primaryStage;

    protected static Exception initializationException = null;

    protected BorderPane rootPane;
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
            Scene scene = new Scene(rootPane, 1200, 700, Color.TRANSPARENT);
            primaryStage.setScene(scene);
            scene.getStylesheets().add(LabelPrinter.class.getResource("/assets/css/LabelPrinter.css").toExternalForm());
            ResizeHelper.addResizeListener(primaryStage);
            primaryStage.show();
        }
    }

    private void showError(Exception exception){
        ExceptionDialog dlg = new ExceptionDialog(exception);
        dlg.getDialogPane().setHeaderText("");
        dlg.setTitle("Label Printer - Envelopes.com");
        dlg.initStyle(StageStyle.UTILITY);
        dlg.initOwner(primaryStage);
        dlg.show();
    }

    private void showAlert(Alert.AlertType type, String[] args) {
        Alert dlg = new Alert(type, "");
        dlg.setHeaderText("");
        dlg.setTitle("Label Printer - Envelopes.com");
        dlg.getDialogPane().setContentText(args[0]);
        dlg.initStyle(StageStyle.UTILITY);
        dlg.initOwner(primaryStage);
        dlg.show();
    }




    protected void clearLabel() {
        rootPane.setRight(null);
        rootPane.setCenter(null);
    }


    protected void showLabelForSKU(String productOrOrderId) {
        LabelObject labelObject = null;
        try {
            labelObject = LabelHelper.getLabelForProductId(productOrOrderId, true);
        } catch (LabelNotFoundException | RuntimeException e) {
            if(e.getCause() != null) {
                showError(e);
            } else {
                showAlert(Alert.AlertType.ERROR, new String[] {e.getMessage()});
            }
        }
        if(labelObject != null) {
            rootPane.setCenter(addMainArea(labelObject));
//            rootPane.setRight(addRightAreaForSKU());
        }

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

    public VBox addMainArea(final LabelObject labelObject) {
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
                    if(!copies.getText().isEmpty()) {
                        System.out.println("printing....");
                        new PDFPrinter(new File(labelObject.getLabelPDFPath()), Integer.parseInt(copies.getText()));
                    }
                }
            });
            printButton.setPrefWidth(200);
            Button cancelButton = new Button("Cancel");
            cancelButton.setId("cancel");
            cancelButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    clearLabel();
                }
            });
            cancelButton.setPrefWidth(200);
            buttons.getChildren().addAll(printButton, cancelButton);
            vBox.getChildren().addAll(buttons);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vBox;


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
        id.setPrefSize(300, 40);
        id.setStyle("-fx-font-size:22px;-fx-font-weight: bold");

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
