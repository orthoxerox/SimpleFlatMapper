package org.sfm.reflect.asm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.Test;
import org.sfm.beans.DbObject;
import org.sfm.jdbc.AbstractResultSetMapperBuilder;
import org.sfm.jdbc.DbHelper;
import org.sfm.jdbc.ResultSetMapperBuilderImpl;
import org.sfm.map.Mapper;
import org.sfm.reflect.InstantiatorFactory;
import org.sfm.reflect.SetterFactory;
import org.sfm.utils.Handler;

public class AsmMapperTest {

	@Test
	public void testMapperBuilder() throws Exception {
		AsmFactory factory = new AsmFactory();
		
		AbstractResultSetMapperBuilder<DbObject> builder = new ResultSetMapperBuilderImpl<>(DbObject.class, new SetterFactory());
		builder.addIndexedColumn("id");
		builder.addIndexedColumn("name");
		builder.addIndexedColumn("email");
		builder.addIndexedColumn("creation_time");
		builder.addIndexedColumn("type_ordinal");
		builder.addIndexedColumn("type_name");
		
		final Mapper<ResultSet, DbObject> mapper = 
				factory.createJdbcMapper(builder.fields(), 
						new InstantiatorFactory(factory).getInstantiator(ResultSet.class, DbObject.class),  
						DbObject.class);
		
		DbHelper.testDbObjectFromDb(new Handler<PreparedStatement>() {
			@Override
			public void handle(PreparedStatement ps) throws Exception {
				ResultSet rs = ps.executeQuery();
				rs.next();
				DbObject object = mapper.map(rs);
				DbHelper.assertDbObjectMapping(object);
			}
		});
	}

}
