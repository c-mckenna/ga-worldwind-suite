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

package gistoolkit.datasources.shapefile;

import gistoolkit.features.*;
/**
 * Just like a polygon, but with an M coordinate.
 */
public class PolygonM extends Polygon {
    /**
     * The minimum M value.
     */
    private double myMinM = 0;
    
    /**
     * The maximum M value.
     */
    private double myMaxM = 0;
    
    /**
     * PolygonM constructor comment.
     */
    public PolygonM() {
        super();
    }
    /**
     * PolygonM constructor comment.
     * @param inLinearRing gistoolkit.features.LinearRing
     */
    public PolygonM(LinearRing inLinearRing) {
        super(inLinearRing);
    }
    /**
     * PolygonM constructor comment.
     * @param inLinearRing gistoolkit.features.LinearRing
     * @param inHoles gistoolkit.features.LinearRing[]
     */
    public PolygonM(LinearRing inLinearRing, gistoolkit.features.LinearRing[] inHoles) {
        super(inLinearRing, inHoles);
    }
    /**
     * returns the maximum M value.
     */
    public double getMaxM(){
        return myMaxM;
    }
    /**
     * returns the minimum M value.
     */
    public double getMinM(){
        return myMinM;
    }
    /**
     * Sets the maximum M value;
     */
    public void setMaxM(double inMaxM){
        myMaxM = inMaxM;
    }
    /**
     * Sets the minimum M value;
     */
    public void setMinM(double inMinM){
        myMinM = inMinM;
    }
}
