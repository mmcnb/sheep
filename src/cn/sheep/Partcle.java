// Particle.java
package cn.sheep;

import java.util.Random;

class Particle {
    int x;
    int y;
    int dx;
    int dy;
    int life = 6;

    Particle(int cx, int cy, Random rnd) {
        this.x = cx;
        this.y = cy;
        this.dx = rnd.nextInt(7) - 3;
        this.dy = rnd.nextInt(7) - 3;
    }
}