package com.example.wanluzhang.mdp_17;


/**
 * Created by wanluzhang on 2/9/16.
 */
public class ArenaThread extends Thread{
    private Arena arena;
    private boolean running = false;
    private final static int sleepTime = 300;

    public ArenaThread(Arena arena){
        super();
        this.arena = arena;
        super.start();
    }

    public void startThread(){
        this.running = true;
    }

    public void run(){
        try{
            while(true){
                arena.update();
                arena.postInvalidate();
                sleep(sleepTime);
                while(this.running == false){
                    try {
                        sleep(sleepTime);
                    }
                    catch (InterruptedException e){}
                }
            }
        }
        catch (InterruptedException e){}
    }

}
