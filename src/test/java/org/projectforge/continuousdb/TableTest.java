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
import junit.framework.Assert;

import org.junit.Test;
import org.projectforge.continuousdb.demo.entities.GroupTaskAccessDO;
import org.projectforge.continuousdb.demo.entities.TaskDO;
import org.projectforge.continuousdb.demo.entities.UserDO;
import org.projectforge.continuousdb.demo.entities.UserRightDO;

public class TableTest
{
  @Test
  public void createTables()
  {
    assertEquals("T_USER", new Table(UserDO.class).getName());
    assertEquals("T_TASK", new Table(TaskDO.class).getName());
    assertEquals("T_GROUP_TASK_ACCESS", new Table(GroupTaskAccessDO.class).getName());

    final Table table = new Table(TaskDO.class);
    table.addAttributes("title",  "maxHours", "responsibleUser");
    assertAttribute(table.getAttributes().get(0), "title");
    assertAttribute(table.getAttributes().get(1), "max_hours");
    assertAttribute(table.getAttributes().get(2), "responsible_user_id");

    assertAttribute(table.getAttributeByProperty("responsibleUser"), "responsible_user_id");
  }

  @Test
  public void autoAddAttributes()
  {
    Table table = new Table(UserDO.class);
    table.autoAddAttributes();
    assertAttribute(table, "username");
    assertAttribute(table, "created");

    table = new Table(UserRightDO.class);
    table.autoAddAttributes();
    assertAttribute(table, "right_id");
  }

  private void assertAttribute(final Table table, final String name)
  {
    for (final TableAttribute attr : table.getAttributes()) {
      if (attr.getName().equals(name) == true) {
        return;
      }
    }
    Assert.fail("Attribute '" + name + "' not found.");
  }

  private void assertAttribute(final TableAttribute attribute, final String name)
  {
    assertEquals(name, attribute.getName());
  }
}
