package lk.ac.mrt.projectx.buildex.complex;

import lk.ac.mrt.projectx.buildex.complex.cordinates.CartesianCoordinate;
import lk.ac.mrt.projectx.buildex.complex.cordinates.PolarCoordinate;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;

/**
 * @author Chathura Widanage
 */
public class CoordinateTransformer {

    public void cartesianToCenter(int width, int height, CartesianCoordinate cartesianCoordinate) {
        cartesianCoordinate.setX(cartesianCoordinate.getX() - (width / 2));
        cartesianCoordinate.setY(cartesianCoordinate.getY() - (height / 2));
    }

    public static PolarCoordinate cartesian2Polar(int width, int height, CartesianCoordinate cartesianCoordinate, boolean normalize) {
        PolarCoordinate polarCoordinate = cartesian2Polar(width, height, cartesianCoordinate);
        if (normalize) {
            double processedTheta = polarCoordinate.getTheta();
            double theta = MathUtils.normalizeAngle(processedTheta, FastMath.PI);
            polarCoordinate.setTheta(theta);/*
            if (processedTheta < 0) {
                processedTheta += (FastMath.PI * 2);
            }*/
        }
        return polarCoordinate;
    }

    public static PolarCoordinate cartesian2Polar(int width, int height, CartesianCoordinate cartesianCoordinate) {
        CartesianCoordinate centered = new CartesianCoordinate(
                cartesianCoordinate.getX() - (width / 2),
                cartesianCoordinate.getY() - (height / 2)
        );
        PolarCoordinate polarCoordinate = cartesian2Polar(centered);
        return polarCoordinate;
    }

    public static PolarCoordinate cartesian2Polar(CartesianCoordinate cartesianCoordinate) {
        return new PolarCoordinate(
                Math.atan2(cartesianCoordinate.getY(), cartesianCoordinate.getX()),
                Math.hypot(cartesianCoordinate.getX(), cartesianCoordinate.getY()));
    }


    public static CartesianCoordinate polar2Cartesian(int width, int height, PolarCoordinate polarCoordinate) {
        double x = polarCoordinate.getR() * Math.cos(polarCoordinate.getTheta());
        double y = polarCoordinate.getR() * Math.sin(polarCoordinate.getTheta());
        return new CartesianCoordinate(x + (width / 2), y + (height / 2));
    }

    public static CartesianCoordinate polar2Cartesian(PolarCoordinate polarCoordinate) {
        double x = polarCoordinate.getR() * Math.cos(polarCoordinate.getTheta());
        double y = polarCoordinate.getR() * Math.sin(polarCoordinate.getTheta());
        return new CartesianCoordinate(x, y);
    }

    public static double atant2(double y, double x) {
        double theta = Math.atan2(y, x);
        return theta < 0 ? theta + (Math.PI * 2) : theta;
    }

}
