package au.gov.ga.worldwind.animator.view;

import au.gov.ga.worldwind.common.render.DrawableSceneController;
import au.gov.ga.worldwind.common.view.delegate.IDelegateView;
import au.gov.ga.worldwind.common.view.delegate.IViewDelegate;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.*;

/**
 * A {@link IViewDelegate} used to split the current viewport into tiles and compute the perspective matrix for the
 * active tile.
 */
public class TileDelegate implements IViewDelegate {
    private final Dimension renderDimensions;
    public final Dimension tileDimensions;

    private final int columns;
    private final int rows;
    private int currentTile;

    public TileDelegate(Dimension renderDimensions, Dimension tileDimensions) {
        this.renderDimensions = renderDimensions;
        this.tileDimensions = tileDimensions;

        this.columns = renderDimensions.width / tileDimensions.width;
        this.rows = renderDimensions.height / tileDimensions.height;
        this.currentTile = 0;
    }

    @Override
    public void installed(IDelegateView view) {
    }

    @Override
    public void uninstalled(IDelegateView view) {
    }

    @Override
    public void beforeComputeMatrices(IDelegateView view) {
    }

    @Override
    public Matrix computeModelView(IDelegateView view) {
        return view.computeModelView();
    }

    @Override
    public Matrix getPretransformedModelView(IDelegateView view) {
        return view.getPretransformedModelView();
    }

    @Override
    public Matrix computeProjection(IDelegateView view, Angle horizontalFieldOfView, double near, double far) {
        int currentRow = getCurrentRow();
        int currentColumn = getCurrentColumn();

        int tileHeight = this.tileDimensions.height;
        int tileWidth = this.tileDimensions.width;

        double aspectRatio = renderDimensions.getWidth() / renderDimensions.getHeight();
        double widthdiv2 = near * horizontalFieldOfView.tanHalfAngle();

        double top = widthdiv2 / aspectRatio;
        double bottom = -widthdiv2 / aspectRatio;
        double left = -widthdiv2;
        double right = widthdiv2;

        double tileLeft = left + (right - left) * (currentColumn * tileWidth) / renderDimensions.getWidth();
        double tileRight = tileLeft + (right - left) * tileWidth / renderDimensions.getWidth();
        double tileBottom = bottom + (top - bottom) * (currentRow * tileHeight) / renderDimensions.getHeight();
        double tileTop = tileBottom + (top - bottom) * tileHeight / renderDimensions.getHeight();

        // Create an off-axis perspective projection matrix to render a tile from the current viewport
        return new Matrix(
                (2.0 * near) / (tileRight - tileLeft), 0.0, (tileRight + tileLeft) / (tileRight - tileLeft), 0.0,
                0.0, (2.0 * near) / (tileTop - tileBottom), (tileTop + tileBottom) / (tileTop - tileBottom), 0.0,
                0.0, 0.0, -(far + near) / (far - near), -(2.0 * far * near) / (far - near),
                0.0, 0.0, -1.0, 0.0
        );
    }

    public void incrementTile() {
        this.currentTile++;
    }

    public boolean hasTileToRender() {
        return currentTile < rows * columns;
    }

    public int getCurrentRow() {
        return rows - (currentTile / columns) - 1;
    }

    public int getCurrentColumn() {
        return currentTile % columns;
    }

    @Override
    public void draw(IDelegateView view, DrawContext drawContext, DrawableSceneController drawableSceneController) {
        view.draw(drawContext, drawableSceneController);
    }

    public int getRows() {
        return rows;
    }

    @Override
    public String toString() {
        return "TileDelegate{" +
                "renderDimensions=" + renderDimensions +
                ", tileDimensions=" + tileDimensions +
                ", columns=" + columns +
                ", rows=" + rows +
                ", currentTile=" + currentTile +
                '}';
    }
}
