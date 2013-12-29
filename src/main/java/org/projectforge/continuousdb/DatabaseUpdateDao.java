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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.UniqueConstraint;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.common.StringHelper;

/**
 * For manipulating the database (patching data etc.)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatabaseUpdateDao
{
  private static final org.projectforge.common.Logger log = org.projectforge.common.Logger.getLogger(DatabaseUpdateDao.class);

  private final UpdaterConfiguration configuration;

  public DatabaseUpdateDao(final UpdaterConfiguration configuration)
  {
    this.configuration = configuration;
  }

  public DatabaseDialect getDialect()
  {
    return this.configuration.getDialect();
  }

  private DatabaseSupport getDatabaseSupport()
  {
    return configuration.getDatabaseSupport();
  }

  private DatabaseExecutor getDatabaseExecutor()
  {
    return configuration.getDatabaseExecutor();
  }

  protected DataSource getDataSource()
  {
    return configuration.getDatabaseExecutor().getDataSource();
  }

  /**
   * Does nothing at default. Override this method for checking the access of the user, e. g. only admin user's should be able to manipulate
   * the database.
   * @param writeaccess
   */
  protected void accessCheck(final boolean writeaccess)
  {
    return;
  }

  public boolean doesTableExist(final String table)
  {
    accessCheck(false);
    return internalDoesTableExist(table);
  }

  public boolean doEntitiesExist(final Class< ? >... entities)
  {
    accessCheck(false);
    for (final Class< ? > entity : entities) {
      if (internalDoesTableExist(new Table(entity).getName()) == false) {
        return false;
      }
    }
    return true;
  }

  public boolean doExist(final Table... tables)
  {
    accessCheck(false);
    for (final Table table : tables) {
      if (internalDoesTableExist(table.getName()) == false) {
        return false;
      }
    }
    return true;
  }

  /**
   * Without check access.
   * @param table
   * @return
   */
  public boolean internalDoesTableExist(final String table)
  {
    /*
     * try { final ResultSet resultSet = dataSource.getConnection().getMetaData().getTables(CATALOG, SCHEMA_PATTERN, table, new String[] {
     * TABLE_TYPE}); return resultSet.next(); } catch (final SQLException ex) { log.error(ex.getMessage(), ex); throw new
     * InternalErrorException(ex.getMessage()); }
     */
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      jdbc.queryForInt("SELECT COUNT(*) FROM " + table);
    } catch (final Exception ex) {
      return false;
    }
    return true;
  }

  public boolean doesTableAttributeExist(final String table, final String attribute)
  {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      jdbc.queryForInt("SELECT COUNT(" + attribute + ") FROM " + table);
    } catch (final Exception ex) {
      return false;
    }
    return true;
  }

  /**
   * 
   * @param entityClass
   * @param properties
   * @return false if at least one property of the given entity doesn't exist in the database, otherwise true.
   */
  public boolean doTableAttributesExist(final Class< ? > entityClass, final String... properties)
  {
    accessCheck(false);
    final Table table = new Table(entityClass);
    return doTableAttributesExist(table, properties);
  }

  /**
   * @param table
   * @param properties
   * @return false if at least one property of the given table doesn't exist in the database, otherwise true.
   */
  public boolean doTableAttributesExist(final Table table, final String... properties)
  {
    accessCheck(false);
    for (final String property : properties) {
      final TableAttribute attr = TableAttribute.createTableAttribute(table.getEntityClass(), property);
      if (attr == null) {
        // Transient or getter method not found.
        return false;
      }
      if (doesTableAttributeExist(table.getName(), attr.getName()) == false) {
        return false;
      }
    }
    return true;
  }

  public boolean isTableEmpty(final String table)
  {
    accessCheck(false);
    return internalIsTableEmpty(table);
  }

  public boolean internalIsTableEmpty(final String table)
  {
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      return jdbc.queryForInt("SELECT COUNT(*) FROM " + table) == 0;
    } catch (final Exception ex) {
      return false;
    }
  }

  /**
   * @param table
   * @return true, if the table is successfully dropped or does not exist.
   */
  public boolean dropTable(final String table)
  {
    accessCheck(true);
    if (doesTableExist(table) == false) {
      // Table is already dropped or does not exist.
      return true;
    }
    if (isTableEmpty(table) == false) {
      // Table is not empty.
      log.warn("Could not drop table '" + table + "' because the table is not empty.");
      return false;
    }
    execute("DROP TABLE " + table);
    return true;
  }

  /**
   * @param table
   * @param attribute
   * @return
   */
  public boolean dropTableAttribute(final String table, final String attribute)
  {
    accessCheck(true);
    execute("ALTER TABLE " + table + " DROP COLUMN " + attribute);
    return true;
  }

  /**
   * @param table
   * @param attribute
   * @param length
   * @return
   */
  public boolean alterTableColumnVarCharLength(final String table, final String attribute, final int length)
  {
    accessCheck(true);
    execute(getDatabaseSupport().alterTableColumnVarCharLength(table, attribute, length), false);
    return true;
  }

  public void buildCreateTableStatement(final StringBuffer buf, final Table table)
  {
    buf.append("CREATE TABLE " + table.getName() + " (\n");
    boolean first = true;
    for (final TableAttribute attr : table.getAttributes()) {
      if (attr.getType().isIn(TableAttributeType.LIST, TableAttributeType.SET) == true) {
        // Nothing to be done here.
        continue;
      }
      if (first == true) {
        first = false;
      } else {
        buf.append(",\n");
      }
      buf.append("  ");
      buildAttribute(buf, attr);
    }
    final TableAttribute primaryKey = table.getPrimaryKey();
    if (primaryKey != null) {
      buf.append(getDatabaseSupport().getPrimaryKeyTableSuffix(primaryKey));
    }
    // Create foreign keys if exist
    for (final TableAttribute attr : table.getAttributes()) {
      if (StringUtils.isNotEmpty(attr.getForeignTable()) == true) {
        // foreign key (user_fk) references t_pf_user(pk)
        buf.append(",\n  FOREIGN KEY (").append(attr.getName()).append(") REFERENCES ").append(attr.getForeignTable()).append("(")
        .append(attr.getForeignAttribute()).append(")");
      }
    }
    final UniqueConstraint[] uniqueConstraints = table.getUniqueConstraints();
    if (uniqueConstraints != null && uniqueConstraints.length > 0) {
      for (final UniqueConstraint uniqueConstraint : uniqueConstraints) {
        final String[] columnNames = uniqueConstraint.columnNames();
        if (columnNames.length > 0) {
          buf.append(",\n  UNIQUE (");
          String separator = "";
          for (final String columnName : columnNames) {
            buf.append(separator).append(columnName);
            separator = ",";
          }
          buf.append(")");
        }
      }
    }
    for (final TableAttribute attr : table.getAttributes()) {
      if (attr.isUnique() == false) {
        continue;
      }
      buf.append(",\n  UNIQUE (").append(attr.getName()).append(")");
    }
    buf.append("\n);\n");
  }

  private void buildAttribute(final StringBuffer buf, final TableAttribute attr)
  {
    buf.append(attr.getName()).append(" ");
    final Column columnAnnotation = attr.getAnnotation(Column.class);
    if (columnAnnotation != null && StringUtils.isNotEmpty(columnAnnotation.columnDefinition()) == true) {
      buf.append(columnAnnotation.columnDefinition());
    } else {
      buf.append(getDatabaseSupport().getType(attr));
    }
    boolean primaryKeyDefinition = false; // For avoiding double 'not null' definition.
    if (attr.isPrimaryKey() == true) {
      final String suffix = getDatabaseSupport().getPrimaryKeyAttributeSuffix(attr);
      if (StringUtils.isNotEmpty(suffix) == true) {
        buf.append(suffix);
        primaryKeyDefinition = true;
      }
    }
    if (primaryKeyDefinition == false) {
      getDatabaseSupport().addDefaultAndNotNull(buf, attr);
    }
    // if (attr.isNullable() == false) {
    // buf.append(" NOT NULL");
    // }
    // if (StringUtils.isNotBlank(attr.getDefaultValue()) == true) {
    // buf.append(" DEFAULT(").append(attr.getDefaultValue()).append(")");
    // }
  }

  public void buildForeignKeyConstraint(final StringBuffer buf, final String table, final TableAttribute attr)
  {
    buf.append("ALTER TABLE ").append(table).append(" ADD CONSTRAINT ").append(table).append("_").append(attr.getName())
    .append(" FOREIGN KEY (").append(attr.getName()).append(") REFERENCES ").append(attr.getForeignTable()).append("(")
    .append(attr.getForeignAttribute()).append(");\n");
  }

  public boolean createTable(final Table table)
  {
    accessCheck(true);
    if (doExist(table) == true) {
      log.info("Table '" + table.getName() + "' does already exist.");
      return false;
    }
    final StringBuffer buf = new StringBuffer();
    buildCreateTableStatement(buf, table);
    execute(buf.toString());
    return true;
  }

  public boolean createSequence(final String name, final boolean ignoreErrors)
  {
    accessCheck(true);
    final String sql = getDatabaseSupport().createSequence(name);
    if (sql != null) {
      execute(sql, ignoreErrors);
    }
    return true;
  }

  public void buildAddTableAttributesStatement(final StringBuffer buf, final String table, final TableAttribute... attributes)
  {
    for (final TableAttribute attr : attributes) {
      if (doesTableAttributeExist(table, attr.getName()) == true) {
        buf.append("-- Does already exist: ");
      }
      buf.append("ALTER TABLE ").append(table).append(" ADD COLUMN ");
      buildAttribute(buf, attr);
      buf.append(";\n");
    }
    for (final TableAttribute attr : attributes) {
      if (attr.getForeignTable() != null) {
        if (doesTableAttributeExist(table, attr.getName()) == true) {
          buf.append("-- Column does already exist: ");
        }
        buildForeignKeyConstraint(buf, table, attr);
      }
    }
  }

  public void buildAddTableAttributesStatement(final StringBuffer buf, final String table, final Collection<TableAttribute> attributes)
  {
    buildAddTableAttributesStatement(buf, table, attributes.toArray(new TableAttribute[0]));
  }

  /**
   * @param entityClass
   * @param attributeNames Property names of the attributes to create.
   * @return
   */
  public boolean addTableAttributes(final Class< ? > entityClass, final String... attributeNames)
  {
    return addTableAttributes(new Table(entityClass), attributeNames);
  }

  /**
   * @param table
   * @param attributeNames Property names of the attributes to create.
   * @return
   */
  public boolean addTableAttributes(final Table table, final String... attributeNames)
  {

    final ArrayList<TableAttribute> list = new ArrayList<TableAttribute>();
    for (int i = 0; i < attributeNames.length; i++) {
      final TableAttribute attr = TableAttribute.createTableAttribute(table.getEntityClass(), attributeNames[i]);
      if (attr == null) {
        log.debug("Property '" + table.getName() + "." + attributeNames[i] + "' is transient.");
        continue;
      }
      list.add(attr);
    }
    final TableAttribute[] attributes = list.toArray(new TableAttribute[0]);
    return addTableAttributes(table, attributes);
  }

  public boolean addTableAttributes(final String table, final TableAttribute... attributes)
  {
    final StringBuffer buf = new StringBuffer();
    buildAddTableAttributesStatement(buf, table, attributes);
    execute(buf.toString());
    return true;
  }

  public boolean addTableAttributes(final Table table, final TableAttribute... attributes)
  {
    return addTableAttributes(table.getName(), attributes);
  }

  public boolean addTableAttributes(final String table, final Collection<TableAttribute> attributes)
  {
    final StringBuffer buf = new StringBuffer();
    buildAddTableAttributesStatement(buf, table, attributes);
    execute(buf.toString());
    return true;
  }

  public boolean addTableAttributes(final Table table, final Collection<TableAttribute> attributes)
  {
    return addTableAttributes(table.getName(), attributes);
  }

  public void buildAddUniqueConstraintStatement(final StringBuffer buf, final String table, final String constraintName,
      final String... attributes)
  {
    buf.append("ALTER TABLE ").append(table).append(" ADD CONSTRAINT ").append(constraintName).append(" UNIQUE (");
    buf.append(StringHelper.listToString(", ", attributes));
    buf.append(");\n");
  }

  public boolean renameTableAttribute(final String table, final String oldName, final String newName)
  {
    final String alterStatement = getDatabaseSupport().renameAttribute(table, oldName, newName);
    execute(alterStatement);
    return true;
  }

  public boolean addUniqueConstraint(final String table, final String constraintName, final String... attributes)
  {
    accessCheck(true);
    final StringBuffer buf = new StringBuffer();
    buildAddUniqueConstraintStatement(buf, table, constraintName, attributes);
    execute(buf.toString());
    return true;
  }

  public boolean dropAndRecreateAllUniqueConstraints(final Class< ? > entity)
  {
    accessCheck(true);
    final Table table = new Table(entity).autoAddAttributes();
    final String[] uniqueConstraintNames = getAllUniqueConstraintNames(table.getName());
    if (uniqueConstraintNames != null) {
      for (final String uniqueConstraintName : uniqueConstraintNames) {
        execute("ALTER TABLE " + table.getName() + " DROP CONSTRAINT " + uniqueConstraintName);
      }
    } else {
      log.info("No unique constraints found for table '" + table.getName() + "'.");
    }
    final UniqueConstraint[] uniqueConstraints = table.getUniqueConstraints();
    final List<String> existingConstraintNames = new LinkedList<String>();
    if (uniqueConstraints != null && uniqueConstraints.length > 0) {
      for (final UniqueConstraint uniqueConstraint : uniqueConstraints) {
        final String[] columnNames = uniqueConstraint.columnNames();
        if (columnNames.length > 0) {
          final String constraintName = createUniqueConstraintName(table.getName(), columnNames,
              existingConstraintNames.toArray(new String[0]));
          addUniqueConstraint(table.getName(), constraintName, columnNames);
          existingConstraintNames.add(constraintName);
        }
      }
    }
    for (final TableAttribute attr : table.getAttributes()) {
      if (attr.isUnique() == false) {
        continue;
      }
      final String[] columnNames = new String[1];
      columnNames[0] = attr.getName();
      final String constraintName = createUniqueConstraintName(table.getName(), columnNames, existingConstraintNames.toArray(new String[0]));
      addUniqueConstraint(table.getName(), constraintName, columnNames);
      existingConstraintNames.add(constraintName);
    }
    return true;
  }

  /**
   * Max length is 30 (may-be for Oracle compatibility).
   * @param table
   * @param columnNames
   * @param existingConstraintNames
   * @return The generated constraint name different to the given names.
   */
  public String createUniqueConstraintName(final String table, final String[] columnNames, final String[] existingConstraintNames)
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(StringUtils.left(table, 15)).append("_uq_").append(StringUtils.left(columnNames[0], 8));
    final String prefix = sb.toString().toLowerCase();
    for (int i = 1; i < 1000; i++) {
      final String name = prefix + i;
      if (existingConstraintNames == null || existingConstraintNames.length == 0) {
        return name;
      }
      boolean exists = false;
      for (final String existingName : existingConstraintNames) {
        if (existingName != null && existingName.equals(name) == true) {
          exists = true;
          break;
        }
      }
      if (exists == false) {
        return name;
      }
    }
    final String message = "Oups, can't find any free constraint name! This must be a bug or a database out of control! Tryiing to find a name '"
        + prefix
        + "[0-999]' for table '"
        + table
        + "'.";
    log.error(message);
    throw new UnsupportedOperationException(message);
  }

  public String[] getAllUniqueConstraintNames(final String table)
  {
    final String uniqueConstraintNamesSql = getDatabaseSupport().getQueryForAllUniqueConstraintNames();
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    final List<DatabaseResultRow> result = jdbc.query(uniqueConstraintNamesSql, table.toLowerCase());
    if (result == null || result.size() == 0) {
      return null;
    }
    final String[] names = new String[result.size()];
    int i = 0;
    for (final DatabaseResultRow row : result) {
      final List<DatabaseResultRowEntry> entries = row.getEntries();
      final int numberOfEntries = entries != null ? entries.size() : 0;
      if (numberOfEntries != 1) {
        log.error("Error while getting unique constraint name for table '"
            + table
            + "'. Eeach result entry of the query should be one but is: "
            + numberOfEntries);
      }
      if (numberOfEntries > 0) {
        final DatabaseResultRowEntry entry = entries.get(0);
        names[i++] = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
      } else {
        names[i++] = null;
      }
    }
    return names;
  }

  /**
   * Creates missing database indices of tables starting with 't_'.
   * @return Number of successful created database indices.
   */
  public int createMissingIndices()
  {
    accessCheck(true);
    log.info("createMissingIndices called.");
    int counter = 0;
    // For user / time period search:
    try {
      final ResultSet reference = getDataSource().getConnection().getMetaData().getCrossReference(null, null, null, null, null, null);
      while (reference.next()) {
        final String fkTable = reference.getString("FKTABLE_NAME");
        final String fkCol = reference.getString("FKCOLUMN_NAME");
        if (fkTable.startsWith("t_") == true) {
          // Table of ProjectForge
          if (createIndex("idx_fk_" + fkTable + "_" + fkCol, fkTable, fkCol) == true) {
            counter++;
          }
        }
      }
    } catch (final SQLException ex) {
      log.error(ex.getMessage(), ex);
    }
    return counter;
  }

  /**
   * You may implement this method to write update entries e. g. in a database table.
   * @param updateEntry
   */
  protected void writeUpdateEntryLog(final UpdateEntry updateEntry)
  {
    // TODO
    // final Table table = new Table(DatabaseUpdateDO.class);
    // if (databaseUpdateDao.doesTableExist(table.getName()) == true) {
    // databaseUpdateDao.insertInto(table.getName(), new String[] { "update_date", "region_id", "version", "execution_result",
    // "executed_by_user_fk", "description"},
    // new Object[] { new Date(), updateEntry.getRegionId(), String.valueOf(updateEntry.getVersion()), updateEntry.getRunningResult(),
    // PFUserContext.getUserId(), updateEntry.getDescription()});
    // } else {
    // log.info("database table '" + table.getName() + "' doesn't (yet) exist. Can't register update (OK).");
    // }
  }

  public void insertInto(final String table, final String[] columns, final Object[] values)
  {
    final StringBuffer buf = new StringBuffer();
    buf.append("insert into ").append(table).append(" (").append(StringHelper.listToString(",", columns)).append(") values (");
    boolean first = true;
    for (int i = 0; i < values.length; i++) {
      first = StringHelper.append(buf, first, "?", ",");
    }
    buf.append(")");
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    final String sql = buf.toString();
    log.info(sql + "; values = " + StringHelper.listToString(", ", values));
    jdbc.update(sql, values);
  }

  /**
   * @param regionId
   * @param version
   * @return true, if any entry for the given regionId and version is found in the database table t_database_update.
   */
  public boolean isVersionUpdated(final String regionId, final String version)
  {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    final int result = jdbc.queryForInt("select count(*) from t_database_update where region_id=? and version=?", regionId, version);
    return result > 0;
  }

  /**
   * Creates the given database index if not already exists.
   * @param name
   * @param table
   * @param attributes
   * @return true, if the index was created, false if an error has occured or the index already exists.
   */
  public boolean createIndex(final String name, final String table, final String attributes)
  {
    accessCheck(true);
    try {
      final String jdbcString = "CREATE INDEX " + name + " ON " + table + "(" + attributes + ");";
      execute(jdbcString, false);
      log.info(jdbcString);
      return true;
    } catch (final Throwable ex) {
      // Index does already exist (or an error has occurred).
      return false;
    }
  }

  /**
   * @param name
   * @param attributes
   * @return true, if the index was dropped, false if an error has occured or the index does not exist.
   */
  public boolean dropIndex(final String name)
  {
    accessCheck(true);
    try {
      execute("DROP INDEX " + name);
      return true;
    } catch (final Throwable ex) {
      // Index does already exist (or an error has occurred).
      return false;
    }
  }

  /**
   * @param jdbcString
   * @see #execute(String, boolean)
   */
  public void execute(final String jdbcString)
  {
    execute(jdbcString, true);
  }

  /**
   * Executes the given String
   * @param jdbcString
   * @param ignoreErrors If true (default) then errors will be caught and logged.
   * @return true if no error occurred (no exception was caught), otherwise false.
   */
  public void execute(final String jdbcString, final boolean ignoreErrors)
  {
    accessCheck(true);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    jdbc.execute(jdbcString, ignoreErrors);
    log.info(jdbcString);
  }

  public int queryForInt(final String jdbcQuery)
  {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    log.info(jdbcQuery);
    return jdbc.queryForInt(jdbcQuery);
  }

  public List<DatabaseResultRow> query(final String sql, final Object... args)
  {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    log.info(sql);
    return jdbc.query(sql, args);
  }

  public int update(final String sql, final Object... args)
  {
    accessCheck(false);
    final DatabaseExecutor jdbc = getDatabaseExecutor();
    log.info(sql);
    return jdbc.update(sql, args);
  }

  /**
   * Will be called on shutdown.
   * @see DatabaseSupport#getShutdownDatabaseStatement()
   */
  public void shutdownDatabase()
  {
    final String statement = getDatabaseSupport().getShutdownDatabaseStatement();
    if (statement == null) {
      return;
    }
    log.info("Executing database shutdown statement: " + statement);
    execute(statement);
  }
}
