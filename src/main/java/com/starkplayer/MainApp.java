package com.starkplayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class MainApp extends Application {
    private Stage primaryStage;
    private boolean mini = false;
    private double prevWidth, prevHeight;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/starkplayer/main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/starkplayer/styles.css").toExternalForm());
        stage.setTitle("StarkPlayer");
        stage.setScene(scene);
        stage.setWidth(900);
        stage.setHeight(600);

        scene.setOnKeyPressed(ev -> {
            try {
                if (ev.getCode() == KeyCode.SPACE) {
                    com.starkplayer.controller.MusicPlayerController ctrl =
                            (com.starkplayer.controller.MusicPlayerController) loader.getController();
                    if (ctrl != null) ctrl.togglePlayPause();
                } else if (ev.getCode() == KeyCode.M) {
                    toggleMiniPlayer();
                } else if (ev.getCode() == KeyCode.N) {
                    com.starkplayer.controller.MusicPlayerController ctrl =
                            (com.starkplayer.controller.MusicPlayerController) loader.getController();
                    if (ctrl != null) ctrl.nextFromShortcut();
                } else if (ev.getCode() == KeyCode.P) {
                    com.starkplayer.controller.MusicPlayerController ctrl =
                            (com.starkplayer.controller.MusicPlayerController) loader.getController();
                    if (ctrl != null) ctrl.prevFromShortcut();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        stage.setOnCloseRequest(e -> {
            com.starkplayer.controller.MusicPlayerController ctrl =
                    (com.starkplayer.controller.MusicPlayerController) loader.getController();
            if (ctrl != null) {
                ctrl.shutdown();
            }
        });

        stage.show();
    }

    private void toggleMiniPlayer() {
        if (!mini) {
            prevWidth = primaryStage.getWidth();
            prevHeight = primaryStage.getHeight();
            primaryStage.setWidth(380);
            primaryStage.setHeight(120);
            primaryStage.setAlwaysOnTop(true);
            mini = true;
        } else {
            primaryStage.setWidth(prevWidth);
            primaryStage.setHeight(prevHeight);
            primaryStage.setAlwaysOnTop(false);
            mini = false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
