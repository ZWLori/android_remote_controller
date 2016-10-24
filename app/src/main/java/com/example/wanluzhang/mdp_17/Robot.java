package com.example.wanluzhang.mdp_17;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;


/**
 * Created by wanluzhang on 2/9/16.
 */
public class Robot {
    private int[] gridSettings;
    private int[][] obstacleArray = new int[20][15];
    private int[][] spArray = new int[20][15];
    private int X;
    private int Y;
    private int _X;
    private int _Y;
    private Paint paint;
    private Canvas canvas;
    private static int size = 0;
    private static final String TAG = "Robot";

    public Robot(){
        super();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    public void setCanvas(Canvas c){
        this.canvas = c;
    }

    public void drawArena(Canvas canvas, int gridSize){
        setCanvas(canvas);
        this.size = gridSize;

        int row = gridSettings[0],  //20
                col = gridSettings[1],  //15
                rHeadX = gridSettings[2],  // center of the head
                rHeadY = gridSettings[3],
                rRobotX = gridSettings[4],  // center of the robot
                rRobotY = gridSettings[5];

        boolean directionUD = false,
                directionLR = false;


        // BG
        for (int i = 1; i <= col; i++){
            for (int j = 1; j <= row; j++){
                drawCell(i, j, gridSize, Color.parseColor("#FFF9C4"), canvas);
            }
        }

        // Obstacles
        for (int i = 0; i < 20; i++){
            for(int j = 0; j < 15; j++) {
                if (this.obstacleArray[i][j] == 0)        // unexplored cell
                    drawCell(j + 1, i + 1, gridSize, Color.TRANSPARENT, canvas);

                else if (this.obstacleArray[i][j] == 1)   // empty cell
                    drawCell(j + 1, i + 1, gridSize, Color.parseColor("#FFFDE7"), canvas);

                else                                      // obstacle
                    drawCell(j + 1, i + 1, gridSize, Color.parseColor("#333333"), canvas);
            }

        }

        // START
        for (int i = 1; i <= 3; i++){
            for (int j = row-2; j <= row; j++){
                drawCell(i, j, gridSize, Color.parseColor("#E0E0E0"), canvas);
            }
        }

        // GOAL
        for (int i = col-2; i <= col; i++){
            for (int j = 1; j <= 3; j++){
                drawCell(i, j, gridSize, Color.parseColor("#efa1bfc7"), canvas);
            }
        }

        // ROBOT
        // See whether the robot is towards horizontal way or vertical way
        if(rHeadX == rRobotX){
            directionUD = true;
            directionLR = false;
        }
        if(rHeadY == rRobotY){
            directionUD = false;
            directionLR = true;
        }

        // Draw Robot
        if(directionLR){
            for (int i = rRobotX-1; i <= rRobotX+1; i++){
                for (int j = rRobotY-1; j < rRobotY+2; j++){
                    drawCell(i, j, gridSize, Color.parseColor("#FFD600"), canvas);
                }
            }
            //head
            for (int j = rRobotY-1; j <= rRobotY+1; j++){
                drawCell(rHeadX, j, gridSize, Color.parseColor("#FFFF00"), canvas);
            }
        }
        if(directionUD){
            for (int i = rRobotX-1; i <= rRobotX+1; i++){
                for (int j = rRobotY-1; j <= rRobotY+1; j++){
                    drawCell(i, j, gridSize, Color.parseColor("#FFD600"), canvas);
                }
            }
            //head
            for (int i = rRobotX-1; i <= rRobotX+1; i++){
                drawCell(i, rHeadY, gridSize, Color.parseColor("#FFFF00"), canvas);
            }
        }

        // shortest path
        for (int i = 0; i < 20; i++){
            for (int j = 0; j < 15; j++){
                if (this.spArray[i][j] == 1) {
                    drawCell(j + 1, i + 1, gridSize, Color.parseColor("#4DD0E1"), canvas);
                }
            }
        }
    }

    public void drawCell(int i, int j, int gridSize, int c, Canvas canvas){
        X = i * gridSize - gridSize / 2;
        Y = j * gridSize - gridSize / 2;
        _X = i * gridSize + gridSize / 2;
        _Y = j * gridSize + gridSize / 2;
        // paint the fill in color
        paint.setColor(c);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(new RectF(X,Y,_X,_Y), paint);
        // paint the stroke border
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(new RectF(X,Y,_X,_Y), paint);
    }

    public void setGridSettings(int[] gridArray) {
        this.gridSettings = gridArray;
    }

    public void setObstacles(int[][] obstacleArray) {
        this.obstacleArray = obstacleArray;
    }
    public void setSpArray(int[][] spArray){
        this.spArray = spArray;
    }
}
