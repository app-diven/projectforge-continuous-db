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

package org.projectforge.continuousdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.projectforge.continuousdb.demo.entities.GroupTaskAccessDO;
import org.projectforge.continuousdb.demo.entities.TaskDO;
import org.projectforge.continuousdb.demo.entities.UserDO;
import org.projectforge.continuousdb.demo.entities.UserRightDO;

public class TableAttributeTest
{
  @Test
  public void createTableAttributes()
  {
    TableAttribute attr;
    attr = assertAttribute(TaskDO.class, "id", "pk", TableAttributeType.INT, true, false);
    attr = assertAttribute(TaskDO.class, "created", "created", TableAttributeType.TIMESTAMP, false, true);
    attr = assertAttribute(TaskDO.class, "lastUpdate", "last_update", TableAttributeType.TIMESTAMP, false, true);
    attr = assertAttribute(UserDO.class, "locale", "locale", TableAttributeType.LOCALE, false, true);
    attr = assertAttribute(TaskDO.class, "duration", "duration", TableAttributeType.DECIMAL, false, true);
    assertEquals(2, attr.getScale());
    assertEquals(10, attr.getPrecision());
    attr = assertAttribute(TaskDO.class, "title", "title", TableAttributeType.VARCHAR, false, false);
    assertEquals(100, attr.getLength());
    attr = assertAttribute(TaskDO.class, "description", "description", TableAttributeType.VARCHAR, false, true);
    assertEquals(1000, attr.getLength());
    attr = assertAttribute(TaskDO.class, "maxHours", "max_hours", TableAttributeType.INT, false, true);
    attr = assertAttribute(TaskDO.class, "parentTask", "parent_task_id", TableAttributeType.INT, false, true);
    assertEquals("T_TASK", attr.getForeignTable());
    assertEquals("pk", attr.getForeignAttribute());

    attr = assertAttribute(UserDO.class, "deleted", "deleted", TableAttributeType.BOOLEAN, false, false);
    attr = assertAttribute(TaskDO.class, "responsibleUser", "responsible_user_id", TableAttributeType.INT, false, true);
    assertEquals("T_USER", attr.getForeignTable());
    assertEquals("pk", attr.getForeignAttribute());
    attr = assertAttribute(UserRightDO.class, "value", "value", TableAttributeType.VARCHAR, false, true);
    assertEquals(40, attr.getLength());

    attr = assertAttribute(UserDO.class, "loginFailures", "loginFailures", TableAttributeType.INT, false, false);
    attr = assertAttribute(GroupTaskAccessDO.class, "recursive", "recursive", TableAttributeType.BOOLEAN, false, false);
  }

  private TableAttribute assertAttribute(final Class< ? > cls, final String property, final String name, final TableAttributeType type,
      final boolean primaryKey, final boolean nullable)
  {
    final TableAttribute attr = new TableAttribute(cls, property);
    assertEquals("Different column name expected.", name, attr.getName());
    assertEquals("Different nullable value expected.", nullable, attr.isNullable());
    if (primaryKey == true) {
      assertTrue(attr.isPrimaryKey());
      assertFalse("Primary key should be not nullable.", attr.isNullable());
    } else {
      assertFalse(attr.isPrimaryKey());
    }
    assertEquals("Different column type expected.", type, attr.getType());
    return attr;
  }
}
