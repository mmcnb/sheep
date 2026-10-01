// sheep.java
package cn.sheep;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class sheep extends MIDlet {
    protected void startApp() {
        Display.getDisplay(this).setCurrent(new Game(this));
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean u) {
    }

    public void exit() {
        notifyDestroyed();
    }
}