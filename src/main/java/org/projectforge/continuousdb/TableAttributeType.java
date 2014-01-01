/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.continuousdb;

/**
 * Represents one attribute type.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum TableAttributeType
{
  INT, LONG, SHORT, VARCHAR, CHAR, LOCALE, TIMESTAMP, DATE, BOOLEAN, DECIMAL, LIST, SET, BINARY;

  public boolean isIn(final TableAttributeType... types)
  {
    for (final TableAttributeType type : types) {
      if (type == this) {
        return true;
      }
    }
    return false;
  }
}
