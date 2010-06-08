/*
 *    GISToolkit - Geographical Information System Toolkit
 *    (C) 2002, Ithaqua Enterprises Inc.
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; 
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *    
 */

package gistoolkit.display;

import java.awt.*;
import gistoolkit.common.*;
import gistoolkit.features.*;
public interface Renderer {
    /**
     * Returns a descriptive name for this renderer.
     */
    public String getRendererName();
    
    /**
     * Called before the layer is initially drawn to allow the renderer to prepare for rendering.
     */
    public void beginDraw();
    
    /**
     * Called after the layer has completed rendering.
     */
    public void endDraw();
    
    
    /**
     * Draw the shape.  The renderer should return true if it successfully drew the shape, and false if it did not.
     */
    public boolean drawShape(Record inRecord, Graphics inGraphics, Converter inConverter, Shader inShader);
    
    /**
     * Highlight the shape.  The renderer should return true if it successfully drew the shape, and false if it did not.
     */
    public boolean drawShapeHighlight(Record inRecord, Graphics inGraphics, Converter inConverter, Shader inShader);
        
    /** Get the configuration information for this renderer */
    public Node getNode();
    
    /** Set the configuration information for this renderer */
    public void setNode(Node inNode) throws Exception;
}