/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.calibration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Num;
import java.util.Arrays;
import org.ejml.simple.SimpleMatrix;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.vision.opencv.Releasable;

/** JSON-serializable image. Data is stored as a raw JSON array. */
public class JsonMatOfDouble implements Releasable {
    public final int rows;
    public final int cols;
    public final int type;
    public final double[] data;

    // Cached matrices to avoid object recreation
    @JsonIgnore private Mat wrappedMat = null;
    @JsonIgnore private Matrix wpilibMat = null;

    private MatOfDouble wrappedMatOfDouble;

    public JsonMatOfDouble(int rows, int cols, double[] data) {
        this(rows, cols, CvType.CV_64FC1, data);
    }

    public JsonMatOfDouble(
            @JsonProperty("rows") int rows,
            @JsonProperty("cols") int cols,
            @JsonProperty("type") int type,
            @JsonProperty("data") double[] data) {
        this.rows = rows;
        this.cols = cols;
        this.type = type;
        this.data = data;
    }

    private static boolean isCameraMatrixMat(Mat mat) {
        return mat.type() == CvType.CV_64FC1 && mat.cols() == 3 && mat.rows() == 3;
    }

    private static boolean isDistortionCoeffsMat(Mat mat) {
        return mat.type() == CvType.CV_64FC1 && mat.cols() == 5 && mat.rows() == 1;
    }

    private static boolean isCalibrationMat(Mat mat) {
        return isDistortionCoeffsMat(mat) || isCameraMatrixMat(mat);
    }

    @JsonIgnore
    public static double[] getDataFromMat(Mat mat) {
        if (!isCalibrationMat(mat)) return null;

        double[] data = new double[(int) (mat.total() * mat.elemSize())];
        mat.get(0, 0, data);

        int dataLen = -1;

        if (isCameraMatrixMat(mat)) dataLen = 9;
        if (isDistortionCoeffsMat(mat)) dataLen = 5;

        // truncate Mat data to correct number data points.
        return Arrays.copyOfRange(data, 0, dataLen);
    }

    public static JsonMatOfDouble fromMat(Mat mat) {
        if (!isCalibrationMat(mat)) return null;
        return new JsonMatOfDouble(mat.rows(), mat.cols(), getDataFromMat(mat));
    }

    @JsonIgnore
    public Mat getAsMat() {
        if (this.type != CvType.CV_64FC1) return null;

        if (wrappedMat == null) {
            this.wrappedMat = new Mat(this.rows, this.cols, this.type);
            this.wrappedMat.put(0, 0, this.data);
        }
        return this.wrappedMat;
    }

    @JsonIgnore
    public MatOfDouble getAsMatOfDouble() {
        if (this.wrappedMatOfDouble == null) {
            this.wrappedMatOfDouble = new MatOfDouble();
            getAsMat().convertTo(wrappedMatOfDouble, CvType.CV_64F);
        }
        return this.wrappedMatOfDouble;
    }

    @JsonIgnore
    public <R extends Num, C extends Num> Matrix<R, C> getAsWpilibMat() {
        if (wpilibMat == null) {
            wpilibMat = new Matrix<R, C>(new SimpleMatrix(rows, cols, true, data));
        }
        return (Matrix<R, C>) wpilibMat;
    }

    @Override
    public void release() {
        getAsMat().release();
    }

    public Packet populatePacket(Packet packet) {
        packet.encode(this.data);
        return packet;
    }

    @Override
    public String toString() {
        return "JsonMat [rows="
                + rows
                + ", cols="
                + cols
                + ", type="
                + type
                + ", data="
                + Arrays.toString(data)
                + ", wrappedMat="
                + wrappedMat
                + ", wpilibMat="
                + wpilibMat
                + ", wrappedMatOfDouble="
                + wrappedMatOfDouble
                + "]";
    }
}
