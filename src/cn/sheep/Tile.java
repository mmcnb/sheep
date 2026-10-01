// Tile.java
package cn.sheep;

class Tile {
    int x;
    int y;
    int z;
    int type;
    boolean alive = true;
    boolean covered;
    int animFrame = 10;
    boolean removed = false;

    Tile() {
    }
}