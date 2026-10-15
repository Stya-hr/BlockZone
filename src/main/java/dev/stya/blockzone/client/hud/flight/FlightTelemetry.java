package dev.stya.blockzone.client.hud.flight;

/** One block is displayed as one metre; native movement is measured per 20Hz game tick. */
public record FlightTelemetry(double altitude, double speed, double descent) {
    public static FlightTelemetry measure(double feetY, double surfaceY, double x, double y, double z) {
        double altitude = Double.isFinite(feetY) && Double.isFinite(surfaceY) ? Math.max(0, feetY - surfaceY) : Double.NaN;
        boolean valid = Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        return new FlightTelemetry(altitude, valid ? Math.sqrt(x * x + y * y + z * z) * 20 : Double.NaN,
                valid ? Math.max(0, -y * 20) : Double.NaN);
    }
}
