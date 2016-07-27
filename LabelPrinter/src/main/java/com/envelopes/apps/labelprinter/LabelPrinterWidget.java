package com.envelopes.apps.labelprinter;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Created by Manu on 7/27/2016.
 */
public class LabelPrinterWidget extends LabelPrinter {


    protected StackPane root;
    protected static int uiWidth = 350;
    protected static int uiHeight = 40;
    protected static int minY = -3;

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
        primaryStage.show();
    }

    protected void createRootNode() {
        root = new StackPane();
        root.setId("widget-root");
        this.bindDraggable();
        Rectangle rect = new Rectangle(uiWidth,uiHeight);
        rect.setArcHeight(10.0);
        rect.setArcWidth(10.0);
        root.setClip(rect);
    }

    protected void setUpPrimaryStage(Stage primaryStage) {
        primaryStage.setAlwaysOnTop(true);
        primaryStage.getIcons().add(new Image(LabelPrinter.class.getResourceAsStream("/assets/images/logo.png")));
        primaryStage.initStyle(StageStyle.TRANSPARENT);
//        primaryStage.setMinWidth(uiWidth);
//        primaryStage.setMinHeight(uiHeight);
        primaryStage.focusedProperty().addListener((observableValue, oldValue, newValue) -> {
            if(newValue) {
                root.setStyle("-fx-opacity: 100%");
            } else {
                root.setStyle("-fx-opacity: 90%");
            }
        });
        this.primaryStage = primaryStage;
    }

    protected HBox createUIBody() {
        HBox uiBody = new HBox();
        uiBody.setSpacing(10);
        uiBody.setAlignment(Pos.CENTER_LEFT);
        uiBody.setPadding(new Insets(5, 5, 5, 12));

        ImageView logo = new ImageView(new Image(LabelPrinter.class.getResourceAsStream("/assets/images/logo.png")));
        logo.setFitWidth(60);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setCache(true);

        final TextField id = new TextField();
        id.setTextFormatter(textFormatter);
        id.setPrefSize(140, 20);

        final Button print = new Button("PRINT");
        print.setId("print-small");
        print.setPrefSize(50, 10);
        uiBody.getChildren().addAll(logo,id, print);

        HBox titleBarButtons = createTitleBarButtons();
        uiBody.getChildren().addAll(titleBarButtons);
        HBox.setHgrow(titleBarButtons, Priority.ALWAYS);

        return uiBody;
    }

    protected Scene createScene() {
        Scene scene = new Scene(root, uiWidth, uiHeight);
        scene.setFill(Color.TRANSPARENT);        scene.getStylesheets().add(LabelPrinter.class.getResource("/assets/css/LabelPrinter.css").toExternalForm());
        scene.getStylesheets().add("http://fonts.googleapis.com/css?family=Lato&subset=latin,latin-ext");
        return scene;
    }

    protected HBox createTitleBarButtons() {
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
}
