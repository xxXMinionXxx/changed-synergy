package net.parkabird.changedsynergy.client;

/** Analytic damped spring: retargeting preserves position and velocity at any frame rate. */
public final class UiSpring {
    private final double frequency;
    private final double damping;
    private double position, velocity, target;
    private long updated;

    public UiSpring(double initial) { this(initial, 30.0, 0.82); }
    public UiSpring(double initial, double frequency, double damping) {
        if (!(frequency > 0.0) || !(damping > 0.0 && damping <= 1.0)) throw new IllegalArgumentException("Invalid spring");
        this.frequency = frequency; this.damping = damping;
        snap(initial, System.nanoTime());
    }
    public void snap(double value, long now) { position = target = value; velocity = 0.0; updated = now; }
    public void target(double value, long now) { advance(now); target = value; }
    public double value(long now) { advance(now); return position; }
    /** Exact resting coordinates avoid integer pixel/floor/ceil flicker near the endpoint. */
    public double displayValue(long now, double tolerance) { return settled(now, tolerance) ? target : position; }
    public double velocity(long now) { advance(now); return velocity; }
    public boolean settled(long now, double tolerance) {
        advance(now); return Math.abs(position - target) < tolerance && Math.abs(velocity) < tolerance * frequency;
    }
    private void advance(long now) {
        if (now <= updated) return;
        double dt = (now - updated) / 1_000_000_000.0;
        updated = now;
        double x = position - target, v = velocity;
        if (damping == 1.0) {
            double decay = Math.exp(-frequency * dt), c = v + frequency * x;
            position = target + (x + c * dt) * decay;
            velocity = (v - frequency * c * dt) * decay;
        } else {
            double a = damping * frequency, b = frequency * Math.sqrt(1.0 - damping * damping);
            double decay = Math.exp(-a * dt), cosine = Math.cos(b * dt), sine = Math.sin(b * dt), c = (v + a * x) / b;
            position = target + decay * (x * cosine + c * sine);
            velocity = decay * (v * cosine - (a * c + b * x) * sine);
        }
    }
    public static float clamp(double value) { return (float)Math.max(0.0, Math.min(1.0, value)); }

    /** Spring response for a one-shot entrance; authoritative timers never use this value. */
    public static float response(double seconds) {
        if (seconds <= 0.0) return 0.0F;
        double a = 24.6, b = 30.0 * Math.sqrt(1.0 - 0.82 * 0.82);
        return (float)(1.0 - Math.exp(-a * seconds) * (Math.cos(b * seconds) + a / b * Math.sin(b * seconds)));
    }
}
