package com.starkplayer.util;

import javafx.scene.Group;
import javafx.scene.shape.*;

public class IconFactory {
    
    public static Group createPreviousIcon() {
        Group group = new Group();
        
        // Left arrow (first part) - double left arrow
        Polygon leftArrow1 = new Polygon();
        leftArrow1.getPoints().addAll(
            3.0, 10.0,   // tip
            7.0, 6.0,    // top
            7.0, 10.0    // bottom
        );
        leftArrow1.setFill(javafx.scene.paint.Color.WHITE);
        
        Polygon leftArrow2 = new Polygon();
        leftArrow2.getPoints().addAll(
            3.0, 10.0,   // tip
            7.0, 14.0,   // bottom
            7.0, 10.0    // top
        );
        leftArrow2.setFill(javafx.scene.paint.Color.WHITE);
        
        // Right arrow (second part)
        Polygon rightArrow1 = new Polygon();
        rightArrow1.getPoints().addAll(
            7.0, 10.0,   // start
            11.0, 6.0,   // top
            11.0, 10.0   // bottom
        );
        rightArrow1.setFill(javafx.scene.paint.Color.WHITE);
        
        Polygon rightArrow2 = new Polygon();
        rightArrow2.getPoints().addAll(
            7.0, 10.0,   // start
            11.0, 14.0, // bottom
            11.0, 10.0  // top
        );
        rightArrow2.setFill(javafx.scene.paint.Color.WHITE);
        
        group.getChildren().addAll(leftArrow1, leftArrow2, rightArrow1, rightArrow2);
        return group;
    }
    
    public static Group createNextIcon() {
        Group group = new Group();
        
        // First right arrow (left part of double arrow) - pointing right
        // Mirror of Previous: Previous has tip at x=3, base at x=7
        // Next should have base at x=9, tip at x=13
        Polygon firstArrow1 = new Polygon();
        firstArrow1.getPoints().addAll(
            9.0, 10.0,   // left base
            13.0, 6.0,   // top right (tip)
            13.0, 10.0   // bottom right
        );
        firstArrow1.setFill(javafx.scene.paint.Color.WHITE);
        
        Polygon firstArrow2 = new Polygon();
        firstArrow2.getPoints().addAll(
            9.0, 10.0,   // left base
            13.0, 14.0,  // bottom right (tip)
            13.0, 10.0   // top right
        );
        firstArrow2.setFill(javafx.scene.paint.Color.WHITE);
        
        // Second right arrow (right part of double arrow) - pointing right
        // Mirror of Previous: Previous has base at x=7, tip at x=11
        // Next should have base at x=13, tip at x=17
        Polygon secondArrow1 = new Polygon();
        secondArrow1.getPoints().addAll(
            13.0, 10.0,  // left base
            17.0, 6.0,   // top right (tip)
            17.0, 10.0   // bottom right
        );
        secondArrow1.setFill(javafx.scene.paint.Color.WHITE);
        
        Polygon secondArrow2 = new Polygon();
        secondArrow2.getPoints().addAll(
            13.0, 10.0,  // left base
            17.0, 14.0,  // bottom right (tip)
            17.0, 10.0   // top right
        );
        secondArrow2.setFill(javafx.scene.paint.Color.WHITE);
        
        group.getChildren().addAll(firstArrow1, firstArrow2, secondArrow1, secondArrow2);
        return group;
    }
    
    public static Group createPlayIcon() {
        Group group = new Group();
        
        // Triangle pointing right (centered in 20x20 space)
        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(
            6.0, 4.0,    // left top
            6.0, 16.0,   // left bottom
            16.0, 10.0   // right tip
        );
        triangle.setFill(javafx.scene.paint.Color.WHITE);
        
        group.getChildren().add(triangle);
        return group;
    }
    
    public static Group createPauseIcon() {
        Group group = new Group();
        
        // Left rectangle (centered)
        Rectangle leftRect = new Rectangle(5, 5, 3, 10);
        leftRect.setFill(javafx.scene.paint.Color.WHITE);
        
        // Right rectangle (centered)
        Rectangle rightRect = new Rectangle(12, 5, 3, 10);
        rightRect.setFill(javafx.scene.paint.Color.WHITE);
        
        group.getChildren().addAll(leftRect, rightRect);
        return group;
    }
    
    public static Group createStopIcon() {
        Group group = new Group();
        
        // Square (centered in 20x20 space)
        Rectangle square = new Rectangle(5, 5, 10, 10);
        square.setFill(javafx.scene.paint.Color.WHITE);
        
        group.getChildren().add(square);
        return group;
    }
}

