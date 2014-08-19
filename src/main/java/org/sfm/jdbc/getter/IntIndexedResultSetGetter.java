package org.sfm.jdbc.getter;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sfm.reflect.Getter;
import org.sfm.reflect.primitive.IntGetter;

public final class IntIndexedResultSetGetter implements IntGetter<ResultSet>, Getter<ResultSet, Integer> {

	private final int column;
	
	public IntIndexedResultSetGetter(final int column) {
		this.column = column;
	}

	@Override
	public int getInt(final ResultSet target) throws SQLException {
		return target.getInt(column);
	}

	@Override
	public Integer get(final ResultSet target) throws Exception {
		final int i = getInt(target);
		if (target.wasNull()) {
			return null;
		} else {
			return Integer.valueOf(i);
		}
	}
}
