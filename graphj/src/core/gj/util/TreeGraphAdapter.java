/**
 * This file is part of GraphJ
 * 
 * Copyright (C) 2002-2004 Nils Meier
 * 
 * GraphJ is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GraphJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GraphJ; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package gj.util;

import gj.model.Edge;
import gj.model.Graph;
import gj.model.Tree;
import gj.model.Vertex;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An adapter for tree to graph
 */
public class TreeGraphAdapter<V extends Vertex> implements Graph {
  
  private Tree<V> tree;
  
  public TreeGraphAdapter(Tree<V> tree) {
    this.tree = tree;
  }

  public Set<V> getVertices() {
    return _getVertices(tree.getRoot(), new LinkedHashSet<V>());
  }
    
  private Set<V> _getVertices(V parent, Set<V> result) {
    result.add(parent);
    for (V child : tree.getChildren(parent)) 
      _getVertices(child, result);
    return result;
  }

  public Set<Edge> getEdges() {
    return _getEdges(tree.getRoot(), new HashSet<Edge>());
  }
  
  public Set<Edge> _getEdges(V parent, Set<Edge> result) {
    for (V child : tree.getChildren(parent)) {
      result.add(new DefaultEdge(parent, child));
      _getEdges(child, result);
    }
    return result;
  }
      
  @SuppressWarnings("unchecked")
  public Set<Edge> getEdges(Vertex vertex) {
    Set<Edge> result = new LinkedHashSet<Edge>();
    if (!vertex.equals(tree.getRoot()))
      result.add(new DefaultEdge(tree.getParent((V)vertex), vertex));
    for (V child : tree.getChildren((V)vertex))
      result.add(new DefaultEdge(vertex, child));
    return result;
  }

}