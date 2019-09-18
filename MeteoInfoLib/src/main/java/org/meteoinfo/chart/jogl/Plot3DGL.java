/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.meteoinfo.chart.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.meteoinfo.chart.ChartColorBar;
import org.meteoinfo.chart.ChartLegend;
import org.meteoinfo.chart.ChartText;
import org.meteoinfo.chart.Margin;
import org.meteoinfo.chart.axis.Axis;
import org.meteoinfo.chart.plot.Plot;
import org.meteoinfo.chart.plot.PlotType;
import org.meteoinfo.chart.plot.XAlign;
import org.meteoinfo.chart.plot.YAlign;
import org.meteoinfo.chart.plot3d.GraphicCollection3D;
import org.meteoinfo.data.Dataset;
import org.meteoinfo.geoprocess.GeometryUtil;
import org.meteoinfo.global.DataConvert;
import org.meteoinfo.global.Extent;
import org.meteoinfo.global.Extent3D;
import org.meteoinfo.legend.ColorBreak;
import org.meteoinfo.legend.LegendScheme;
import org.meteoinfo.legend.LegendType;
import org.meteoinfo.legend.PointBreak;
import org.meteoinfo.legend.PolygonBreak;
import org.meteoinfo.legend.PolylineBreak;
import org.meteoinfo.shape.Graphic;
import org.meteoinfo.shape.ImageShape;
import org.meteoinfo.shape.PointZ;
import org.meteoinfo.shape.PointZShape;
import org.meteoinfo.shape.PolygonZ;
import org.meteoinfo.shape.PolygonZShape;
import org.meteoinfo.shape.Polyline;
import org.meteoinfo.shape.PolylineZShape;
import org.meteoinfo.shape.Shape;
import org.meteoinfo.shape.ShapeTypes;
import static org.meteoinfo.shape.ShapeTypes.PointZ;

/**
 *
 * @author wyq
 */
public class Plot3DGL extends Plot implements GLEventListener {

    // <editor-fold desc="Variables">
    private final GLU glu = new GLU();
    private final GraphicCollection3D graphics;
    private Extent3D extent;
    private ChartText title;
    private List<ChartLegend> legends;
    private final Axis xAxis;
    private final Axis yAxis;
    private final Axis zAxis;
    private float xmin, xmax = 1.0f, ymin;
    private float ymax = 1.0f, zmin, zmax = 1.0f;

    private Color boxColor = Color.getHSBColor(0f, 0f, 0.95f);
    private Color lineboxColor = Color.getHSBColor(0f, 0f, 0.8f);

    private boolean boxed, mesh, scaleBox, displayXY, displayZ,
            displayGrids, drawBoundingBox, hideOnDrag;

    int viewport[] = new int[4];
    float mvmatrix[] = new float[16];
    float projmatrix[] = new float[16];

    private float angleX = -45.0f;
    private float angleY = 45.0f;
    private float distanceX = 0.0f;
    private float distanceY = 0.0f;
    tessellCallBack tessCallback;
    private int width;
    private int height;
    float tickSpace = 5.0f;
    float tickLen = 0.08f;

    // </editor-fold>
    // <editor-fold desc="Constructor">
    /**
     * Constructor
     */
    public Plot3DGL() {
        this.legends = new ArrayList<>();
        //this.legends.add(new ChartColorBar(new LegendScheme(ShapeTypes.Polygon, 5)));
        this.xAxis = new Axis();
        this.xAxis.setLabel("X");
        this.xAxis.setTickLength(8);
        this.yAxis = new Axis();
        this.yAxis.setLabel("Y");
        this.yAxis.setTickLength(8);
        this.zAxis = new Axis();
        this.zAxis.setLabel("Z");
        this.zAxis.setTickLength(8);
        this.graphics = new GraphicCollection3D();
        this.hideOnDrag = false;
        this.boxed = true;
        this.displayGrids = false;
        this.displayXY = true;
        this.displayZ = true;
        this.drawBoundingBox = false;
    }

    // </editor-fold>
    // <editor-fold desc="GetSet">
    /**
     * Get extent
     *
     * @return Extent
     */
    public Extent3D getExtent() {
        return this.extent;
    }

    /**
     * Set extent
     *
     * @param value Extent
     */
    public void setExtent(Extent3D value) {
        this.extent = value;
        xmin = (float) extent.minX;
        xmax = (float) extent.maxX;
        ymin = (float) extent.minY;
        ymax = (float) extent.maxY;
        zmin = (float) extent.minZ;
        zmax = (float) extent.maxZ;
        xAxis.setMinMaxValue(xmin, xmax);
        yAxis.setMinMaxValue(ymin, ymax);
        zAxis.setMinMaxValue(zmin, zmax);
    }

    /**
     * Get box color
     *
     * @return Box color
     */
    public Color getBoxColor() {
        return this.boxColor;
    }

    /**
     * Set box color
     *
     * @param value Box color
     */
    public void setBoxColor(Color value) {
        this.boxColor = value;
    }

    /**
     * Get box line color
     *
     * @return Box line color
     */
    public Color getLineBoxColor() {
        return this.lineboxColor;
    }

    /**
     * Set box line color
     *
     * @param value Box line color
     */
    public void setLineBoxColor(Color value) {
        this.lineboxColor = value;
    }
    
    /**
     * Get if draw bounding box or not
     *
     * @return Boolean
     */
    public boolean isDrawBoundingBox() {
        return this.drawBoundingBox;
    }

    /**
     * Set if draw bounding box or not
     *
     * @param value Boolean
     */
    public void setDrawBoundingBox(boolean value) {
        this.drawBoundingBox = value;
    }
    
    
    /**
     * Set display X/Y axis or not
     *
     * @param value Boolean
     */
    public void setDisplayXY(boolean value) {
        this.displayXY = value;
    }

    /**
     * Set display Z axis or not
     *
     * @param value Boolean
     */
    public void setDisplayZ(boolean value) {
        this.displayZ = value;
    }

    /**
     * Set display grids or not
     *
     * @param value Boolean
     */
    public void setDisplayGrids(boolean value) {
        this.displayGrids = value;
    }

    /**
     * Set display box or not
     *
     * @param value Boolean
     */
    public void setBoxed(boolean value) {
        this.boxed = value;
    }

    /**
     * Get x rotate angle
     *
     * @return X rotate angle
     */
    public float getAngleX() {
        return this.angleX;
    }

    /**
     * Set x rotate angle
     *
     * @param value X rotate angle
     */
    public void setAngleX(float value) {
        this.angleX = value;
    }

    /**
     * Get y rotate angle
     *
     * @return Y rotate angle
     */
    public float getAngleY() {
        return this.angleY;
    }

    /**
     * Set y rotate angle
     *
     * @param value Y rotate angle
     */
    public void setAngleY(float value) {
        this.angleY = value;
    }

    /**
     * Get title
     *
     * @return Title
     */
    public ChartText getTitle() {
        return this.title;
    }

    /**
     * Set title
     *
     * @param value Title
     */
    public void setTitle(ChartText value) {
        this.title = value;
    }

    /**
     * Set title
     *
     * @param text Title text
     */
    public void setTitle(String text) {
        if (this.title == null) {
            this.title = new ChartText(text);
        } else {
            this.title.setText(text);
        }
    }

    /**
     * Get legends
     *
     * @return Legends
     */
    public List<ChartLegend> getLegends() {
        return this.legends;
    }

    /**
     * Get chart legend
     *
     * @param idx Index
     * @return Chart legend
     */
    public ChartLegend getLegend(int idx) {
        if (this.legends.isEmpty()) {
            return null;
        } else {
            return this.legends.get(idx);
        }
    }

    /**
     * Get chart legend
     *
     * @return Chart legend
     */
    public ChartLegend getLegend() {
        if (this.legends.isEmpty()) {
            return null;
        } else {
            return this.legends.get(this.legends.size() - 1);
        }
    }

    /**
     * Set chart legend
     *
     * @param value Legend
     */
    public void setLegend(ChartLegend value) {
        this.legends.clear();
        this.legends.add(value);
    }

    /**
     * Set legends
     *
     * @param value Legends
     */
    public void setLegends(List<ChartLegend> value) {
        this.legends = value;
    }

    /**
     * Get x axis
     *
     * @return X axis
     */
    public Axis getXAxis() {
        return this.xAxis;
    }

    /**
     * Get y axis
     *
     * @return Y axis
     */
    public Axis getYAxis() {
        return this.yAxis;
    }

    /**
     * Get z axis
     *
     * @return Z axis
     */
    public Axis getZAxis() {
        return this.zAxis;
    }

    /**
     * Get x minimum
     *
     * @return X minimum
     */
    public float getXMin() {
        return this.xmin;
    }

    /**
     * Set minimum x
     *
     * @param value Minimum x
     */
    public void setXMin(float value) {
        this.xmin = value;
        updateExtent();
        this.xAxis.setMinMaxValue(xmin, xmax);
    }

    /**
     * Get x maximum
     *
     * @return X maximum
     */
    public float getXMax() {
        return this.xmax;
    }

    /**
     * Set maximum x
     *
     * @param value Maximum x
     */
    public void setXMax(float value) {
        this.xmax = value;
        updateExtent();
        this.xAxis.setMinMaxValue(xmin, xmax);
    }

    /**
     * Set x minimum and maximum values
     *
     * @param min Minimum value
     * @param max Maximum value
     */
    public void setXMinMax(float min, float max) {
        this.xmin = min;
        this.xmax = max;
        updateExtent();
        this.xAxis.setMinMaxValue(min, max);
    }

    /**
     * Get y minimum
     *
     * @return Y minimum
     */
    public float getYMin() {
        return this.ymin;
    }

    /**
     * Set minimum y
     *
     * @param value Minimum y
     */
    public void setYMin(float value) {
        this.ymin = value;
        updateExtent();
        this.yAxis.setMinMaxValue(ymin, ymax);
    }

    /**
     * Get y maximum
     *
     * @return Y maximum
     */
    public float getYMax() {
        return this.ymax;
    }

    /**
     * Set Maximum y
     *
     * @param value Maximum y
     */
    public void setYMax(float value) {
        this.ymax = value;
        updateExtent();
        this.yAxis.setMinMaxValue(ymin, ymax);
    }

    /**
     * Set y minimum and maximum values
     *
     * @param min Minimum value
     * @param max Maximum value
     */
    public void setYMinMax(float min, float max) {
        this.ymin = min;
        this.ymax = max;
        updateExtent();
        this.yAxis.setMinMaxValue(min, max);
    }

    /**
     * Get z minimum
     *
     * @return Z minimum
     */
    public float getZMin() {
        return this.zmin;
    }

    /**
     * Set minimum z
     *
     * @param value Minimum z
     */
    public void setZMin(float value) {
        this.zmin = value;
        updateExtent();
        this.zAxis.setMinMaxValue(zmin, zmax);
    }

    /**
     * Get z maximum
     *
     * @return Z maximum
     */
    public float getZMax() {
        return this.zmax;
    }

    /**
     * Set maximum z
     *
     * @param value Maximum z
     */
    public void setZMax(float value) {
        this.zmax = value;
        updateExtent();
        this.zAxis.setMinMaxValue(zmin, zmax);
    }

    /**
     * Set z minimum and maximum values
     *
     * @param min Minimum value
     * @param max Maximum value
     */
    public void setZMinMax(float min, float max) {
        this.zmin = min;
        this.zmax = max;
        updateExtent();
        this.zAxis.setMinMaxValue(min, max);
    }

    // </editor-fold>
    // <editor-fold desc="methods">
    /**
     * Add a legend
     *
     * @param legend The legend
     */
    public void addLegend(ChartLegend legend) {
        this.legends.clear();
        this.legends.add(legend);
    }

    /**
     * Remove a legend
     *
     * @param legend The legend
     */
    public void removeLegend(ChartLegend legend) {
        this.legends.remove(legend);
    }

    /**
     * Remove a legend by index
     *
     * @param idx The legend index
     */
    public void removeLegend(int idx) {
        this.legends.remove(idx);
    }

    /**
     * Get outer position area
     *
     * @param area Whole area
     * @return Position area
     */
    @Override
    public Rectangle2D getOuterPositionArea(Rectangle2D area) {
        Rectangle2D rect = this.getOuterPosition();
        double x = area.getWidth() * rect.getX() + area.getX();
        double y = area.getHeight() * (1 - rect.getHeight() - rect.getY()) + area.getY();
        double w = area.getWidth() * rect.getWidth();
        double h = area.getHeight() * rect.getHeight();
        return new Rectangle2D.Double(x, y, w, h);
    }

    @Override
    public Dataset getDataset() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDataset(Dataset dataset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PlotType getPlotType() {
        return PlotType.XYZ;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area) {

    }

    private void updateExtent() {
        this.extent = new Extent3D(xmin, xmax, ymin, ymax, zmin, zmax);
    }

    /**
     * Set axis tick font
     *
     * @param font Font
     */
    public void setAxisTickFont(Font font) {
        this.xAxis.setTickLabelFont(font);
        this.yAxis.setTickLabelFont(font);
        this.zAxis.setTickLabelFont(font);
    }

    /**
     * Add a graphic
     *
     * @param g Grahic
     */
    public void addGraphic(Graphic g) {
        this.graphics.add(g);
        Extent ex = this.graphics.getExtent();
        if (!ex.is3D()) {
            ex = ex.to3D();
        }
        this.setExtent((Extent3D) ex);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        gl.glPushMatrix();

        gl.glRotatef(angleX, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(angleY, 0.0f, 0.0f, 1.0f);

        this.updateMatrix(gl);

        gl.glColor3f(0.0f, 0.0f, 0.0f);

        //Draw box
        drawBoxGridsTicksLabels(gl);

        //Draw title
        this.drawTitle();

        //Draw graphics
        float s = 1.01f;
        gl.glClipPlanef(GL2.GL_CLIP_PLANE0, new float[]{1, 0, 0, s}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE0);
        gl.glClipPlanef(GL2.GL_CLIP_PLANE1, new float[]{-1, 0, 0, s}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE1);
        gl.glClipPlanef(GL2.GL_CLIP_PLANE2, new float[]{0, -1, 0, s}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE2);
        gl.glClipPlanef(GL2.GL_CLIP_PLANE3, new float[]{0, 1, 0, s}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE3);
        gl.glClipPlanef(GL2.GL_CLIP_PLANE4, new float[]{0, 0, 1, s}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE4);
        gl.glClipPlanef(GL2.GL_CLIP_PLANE5, new float[]{0, 0, -1, s}, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE5);
        for (int m = 0; m < this.graphics.getNumGraphics(); m++) {
            Graphic graphic = this.graphics.get(m);
            drawGraphics(gl, graphic);
        }
        gl.glDisable(GL2.GL_CLIP_PLANE0);
        gl.glDisable(GL2.GL_CLIP_PLANE1);
        gl.glDisable(GL2.GL_CLIP_PLANE2);
        gl.glDisable(GL2.GL_CLIP_PLANE3);
        gl.glDisable(GL2.GL_CLIP_PLANE4);
        gl.glDisable(GL2.GL_CLIP_PLANE5);

        //Draw legend
        gl.glPopMatrix();
        this.updateMatrix(gl);
        this.drawLegend(gl);

        gl.glFlush();
    }

    /**
     * Draws the base plane. The base plane is the x-y plane.
     *
     * @param g the graphics context to draw.
     * @param x used to retrieve x coordinates of drawn plane from this method.
     * @param y used to retrieve y coordinates of drawn plane from this method.
     */
    private void drawBase(GL2 gl) {
        float[] rgba = this.lineboxColor.getRGBComponents(null);
        gl.glColor3f(rgba[0], rgba[1], rgba[2]);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glEnd();
    }

    private void updateMatrix(GL2 gl) {
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mvmatrix, 0);
        gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projmatrix, 0);
    }

    private float[] toScreen(float vx, float vy, float vz) {
        //Get screen coordinates
        float coord[] = new float[4];// x, y;// returned xy 2d coords        
        glu.gluProject(vx, vy, vz, mvmatrix, 0, projmatrix, 0, viewport, 0, coord, 0);

        return coord;
    }

    private float toScreenLength(float x1, float y1, float z1, float x2, float y2, float z2) {
        float[] coord = toScreen(x1, y1, z1);
        float sx1 = coord[0];
        float sy1 = coord[1];
        coord = toScreen(x2, y2, z2);
        float sx2 = coord[0];
        float sy2 = coord[1];

        return (float) Math.sqrt(Math.pow(sx2 - sx1, 2) + Math.pow(sy2 - sy1, 2));
    }

    private int getLabelGap(Font font, List<ChartText> labels, double len) {
        TextRenderer textRenderer = new TextRenderer(font);
        int n = labels.size();
        int nn;
        Rectangle2D rect = textRenderer.getBounds("Text".subSequence(0, 4));
        nn = (int) (len / rect.getHeight());
        if (nn == 0) {
            nn = 1;
        }
        return n / nn + 1;
    }

    private int getLegendTickGap(ChartColorBar legend, double len) {
        if (legend.getTickLabelAngle() != 0) {
            return 1;
        }

        TextRenderer textRenderer = new TextRenderer(legend.getTickLabelFont());
        int n = legend.getLegendScheme().getBreakNum();
        int nn;
        Rectangle2D rect = textRenderer.getBounds("Text".subSequence(0, 4));
        nn = (int) (len / rect.getHeight());
        if (nn == 0) {
            nn = 1;
        }
        return n / nn + 1;
    }

    private void drawBoxGridsTicksLabels(GL2 gl) {
        this.drawBase(gl);

        float[] rgba;
        //Draw box
        if (boxed) {
            if (this.angleY >= 180 && this.angleY < 360) {
                rgba = this.lineboxColor.getRGBComponents(null);
                gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                gl.glLineWidth(1.0f);
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3f(-1.0f, 1.0f, -1.0f);
                gl.glVertex3f(-1.0f, -1.0f, -1.0f);
                gl.glVertex3f(-1.0f, -1.0f, 1.0f);
                gl.glVertex3f(-1.0f, 1.0f, 1.0f);
                gl.glVertex3f(-1.0f, 1.0f, -1.0f);
                gl.glEnd();
            } else {
                rgba = this.lineboxColor.getRGBComponents(null);
                gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                gl.glLineWidth(1.0f);
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3f(1.0f, 1.0f, -1.0f);
                gl.glVertex3f(1.0f, -1.0f, -1.0f);
                gl.glVertex3f(1.0f, -1.0f, 1.0f);
                gl.glVertex3f(1.0f, 1.0f, 1.0f);
                gl.glVertex3f(1.0f, 1.0f, -1.0f);
                gl.glEnd();
            }
            if (this.angleY >= 90 && this.angleY < 270) {
                rgba = this.lineboxColor.getRGBComponents(null);
                gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                gl.glLineWidth(1.0f);
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3f(-1.0f, -1.0f, -1.0f);
                gl.glVertex3f(1.0f, -1.0f, -1.0f);
                gl.glVertex3f(1.0f, -1.0f, 1.0f);
                gl.glVertex3f(-1.0f, -1.0f, 1.0f);
                gl.glVertex3f(-1.0f, -1.0f, -1.0f);
                gl.glEnd();
            } else {
                rgba = this.lineboxColor.getRGBComponents(null);
                gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                gl.glLineWidth(1.0f);
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3f(-1.0f, 1.0f, -1.0f);
                gl.glVertex3f(1.0f, 1.0f, -1.0f);
                gl.glVertex3f(1.0f, 1.0f, 1.0f);
                gl.glVertex3f(-1.0f, 1.0f, 1.0f);
                gl.glVertex3f(-1.0f, 1.0f, -1.0f);
                gl.glEnd();
            }
        }

        //Draw axis
        float x, y, v;
        int skip;
        XAlign xAlign;
        YAlign yAlign;
        Rectangle2D rect;
        float strWidth, strHeight;
        if (this.displayXY) {
            //Draw x/y axis lines
            //x axis line            
            if (this.angleY >= 90 && this.angleY < 270) {
                y = 1.0f;
            } else {
                y = -1.0f;
            }
            rgba = this.xAxis.getLineColor().getRGBComponents(null);
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glLineWidth(this.xAxis.getLineWidth());
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex3f(-1.0f, y, -1.0f);
            gl.glVertex3f(1.0f, y, -1.0f);
            gl.glEnd();

            //x axis ticks
            this.xAxis.updateTickLabels();
            List<ChartText> tlabs = this.xAxis.getTickLabels();
            float axisLen = this.toScreenLength(-1.0f, y, -1.0f, 1.0f, y, -1.0f);
            skip = getLabelGap(this.xAxis.getTickLabelFont(), tlabs, axisLen);
            float y1 = y > 0 ? y + tickLen : y - tickLen;
            if (this.angleY < 90 || (this.angleY >= 180 && this.angleY < 270)) {
                xAlign = XAlign.LEFT;
            } else {
                xAlign = XAlign.RIGHT;
            }
            if (this.angleX > -120) {
                yAlign = YAlign.TOP;
            } else {
                yAlign = YAlign.BOTTOM;
            }
            strWidth = 0.0f;
            strHeight = 0.0f;
            for (int i = 0; i < this.xAxis.getTickValues().length; i += skip) {
                v = (float) this.xAxis.getTickValues()[i];
                if (v < xmin || v > xmax) {
                    continue;
                }
                v = this.transform_xf(v);
                if (i == tlabs.size()) {
                    break;
                }

                //Draw grid line
                if (this.displayGrids && (v != -1.0f && v != 1.0f)) {
                    rgba = this.getLineBoxColor().getRGBComponents(null);
                    gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                    gl.glLineWidth(1.0f);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(v, y, -1.0f);
                    gl.glVertex3f(v, -y, -1.0f);
                    gl.glEnd();
                    if (this.displayZ && this.boxed) {
                        gl.glBegin(GL2.GL_LINES);
                        gl.glVertex3f(v, -y, -1.0f);
                        gl.glVertex3f(v, -y, 1.0f);
                        gl.glEnd();
                    }
                }

                //Draw tick line
                rgba = this.xAxis.getLineColor().getRGBComponents(null);
                gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                gl.glLineWidth(this.xAxis.getLineWidth());
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex3f(v, y, -1.0f);
                gl.glVertex3f(v, y1, -1.0f);
                gl.glEnd();

                //Draw tick label                
                rect = drawString(gl, tlabs.get(i), v, y1, -1.0f, xAlign, yAlign);
                if (strWidth < rect.getWidth()) {
                    strWidth = (float) rect.getWidth();
                }
                if (strHeight < rect.getHeight()) {
                    strHeight = (float) rect.getHeight();
                }
            }

            //Draw x axis label
            ChartText label = this.xAxis.getLabel();
            if (label != null) {
                strWidth += 10;
                float xShift = xAlign == XAlign.LEFT ? strWidth : -strWidth;
                float yShift = this.angleX > -120 ? -strHeight : strHeight;
                drawString(gl, label, 0.0f, y1, -1.0f, xAlign, yAlign, xShift, yShift);
            }

            ////////////////////////////////////////////
            //y axis line
            if (this.angleY >= 180 && this.angleY < 360) {
                x = 1.0f;
            } else {
                x = -1.0f;
            }
            rgba = this.yAxis.getLineColor().getRGBComponents(null);
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glLineWidth(this.yAxis.getLineWidth());
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex3f(x, -1.0f, -1.0f);
            gl.glVertex3f(x, 1.0f, -1.0f);
            gl.glEnd();

            //y axis ticks
            this.yAxis.updateTickLabels();
            tlabs = this.yAxis.getTickLabels();
            axisLen = this.toScreenLength(x, -1.0f, -1.0f, x, 1.0f, -1.0f);
            skip = getLabelGap(this.yAxis.getTickLabelFont(), tlabs, axisLen);
            float x1 = x > 0 ? x + tickLen : x - tickLen;
            if (this.angleY < 90 || (this.angleY >= 180 && this.angleY < 270)) {
                xAlign = XAlign.RIGHT;
            } else {
                xAlign = XAlign.LEFT;
            }
            if (this.angleX > -120) {
                yAlign = YAlign.TOP;
            } else {
                yAlign = YAlign.BOTTOM;
            }
            strWidth = 0.0f;
            strHeight = 0.0f;
            for (int i = 0; i < this.yAxis.getTickValues().length; i += skip) {
                v = (float) this.yAxis.getTickValues()[i];
                if (v < ymin || v > ymax) {
                    continue;
                }
                v = this.transform_yf(v);
                if (i == tlabs.size()) {
                    break;
                }

                //Draw grid line
                if (this.displayGrids && (v != -1.0f && v != 1.0f)) {
                    rgba = this.getLineBoxColor().getRGBComponents(null);
                    gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                    gl.glLineWidth(1.0f);
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(x, v, -1.0f);
                    gl.glVertex3f(-x, v, -1.0f);
                    gl.glEnd();
                    if (this.displayZ && this.boxed) {
                        gl.glBegin(GL2.GL_LINES);
                        gl.glVertex3f(-x, v, -1.0f);
                        gl.glVertex3f(-x, v, 1.0f);
                        gl.glEnd();
                    }
                }

                //Draw tick line
                rgba = this.yAxis.getLineColor().getRGBComponents(null);
                gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                gl.glLineWidth(this.yAxis.getLineWidth());
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex3f(x, v, -1.0f);
                gl.glVertex3f(x1, v, -1.0f);
                gl.glEnd();

                //Draw tick label                
                rect = drawString(gl, tlabs.get(i), x1, v, -1.0f, xAlign, yAlign);
                if (strWidth < rect.getWidth()) {
                    strWidth = (float) rect.getWidth();
                }
                if (strHeight < rect.getHeight()) {
                    strHeight = (float) rect.getHeight();
                }
            }

            //Draw y axis label
            label = this.yAxis.getLabel();
            if (label != null) {
                strWidth += 10;
                float xShift = xAlign == XAlign.LEFT ? strWidth : -strWidth;
                float yShift = this.angleX > -120 ? -strHeight : strHeight;
                drawString(gl, label, x1, 0.0f, -1.0f, xAlign, yAlign, xShift, yShift);
            }
        }

        //Draw z axis
        if (this.displayZ) {
            //z axis line
            if (this.angleY < 90) {
                x = -1.0f;
                y = 1.0f;
            } else if (this.angleY < 180) {
                x = 1.0f;
                y = 1.0f;
            } else if (this.angleY < 270) {
                x = 1.0f;
                y = -1.0f;
            } else {
                x = -1.0f;
                y = -1.0f;
            }
            rgba = this.zAxis.getLineColor().getRGBComponents(null);
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glLineWidth(this.zAxis.getLineWidth());
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex3f(x, y, -1.0f);
            gl.glVertex3f(x, y, 1.0f);
            gl.glEnd();

            //z axis ticks
            this.zAxis.updateTickLabels();
            List<ChartText> tlabs = this.zAxis.getTickLabels();
            float axisLen = this.toScreenLength(x, y, -1.0f, x, y, 1.0f);
            skip = getLabelGap(this.zAxis.getTickLabelFont(), tlabs, axisLen);
            float x1 = x;
            float y1 = y;
            if (x < 0) {
                if (y > 0) {
                    y1 += tickLen;
                } else {
                    x1 -= tickLen;
                }
            } else {
                if (y > 0) {
                    x1 += tickLen;
                } else {
                    y1 -= tickLen;
                }
            }
            xAlign = XAlign.RIGHT;
            yAlign = YAlign.CENTER;
            strWidth = 0.0f;
            for (int i = 0; i < this.zAxis.getTickValues().length; i += skip) {
                v = (float) this.zAxis.getTickValues()[i];
                if (v < zmin || v > zmax) {
                    continue;
                }
                v = this.transform_zf(v);
                if (i == tlabs.size()) {
                    break;
                }

                //Draw grid line
                if (this.displayGrids && this.boxed && (v != -1.0f && v != 1.0f)) {
                    rgba = this.getLineBoxColor().getRGBComponents(null);
                    gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                    gl.glLineWidth(1.0f);
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3f(x, y, v);
                    if (x < 0) {
                        if (y > 0) {
                            gl.glVertex3f(-x, y, v);
                            gl.glVertex3f(-x, -y, v);
                        } else {
                            gl.glVertex3f(x, -y, v);
                            gl.glVertex3f(-x, -y, v);
                        }
                    } else {
                        if (y > 0) {
                            gl.glVertex3f(x, -y, v);
                            gl.glVertex3f(-x, -y, v);
                        } else {
                            gl.glVertex3f(-x, y, v);
                            gl.glVertex3f(-x, -y, v);
                        }
                    }
                    gl.glEnd();
                }

                //Draw tick line
                rgba = this.zAxis.getLineColor().getRGBComponents(null);
                gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                gl.glLineWidth(this.zAxis.getLineWidth());
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex3f(x, y, v);
                gl.glVertex3f(x1, y1, v);
                gl.glEnd();

                //Draw tick label                
                rect = drawString(gl, tlabs.get(i), x1, y1, v, xAlign, yAlign);
                if (strWidth < rect.getWidth()) {
                    strWidth = (float) rect.getWidth();
                }
            }

            //Draw z axis label
            ChartText label = this.zAxis.getLabel();
            if (label != null) {
                strWidth += 10;
                float xShift = xAlign == XAlign.LEFT ? strWidth : -strWidth;
                drawString(gl, label, x1, y1, 0.0f, xAlign, yAlign, xShift, 0);
            }
        }
    }

    Rectangle2D drawString(GL2 gl, ChartText text, float vx, float vy, float vz, XAlign xAlign, YAlign yAlign) {
        return drawString(gl, text, vx, vy, vz, xAlign, yAlign, 0, 0);
    }

    Rectangle2D drawString(GL2 gl, ChartText text, float vx, float vy, float vz,
            XAlign xAlign, YAlign yAlign, float xShift, float yShift) {
        return drawString(gl, text.getText(), text.getFont(), text.getColor(), vx,
                vy, vz, xAlign, yAlign, xShift, yShift);        
    }
    
    Rectangle2D drawString(GL2 gl, String str, Font font, Color color, float vx, float vy, float vz,
            XAlign xAlign, YAlign yAlign, float xShift, float yShift) {
        //Get screen coordinates
        float coord[] = this.toScreen(vx, vy, vz);
        float x = coord[0];
        float y = coord[1];

        //Rendering text string
        TextRenderer textRenderer = new TextRenderer(font, true);
        textRenderer.beginRendering(this.width, this.height);
        textRenderer.setColor(color);
        textRenderer.setSmoothing(true);
        Rectangle2D rect = textRenderer.getBounds(str.subSequence(0, str.length()));
        switch (xAlign) {
            case CENTER:
                x -= rect.getWidth() * 0.5;
                break;
            case RIGHT:
                x -= rect.getWidth();
                break;
        }
        switch (yAlign) {
            case CENTER:
                y -= rect.getHeight() * 0.5;
                break;
            case TOP:
                y -= rect.getHeight();
                break;
        }
        if (xAlign == XAlign.RIGHT && yAlign == YAlign.CENTER) {
            x -= this.tickSpace;
        }
        textRenderer.draw(str, (int) (x + xShift), (int) (y + yShift));
        textRenderer.endRendering();

        return rect;
    }

    void drawTitle() {
        if (title != null) {
            TextRenderer textRenderer = new TextRenderer(title.getFont(), true);
            textRenderer.beginRendering(this.width, this.height);
            textRenderer.setColor(title.getColor());
            textRenderer.setSmoothing(true);
            Rectangle2D rect = textRenderer.getBounds(title.getText().subSequence(0, title.getText().length()));
            float x = (float) (this.width / 2.0f) - (float) rect.getWidth() / 2.0f;
            float y = this.height - (float) rect.getHeight();
            textRenderer.draw(title.getText(), (int) x, (int) y);
            textRenderer.endRendering();
        }
    }

    private void drawGraphics(GL2 gl, Graphic graphic) {
        if (graphic.getNumGraphics() == 1) {
            Graphic gg = graphic.getGraphicN(0);
            this.drawGraphic(gl, gg);
        } else {
            boolean isDraw = true;
            if (graphic instanceof GraphicCollection3D) {
                GraphicCollection3D gg = (GraphicCollection3D) graphic;
                if (gg.isAllQuads()) {
                    this.drawQuadsPolygons(gl, gg);
                    isDraw = false;
                }
            }
            if (isDraw) {
                for (int i = 0; i < graphic.getNumGraphics(); i++) {
                    Graphic gg = graphic.getGraphicN(i);
                    this.drawGraphic(gl, gg);
                }
            }
        }
    }

    private void drawGraphic(GL2 gl, Graphic graphic) {
        Shape shape = graphic.getGraphicN(0).getShape();
        switch (shape.getShapeType()) {
            case Point:
            case PointZ:
                this.drawPoint(gl, graphic);
                break;
            case TEXT:
                //this.drawText((ChartText3D) shape, g);
                break;
            case Polyline:
            case PolylineZ:
                this.drawLineString(gl, graphic);
                break;
            case Polygon:
            case PolygonZ:
                this.drawPolygonShape(gl, graphic);
                break;
            case WindArraw:
                //this.drawWindArrow(g, graphic);
                break;
            case Image:
                this.drawImage(gl, graphic);
                break;
            case TEXTURE:
                this.drawTexture(gl, graphic);
                break;
        }
    }

    private void drawPoint(GL2 gl, Graphic graphic) {
        if (extent.intersects(graphic.getExtent())) {
            PointZShape shape = (PointZShape) graphic.getShape();
            PointBreak pb = (PointBreak) graphic.getLegend();
            float[] rgba = pb.getColor().getRGBComponents(null);
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glPointSize(pb.getSize());
            gl.glBegin(GL2.GL_POINTS);
            PointZ p = (PointZ) shape.getPoint();
            gl.glVertex3f(transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z));
            gl.glEnd();
        }
    }

    private void drawLineString(GL2 gl, Graphic graphic) {
        if (extent.intersects(graphic.getExtent())) {
            PolylineZShape shape = (PolylineZShape) graphic.getShape();
            PolylineBreak pb = (PolylineBreak) graphic.getLegend();
            float[] rgba = pb.getColor().getRGBComponents(null);
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glLineWidth(pb.getWidth());
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (Polyline line : shape.getPolylines()) {
                List<PointZ> ps = (List<PointZ>) line.getPointList();
                for (PointZ p : ps) {
                    gl.glVertex3f(transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z));
                }
            }
            gl.glEnd();
        }
    }

    private void drawPolygonShape(GL2 gl, Graphic graphic) {
        if (extent.intersects(graphic.getExtent())) {
            PolygonZShape shape = (PolygonZShape) graphic.getShape();
            PolygonBreak pb = (PolygonBreak) graphic.getLegend();
            for (PolygonZ poly : (List<PolygonZ>) shape.getPolygons()) {
                if (GeometryUtil.isConvex(poly)) {
                    drawConvexPolygon(gl, poly, pb);
                } else {
                    drawPolygon(gl, poly, pb);
                }
            }
        }
    }

    private void drawPolygon(GL2 gl, PolygonZ aPG, PolygonBreak aPGB) {
        PointZ p;
        if (aPGB.isDrawFill()) {
            float[] rgba = aPGB.getColor().getRGBComponents(null);
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);

            try {
                //int startList = gl.glGenLists(1);
                GLUtessellator tobj = GLU.gluNewTess();
                GLU.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);// glVertex3dv);
                GLU.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);
                GLU.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);// beginCallback);
                GLU.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);// endCallback);
                GLU.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);// errorCallback);

                //gl.glNewList(startList, GL2.GL_COMPILE);
                //gl.glShadeModel(GL2.GL_FLAT);
                GLU.gluTessBeginPolygon(tobj, null);
                GLU.gluTessBeginContour(tobj);
                double[] v;
                for (int i = 0; i < aPG.getOutLine().size() - 2; i++) {
                    p = ((List<PointZ>) aPG.getOutLine()).get(i);
                    v = transform(p);
                    GLU.gluTessVertex(tobj, v, 0, v);
                }
                GLU.gluTessEndContour(tobj);
                GLU.gluTessEndPolygon(tobj);
                //gl.glEndList();
                GLU.gluDeleteTess(tobj);

                //gl.glCallList(startList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (aPGB.isDrawOutline()) {
            float[] rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize());
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f(transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z));
            }
            gl.glEnd();

            if (aPG.hasHole()) {
                List<PointZ> newPList;
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int h = 0; h < aPG.getHoleLines().size(); h++) {
                    newPList = (List<PointZ>) aPG.getHoleLines().get(h);
                    for (int j = 0; j < newPList.size(); j++) {
                        p = newPList.get(j);
                        gl.glVertex3f(transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z));
                    }
                }
                gl.glEnd();
            }
        }
    }

    private void drawConvexPolygon(GL2 gl, PolygonZ aPG, PolygonBreak aPGB) {
        PointZ p;
        if (aPGB.isDrawFill()) {
            float[] rgba = aPGB.getColor().getRGBComponents(null);
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glBegin(GL2.GL_POLYGON);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f(transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z));
            }
            gl.glEnd();
        }

        if (aPGB.isDrawOutline()) {
            float[] rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize());
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f(transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z));
            }
            gl.glEnd();
        }
    }

    private void drawQuadsPolygons(GL2 gl, GraphicCollection3D graphic) {
        PointZ p;
        for (int i = 0; i < graphic.getNumGraphics(); i++) {
            Graphic gg = graphic.getGraphicN(i);
            if (extent.intersects(gg.getExtent())) {
                PolygonZShape shape = (PolygonZShape) gg.getShape();
                PolygonBreak pb = (PolygonBreak) gg.getLegend();
                for (PolygonZ poly : (List<PolygonZ>) shape.getPolygons()) {
                    drawQuads(gl, poly, pb);
                }
            }
        }
    }

    private void drawQuads(GL2 gl, PolygonZ aPG, PolygonBreak aPGB) {
        PointZ p;
        float[] rgba = aPGB.getColor().getRGBComponents(null);
        if (aPGB.isDrawFill()) {
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glBegin(GL2.GL_QUADS);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f(transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z));
            }
            gl.glEnd();
        }

        if (aPGB.isDrawOutline()) {
            rgba = aPGB.getOutlineColor().getRGBComponents(null);
            gl.glLineWidth(aPGB.getOutlineSize());
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < aPG.getOutLine().size(); i++) {
                p = ((List<PointZ>) aPG.getOutLine()).get(i);
                gl.glVertex3f(transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z));
            }
            gl.glEnd();
        }
    }

    private void drawImage(GL2 gl, Graphic graphic) {
        ImageShape ishape = (ImageShape) graphic.getShape();
        BufferedImage image = ishape.getImage();
        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, true);
        //Texture texture = this.imageCache.get(image);
        int idTexture = texture.getTextureObject();
        List<PointZ> coords = ishape.getCoords();

        gl.glColor3f(1f, 1f, 1f);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, idTexture);

        // Texture parameterization
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        // Draw image
        gl.glBegin(GL2.GL_QUADS);
        // Front Face
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(transform_xf((float) coords.get(0).X), transform_yf((float) coords.get(0).Y), transform_zf((float) coords.get(0).Z));
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(transform_xf((float) coords.get(1).X), transform_yf((float) coords.get(1).Y), transform_zf((float) coords.get(1).Z));
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(transform_xf((float) coords.get(2).X), transform_yf((float) coords.get(2).Y), transform_zf((float) coords.get(2).Z));
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(transform_xf((float) coords.get(3).X), transform_yf((float) coords.get(3).Y), transform_zf((float) coords.get(3).Z));
        gl.glEnd();

        // Unbinding the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
    }

    private void drawImage(GL2 gl) throws IOException {
        File im = new File("D:\\Temp\\image\\lenna.jpg ");
        BufferedImage image = ImageIO.read(im);
        Texture t = AWTTextureIO.newTexture(gl.getGLProfile(), image, true);
        //Texture t = TextureIO.newTexture(im, true);
        //Texture t = this.imageCache.get(image);
        int idTexture = t.getTextureObject(gl);

        gl.glColor3f(1f, 1f, 1f);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, idTexture);

//        // Texture parameterization
//        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
//        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        // Draw image
        gl.glBegin(GL2.GL_QUADS);
        // Front Face
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
        gl.glEnd();

        // Unbinding the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
    }

    private void drawTexture(GL2 gl, Graphic graphic) {
        TextureShape ishape = (TextureShape) graphic.getShape();
        Texture texture = ishape.getTexture();
        if (texture == null) {
            try {
                ishape.loadTexture();
                texture = ishape.getTexture();
            } catch (IOException ex) {
                Logger.getLogger(Plot3DGL.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (texture == null) {
            return;
        }

        int idTexture = texture.getTextureObject();
        List<PointZ> coords = ishape.getCoords();

        gl.glColor3f(1f, 1f, 1f);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, idTexture);

        // Texture parameterization
        //gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        // Draw image
        gl.glBegin(GL2.GL_QUADS);
        // Front Face
        gl.glTexCoord2f(0.0f, 0.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(transform_xf((float) coords.get(0).X), transform_yf((float) coords.get(0).Y), transform_zf((float) coords.get(0).Z));
        gl.glTexCoord2f(1.0f, 0.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(transform_xf((float) coords.get(1).X), transform_yf((float) coords.get(1).Y), transform_zf((float) coords.get(1).Z));
        gl.glTexCoord2f(1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(transform_xf((float) coords.get(2).X), transform_yf((float) coords.get(2).Y), transform_zf((float) coords.get(2).Z));
        gl.glTexCoord2f(0.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(transform_xf((float) coords.get(3).X), transform_yf((float) coords.get(3).Y), transform_zf((float) coords.get(3).Z));
        gl.glEnd();

        // Unbinding the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
    }

    void drawLegend(GL2 gl) {
        if (!this.legends.isEmpty()) {
            ChartColorBar legend = (ChartColorBar) this.legends.get(0);
            LegendScheme ls = legend.getLegendScheme();
            int bNum = ls.getBreakNum();
            if (ls.getLegendBreaks().get(bNum - 1).isNoData()) {
                bNum -= 1;
            }

            float x = 1.6f;
            float y = -1.0f;
            float lHeight = 2.0f;
            float lWidth = lHeight / legend.getAspect();
            List<Integer> labelIdxs = new ArrayList<>();
            List<String> tLabels = new ArrayList<>();
            if (legend.isAutoTick()) {
                float legendLen = this.toScreenLength(x, y, 0.0f, x, y + lHeight, 0.0f);
                int tickGap = this.getLegendTickGap(legend, legendLen);
                int sIdx = (bNum % tickGap) / 2;
                int labNum = bNum - 1;
                if (ls.getLegendType() == LegendType.UniqueValue) {
                    labNum += 1;
                } else if (legend.isDrawMinLabel()) {
                    sIdx = 0;
                    labNum = bNum;
                }
                while (sIdx < labNum) {
                    labelIdxs.add(sIdx);
                    sIdx += tickGap;
                }
            } else {
                int tickIdx;
                for (int i = 0; i < bNum; i++) {
                    ColorBreak cb = ls.getLegendBreaks().get(i);
                    double v = Double.parseDouble(cb.getEndValue().toString());
                    if (legend.getTickLocations().contains(v)) {
                        labelIdxs.add(i);
                        tickIdx = legend.getTickLocations().indexOf(v);
                        tLabels.add(legend.getTickLabels().get(tickIdx).getText());
                    }
                }
            }

            float barHeight = lHeight / bNum;

            //Draw color bar
            float yy = y;
            float[] rgba;
            for (int i = 0; i < bNum; i++) {
                //Fill color
                rgba = ls.getLegendBreak(i).getColor().getRGBComponents(null);
                gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                gl.glBegin(GL2.GL_QUADS);
                gl.glVertex2f(x, yy);
                gl.glVertex2f(x + lWidth, yy);
                gl.glVertex2f(x + lWidth, yy + barHeight);
                gl.glVertex2f(x, yy + barHeight);
                gl.glEnd();
                yy += barHeight;
            }

            //Draw neatline
            rgba = Color.black.getRGBComponents(null);
            gl.glColor3f(rgba[0], rgba[1], rgba[2]);
            gl.glLineWidth(1.0f);
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex2f(x, y);
            gl.glVertex2f(x, y + lHeight);
            gl.glVertex2f(x + lWidth, y + lHeight);
            gl.glVertex2f(x + lWidth, y);
            gl.glVertex2f(x, y);
            gl.glEnd();

            //Draw ticks
            int idx = 0;
            yy = y;
            String caption;
            for (int i = 0; i < bNum; i++) {
                if (labelIdxs.contains(i)) {
                    ColorBreak cb = ls.getLegendBreaks().get(i);
                    if (legend.isAutoTick()) {
                        if (ls.getLegendType() == LegendType.UniqueValue) {
                            caption = cb.getCaption();
                        } else {
                            caption = DataConvert.removeTailingZeros(cb.getEndValue().toString());
                        }
                    } else {
                        caption = tLabels.get(idx);
                    }
                    if (ls.getLegendType() == LegendType.UniqueValue) {
                        this.drawString(gl, caption, legend.getTickLabelFont(), Color.black, 
                                x + lWidth, yy + barHeight * 0.5f, 0, XAlign.LEFT, YAlign.CENTER, 5, 0);
                    } else {
                        rgba = Color.black.getRGBComponents(null);
                        gl.glColor3f(rgba[0], rgba[1], rgba[2]);
                        gl.glLineWidth(1.0f);
                        gl.glBegin(GL2.GL_LINES);
                        gl.glVertex2f(x + lWidth * 0.5f, yy + barHeight);
                        gl.glVertex2f(x + lWidth, yy + barHeight);
                        gl.glEnd();
                        this.drawString(gl, caption, legend.getTickLabelFont(), Color.black, 
                                x + lWidth, yy + barHeight, 0, XAlign.LEFT, YAlign.CENTER, 5, 0);
                    }
                    
                    idx += 1;
                }
                yy += barHeight;
            }
        }
    }

    private float transform_xf(float v) {
        return (v - xmin) / (xmax - xmin) * 2.f - 1.0f;
    }

    private double transform_x(double v) {
        return (v - xmin) / (xmax - xmin) * 2. - 1.0;
    }

    private float transform_yf(float v) {
        return (v - ymin) / (ymax - ymin) * 2.f - 1.0f;
    }

    private double transform_y(double v) {
        return (v - ymin) / (ymax - ymin) * 2. - 1.0;
    }

    private float transform_zf(float v) {
        return (v - zmin) / (zmax - zmin) * 2.f - 1.0f;
    }

    private double transform_z(double v) {
        return (v - zmin) / (zmax - zmin) * 2. - 1.0;
    }

    private float[] transformf(PointZ p) {
        return new float[]{transform_xf((float) p.X), transform_yf((float) p.Y), transform_zf((float) p.Z)};
    }

    private double[] transform(PointZ p) {
        return new double[]{transform_x(p.X), transform_y(p.Y), transform_z(p.Z)};
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // method body
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        //White background
        gl.glClearColor(1f, 1f, 1f, 1.0f);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        gl.glEnable(GL2.GL_TEXTURE_2D);

        //jogl specific addition for tessellation
        tessCallback = new tessellCallBack(gl, glu);

        this.positionArea = new Rectangle2D.Double(0, 0, 1, 1);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        this.positionArea = this.getPositionArea(new Rectangle2D.Double(0, 0, width, height));

        final GL2 gl = drawable.getGL().getGL2();
        //gl.glTranslatef(0f, 0f, 5f);
        if (height <= 0) {
            height = 1;
        }

        final float h = (float) width / (float) height;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        //glu.gluPerspective(45.0f, h, 1.0, 20.0);
        float v = 2.0f;
        gl.glOrthof(-v, v, -v, v, -v, v);
        //glu.gluLookAt(0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);        
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /**
     * Get tight inset area
     *
     * @param g Graphics2D
     * @param positionArea Position area
     * @return Tight inset area
     */
    @Override
    public Margin getTightInset(Graphics2D g, Rectangle2D positionArea) {
        return null;
    }

    public static void main(String[] args) {

        final GLProfile gp = GLProfile.get(GLProfile.GL2);
        GLCapabilities cap = new GLCapabilities(gp);

        final GLChartPanel gc = new GLChartPanel(cap, new Plot3DGL());
        gc.setSize(400, 400);

        final JFrame frame = new JFrame("JOGL Line");
        frame.add(gc);
        frame.setSize(500, 400);
        frame.setVisible(true);

        //gc.animator_start();
    }
    // </editor-fold>

    /*
     * Tessellator callback implemenation with all the callback routines. YOu
     * could use GLUtesselatorCallBackAdapter instead. But
     */
    class tessellCallBack
            implements GLUtessellatorCallback {

        private final GL2 gl;
        private final GLU glu;

        public tessellCallBack(GL2 gl, GLU glu) {
            this.gl = gl;
            this.glu = glu;
        }

        @Override
        public void begin(int type) {
            gl.glBegin(type);
        }

        @Override
        public void end() {
            gl.glEnd();
        }

        @Override
        public void vertex(Object vertexData) {
            double[] pointer;
            if (vertexData instanceof double[]) {
                pointer = (double[]) vertexData;
                if (pointer.length == 6) {
                    gl.glColor3dv(pointer, 3);
                }
                gl.glVertex3dv(pointer, 0);
            }
        }

        @Override
        public void vertexData(Object vertexData, Object polygonData) {
        }

        /*
         * combineCallback is used to create a new vertex when edges intersect.
         * coordinate location is trivial to calculate, but weight[4] may be used to
         * average color, normal, or texture coordinate data. In this program, color
         * is weighted.
         */
        @Override
        public void combine(double[] coords, Object[] data, //
                float[] weight, Object[] outData) {
            double[] vertex = new double[3];
            //int i;

            vertex[0] = coords[0];
            vertex[1] = coords[1];
            vertex[2] = coords[2];
//            for (i = 3; i < 6/* 7OutOfBounds from C! */; i++) {
//                vertex[i] = weight[0] //
//                        * ((double[]) data[0])[i] + weight[1]
//                        * ((double[]) data[1])[i] + weight[2]
//                        * ((double[]) data[2])[i] + weight[3]
//                        * ((double[]) data[3])[i];
//            }
            outData[0] = vertex;
        }

        @Override
        public void combineData(double[] coords, Object[] data, //
                float[] weight, Object[] outData, Object polygonData) {
        }

        @Override
        public void error(int errnum) {
            String estring;

            estring = glu.gluErrorString(errnum);
            System.err.println("Tessellation Error: " + estring);
            System.exit(0);
        }

        @Override
        public void beginData(int type, Object polygonData) {
        }

        @Override
        public void endData(Object polygonData) {
        }

        @Override
        public void edgeFlag(boolean boundaryEdge) {
        }

        @Override
        public void edgeFlagData(boolean boundaryEdge, Object polygonData) {
        }

        @Override
        public void errorData(int errnum, Object polygonData) {
        }
    }// tessellCallBack
}
