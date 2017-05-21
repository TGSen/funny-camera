package top.icecream.testme.main.utils;

import android.view.View;

import java.util.LinkedList;


/**
 * AUTHOR: 86417
 * DATE: 5/11/2017
 */

public class AnimatorHelper {

    private static final int DISTANCE = 1000;

    private static final int DURATION_BUTTON = 200;
    private static final int DURATION_LIST = 700;

    public static void buttonMoveIn(View... views) {
        LinkedList<View> list = new LinkedList<>();
        for (View view : views) {
            view.setVisibility(View.INVISIBLE);
            view.setX(view.getX()+ DISTANCE);
            list.add(view);
        }
        show(list);
    }

    private static void show(final LinkedList<View> list) {
        if (list.size() == 0) {
            return;
        }
        View view = list.poll();
        view.setVisibility(View.VISIBLE);
        view.animate().translationXBy(-DISTANCE).setDuration(DURATION_BUTTON).withEndAction(new Runnable() {
            @Override
            public void run() {
                show(list);
            }
        });
    }


    public static void buttonVanish(View... views){
        for(final View view:views){
            view.setEnabled(false);
            view.animate()
                    .scaleX(0)
                    .scaleY(0)
                    .setDuration(DURATION_LIST)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            view.setVisibility(View.INVISIBLE);
                        }
                    }).start();
        }
    }

    public static void buttonEmerge(View... views) {
        for (final View view : views) {
            view.setVisibility(View.VISIBLE);
            view.animate()
                    .scaleX(1)
                    .scaleY(1)
                    .setDuration(DURATION_LIST)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            view.setEnabled(true);
                        }
                    })
                    .start();
        }
    }

    public static void listVanish(View... views) {
        for(final View view: views){
            view.animate()
                    .setDuration(DURATION_LIST)
                    .translationYBy(view.getHeight())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            view.setVisibility(View.INVISIBLE);
                            view.setY(view.getY()-view.getHeight());
                        }
                    })
                    .start();
        }
    }

    public static void listEmerge(View... views) {
        for(final View view: views){
            view.setY(view.getY()+view.getHeight());
            view.setVisibility(View.VISIBLE);
            view.animate()
                    .setDuration(DURATION_LIST)
                    .translationYBy(-view.getHeight())
                    .start();
        }
    }

}
