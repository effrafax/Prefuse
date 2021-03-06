/*  
 * Copyright (c) 2004-2013 Regents of the University of California.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of the University nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * Copyright (c) 2014 Martin Stockhammer
 */
/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "LICENSE.txt" for licensing terms.
 */
package prefux.data.io;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import prefux.data.Edge;
import prefux.data.Graph;
import prefux.data.Node;
import prefux.data.Schema;
import prefux.util.io.XMLWriter;

/**
 * GraphWriter instance that writes a graph file formatted using the
 * GraphML file format. GraphML is an XML format supporting graph
 * structure and typed data schemas for both nodes and edges. For more
 * information about the format, please see the
 * <a href="http://graphml.graphdrawing.org/">GraphML home page</a>.
 * 
 * <p>The GraphML spec only supports the data types <code>int</code>,
 * <code>long</code>, <code>float</code>, <code>double</code>,
 * <code>boolean</code>, and <code>string</code>. An exception will
 * be thrown if a data type outside these allowed types is
 * encountered.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class GraphMLWriter extends AbstractGraphWriter {

    /**
     * String tokens used in the GraphML format.
     */
    public interface Tokens extends GraphMLReader.Tokens  {
        public static final String GRAPHML = "graphml";
        
        public static final String GRAPHML_HEADER =
            "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n" 
            +"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            +"  xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns\n"
            +"  http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n\n";
    }
    
    /**
     * Map containing legal data types and their names in the GraphML spec
     */
    private static final HashMap TYPES = new HashMap();
    static {
        TYPES.put(int.class, Tokens.INT);
        TYPES.put(long.class, Tokens.LONG);
        TYPES.put(float.class, Tokens.FLOAT);
        TYPES.put(double.class, Tokens.DOUBLE);
        TYPES.put(boolean.class, Tokens.BOOLEAN);
        TYPES.put(String.class, Tokens.STRING);
    }
    
    /**
     * @see prefux.data.io.GraphWriter#writeGraph(prefux.data.Graph, java.io.OutputStream)
     */
    public void writeGraph(Graph graph, OutputStream os) throws DataIOException
    {
        // first, check the schemas to ensure GraphML compatibility
        Schema ns = graph.getNodeTable().getSchema();
        Schema es = graph.getEdgeTable().getSchema();
        checkGraphMLSchema(ns);
        checkGraphMLSchema(es);
        
        XMLWriter xml = new XMLWriter(new PrintWriter(os));
        xml.begin(Tokens.GRAPHML_HEADER, 2);
        
        xml.comment("prefux GraphML Writer | "
                + new Date(System.currentTimeMillis()));
        
        // print the graph schema
        printSchema(xml, Tokens.NODE, ns, null);
        printSchema(xml, Tokens.EDGE, es, new String[] {
            graph.getEdgeSourceField(), graph.getEdgeTargetField()
        });
        xml.println();
        
        // print graph contents
        xml.start(Tokens.GRAPH, Tokens.EDGEDEF,
            graph.isDirected() ? Tokens.DIRECTED : Tokens.UNDIRECTED);
        
        // print the nodes
        xml.comment("nodes");
        Iterator nodes = graph.nodes();
        while ( nodes.hasNext() ) {
            Node n = (Node)nodes.next();
            
            if ( ns.getColumnCount() > 0 ) {
                xml.start(Tokens.NODE, Tokens.ID, String.valueOf(n.getRow()));
                for ( int i=0; i<ns.getColumnCount(); ++i ) {
                    String field = ns.getColumnName(i);
                    xml.contentTag(Tokens.DATA, Tokens.KEY, field,
                                   n.getString(field));
                }
                xml.end();
            } else {
                xml.tag(Tokens.NODE, Tokens.ID, String.valueOf(n.getRow()));
            }
        }
        
        // add a blank line
        xml.println();
        
        // print the edges
        String[] attr = new String[]{Tokens.ID, Tokens.SOURCE, Tokens.TARGET};
        String[] vals = new String[3];
        
        xml.comment("edges");
        Iterator edges = graph.edges();
        while ( edges.hasNext() ) {
            Edge e = (Edge)edges.next();
            vals[0] = String.valueOf(e.getRow());
            vals[1] = String.valueOf(e.getSourceNode().getRow());
            vals[2] = String.valueOf(e.getTargetNode().getRow());
            
            if ( es.getColumnCount() > 2 ) {
                xml.start(Tokens.EDGE, attr, vals, 3);
                for ( int i=0; i<es.getColumnCount(); ++i ) {
                    String field = es.getColumnName(i);
                    if ( field.equals(graph.getEdgeSourceField()) ||
                         field.equals(graph.getEdgeTargetField()) )
                        continue;
                    
                    xml.contentTag(Tokens.DATA, Tokens.KEY, field, 
                                   e.getString(field));
                }
                xml.end();
            } else {
                xml.tag(Tokens.EDGE, attr, vals, 3);
            }
        }
        xml.end();
        
        // finish writing file
        xml.finish("</"+Tokens.GRAPHML+">\n");
    }
    
    /**
     * Print a table schema to a GraphML file
     * @param xml the XMLWriter to write to
     * @param group the data group (node or edge) for the schema
     * @param s the schema
     */
    private void printSchema(XMLWriter xml, String group, Schema s,
                             String[] ignore)
    {
        String[] attr = new String[] {Tokens.ID, Tokens.FOR,
                Tokens.ATTRNAME, Tokens.ATTRTYPE };
        String[] vals = new String[4];

OUTER:
        for ( int i=0; i<s.getColumnCount(); ++i ) {
            vals[0] = s.getColumnName(i);
            
            for ( int j=0; ignore!=null && j<ignore.length; ++j ) {
                if ( vals[0].equals(ignore[j]) )
                    continue OUTER;
            }
            
            vals[1] = group;
            vals[2] = vals[0];
            vals[3] = (String)TYPES.get(s.getColumnType(i));
            Object dflt = s.getDefault(i);
            
            if ( dflt == null ) {
                xml.tag(Tokens.KEY, attr, vals, 4);
            } else {
                xml.start(Tokens.KEY, attr, vals, 4);
                xml.contentTag(Tokens.DEFAULT, dflt.toString());
                xml.end();
            }
        }
    }
    
    /**
     * Checks if all Schema types are compatible with the GraphML specification.
     * The GraphML spec only allows the types <code>int</code>,
     * <code>long</code>, <code>float</code>, <code>double</code>,
     * <code>boolean</code>, and <code>string</code>.
     * @param s the Schema to check
     */
    private void checkGraphMLSchema(Schema s) throws DataIOException {
        for ( int i=0; i<s.getColumnCount(); ++i ) {
            Class type = s.getColumnType(i);
            if ( TYPES.get(type) == null ) {
                throw new DataIOException("Data type unsupported by the "
                    + "GraphML format: " + type.getName());
            }
        }
    }
    
} // end of class GraphMLWriter