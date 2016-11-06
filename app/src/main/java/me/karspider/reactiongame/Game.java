package me.karspider.reactiongame;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.renderscript.ScriptGroup;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

public class Game extends Activity implements View.OnTouchListener {
    enum GameState {START, GAME, END}

    OurView v;
    Random random = new Random();
    RectF rect;
    float randX = random.nextFloat();
    float randY = random.nextFloat();
    float x, y;
    String timer;
    CountDownTimer cdt;
    int count;
    Canvas c;
    GameState state = GameState.START;
    boolean delayOver = false;
    Paint circlePaint, textPaint;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        v = new OurView(this);
        v.setOnTouchListener(this);
        count = 0;
        circlePaint = new Paint();
        circlePaint.setColor(Color.BLUE);
        circlePaint.setAntiAlias(true);
        textPaint = new Paint();
        textPaint.setTextSize(100);
        textPaint.setTextAlign(Paint.Align.CENTER);
        timer = "10";
        prefs = this.getSharedPreferences("highscore", Context.MODE_PRIVATE);
        setContentView(v);
    }

    @Override
    protected void onPause() {
        super.onPause();
        v.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        v.resume();
    }

    public class OurView extends SurfaceView implements Runnable {
        Thread t = null;
        SurfaceHolder holder;
        boolean running = false;

        public OurView(Context context) {
            super(context);
            holder = getHolder();
        }

        @Override
        public void run() {
            while (running) {
                if (!holder.getSurface().isValid()) {
                    continue;
                }

                c = holder.lockCanvas();
                c.drawARGB(255, 255, 255, 200);

                switch (state) {
                    case START:
                        gameStart();
                        break;
                    case GAME:
                        gameRun();
                        break;
                    case END:
                        gameEnd();
                        break;
                }
            }
        }

        public void gameStart() {
            String inst = "Tap anywhere to start";
            c.drawText(inst, c.getWidth() / 2, c.getHeight() / 2, textPaint);
            holder.unlockCanvasAndPost(c);
        }

        public void gameRun() {
            float dimension = c.getWidth() / 5;
            x = randX * (c.getWidth() - dimension);
            y = randY * (c.getHeight() - dimension - c.getHeight() / 10) + c.getHeight() / 10;
            rect = new RectF(x, y, x + dimension, y + dimension);
            c.drawOval(rect, circlePaint);
            c.drawText(timer, c.getWidth() / 2, c.getHeight() / 10, textPaint);
            holder.unlockCanvasAndPost(c);
        }

        public void gameEnd() {
            boolean newHighScore = false;
            int currentHighScore = prefs.getInt("highscores", 0);
            if (count > currentHighScore) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("highscores", count);
                editor.commit();
                newHighScore = true;
            }

            String score = "You scored: " + count;
            c.drawText(score, c.getWidth() / 2, c.getHeight() / 4, textPaint);
            String highScore = (newHighScore) ? "NEW HIGH SCORE!" : ("Your high score: " + prefs.getInt("highscores", 0));
            c.drawText(highScore, c.getWidth() / 2, c.getHeight() / 3, textPaint);
            String inst = "Tap to start again";
            c.drawText(inst, c.getWidth() / 2, c.getHeight() / 2, textPaint);
            holder.unlockCanvasAndPost(c);
            if (!delayOver) {
                try {
                    Thread.sleep(1000);
                    delayOver = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void pause() {
            running = false;
            while (true) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            t = null;
        }

        public void resume() {
            running = true;
            t = new Thread(this);
            t.start();
        }
    }

    public void startTimer() {
        cdt = new CountDownTimer(10000, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                float seconds = ((float) millisUntilFinished) / 1000;
                timer = "" + seconds;
            }

            @Override
            public void onFinish() {
                state = GameState.END;
                timer = "FINISH";
            }
        }.start();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (state) {
            case START:
                state = GameState.GAME;
                startTimer();
                break;
            case GAME:
                delayOver = false;
                if ((touchX > rect.left && touchX < rect.right) &&
                        (touchY > rect.top && touchY < rect.bottom)) {
                    count++;
                    randX = random.nextFloat();
                    randY = random.nextFloat();
                }
                break;
            case END:
                if (delayOver) {
                    count = 0;
                    state = GameState.GAME;
                    startTimer();
                }
                break;
        }

        return false;
    }

}
