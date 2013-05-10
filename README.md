projectforge-continuous-db
==========================

Use this package for continuous delivery of your software.

Any initial setup of the data-base as well as update-scripts are very easy to manage programmatically. The JPA annotations are supported,
therefore you may set-up a complete data-base schema with only a few lines of code.

## Example: Initial setup of a data-base

The following example will create the whole data-base schema for the given entities. Please refer ```DemoMain.java``` for a first example.

```java
Class< ? >[] entities = new Class< ? >[] {
  UserDO.class,
  TaskDO.class, GroupDO.class, TaskDO.class, GroupTaskAccessDO.class,
  AccessEntryDO.class
};

if (databaseUpdateDao.doesEntitiesExist(entities) == false) {
  // At least one table of the given entities doesn't exist. Create the missing tables:
  SchemaGenerator schemaGenerator = configuration.createSchemaGenerator().add(entities);
  schemaGenerator.createSchema();
  databaseUpdateDao.createMissingIndices(); // Create missing indices.
}
```

Please note: foreign-keys, one-to-many, many-to-one and many-to-many relations are supported as well as different column types. You may extend
this module very easy for support of more JPA annotations.

## Example: Update script

You may add columns to a table within your new version:

```java
if (databaseUpdateDao.doesTableAttributesExist(AddressDO.class, "birthday", "address") == false) {
  // One or both attributes don't yet exist, alter table to add the missing columns now:
  databaseUpdateDao.addTableAttributes(Address2DO.class, "birthday", "address");
  // Works also, if one of both attributes does already exist.
}
```

## Manual creation without JPA annotations
You may create and update the data-base schema without JPA annotations:
```java
Table table = new Table("t_address");
if (databaseUpdateDao.doesExist(table) == false) {
  table.addAttribute(new TableAttribute("name", TableAttributeType.VARCHAR, 255))
       .addAttribute(new TableAttribute("birthday", TableAttributeType.DATE));
  databaseUpdateDao.createTable(table);
}
```

## Advantage in comparison with other tools
You may organize your data-base inital and update scripts programmatically. Therefore it's very easy to do further migration
modifications during your update (e. g. merge columns etc.).  
It's easy to handle multiple data-base dialects (you write only code once which fits all data-base dialects). The data-base dialect of the destination system is used.

## UpdateEntries
For large projects with sub-modules (such as ProjectForge itself) it's recommended to organize setups by using the UpdateEntry class. Please refer http://www.projectforge.org/pf-en/Convenientupdates for seeing ProjectForge in action. The administration user is able to update the system by simply clicking the versioned update entries.
They have full control over the update process, no magic by automatical schema update of Hibernate etc.

## Restrictions
* Currently PostgreSQL and HSQLDB are supported. Please refer ```DatabaseSupport.java``` for adding new data-base dialects (this should be very easy).
* Currently only annotations of getter methods are supported (but it should be very easy to support also field annotations).
* The main JPA annotations are supported. For Hibernate specific annotations such as Type please refer the hook class ```TableAttributeHookImpl``` of the hibernate package.

## Developers are welcome!
Feel free to fork and we appreciate any pull requests.