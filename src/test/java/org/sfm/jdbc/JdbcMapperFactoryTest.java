package org.sfm.jdbc;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.junit.Test;
import org.sfm.beans.DbFinalObject;
import org.sfm.beans.DbObject;
import org.sfm.map.FieldMapper;
import org.sfm.map.FieldMapperErrorHandler;
import org.sfm.utils.ListHandler;
import org.sfm.utils.RowHandler;

public class JdbcMapperFactoryTest {

	JdbcMapperFactory asmFactory = JdbcMapperFactory.newInstance().useAsm(true);
	JdbcMapperFactory nonAsmFactory = JdbcMapperFactory.newInstance().useAsm(false);
	
	@Test
	public void testAsmDbObjectMappingFromDbWithMetaData()
			throws SQLException, Exception, ParseException {
		DbHelper.testDbObjectFromDb(new RowHandler<PreparedStatement>() {
			@Override
			public void handle(PreparedStatement ps) throws Exception {
				ResultSet rs = ps.executeQuery();
				JdbcMapper<DbObject> mapper = asmFactory.newMapper(DbObject.class, rs.getMetaData());
				assertMapPsDbObject(rs, mapper);
			}
		});
	}
	
	@Test
	public void testNonAsmDbObjectMappingFromDbWithMetaData()
			throws SQLException, Exception, ParseException {
		DbHelper.testDbObjectFromDb(new RowHandler<PreparedStatement>() {
			@Override
			public void handle(PreparedStatement ps) throws Exception {
				ResultSet rs = ps.executeQuery();
				JdbcMapper<DbObject> mapper = nonAsmFactory.newMapper(DbObject.class, rs.getMetaData());
				assertMapPsDbObject(rs, mapper);
			}
		});
	}
	
	@Test
	public void testAsmDbObjectMappingFromDbDynamic()
			throws SQLException, Exception, ParseException {
		DbHelper.testDbObjectFromDb(new RowHandler<PreparedStatement>() {
			@Override
			public void handle(PreparedStatement ps) throws Exception {
				JdbcMapper<DbObject> mapper = asmFactory.newMapper(DbObject.class);
				assertMapPsDbObject(ps.executeQuery(), mapper);
			}
		});
	}
	
	@Test
	public void testNonAsmDbObjectMappingFromDbDynamic()
			throws SQLException, Exception, ParseException {
		DbHelper.testDbObjectFromDb(new RowHandler<PreparedStatement>() {
			@Override
			public void handle(PreparedStatement ps) throws Exception {
				JdbcMapper<DbObject> mapper = nonAsmFactory.newMapper(DbObject.class);
				assertMapPsDbObject(ps.executeQuery(), mapper);
			}
		});
	}
	
	@Test
	public void testAsmFinalDbObjectMappingFromDbDynamic()
			throws SQLException, Exception, ParseException {
		DbHelper.testDbObjectFromDb(new RowHandler<PreparedStatement>() {
			@Override
			public void handle(PreparedStatement ps) throws Exception {
				JdbcMapper<DbFinalObject> mapper = asmFactory.newMapper(DbFinalObject.class);
				assertMapPsFinalDbObject(ps.executeQuery(), mapper);
			}
		});
	}
	
	@Test
	public void testNonAsmFinalDbObjectMappingFromDbDynamic()
			throws SQLException, Exception, ParseException {
		DbHelper.testDbObjectFromDb(new RowHandler<PreparedStatement>() {
			@Override
			public void handle(PreparedStatement ps) throws Exception {
				JdbcMapper<DbFinalObject> mapper = nonAsmFactory.newMapper(DbFinalObject.class);
				assertMapPsFinalDbObject(ps.executeQuery(), mapper);
			}
		});
	}	
	
	@Test
	public void testFieldErrorHandling()
			throws SQLException, Exception, ParseException {
		@SuppressWarnings("unchecked")
		FieldMapperErrorHandler<JdbcColumnKey> fieldMapperErrorHandler  = mock(FieldMapperErrorHandler.class);
		final Exception exception = new Exception("Error!");
		JdbcMapper<DbObject> mapper = JdbcMapperFactory.newInstance()
			.fieldMapperErrorHandler(fieldMapperErrorHandler)
			.addCustomFieldMapper("id",  new FieldMapper<ResultSet, DbObject>() {
				@Override
				public void map(ResultSet source, DbObject target) throws Exception {
					throw exception;
				}
			}).newBuilder(DbObject.class).addMapping("id").mapper();
		
		List<DbObject> list = mapper.forEach(new MockDbObjectResultSet(1), new ListHandler<DbObject>()).getList();
		assertNotNull(list.get(0));
		verify(fieldMapperErrorHandler).errorMappingField(eq(new JdbcColumnKey("id", 1)), any(), same(list.get(0)), same(exception));
	}
	
	
	@Test
	public void testFieldErrorHandlingOnResultSet()
			throws SQLException, Exception, ParseException {
		@SuppressWarnings("unchecked")
		FieldMapperErrorHandler<JdbcColumnKey> fieldMapperErrorHandler  = mock(FieldMapperErrorHandler.class);
		ResultSet rs = mock(ResultSet.class);
		
		final Exception exception = new SQLException("Error!");
		JdbcMapper<DbObject> mapper = JdbcMapperFactory.newInstance()
			.fieldMapperErrorHandler(fieldMapperErrorHandler)
			.newBuilder(DbObject.class).addMapping("id").mapper();
		
		when(rs.next()).thenReturn(true, false);
		when(rs.getLong(1)).thenThrow(exception);
		
		List<DbObject> list = mapper.forEach(rs, new ListHandler<DbObject>()).getList();
		assertNotNull(list.get(0));
		verify(fieldMapperErrorHandler).errorMappingField(eq(new JdbcColumnKey("id", 1)), any(), same(list.get(0)), same(exception));
	}
	
	private void assertMapPsDbObject(ResultSet rs,
			JdbcMapper<DbObject> mapper) throws Exception,
			ParseException {
		List<DbObject> list = mapper.forEach(rs, new ListHandler<DbObject>()).getList();
		assertEquals(1,  list.size());
		DbHelper.assertDbObjectMapping(list.get(0));
	}
	
	
	private void assertMapPsFinalDbObject(ResultSet rs,
			JdbcMapper<DbFinalObject> mapper) throws Exception,
			ParseException {
		List<DbFinalObject> list = mapper.forEach(rs, new ListHandler<DbFinalObject>()).getList();
		assertEquals(1,  list.size());
		DbHelper.assertDbObjectMapping(list.get(0));
	}
}
