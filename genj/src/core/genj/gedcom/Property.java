/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2002 Nils Meier <nils@meiers.net>
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package genj.gedcom;

import genj.util.Resources;
import genj.util.swing.ImageIcon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

/**
 * Abstract base type for all GEDCOM properties
 */
public abstract class Property implements Comparable {

  /** static strings */
  protected final static String 
    EMPTY_STRING = "",
    UNSUPPORTED_TAG = "Unsupported Tag";
    
  /** query flags */
  public static final int
    QUERY_ALL          =  0,
    QUERY_VALID_TRUE   =  1,
    QUERY_FOLLOW_LINK  =  4,
    QUERY_NO_LINK      =  8,
    QUERY_NO_BACKTRACK = 16;
    
  /** parent of this property */
  private Property parent=null;
  
  /** children of this property */
  private List children = new ArrayList();
  
  /** images */
  protected ImageIcon image, imageErr;
  
  /** whether we're transient or not */
  protected boolean isTransient = false;

  /** whether we're private or not */
  private boolean isPrivate = false;
  
  /** resources */
  protected final static Resources resources = Gedcom.resources;

  /** a localized label */
  public final static String LABEL = resources.getString("prop");

  /** the meta information */
  private MetaProperty meta = null;

  /**
   * Lifecycle - callback when being added to parent
   */
  /*package*/ void addNotify(Property parent, Transaction tx) {

    // remember parent
    this.parent=parent;

    // propage to still active children
    Property[] props = getProperties();
    for (int i=0,j=props.length;i<j;i++) {
      Property child = (Property)props[i];
      child.addNotify(this, tx);
    }

    // remember being added
    if (tx!=null)
      tx.get(Transaction.PROPERTIES_ADDED).add(this);

  }

  /**
   * Lifecycle - callback when being removed from parent
   */
  /*package*/ void delNotify(Transaction tx) {
  
    // reset meta
    meta = null;
    
    // delete children first
    Property[] props = getProperties();
    for (int i=props.length-1;i>=0;i--) {
      props[i].delNotify(tx);
    }
    
    // remember being deleted
    if (tx!=null)
      tx.get(Transaction.PROPERTIES_DELETED).add(this);
      
    // reset parent
    parent = null;
    
    // continue
  }

  /*package*/ void modNotify(Transaction tx) {

    // remember being modified
    if (tx!=null)
      tx.get(Transaction.PROPERTIES_MODIFIED).add(this);
      
  }
  
  /**
   * Propagate changed property
   */
  /*package*/ void propagateChanged(Property changed) {

    // tell it to parent
    if (parent!=null)
      parent.propagateChanged(changed);
  }
  
  /**
   * Propagate added property
   */
  /*package*/ void propagateAdded(Property owner, int pos, Property added) {
    // tell it to parent
    if (parent!=null)
      parent.propagateAdded(owner, pos, added);
  }

  /**
   * Propagate removed property
   */
  /*package*/ void propagateRemoved(Property owner, int pos, Property removed) {

    // tell it to parent
    if (parent!=null)
      parent.propagateRemoved(owner, pos, removed);
      
  }
  
  /**
   * Adds a copy of given property 
   * @param prop the property to add as copy
   * @param pos the position of prop after adding
   * @return the copy of prop that was added
   */
  public Property addCopy(Property prop, int pos) {

    // create a copy of prop
    MetaProperty meta = getMetaProperty();
    
    MetaProperty copyMeta = meta.get(prop.getTag(), true);
    String      copyValue = prop.getValue();

    Property copy = copyMeta.create(copyValue);

    // keep it    
    addProperty(copy, pos);
    
    // link it if applicable
    if (copy instanceof PropertyXRef) try {
      ((PropertyXRef)copy).link();
    } catch (GedcomException e) {
    }
    
    // do it recursively for every child of prop
    Property[] children = prop.getProperties();
    for (int c=0; c<children.length; c++) 
      copy.addCopy(children[c], -1);

    // done
    return copy;
  }
  
  /**
   * Adds a sub-property to this property
   * @param prop new property to add
   */
  public Property addProperty(Property prop) {
    return addProperty(prop, true);
  }

  /**
   * Adds another property to this property
   * @param prop new property to add
   * @param place whether to place the sub-property according to grammar
   */
  public Property addProperty(Property prop, boolean place) {

    // check grammar for placement if applicable
    int pos = -1;
    
    if (place&&getNoOfProperties()>0&&getEntity()!=null) {

      MetaProperty meta = getMetaProperty();
      
      pos = 0;
      int index = meta.getIndex(prop.getTag());
      for (;pos<getNoOfProperties();pos++) {
        if (meta.getIndex(getProperty(pos).getTag())>index)
          break;
      }
    }
    
    // add property
    return addProperty(prop, pos);
    
  }
  
  /**
   * Adds another property to this property
   */
  public Property addProperty(Property child, int pos) {

    // check against meta of child
    if (child.meta!=null && getMetaProperty().get(child.getTag(), false) != child.getMetaProperty())
      throw new IllegalArgumentException("illegal use of property "+child.getTag()+" in "+getPath());

    // position valid?
    if (pos>=0&&pos<children.size())
      children.add(pos, child);
    else {
      children.add(child);
      pos = children.size()-1;
    }

    // propagate addition
    propagateAdded(this, pos, child);
    
    // Done
    return child;
  }
  
  /**
   * Removes a property by looking in the property's properties
   */
  public void delProperty(Property deletee) {

    // find position (throw outofbounds if n/a)
    int pos = 0;
    for (;;pos++) {
      if (children.get(pos)==deletee)
        break;
    }

    // tell about it
    propagateRemoved(this, pos, deletee);
  
    // remove   
    children.remove(pos);
    
  }

  /**
   * Removes a property by position
   */
  public void delProperty(int pos) {

    // range check
    if (pos<0||pos>=children.size())
      throw new IndexOutOfBoundsException();

    // tell about it
    propagateRemoved(this, pos, (Property)children.get(pos));
  
    // remove   
    children.remove(pos);

  }
  
  /**
   * Returns a warning string that describes what happens when this
   * property would be deleted
   * @return warning as <code>String</code>, <code>null</code> when no warning
   */
  public String getDeleteVeto() {
    return null;
  }

  /**
   * Returns the entity this property belongs to - simply looking up
   */
  public Entity getEntity() {
    return parent==null ? null : parent.getEntity();
  }

  /**
   * Returns the gedcom this property belongs to
   */
  public Gedcom getGedcom() {

    // Entity ?
    Entity entity = getEntity();
    if (entity==null) {
      return null;
    }

    // Ask Entity
    return entity.getGedcom();

  }
  
  /**
   * Returns the image which is associated with this property.
   */
  public ImageIcon getImage(boolean checkValid) {
    
    // valid or not ?
    if (!checkValid||isValid()) {
      if (image==null) 
        image = getMetaProperty().getImage(); 
      return image;
    }
    
    // not valid
    if (imageErr==null) 
      imageErr = getMetaProperty().getImage("err"); 
      
    return imageErr;
  }

  /**
   * Calculates the number of properties this property has.
   */
  public int getNoOfProperties() {
    return children.size();
  }

  /**
   * Returns the property this property belongs to
   */
  public Property getParent() {
    return parent;
  }
  
  /**
   * Returns the path to this property
   */
  public TagPath getPath() {

    Stack stack = new Stack();

    // build path start with this
    String tag = getTag();
    if (tag==null)
      throw new IllegalArgumentException("encountered getTag()==null");
    stack.push(tag);
    
    // loop through parents
    Property parent = getParent();
    while (parent!=null) {
      tag = parent.getTag();
      if (tag==null)
        throw new IllegalArgumentException("encountered getTag()==null");
      stack.push(tag);
      parent = parent.getParent();
    }

    // done
    return new TagPath(stack);
    
  }

  /**
   * Returns this property's properties (all children)
   */
  public Property[] getProperties() {
    return toArray(children);
  }
  
  /**
   * Returns this property's properties adhering criteria
   */
  public Property[] getProperties(int criteria) {
    List result = new ArrayList(children.size());
    for (int i=0;i<children.size();i++) {
      Property child = (Property)children.get(i);
      if (!child.is(criteria))
        continue;
      result.add(child);
    }
    return toArray(result);
  }  

  /**
   * Returns this property's properties by tag
   * (only valid children are considered)
   */
  public Property[] getProperties(String tag) {
    return getProperties(tag, QUERY_VALID_TRUE);
  }

  /**
   * Returns this property's properties by tag
   */
  public Property[] getProperties(String tag, int qfilter) {
    
    // safety check
    if (tag.indexOf(':')>0) throw new IllegalArgumentException("Path not allowed");
    
    // loop children
    ArrayList result = new ArrayList();
    for (int c=0;c<getNoOfProperties();c++) {
      Property child = getProperty(c);
      // filter
      if (!child.getTag().equals(tag))
        continue;
      // 20031027 changed from is() to child.is()
      if (!child.is(qfilter))
        continue;
      // hit!        
      result.add(child);  
    }
    
    // not found
    return toArray(result);
  }

  /**
   * Returns this property's properties which are of given type
   */
  public List getProperties(Class type) {
    List props = new ArrayList(10);
    getPropertiesRecursively(props, type);
    return props;
  }
  
  private void getPropertiesRecursively(List props, Class type) {
    for (int c=0;c<getNoOfProperties();c++) {
      Property child = getProperty(c);
      if (type.isAssignableFrom(child.getClass())) {
        props.add(child);
      }
      child.getPropertiesRecursively(props, type);
    }
  }

  /**
   * Returns this property's properties by path
   */
  public Property[] getProperties(TagPath path, int qfilter) {

    // Gather 'em
    List result = new ArrayList(children.size());
    getPropertiesRecursively(path, 0, result, qfilter);

    // done
    return toArray(result);
  }

  private List getPropertiesRecursively(TagPath path, int pos, List fill, int qfilter) {

    // Correct here ?
    if (!path.get(pos).equals(getTag())) return fill;

    // Me the last one?
    if (pos==path.length()-1) {
      
      if (is(qfilter)) 
        fill.add(this);
        
      // .. done
      return fill;
    }

    // Search in properties
    for (int i=0;i<children.size();i++) {
      getProperty(i).getPropertiesRecursively(path,pos+1,fill,qfilter);
    }

    // done
    return fill;
  }
  
  /**
   * Returns a sub-property position
   */
  public int getPropertyPosition(Property prop) {
    for (int i=0;i<children.size();i++) {
      if (children.get(i)==prop)
        return i;
    }
    throw new IllegalArgumentException();
  }

  /**
   * Returns this property's nth property
   * kenmraz: added checks since I was able
   * to get an indexOutOfBounds error when DnDing
   * to the end of list of children.  
   * nmeier: remove check again to force valid
   * index argument - fixed DnD code to provide
   * correct parameter
   */
  public Property getProperty(int n) {
    return (Property)children.get(n);
  }

  /**
   * Returns this property's property by tag
   * (only valid children are considered)
   */
  public Property getProperty(String tag) {
    return getProperty(tag, true);
  }

  /**
   * Returns this property's property by path
   */
  public Property getProperty(String tag, boolean validOnly) {
    // safety check
    if (tag.indexOf(':')>0) throw new IllegalArgumentException("Path not allowed");
    // loop children
    for (int c=0;c<getNoOfProperties();c++) {
      Property child = getProperty(c);
      if (!child.getTag().equals(tag)) continue;
      if (validOnly&&!child.isValid()) continue;
      return child;
    }
    // not found
    return null;
  }

  /**
   * Returns this property's property by path
   */
  public Property getProperty(TagPath path) {
    return getProperty(path, QUERY_VALID_TRUE);
  }
  
  /**
   * Returns this property's property by path
   */
  public Property getProperty(TagPath path, int qfilter) {
    return getPropertyRecursively(path, 0, qfilter);
  }
  
  private Property getPropertyRecursively(TagPath path, int pos, int qfilter) {

    String tag = path.get(pos);
    
    // a '..'?
    if (tag.equals("..")) 
      return parent==null ? null : parent.getPropertyRecursively(path, pos+1, qfilter);

    // Correct here?
    if (!tag.equals(getTag())) 
      return null;

    // test filter
    if (!is(qfilter))
      return null;
    
    // path traversed? (it's me)
    if (pos==path.length()-1) 
      return this;
      
    // follow a link? (this probably should be into PropertyXref.class - override)
    if ((qfilter&QUERY_FOLLOW_LINK)!=0&&this instanceof PropertyXRef) {
      Property p = ((PropertyXRef)this).getReferencedEntity();
      if (p!=null)
        p = p.getPropertyRecursively(path, pos+1, qfilter);
      if (p!=null)
        return p;
    }
    
    // Search for next tag in properties
    for (int i=0;i<getNoOfProperties();i++) {

      Property ith = getProperty(i);
      
      // try recursively
      Property p = ith.getPropertyRecursively(path, pos+1, qfilter);
      if (p!=null)
        return p;

      // FIXME this doesn't allow for advanced selector like 'nth'
      if (ith.getTag().equals(path.get(pos+1))&&(qfilter&QUERY_NO_BACKTRACK)!=0)
        break;
    }
    
    // not found
    return null;
  }

  /**
   * Returns the logical name of the proxy-object which knows this object
   */
  public String getProxy() {
    return "SimpleValue";
  }

  /**
   * Returns the Gedcom-Tag of this property
   */
  public abstract String getTag();

  /**
   * Initializes this poperty giving it a chance to inspect
   * support for tag and value, eventually returning a 
   * different type that is better suited for the SPECIFIC
   * combination.
   */
  /*package*/ Property init(MetaProperty meta, String value) throws GedcomException {
    // remember meta
    this.meta = meta;
    // assuming concrete sub-type handles tag - keep value
    setValue(value);
    // we stay around
    return this;
  }
  
  /**
   * Assertion (this was assert once but that's deprecated with 1.4)
   */
  protected void assume(boolean condition, String explanation) throws GedcomException {
    if (!condition) throw new GedcomException(explanation);
  }

  /**
   * Returns the value of this property as string.
   */
  abstract public String getValue();

  /**
   * Sets this property's value as string.
   */
  public abstract void setValue(String value);

  /**
   * Returns <b>true</b> if this property is valid
   */
  public boolean isValid() {
    return true;
  }

  /**
   * The default toString returns the value of this property
   * NM 19990715 introduced to allow access to a property on a
   *             more abstract level than getValue()
   * NM 20020221 changed to return value only
   * NM sometime changed to return TAG VALUE
   * NM 20040317 changed back to return value only - avoids need for renderer everywhere where a simple text representation is needed
   */
  public String toString() {
    return getValue();
  }
  
  /**
   * Compares this property to another property
   * @return -1 this &lt; property <BR>
   *          0 this = property <BR>
   *          1 this &gt; property
   */
  public int compareTo(Object that) {
    // safety check
    if (!(that instanceof Property)) 
      throw new ClassCastException("compareTo("+that+")");
    // no gedcom available?
    return compare(this.getValue(), ((Property)that).getValue() );
  }
  
  /**
   * Compare to string gedcom language aware
   */
  protected int compare(String s1, String s2) {
    
    // I was pondering the notion of keeping cached CollationKeys
    // for faster recurring comparisons but apparently that's not
    // always faster and the keys have a respectable size themselves
    // which leads me to believe a simple Collator.compare() is
    // the better compromise here (even for sort algs)
    
    // gedcom and its collator available?
    Gedcom ged = getGedcom();
    if (ged!=null)
      return ged.getCollator().compare(s1,s2);
      
    // fallback to simple compare
    return s1.compareTo(s2);
  }
  
  /**
   * Whether this property is transient and therfor shouldn't
   * - act as a template
   * - be saved
   */
  public boolean isTransient() {
    return isTransient;
  }

  /**
   * A read-only attribute that can be honoured by the UI
   */
  public boolean isReadOnly() {
    return false;
  }

  /**
   * Adds default properties to this property
   */
  public final Property addDefaultProperties() {
    
    // only if parent set
    if (getEntity()==null) throw new IllegalArgumentException("entity is null!");
    
    // loop
    MetaProperty[] subs = getSubMetaProperties(MetaProperty.FILTER_DEFAULT); 
    for (int s=0; s<subs.length; s++) {
      if (getProperty(subs[s].getTag())==null)
        addProperty(subs[s].create(EMPTY_STRING)).addDefaultProperties();
    }

    // done    
    return this;
  }
  
  /**
   * Resolve meta property
   */
  public MetaProperty getMetaProperty() {
    if (meta==null)
      meta = MetaProperty.get(getPath());    
    return meta;
  }

  /**
   * Resolve meta properties
   * @param filter one/many of QUERY_ALL, QUERY_VALID_TRUE, QUERY_SYSTEM_FALSE, QUERY_FOLLOW_LINK
   */
  public MetaProperty[] getSubMetaProperties(int filter) {
    return getMetaProperty().getSubs(filter);
  }

  /**
   * Convert collection of properties into array
   */
  protected static Property[] toArray(Collection ps) {
    return (Property[])ps.toArray(new Property[ps.size()]);
  }
  
  /**
   * test for given criteria
   */
  private final boolean is(int criteria) {
    
    if ((criteria&QUERY_VALID_TRUE)!=0&&!isValid())
      return false;
      
    if ((criteria&QUERY_NO_LINK)!=0&&this instanceof PropertyXRef)
      return false;
    
    return true;
  }

  /**
   * Accessor - private
   */
  public boolean isPrivate() {
    return isPrivate;
  }
  
  /**
   * Accessor - secret is private and unknown password
   */
  public boolean isSecret() {
    return isPrivate && getGedcom().getPassword()==Gedcom.PASSWORD_UNKNOWN;
  }
  
  /**
   * Accessor - private
   */
  public void setPrivate(boolean set, boolean recursively) {
    
    // change state
    if (recursively) {
      for (int c=0;c<getNoOfProperties();c++) {
        Property child = getProperty(c);
        child.setPrivate(set, recursively);
      }
    }
    isPrivate = set;
    
    // bookkeeping
    propagateChanged(this);
    
    // done
  }

  /**
   * Resolves end-user information about this property - by
   * default whatever is in the language resource files
   * @return info or null
   */
  public String getInfo() {
    return getMetaProperty().getInfo();
  }
  
} //Property

