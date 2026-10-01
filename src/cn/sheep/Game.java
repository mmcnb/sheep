// Game.java
package cn.sheep;

import java.io.InputStream;
import java.util.Random;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

public class Game extends Canvas implements Runnable {
    static final int SLOT_MAX = 7;
    static final int MAX_Z = 9;
    static final int SAFE_BOTTOM = 32;
    static final int COLOR_PINK = 16738740;

    int TILE_W = 32;
    int TILE_H = 36;

    final int STATE_PLAY;
    final int STATE_WIN;
    final int STATE_FAIL;

    int gameState = 0;
    long stateTime;

    Tile[] tiles = new Tile[600];
    int tileCount;

    ParticleTile[] pTiles = new ParticleTile[200];
    int pTileCount;

    int[] slot = new int[7];
    int slotCount;

    int level = 1;

    boolean showHelp = false;
    boolean showAbout = false;
    boolean paused = false;

    String[] pauseItems = new String[]{"继续", "重开", "帮助", "关于", "退出"};
    int pauseIndex = 0;
    int helpPage = 0;
    int aboutPage = 0;

    String[][] HELP_TEXT = new String[][]{
            {"玩法说明", "点最上层方块 三个相同自动消除", "槽位满 7 个失败 被遮挡的不能点"}
    };

    String[][] ABOUT_TEXT = new String[][]{
            {
                    "厂商 xmwold.com 第一次制作游戏",
                    "完成了关卡和三消玩法 欢迎大家支持鼓励！"
            },
            {
                    "续梦网出品",
                    "有问题请通过各大社交平台联系我"
            }
    };

    int cursorIndex = -1;
    boolean cursorActive = false;
    int highlightBlink = 0;
    boolean touchMode = false;

    Image bg;
    Image blockBg;
    Image slotBg;
    Image[] block = new Image[17];
    Image[][] blockDark = new Image[17][3];
    Image[] blockBgDark = new Image[3];

    Player startBgm;
    Player loopBgm;

    int screenW;
    int screenH;
    int slotX;
    int slotY;

    Random rnd = new Random();
    Thread loop;
    boolean initialized = false;

    int redBoxX;
    int redBoxY;

    private sheep midlet;

    public Game(sheep midlet) {
        this.midlet = midlet;

        this.STATE_PLAY = 0;
        this.STATE_WIN = 1;
        this.STATE_FAIL = 2;

        this.setFullScreenMode(true);
        this.loadRes();
        this.playBGM();
        this.initLevel();

        this.initialized = true;
        this.loop = new Thread(this);
        this.loop.start();
    }

    void loadRes() {
        try {
            Image bgRaw = Image.createImage("/block_lawn.png");
            this.bg = this.adaptBackground(bgRaw, this.getWidth(), this.getHeight());

            this.blockBg = Image.createImage("/block_bg.png");
            this.slotBg = Image.createImage("/slot_bg.png");

            this.blockBgDark[0] = Image.createImage("/block_bg_dark_1.png");
            this.blockBgDark[1] = Image.createImage("/block_bg_dark_2.png");
            this.blockBgDark[2] = Image.createImage("/block_bg_dark_3.png");

            int i = 1;
            while (i <= 16) {
                this.block[i] = Image.createImage("/block_" + i + ".png");

                int f = 0;
                while (f < 3) {
                    this.blockDark[i][f] = Image.createImage("/block_" + i + "_dark_" + (f + 1) + ".png");
                    ++f;
                }
                ++i;
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

    Image adaptBackground(Image src, int screenW, int screenH) {
        int srcH;
        float screenRatio = (float) screenW / (float) screenH;
        int srcW = src.getWidth();
        float srcRatio = (float) srcW / (float) (srcH = src.getHeight());
        boolean stretch = (double) Math.abs(screenRatio - srcRatio) > 0.25;

        if (stretch) {
            int[] rgb = new int[srcW * srcH];
            src.getRGB(rgb, 0, srcW, 0, 0, srcW, srcH);

            int[] newRgb = new int[screenW * screenH];
            int y = 0;
            while (y < screenH) {
                int srcY = y * srcH / screenH;
                int x = 0;
                while (x < screenW) {
                    int srcX = x * srcW / screenW;
                    newRgb[y * screenW + x] = rgb[srcY * srcW + srcX];
                    ++x;
                }
                ++y;
            }
            return Image.createRGBImage(newRgb, screenW, screenH, true);
        }

        Image bgTile = Image.createImage(screenW, screenH);
        Graphics g = bgTile.getGraphics();

        int y = 0;
        while (y < screenH) {
            int x = 0;
            while (x < screenW) {
                g.drawImage(src, x, y, 0);
                x += srcW;
            }
            y += srcH;
        }

        return bgTile;
    }

    void playBGM() {
        try {
            this.startBgm = Manager.createPlayer(
                    this.getClass().getResourceAsStream("/lawn_1.wav"),
                    "audio/x-wav"
            );

            this.startBgm.addPlayerListener(new PlayerListener() {
                public void playerUpdate(Player p, String event, Object data) {
                    if (PlayerListener.END_OF_MEDIA.equals(event)) {
                        try {
                            p.stop();
                            p.close();
                            Game.this.playLoopBGM();
                        } catch (Exception exception) {
                            // empty catch block
                        }
                    }
                }
            });

            this.startBgm.prefetch();
            this.startBgm.start();
        } catch (Exception exception) {
            // empty catch block
        }
    }

    void playLoopBGM() {
        try {
            this.loopBgm = Manager.createPlayer(
                    this.getClass().getResourceAsStream("/lawn_2.wav"),
                    "audio/x-wav"
            );
            this.loopBgm.setLoopCount(-1);
            this.loopBgm.prefetch();
            this.loopBgm.start();
        } catch (Exception exception) {
            // empty catch block
        }
    }

    void initLevel() {
        this.screenW = this.getWidth();
        this.screenH = this.getHeight();

        this.slotX = (this.screenW - 238) / 2;
        this.slotY = this.screenH - 61 - 32;

        this.redBoxX = this.screenW / 2 - this.TILE_W / 2 - 45;
        this.redBoxY = this.slotY - this.TILE_H - 6;

        this.tileCount = 0;
        this.slotCount = 0;
        this.showHelp = false;
        this.gameState = 0;
        this.pTileCount = 0;
        this.cursorActive = false;
        this.cursorIndex = -1;
        this.highlightBlink = 0;

        if (this.level == 1) {
            this.initLevel1();
        } else {
            this.initLevel2();
        }

        this.calcCovered();
        this.stateTime = System.currentTimeMillis();
    }

    void initLevel1() {
        int sx = this.screenW / 2 - 60;
        int sy = 70;

        int[] bag = new int[18];
        int k = 0;
        int t = 1;
        while (t <= 3) {
            int i = 0;
            while (i < 6) {
                bag[k++] = t;
                ++i;
            }
            ++t;
        }

        this.shuffle(bag);

        k = 0;
        int z = 0;
        while (z < 2) {
            int y = 0;
            while (y < 3) {
                int x = 0;
                while (x < 3) {
                    Tile t2 = new Tile();
                    t2.x = sx + x * 40;

                    int offset = z == 0 ? (y <= 1 ? -15 : -5) : 0;
                    t2.y = sy + y * 42 + offset;
                    t2.z = z;
                    t2.type = bag[k++];

                    if (z == 1 && y == 2) {
                        t2.animFrame = 0;
                    }

                    this.tiles[this.tileCount++] = t2;
                    ++x;
                }
                ++y;
            }
            ++z;
        }
    }

    void initLevel2() {
        int t;
        int rows = 4;
        int cols = 5;
        int layers = 8;

        int baseX = 20;
        int baseY = 10;
        int xSpacing = (this.screenW - 2 * baseX) / cols;
        int ySpacing = 38;

        int totalTiles = rows * cols * layers;
        totalTiles -= totalTiles % 3;

        int typeCount = 16;
        int groupCount = totalTiles / 3;

        int[] bag = new int[totalTiles];
        int k = 0;
        Random rnd = new Random();

        int[] groups = new int[typeCount + 1];
        int i = 1;
        while (i <= typeCount) {
            groups[i] = 1;
            ++i;
        }

        int remain = groupCount - typeCount;
        while (remain > 0) {
            t = rnd.nextInt(typeCount) + 1;
            groups[t] = groups[t] + 1;
            --remain;
        }

        t = 1;
        while (t <= typeCount) {
            int g = 0;
            while (g < groups[t]) {
                bag[k++] = t;
                bag[k++] = t;
                bag[k++] = t;
                ++g;
            }
            ++t;
        }

        this.shuffle(bag);

        this.tileCount = 0;
        k = 0;

        int l = 0;
        while (l < layers) {
            int r = 0;
            while (r < rows) {
                int c = 0;
                while (c < cols) {
                    if (k >= bag.length) break;

                    Tile t2 = new Tile();
                    t2.x = baseX + c * xSpacing + rnd.nextInt(7) - 3;
                    t2.y = baseY + r * ySpacing + l * 3 + rnd.nextInt(5) - 2;
                    t2.z = l;
                    t2.type = bag[k++];

                    this.tiles[this.tileCount++] = t2;
                    ++c;
                }
                ++r;
            }
            ++l;
        }

        this.calcCovered();
    }

    void shuffle(int[] a) {
        int i = 0;
        while (i < a.length) {
            int r = this.rnd.nextInt(a.length);
            int t = a[i];
            a[i] = a[r];
            a[r] = t;
            ++i;
        }
    }

    void calcCovered() {
        int i = 0;
        while (i < this.tileCount) {
            Tile b = this.tiles[i];
            b.covered = false;

            if (b.alive) {
                int j = 0;
                while (j < this.tileCount) {
                    Tile t = this.tiles[j];
                    if (t.alive && t.z > b.z && this.overlap(b, t)) {
                        b.covered = true;
                        break;
                    }
                    ++j;
                }
            }
            ++i;
        }
    }

    void togglePause() {
        this.paused = !this.paused;
        this.pauseIndex = 0;
        this.repaint();

        try {
            if (this.paused) {
                if (this.startBgm != null) {
                    this.startBgm.stop();
                }
                if (this.loopBgm != null) {
                    this.loopBgm.stop();
                }
                this.pauseIndex = 0;
            } else if (this.loopBgm != null) {
                this.loopBgm.start();
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

    void activateCursor() {
        this.cursorActive = true;
        this.cursorIndex = -1;

        int z = 8;
        while (z >= 0) {
            int i = 0;
            while (i < this.tileCount) {
                Tile t = this.tiles[i];
                if (t.alive && !t.covered && t.z == z) {
                    this.cursorIndex = i;
                    return;
                }
                ++i;
            }
            --z;
        }
    }

    boolean overlap(Tile a, Tile b) {
        return a.x < b.x + this.TILE_W
                && a.x + this.TILE_W > b.x
                && a.y < b.y + this.TILE_H
                && a.y + this.TILE_H > b.y;
    }

    protected void paint(Graphics g) {
        int y = 0;
        while (y < this.screenH) {
            int x = 0;
            while (x < this.screenW) {
                g.drawImage(this.bg, x, y, 0);
                x += this.bg.getWidth();
            }
            y += this.bg.getHeight();
        }

        int z = 0;
        while (z < 9) {
            int i = 0;
            while (i < this.tileCount) {
                Tile t = this.tiles[i];
                if (t.alive && t.z == z) {
                    int cx = t.x + this.TILE_W / 2;
                    int cy = t.y + this.TILE_H / 2;

                    if (this.level == 1 && z == 1 && t.animFrame < 10) {
                        ++t.animFrame;
                        g.drawImage(this.block[t.type], cx, cy += 10 - t.animFrame, 3);
                    } else if (t.covered) {
                        g.drawImage(this.blockBgDark[2], cx, cy, 3);
                        g.drawImage(this.blockDark[t.type][2], cx, cy, 3);
                    } else {
                        g.drawImage(this.blockBg, cx, cy, 3);
                        g.drawImage(this.block[t.type], cx, cy, 3);
                    }
                }
                ++i;
            }
            ++z;
        }

        if (this.cursorActive && this.cursorIndex >= 0 && this.highlightBlink < 10) {
            Tile t = this.tiles[this.cursorIndex];
            g.setColor(0xFF0000);
            g.drawRect(t.x - 2, t.y - 2, this.TILE_W + 3, this.TILE_H + 3);
            g.drawRect(t.x - 3, t.y - 3, this.TILE_W + 5, this.TILE_H + 5);
        }

        g.drawImage(this.slotBg, this.slotX, this.slotY, 0);

        int i = 0;
        while (i < this.slotCount) {
            int cx = this.slotX + 6 + i * 34 + this.TILE_W / 2;
            int cy = this.slotY + 30;
            g.drawImage(this.blockBg, cx, cy, 3);
            g.drawImage(this.block[this.slot[i]], cx, cy, 3);
            ++i;
        }

        i = 0;
        while (i < this.pTileCount) {
            ParticleTile pt = this.pTiles[i];
            if (pt.alive) {
                boolean aliveAny = false;
                g.setColor(0xFFFF00);

                int j = 0;
                while (j < pt.particles.length) {
                    Particle p = pt.particles[j];
                    if (p.life > 0) {
                        aliveAny = true;
                        g.fillRect(p.x - 1, p.y - 1, 3, 3);
                        p.x += p.dx;
                        p.y += p.dy;
                        --p.life;
                    }
                    ++j;
                }

                if (!aliveAny) {
                    pt.alive = false;
                }
            }
            ++i;
        }

        if (this.paused) {
            g.setColor(0x66000000);
            g.fillRect(0, 0, this.screenW, this.screenH);

            int w = 120;
            int h = 80;
            int px = (this.screenW - w) / 2;
            int py = (this.screenH - h) / 2;

            g.setColor(0);
            g.fillRect(px, py, w, h);
            g.setColor(0xFFFFFF);

            Font f2 = g.getFont();
            int i2 = 0;
            while (i2 < this.pauseItems.length) {
                int y2 = py + 20 + i2 * (f2.getHeight() + 6);
                if (i2 == this.pauseIndex) {
                    g.drawString("▶ " + this.pauseItems[i2], this.screenW / 2, y2, 17);
                } else {
                    g.drawString(this.pauseItems[i2], this.screenW / 2, y2, 17);
                }
                ++i2;
            }
        }

        g.setColor(COLOR_PINK);
        Font f = g.getFont();

        g.drawString("关卡 " + this.level, this.screenW / 2, 6, 17);

        int softY = this.screenH - f.getHeight() - 2;
        g.drawString("菜单", 4, softY, 20);

        String rightText = this.paused ? "继续" : "移出";
        g.drawString("*" + rightText, this.screenW - 4, softY, 24);

        if (this.showHelp || this.showAbout) {
            int popupHeight = 100;

            g.setColor(0);
            g.fillRect(0, this.screenH / 2 - popupHeight / 2, this.screenW, popupHeight);

            g.setColor(COLOR_PINK);
            g.setFont(f);

            int padding = 4;
            int maxWidth = this.screenW - padding * 2;
            int lineSpacing = 2;
            int startY = this.screenH / 2 - popupHeight / 2 + padding;

            String[][] textLines = this.showHelp ? this.HELP_TEXT : this.ABOUT_TEXT;
            int page = this.showHelp ? this.helpPage : this.aboutPage;
            String[] lines = textLines[page];

            int i3 = 0;
            while (i3 < lines.length) {
                String line = lines[i3];

                while (f.stringWidth(line) > maxWidth) {
                    int cut = line.length() * maxWidth / f.stringWidth(line);
                    String part = line.substring(0, cut);
                    g.drawString(part, this.screenW / 2, startY, 17);
                    line = line.substring(cut);
                    startY += f.getHeight() + lineSpacing;
                }

                g.drawString(line, this.screenW / 2, startY, 17);
                startY += f.getHeight() + lineSpacing;
                ++i3;
            }

            String pageStr = String.valueOf(page + 1) + "/" + textLines.length;
            g.drawString(
                    pageStr,
                    this.screenW / 2,
                    this.screenH / 2 + popupHeight / 2 - padding - f.getHeight() * 2,
                    17
            );

            if (textLines.length > 1) {
                g.drawString(
                        "<  >",
                        this.screenW / 2,
                        this.screenH / 2 + popupHeight / 2 - padding - f.getHeight(),
                        17
                );
            }
        }

        if (this.gameState == 1) {
            g.setColor(0);
            g.fillRect(0, this.screenH / 2 - 40, this.screenW, 80);
            g.setColor(COLOR_PINK);

            String msg = this.level == 1 ? "难度飙升" : "胜利！加入羊村";
            g.drawString(msg, this.screenW / 2, this.screenH / 2, 65);
        } else if (this.gameState == 2) {
            g.setColor(0);
            g.fillRect(0, this.screenH / 2 - 40, this.screenW, 80);
            g.setColor(COLOR_PINK);
            g.drawString("失败！重新开始", this.screenW / 2, this.screenH / 2, 65);
        }
    }

    protected void pointerPressed(int x, int y) {
        this.touchMode = true;

        if (this.paused) {
            int w = 120;
            int h = 80;
            int px = (this.screenW - w) / 2;
            int py = (this.screenH - h) / 2;

            int itemH = 16;
            int i = 0;
            while (i < this.pauseItems.length) {
                int iy = py + 20 + i * itemH;
                if (x > px && x < px + w && y > iy && y < iy + itemH) {
                    this.pauseIndex = i;
                    switch (i) {
                        case 0:
                            this.togglePause();
                            break;
                        case 1:
                            this.togglePause();
                            this.level = 1;
                            this.initLevel();
                            break;
                        case 2:
                            this.paused = false;
                            this.showHelp = true;
                            break;
                        case 3:
                            this.paused = false;
                            this.showAbout = true;
                            break;
                        case 4:
                            this.midlet.exit();
                            break;
                    }
                    this.repaint();
                    return;
                }
                ++i;
            }

            int softY = this.screenH - itemH - 2;
            int softHeight = itemH;
            int softXStart = this.screenW - 50;
            int softXEnd = this.screenW - 4;

            if (x >= softXStart && x <= softXEnd && y >= softY && y <= softY + softHeight) {
                this.togglePause();
                this.repaint();
                return;
            }
            return;
        }

        this.cursorActive = true;
        this.handleInput(x, y);
    }

    protected void keyPressed(int keyCode) {
        this.touchMode = false;
        int action = this.getGameAction(keyCode);

        if (keyCode == -6 || keyCode == -21) {
            this.togglePause();
            this.repaint();
            return;
        }

        if (keyCode == 42) {
            this.undoSlot();
            this.activateCursor();
            this.repaint();
            return;
        }

        if (this.paused) {
            if (action == 1) {
                this.pauseIndex = (this.pauseIndex + this.pauseItems.length - 1) % this.pauseItems.length;
                this.repaint();
                return;
            }

            if (action == 6) {
                this.pauseIndex = (this.pauseIndex + 1) % this.pauseItems.length;
                this.repaint();
                return;
            }

            if (keyCode == 53 || keyCode == -5 || keyCode == 8) {
                switch (this.pauseIndex) {
                    case 0:
                        this.togglePause();
                        break;
                    case 1:
                        this.togglePause();
                        this.level = 1;
                        this.initLevel();
                        break;
                    case 2:
                        this.paused = false;
                        this.showHelp = true;
                        break;
                    case 3:
                        this.paused = false;
                        this.showAbout = true;
                        break;
                    case 4:
                        this.midlet.exit();
                        break;
                }
                this.repaint();
                return;
            }

            if (keyCode == -7 || keyCode == -21) {
                this.togglePause();
                this.repaint();
                return;
            }
            return;
        }

        if (this.showHelp || this.showAbout) {
            if (keyCode == 52 || action == 2) {
                if (this.showHelp) {
                    this.helpPage = (this.helpPage - 1 + this.HELP_TEXT.length) % this.HELP_TEXT.length;
                }
                if (this.showAbout) {
                    this.aboutPage = (this.aboutPage - 1 + this.ABOUT_TEXT.length) % this.ABOUT_TEXT.length;
                }
            } else if (keyCode == 54 || action == 5) {
                if (this.showHelp) {
                    this.helpPage = (this.helpPage + 1) % this.HELP_TEXT.length;
                }
                if (this.showAbout) {
                    this.aboutPage = (this.aboutPage + 1) % this.ABOUT_TEXT.length;
                }
            } else if (keyCode == 53 || keyCode == -5) {
                this.showHelp = false;
                this.showAbout = false;
            }

            this.repaint();
            return;
        }

        if (this.gameState != 0) {
            this.level = this.gameState == 1 && this.level == 1 ? 2 : 1;
            this.initLevel();
            return;
        }

        if (this.cursorIndex < 0) {
            this.activateCursor();
        }

        switch (action) {
            case 1:
                this.moveCursor(0);
                break;
            case 6:
                this.moveCursor(1);
                break;
            case 2:
                this.moveCursor(2);
                break;
            case 5:
                this.moveCursor(3);
                break;
        }

        if (keyCode == 53 || keyCode == -5 || keyCode == 8) {
            if (this.cursorActive && this.cursorIndex >= 0) {
                Tile t = this.tiles[this.cursorIndex];
                this.addTileToSlot(t);
                this.calcCovered();
                this.relocateCursor(t);
            }
            this.repaint();
            return;
        }

        if (keyCode == -7 || keyCode == -22 || keyCode == 42) {
            this.undoSlot();
            this.activateCursor();
            this.repaint();
            return;
        }
    }

    void handleInput(int x, int y) {
        this.touchMode = true;

        if (this.showHelp) {
            this.showHelp = false;
            this.helpPage = (this.helpPage + 1) % 2;
            this.repaint();
            return;
        }

        if (this.gameState != 0 && (this.gameState == 1 || this.gameState == 2)) {
            this.level = this.gameState == 1 && this.level == 1 ? 2 : 1;
            this.initLevel();
            return;
        }

        if (y >= this.screenH - 20) {
            if (x >= 4 && x < 50) {
                this.togglePause();
            } else if (x >= this.screenW - 40) {
                this.undoSlot();
            }
            this.repaint();
            return;
        }

        if (x >= this.slotX && x <= this.slotX + 238 && y >= this.slotY && y <= this.slotY + 61) {
            return;
        }

        int z = 10;
        while (z >= 0) {
            int i = 0;
            while (i < this.tileCount) {
                Tile t = this.tiles[i];
                if (t.alive && !t.covered && t.z == z
                        && x >= t.x - 4 && x <= t.x + this.TILE_W + 4
                        && y >= t.y - 4 && y <= t.y + this.TILE_H + 4) {
                    this.addTileToSlot(t);
                    return;
                }
                ++i;
            }
            --z;
        }
    }

    void addTileToSlot(Tile t) {
        if (!this.initialized) {
            return;
        }

        if (!this.cursorActive) {
            return;
        }

        if (this.slotCount >= 7) {
            return;
        }

        this.slot[this.slotCount++] = t.type;
        t.alive = false;
        t.removed = false;

        this.calcCovered();
        this.autoSortSlot();
        this.checkMatch3();
        this.checkLevelClear();
        this.repaint();
    }

    void undoSlot() {
        if (this.slotCount <= 0) {
            return;
        }

        int i = 0;
        while (i < this.tileCount) {
            Tile t = this.tiles[i];
            if (t.alive && t.removed) {
                return;
            }
            ++i;
        }

        int undoX = this.redBoxX;
        int undoY = this.redBoxY;
        int spacing = this.TILE_W + 4;

        int[] indices = new int[this.slotCount];
        int count = 0;

        int i2 = 0;
        while (i2 < this.slotCount) {
            int j = 0;
            while (j < this.tileCount) {
                Tile t = this.tiles[j];
                if (!t.alive && t.type == this.slot[i2] && !t.removed) {
                    indices[count++] = i2;
                    break;
                }
                ++j;
            }
            ++i2;
        }

        if (count < 3) {
            return;
        }

        Random rand = new Random();
        int i3 = 0;
        while (i3 < count) {
            int r = i3 + rand.nextInt(count - i3);
            int tmp = indices[i3];
            indices[i3] = indices[r];
            indices[r] = tmp;
            ++i3;
        }

        int undoNum = 3;
        int maxZ = 0;

        int i4 = 0;
        while (i4 < this.tileCount) {
            if (this.tiles[i4].alive && this.tiles[i4].z > maxZ) {
                maxZ = this.tiles[i4].z;
            }
            ++i4;
        }

        i4 = 0;
        while (i4 < undoNum) {
            int type = this.slot[indices[i4]];
            int j = 0;
            while (j < this.tileCount) {
                Tile t = this.tiles[j];
                if (!t.alive && t.type == type && !t.removed) {
                    t.alive = true;
                    t.covered = false;
                    t.removed = true;
                    t.x = undoX;
                    t.y = undoY;
                    t.z = maxZ;
                    undoX += spacing;
                    break;
                }
                ++j;
            }
            ++i4;
        }

        int newCount = 0;
        int[] newSlot = new int[7];
        boolean[] flags = new boolean[this.slotCount];

        int i5 = 0;
        while (i5 < undoNum) {
            flags[indices[i5]] = true;
            ++i5;
        }

        i5 = 0;
        while (i5 < this.slotCount) {
            if (!flags[i5]) {
                newSlot[newCount++] = this.slot[i5];
            }
            ++i5;
        }

        this.slot = newSlot;
        this.slotCount = newCount;

        this.calcCovered();

        i5 = 0;
        while (i5 < this.tileCount) {
            Tile t = this.tiles[i5];
            if (t.alive && t.removed) {
                t.covered = false;
            }
            ++i5;
        }

        this.repaint();
    }

    void autoSortSlot() {
        if (this.slotCount <= 1) {
            return;
        }

        int[] temp = new int[7];
        int k = 0;
        boolean[] used = new boolean[this.slotCount];

        int i = 0;
        while (i < this.slotCount) {
            if (!used[i]) {
                int type = this.slot[i];
                temp[k++] = type;
                used[i] = true;

                int j = i + 1;
                while (j < this.slotCount) {
                    if (!used[j] && this.slot[j] == type) {
                        temp[k++] = type;
                        used[j] = true;
                    }
                    ++j;
                }
            }
            ++i;
        }

        i = 0;
        while (i < this.slotCount) {
            this.slot[i] = temp[i];
            ++i;
        }
    }

    void placeTileToSlot() {
        if (this.slotCount <= 0) {
            return;
        }

        int lastIndex = this.slotCount - 1;

        if (this.pTileCount < this.pTiles.length) {
            ParticleTile pt = new ParticleTile();
            pt.particles = new Particle[12];

            int cx = this.slotX + 6 + lastIndex * 34 + this.TILE_W / 2;
            int cy = this.slotY + 30;

            int i = 0;
            while (i < 12) {
                pt.particles[i] = new Particle(cx, cy, rnd);
                ++i;
            }

            this.pTiles[this.pTileCount++] = pt;
        }

        --this.slotCount;
    }

    void checkMatch3() {
        int i = 0;
        while (i < this.slotCount) {
            int type = this.slot[i];
            int cnt = 0;

            int j = 0;
            while (j < this.slotCount) {
                if (this.slot[j] == type) {
                    ++cnt;
                }
                ++j;
            }

            if (cnt >= 3) {
                int removed = 0;
                int k = 0;
                int[] n = new int[7];

                int j2 = 0;
                while (j2 < this.slotCount) {
                    if (this.slot[j2] == type && removed < 3) {
                        if (this.pTileCount < this.pTiles.length) {
                            ParticleTile pt = new ParticleTile();
                            pt.particles = new Particle[12];

                            int cx = this.slotX + 6 + j2 * 34 + this.TILE_W / 2;
                            int cy = this.slotY + 30;

                            int p = 0;
                            while (p < 12) {
                                pt.particles[p] = new Particle(cx, cy, rnd);
                                ++p;
                            }

                            this.pTiles[this.pTileCount++] = pt;
                        }
                        ++removed;
                    } else {
                        n[k++] = this.slot[j2];
                    }
                    ++j2;
                }

                this.slot = n;
                this.slotCount = k;
                return;
            }

            ++i;
        }
    }

    void checkLevelClear() {
        boolean alive = false;

        int i = 0;
        while (i < this.tileCount) {
            if (this.tiles[i].alive) {
                alive = true;
                break;
            }
            ++i;
        }

        if (!alive) {
            this.gameState = 1;
        } else if (this.slotCount >= 7) {
            boolean canMatch = false;

            int i2 = 0;
            while (i2 < this.slotCount) {
                int cnt = 0;
                int j = 0;
                while (j < this.slotCount) {
                    if (this.slot[i2] == this.slot[j]) {
                        ++cnt;
                    }
                    ++j;
                }

                if (cnt >= 3) {
                    canMatch = true;
                    break;
                }
                ++i2;
            }

            if (!canMatch) {
                this.gameState = 2;
            }
        }

        this.stateTime = System.currentTimeMillis();
    }

    void moveCursor(int dir) {
        if (this.cursorIndex < 0) {
            return;
        }

        Tile cur = this.tiles[this.cursorIndex];
        int best = -1;
        int bestDist = Integer.MAX_VALUE;

        int i = 0;
        while (i < this.tileCount) {
            block13: {
                int dist;
                if (i == this.cursorIndex) break block13;

                Tile t = this.tiles[i];
                if (!t.alive || t.covered) break block13;

                int dx = t.x - cur.x;
                int dy = t.y - cur.y;

                switch (dir) {
                    case 0: {
                        if (dy < 0) {
                            dx = Math.abs(dx);
                            dy = -dy;
                            break;
                        }
                        break block13;
                    }
                    case 1: {
                        if (dy > 0) {
                            dx = Math.abs(dx);
                            break;
                        }
                        break block13;
                    }
                    case 2: {
                        if (dx < 0) {
                            dy = Math.abs(dy);
                            dx = -dx;
                            break;
                        }
                        break block13;
                    }
                    case 3: {
                        if (dx <= 0) break block13;
                        dy = Math.abs(dy);
                    }
                }

                int tolerance = 2;
                if ((dir != 0 && dir != 1 || dx <= this.TILE_W + tolerance)
                        && (dir != 2 && dir != 3 || dy <= this.TILE_H + tolerance)
                        && (dist = dx * dx + dy * dy) < bestDist) {
                    bestDist = dist;
                    best = i;
                }
            }
            ++i;
        }

        if (best >= 0) {
            this.cursorIndex = best;
        }
    }

    void relocateCursor(Tile from) {
        this.cursorIndex = -1;

        int best = -1;
        int bestDist = 999999;

        int i = 0;
        while (i < this.tileCount) {
            Tile t = this.tiles[i];
            if (t.alive && !t.covered) {
                int dx = t.x - from.x;
                int dy = t.y - from.y;
                int d = dx * dx + dy * dy;

                if (from.z == t.z) {
                    d -= 100000;
                }

                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
            ++i;
        }

        if (best >= 0) {
            this.cursorIndex = best;
        }
    }

    public void run() {
        while (true) {
            if (this.paused) {
                this.repaint();
                try {
                    Thread.sleep(100L);
                } catch (Exception exception) {
                }
                continue;
            }

            this.highlightBlink = (this.highlightBlink + 1) % 20;

            if (this.gameState == 1 && this.level == 1) {
                long now = System.currentTimeMillis();
                if (now - this.stateTime >= 3000L) {
                    this.level = 2;
                    this.initLevel();
                }
            }

            this.repaint();

            try {
                Thread.sleep(80L);
            } catch (Exception exception) {
            }
        }
    }
}
