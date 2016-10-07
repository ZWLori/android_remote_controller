package com.example.wanluzhang.mdp_17;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import java.lang.reflect.Array;
import java.util.ArrayList;


/**
 * Created by wanluzhang on 2/9/16.
 */
public class Arena extends View {
    private int col = 15;
    private Robot robot;
    private ArenaThread thread;
    private int gridSize;
    private int[] grid;
    private int[][] obstacles = new int[15][20];
    private int[][] spArray = new int[15][20];

    public Arena(Context context, int[] array){
        super(context);
        robot = new Robot();
        thread = new ArenaThread(this);
        thread.startThread();
    }

    @Override
    public void onDraw(Canvas canvas){
        RelativeLayout arenaView = (RelativeLayout) getRootView().findViewById(R.id.arenaView);
        gridSize = ((arenaView.getMeasuredWidth()) - (arenaView.getMeasuredWidth() / col)) / col;;
        robot.drawArena(canvas, gridSize);
    }

    public void setGridArray(int[] gridArray){
        Log.d("setGridArray()", "");
        this.grid = gridArray;
    }
    public void setSpArray(int[][] spArray){
        this.spArray = spArray;
    }
    public void setObstacles(int[][] obstacles){
        this.obstacles = obstacles;
    }
    public void update(){
        robot.setGridSettings(grid);
        robot.setObstacles(obstacles);
        robot.setSpArray(spArray);
    }
}
