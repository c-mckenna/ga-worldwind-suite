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

package gistoolkit.datasources.filter;

import gistoolkit.common.*;
import gistoolkit.features.*;

/**
 *  A base class to provide a way to filter a data source bassed on attributes, or on shapes.
 */
public interface Filter {
            
    /** 
     * Determines if this record should or should not be returned as part of the resulting dataset.
     * 
     * <p> Returns True if the record should be included, and returns False if it should not. <p>
     */
    public boolean contains(Record inRecord);
    
    /**
     * Returns a name for this filter. 
     */
    public String getFilterName();

    /**
     * Get the configuration information for the filter.
     */
    public Node getNode();
    /**
     * Set the configuration information in the filter.
     */
    public void setNode(Node inNode) throws Exception ;

}
