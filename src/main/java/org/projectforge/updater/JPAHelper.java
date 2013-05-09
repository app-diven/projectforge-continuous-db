/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.updater;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.projectforge.common.BeanHelper;

/**
 * For manipulating the database (patching data etc.)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class JPAHelper
{
  public static final String getIdProperty(final Class< ? > clazz)
  {
    final List<Field> fields = getAllDeclaredFields(clazz);
    for (final Field field : fields) {
      final Id id = getIdAnnotation(clazz, field.getName());
      if (id != null) {
        return field.getName();
      }
    }
    return null;
  }

  /**
   * Tries to find the Column definition from the annotated getter, setter or field.
   * @param clazz
   * @param property
   * @return
   */
  public static Column getColumnAnnotation(final Class< ? > clazz, final String property)
  {
    Column column = getColumnAnnotation(BeanHelper.determineGetter(clazz, property));
    if (column == null) {
      column = getColumnAnnotation(BeanHelper.determineSetter(clazz, property));
      if (column == null) {
        final Field field = getField(clazz, property);
        if (field != null) {
          return field.getAnnotation(Column.class);
        }
      }
    }
    return column;
  }

  /**
   * Tries to find the Id definition from the annotated getter, setter or field.
   * @param clazz
   * @param property
   * @return
   */
  public static Id getIdAnnotation(final Class< ? > clazz, final String property)
  {
    Id id = getIdAnnotation(BeanHelper.determineGetter(clazz, property));
    if (id == null) {
      id = getIdAnnotation(BeanHelper.determineSetter(clazz, property));
      if (id == null) {
        final Field field = getField(clazz, property);
        if (field != null) {
          return field.getAnnotation(Id.class);
        }
      }
    }
    return id;
  }

  /**
   * Tries to find the JoinColumn definition from the annotated getter, setter or field.
   * @param clazz
   * @param property
   * @return
   */
  public static JoinColumn getJoinColumnAnnotation(final Class< ? > clazz, final String property)
  {
    JoinColumn joinColumn = getJoinColumnAnnotation(BeanHelper.determineGetter(clazz, property));
    if (joinColumn == null) {
      joinColumn = getJoinColumnAnnotation(BeanHelper.determineSetter(clazz, property));
      if (joinColumn == null) {
        final Field field = getField(clazz, property);
        if (field != null) {
          return field.getAnnotation(JoinColumn.class);
        }
      }
    }
    return joinColumn;
  }

  public static boolean isPersistencyAnnotationPresent(final AccessibleObject obj)
  {
    List<Annotation> list = getPersistencyAnnotations(obj);
    return list != null && list.size() > 0;
  }

  public static List<Annotation> getPersistencyAnnotations(final AccessibleObject object)
  {
    if (object == null) {
      return null;
    }
    List<Annotation> list = null;
    list = handlePersistencyAnnotation(list, object, Basic.class);
    list = handlePersistencyAnnotation(list, object, Column.class);
    list = handlePersistencyAnnotation(list, object, GeneratedValue.class);
    list = handlePersistencyAnnotation(list, object, Id.class);
    list = handlePersistencyAnnotation(list, object, JoinColumn.class);
    list = handlePersistencyAnnotation(list, object, JoinTable.class);
    list = handlePersistencyAnnotation(list, object, Lob.class);
    list = handlePersistencyAnnotation(list, object, ManyToMany.class);
    list = handlePersistencyAnnotation(list, object, ManyToOne.class);
    list = handlePersistencyAnnotation(list, object, OneToMany.class);
    return list;
  }

  private static List<Annotation> handlePersistencyAnnotation(List<Annotation> list, final AccessibleObject object,
      final Class< ? extends Annotation> annotation)
      {
    if (object.isAnnotationPresent(annotation) == false) {
      return list;
    }
    if (list == null) {
      list = new LinkedList<Annotation>();
    }
    list.add(object.getAnnotation(annotation));
    return list;
      }

  private static Column getColumnAnnotation(final Method method)
  {
    if (method == null) {
      return null;
    }
    return method.getAnnotation(Column.class);
  }

  private static JoinColumn getJoinColumnAnnotation(final Method method)
  {
    if (method == null) {
      return null;
    }
    return method.getAnnotation(JoinColumn.class);
  }

  private static Id getIdAnnotation(final Method method)
  {
    if (method == null) {
      return null;
    }
    return method.getAnnotation(Id.class);
  }

  private static Field getField(final Class< ? > clazz, final String fieldName)
  {
    Field field;
    try {
      field = clazz.getDeclaredField(fieldName);
      if (field != null) {
        return field;
      }
    } catch (final SecurityException ex) {
      // OK, nothing to do.
    } catch (final NoSuchFieldException ex) {
      // OK, nothing to do.
    }
    if (clazz.getSuperclass() != null) {
      return getField(clazz.getSuperclass(), fieldName);
    }
    return null;
  }

  private static List<Field> getAllDeclaredFields(final Class< ? > clazz)
  {
    return getAllDeclaredFields(new ArrayList<Field>(), clazz);
  }

  private static List<Field> getAllDeclaredFields(final List<Field> list, final Class< ? > clazz)
  {
    final Field[] fields = clazz.getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    for (final Field field : fields) {
      list.add(field);
    }
    if (clazz.getSuperclass() != null) {
      getAllDeclaredFields(list, clazz.getSuperclass());
    }
    return list;
  }
}
