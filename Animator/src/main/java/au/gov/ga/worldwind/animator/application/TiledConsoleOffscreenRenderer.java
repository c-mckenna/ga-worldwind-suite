package au.gov.ga.worldwind.animator.application;

import au.gov.ga.worldwind.animator.animation.Animation;
import au.gov.ga.worldwind.animator.animation.RenderParameters;
import au.gov.ga.worldwind.animator.view.TileDelegate;
import au.gov.ga.worldwind.common.render.FrameBuffer;
import au.gov.ga.worldwind.common.render.PaintTask;
import au.gov.ga.worldwind.common.view.delegate.IDelegateView;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL2;
import java.awt.*;
import java.io.File;

/**
 * Renderer implementation used by the {@link Console} version of the animator to render the current viewport as tiles
 * to an offscreen buffer and output the tile files.
 *
 * <p>
 * This bare minimum implementation is intended to render a single frame at very high resolutions that exceed physical
 * system memory.
 */
public class TiledConsoleOffscreenRenderer extends ConsoleOffscreenRenderer {
    private final Dimension tileDimensions;
    private final FrameBuffer frameBuffer = new FrameBuffer();

    private TileDelegate tileDelegate;

    public TiledConsoleOffscreenRenderer(WorldWindow wwd, Dimension tileDimensions) {
        super(wwd);
        this.tileDimensions = tileDimensions;
    }

    @Override
    protected void doPreRender(final Animation animation, final RenderParameters renderParams)
    {
        setupForRendering(renderParams.getDetailLevel());

        final Dimension renderDimensions = renderParams.getRenderDimension();

        this.tileDelegate = new TileDelegate(renderDimensions, tileDimensions);

        animatorSceneController.setRenderDimensions(tileDimensions);
        animatorSceneController.addPrePaintTask(new PaintTask()
        {
            @Override
            public void run(DrawContext dc)
            {
                frameBuffer.create(dc.getGL().getGL2(), tileDimensions);
            }
        });

        //create a pre PaintTask which will setup the viewport and FBO every frame
        preRenderTask = new PaintTask()
        {
            @Override
            public void run(DrawContext dc)
            {
                GL2 gl = dc.getGL().getGL2();
                frameBuffer.bind(gl);
                gl.glViewport(0, 0, tileDimensions.width, tileDimensions.height);
            }
        };

        prePostRenderTask = new PaintTask()
        {
            @Override
            public void run(DrawContext dc)
            {
                GL2 gl = dc.getGL().getGL2();
                gl.glViewport(0, 0, tileDimensions.width, tileDimensions.height);
            }
        };

        //create a post PaintTask which will reset the viewport, unbind the FBO, and draw the
        //offscreen texture to a quad in screen coordinates
        postRenderTask = new PaintTask()
        {
            @Override
            public void run(DrawContext dc)
            {
                GL2 gl = dc.getGL().getGL2();
                frameBuffer.unbind(gl);
                gl.glViewport(0, 0, dc.getDrawableWidth(), dc.getDrawableHeight());
                FrameBuffer.renderTexturedQuad(gl, frameBuffer.getTexture().getId());
            }
        };

        wwd.redrawNow();
    }

    @Override
    protected void renderFrame(int frame, Animation animation, RenderParameters renderParams) {
        IDelegateView view = (IDelegateView) wwd.getView();
        view.setDelegate(tileDelegate);

        while (tileDelegate.hasTileToRender()) {
            File targetFile = new File(renderParams.getRenderDirectory(), "Tile_" + String.format("%04d", tileDelegate.getRows() - tileDelegate.getCurrentRow()) + "_" + String.format("%04d", tileDelegate.getCurrentColumn()) + ".tga");
            doRender(frame, targetFile, animation, renderParams);

            tileDelegate.incrementTile();
        }

        Logging.logger().info(tileDelegate.toString());

        view.setDelegate(null);
    }

    @Override
    protected void doPostRender(Animation animation, RenderParameters renderParams)
    {
        animatorSceneController.setRenderDimensions(null);
        animatorSceneController.addPostPaintTask(new PaintTask()
        {
            @Override
            public void run(DrawContext dc)
            {
                frameBuffer.delete(dc.getGL().getGL2());
            }
        });

        wwd.redrawNow();
        resetViewingParameters();
    }
}
