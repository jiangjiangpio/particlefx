package dev.particlefx.shape;
final class ShapeMath {
    static final double TAU = Math.PI * 2;
    static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    static double unit(int i, int n) { return n <= 1 ? 0 : (double) i / (n - 1); }
    static double radicalInverse(int n, int base) {
        double result = 0, fraction = 1.0 / base;
        while (n > 0) { result += (n % base) * fraction; n /= base; fraction /= base; }
        return result;
    }
    private ShapeMath() {}
}
